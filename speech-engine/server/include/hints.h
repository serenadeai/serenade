#ifndef LIBRARY_HINTS_H
#define LIBRARY_HINTS_H

#include "fstext/fstext-lib.h"
#include "hmm/transition-model.h"
#include "phoneme_converter.h"
#include "tree/context-dep.h"

namespace speech_engine {

class HintGraphCreator {
 private:
  std::unordered_map<std::string, std::string> lexicon_;
  std::unordered_map<std::string, int32> phones_;
  PhonemeConverter &phoneme_converter_;
  const kaldi::TransitionModel &transition_model_;
  const kaldi::ContextDependency &context_dependency_;
  const std::vector<int32> disambig_syms_;
  int32 hint_start_;
  float hint_weight_;
  int32 nonterm_phones_offset_;
  int32 nonterminal_begin_;
  int32 nonterminal_end_;
  int32 nonterminal_hint_;
  std::vector<int32> left_context_phones_;

 public:
  HintGraphCreator(PhonemeConverter &phoneme_converter,
                   const kaldi::TransitionModel &transition_model,
                   const kaldi::ContextDependency &context_dependency,
                   const std::vector<int32> disambig_syms, int hint_start,
                   const float hint_weight, const int nonterm_phones_offset,
                   std::string lexicon_path, std::string phones_path,
                   std::string left_context_phones_path);
  std::shared_ptr<const fst::ConstFst<fst::StdArc>> Create(
      const std::vector<std::string> &hints) const;
};

std::shared_ptr<const fst::ConstFst<fst::StdArc>> ReadAsConstFst(
    std::string rxfilename);

}  // namespace speech_engine

#endif  // LIBRARY_HINTS_H
