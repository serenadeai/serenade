
#include "base/kaldi-common.h"
#include "decoder/grammar-fst.h"
#include "fstext/fstext-lib.h"
#include "fstext/grammar-context-fst.h"
#include "fstext/push-special.h"
#include "hmm/hmm-utils.h"
#include "hmm/transition-model.h"
#include "tree/context-dep.h"
#include "util/common-utils.h"

namespace speech_engine {

std::shared_ptr<const fst::ConstFst<fst::StdArc>> CompileGraph(
    const kaldi::TransitionModel& trans_model,
    const kaldi::ContextDependency& context_dependency,
    const fst::VectorFst<fst::StdArc>& lexicon_fst,
    const fst::VectorFst<fst::StdArc>& grammar_fst,
    const std::vector<int32> disambig_syms, int nonterm_phones_offset);

}
