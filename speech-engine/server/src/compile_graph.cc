
#include "base/kaldi-common.h"
#include "decoder/grammar-fst.h"
#include "fst/arcsort.h"
#include "fstext/fstext-lib.h"
#include "fstext/grammar-context-fst.h"
#include "fstext/push-special.h"
#include "hmm/hmm-utils.h"
#include "hmm/transition-model.h"
#include "tree/context-dep.h"
#include "util/common-utils.h"

namespace speech_engine {
using namespace kaldi;
typedef kaldi::int32 int32;
using fst::StdArc;
using fst::SymbolTable;
using fst::VectorFst;

std::shared_ptr<const fst::ConstFst<fst::StdArc>> CompileGraph(
    const kaldi::TransitionModel& trans_model, const ContextDependency& ctx_dep,
    const VectorFst<StdArc>& lexicon_fst, const VectorFst<StdArc>& grammar_fst,
    const std::vector<int32> disambig_syms, int nonterm_phones_offset) {
  BaseFloat transition_scale = 1.0;
  BaseFloat self_loop_scale = 0.1;

  const std::vector<int32>& phone_syms = trans_model.GetPhones();
  for (int32 i = 0; i < disambig_syms.size(); i++)
    if (std::binary_search(phone_syms.begin(), phone_syms.end(),
                           disambig_syms[i]))
      KALDI_ERR << "Disambiguation symbol " << disambig_syms[i]
                << " is also a phone.";
  VectorFst<StdArc> lg_fst;

  TableCompose(lexicon_fst, grammar_fst, &lg_fst);
  DeterminizeStarInLog(&lg_fst, fst::kDelta);
  MinimizeEncoded(&lg_fst, fst::kDelta);
  fst::PushSpecial(&lg_fst, fst::kDelta);

  VectorFst<StdArc> clg_fst;
  std::vector<std::vector<int32>> ilabels;
  ComposeContextLeftBiphone(nonterm_phones_offset, disambig_syms, lg_fst,
                            &clg_fst, &ilabels);
  lg_fst.DeleteStates();

  HTransducerConfig h_cfg;
  h_cfg.transition_scale = transition_scale;
  h_cfg.nonterm_phones_offset = nonterm_phones_offset;
  std::vector<int32> disambig_syms_h;  // disambiguation symbols on
                                       // input side of H.
  VectorFst<StdArc>* h_fst =
      GetHTransducer(ilabels, ctx_dep, trans_model, h_cfg, &disambig_syms_h);
  VectorFst<StdArc> hclg_fst;  // transition-id to word.
  TableCompose(*h_fst, clg_fst, &hclg_fst);
  clg_fst.DeleteStates();
  delete h_fst;

  KALDI_ASSERT(hclg_fst.Start() != fst::kNoStateId);

  // Epsilon-removal and determinization combined. This will fail if not
  // determinizable.
  DeterminizeStarInLog(&hclg_fst);

  if (!disambig_syms_h.empty()) {
    RemoveSomeInputSymbols(disambig_syms_h, &hclg_fst);
    RemoveEpsLocal(&hclg_fst);
  }

  // Encoded minimization.
  MinimizeEncoded(&hclg_fst);

  std::vector<int32> disambig;
  bool check_no_self_loops = true, reorder = true;
  AddSelfLoops(trans_model, disambig, self_loop_scale, reorder,
               check_no_self_loops, &hclg_fst);

  if (nonterm_phones_offset >= 0) {
    PrepareForGrammarFst(nonterm_phones_offset, &hclg_fst);
  }

  return std::make_shared<const fst::ConstFst<fst::StdArc>>(hclg_fst);
}
}  // namespace speech_engine
