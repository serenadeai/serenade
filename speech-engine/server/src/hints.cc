#include "hints.h"

#include <algorithm>
#include <boost/uuid/uuid.hpp>
#include <boost/uuid/uuid_generators.hpp>
#include <boost/uuid/uuid_io.hpp>
#include <map>

#include "base/timer.h"
#include "compile_graph.h"
#include "decoder/grammar-fst.h"
#include "fst/symbol-table.h"
#include "fstext/kaldi-fst-io.h"
#include "hmm/hmm-utils.h"
#include "io.h"
#include "util/simple-io-funcs.h"

namespace speech_engine {

std::string AddPositionPostfixes(const std::string &s) {
  std::istringstream iss(s);
  std::vector<std::string> pieces{std::istream_iterator<std::string>(iss), {}};
  std::string word = pieces[0];
  std::string pronunciation = "";
  for (int i = 0; i < pieces.size(); i++) {
    pronunciation += pieces[i];
    if (i == 0) {
      if (i == pieces.size() - 1) {
        pronunciation += "_S";
        break;
      } else {
        pronunciation += "_B";
      }
    } else if (i == pieces.size() - 1) {
      pronunciation += "_E";
      break;
    } else {
      pronunciation += "_I";
    }
    pronunciation += " ";
  }
  return pronunciation;
}

HintGraphCreator::HintGraphCreator(
    PhonemeConverter &phoneme_converter,
    const kaldi::TransitionModel &transition_model,
    const kaldi::ContextDependency &context_dependency,
    const std::vector<int32> disambig_syms, const int hint_start,
    const float hint_weight, const int nonterm_phones_offset,
    std::string lexicon_path, std::string phones_path,
    std::string left_context_phones_path)
    : phoneme_converter_(phoneme_converter),
      transition_model_(transition_model),
      context_dependency_(context_dependency),
      disambig_syms_(disambig_syms),
      hint_start_(hint_start),
      hint_weight_(hint_weight),
      nonterm_phones_offset_(nonterm_phones_offset),
      nonterminal_begin_(hint_start - 3),
      nonterminal_end_(hint_start - 2),
      nonterminal_hint_(hint_start - 1) {
  bool binary;
  std::unique_ptr<std::istream> is = KaldiFileStream(lexicon_path, &binary);
  std::string line;
  while (std::getline(*is, line)) {
    for (int i = 0; i < line.size(); i++) {
      if (std::isspace(line[i])) {
        lexicon_[line.substr(0, i)] = AddPositionPostfixes(line.substr(i + 1));
        break;
      }
    }
  }

  std::unique_ptr<std::istream> left_phones_stream =
      KaldiFileStream(left_context_phones_path, &binary);
  int32 phone;
  while (!left_phones_stream->eof()) {
    *left_phones_stream >> phone;
    left_context_phones_.push_back(phone);
  }

  std::unique_ptr<std::istream> phones_stream =
      KaldiFileStream(phones_path, &binary);
  std::string phone_name;
  while (!phones_stream->eof()) {
    *phones_stream >> phone_name;
    *phones_stream >> phone;
    phones_[phone_name] = phone;
  }
}

int DisambiguateLexicon(
    const std::map<std::string, std::string> &lexicon,
    std::map<std::string, std::string> &disambiguated_lexicon) {
  std::set<std::string> duplicated;
  std::vector<std::string> pronunciations;
  for (std::pair<std::string, std::string> entry : lexicon) {
    pronunciations.push_back(entry.second);
  }
  std::sort(pronunciations.begin(), pronunciations.end());
  for (int i = 0; i < pronunciations.size() - 1; i++) {
    if (pronunciations[i + 1].rfind(pronunciations[i], 0) == 0) {
      duplicated.insert(pronunciations[i]);
    }
  }

  int max = 0;
  std::map<std::string, int> pronunciation_counts;
  for (std::pair<std::string, std::string> entry : lexicon) {
    if (duplicated.find(entry.second) == duplicated.end()) {
      disambiguated_lexicon[entry.first] = entry.second;
      continue;
    }
    if (pronunciation_counts.find(entry.second) == pronunciation_counts.end()) {
      pronunciation_counts[entry.second] = 0;
    }
    pronunciation_counts[entry.second]++;  // start at 1.
    disambiguated_lexicon[entry.first] =
        entry.second + " #" +
        std::to_string(pronunciation_counts[entry.second]);
    max = std::max(pronunciation_counts[entry.second], max);
  }

  return max;
}

std::shared_ptr<const fst::ConstFst<fst::StdArc>> HintGraphCreator::Create(
    const std::vector<std::string> &hints) const {
  if (hints.size() == 0) {
    fst::VectorFst<fst::StdArc> fst;
    return std::make_shared<const fst::ConstFst<fst::StdArc>>(fst);
  }

  std::map<std::string, std::string> lexicon;
  std::set<std::string> skipped;
  for (std::string hint_word : hints) {
    std::string pronunciation;
    if (lexicon_.find(hint_word) != lexicon_.end()) {
      pronunciation = lexicon_.at(hint_word);
    } else {
      pronunciation = phoneme_converter_.CharactersToPhonemes(hint_word);
      if (pronunciation == "") {
        skipped.insert(hint_word);
        continue;
      }
      pronunciation = AddPositionPostfixes(pronunciation);
    }
    lexicon[hint_word] = pronunciation;
  }

  if (skipped.size() == hints.size()) {
    fst::VectorFst<fst::StdArc> fst;
    return std::make_shared<const fst::ConstFst<fst::StdArc>>(fst);
  }

  std::map<std::string, std::string> disambiguated_lexicon;
  int num_disambig = DisambiguateLexicon(lexicon, disambiguated_lexicon) + 1;

  float sil_cost = -log(0.5);
  float no_sil_cost = -log(1.0 - 0.5);

  fst::VectorFst<fst::StdArc> lexicon_fst;
  int32 start_state = lexicon_fst.AddState();
  int32 loop_state = lexicon_fst.AddState();

  int32 sil_state = lexicon_fst.AddState();
  int32 sil_disambig_state = lexicon_fst.AddState();
  int32 sil_disambig = phones_.at("#" + std::to_string(num_disambig));

  lexicon_fst.AddArc(start_state, fst::StdArc(/* <eps> */ 0, /* <eps> */ 0,
                                              no_sil_cost, loop_state));
  lexicon_fst.AddArc(start_state, fst::StdArc(/* <eps> */ 0, /* <eps> */ 0,
                                              sil_cost, sil_state));
  lexicon_fst.AddArc(sil_state, fst::StdArc(phones_.at("SIL"), /* <eps> */ 0,
                                            0.0, sil_disambig_state));
  lexicon_fst.AddArc(sil_disambig_state,
                     fst::StdArc(sil_disambig, /* <eps> */ 0, 0.0, loop_state));

  for (int i = 0; i < hints.size(); i++) {
    if (skipped.find(hints[i]) != skipped.end()) {
      continue;
    }
    std::vector<int32> hint_phones;
    std::string pronunciation = disambiguated_lexicon[hints[i]];
    std::stringstream pronunciation_stream(pronunciation);
    std::string phone_name;
    while (std::getline(pronunciation_stream, phone_name, ' ')) {
      hint_phones.push_back(phones_.at(phone_name));
    }

    float cost = -log(0.983053);
    int32 current_state = loop_state;
    for (int j = 0; j < hint_phones.size() - 1; j++) {
      int32 next_state = lexicon_fst.AddState();
      lexicon_fst.AddArc(
          current_state,
          fst::StdArc(hint_phones[j], j == 0 ? hint_start_ + i : 0,
                      j == 0 ? cost : 0.0, next_state));
      current_state = next_state;
    }
    lexicon_fst.AddArc(
        current_state,
        fst::StdArc(hint_phones[hint_phones.size() - 1],
                    hint_phones.size() == 1 ? hint_start_ + i : 0,
                    hint_phones.size() == 1 ? cost : 0.0, loop_state));
    lexicon_fst.AddArc(
        current_state,
        fst::StdArc(hint_phones[hint_phones.size() - 1],
                    hint_phones.size() == 1 ? hint_start_ + i : 0,
                    hint_phones.size() == 1 ? cost : 0.0, sil_state));
  }
  lexicon_fst.SetStart(start_state);
  lexicon_fst.SetFinal(loop_state, 0.0);

  int32 shared_state = lexicon_fst.AddState();
  int32 final_state = lexicon_fst.AddState();
  lexicon_fst.AddArc(
      start_state, fst::StdArc(phones_.at("#nonterm_begin"), nonterminal_begin_,
                               0.0, shared_state));
  lexicon_fst.AddArc(loop_state,
                     fst::StdArc(phones_.at("#nonterm:hint"), nonterminal_hint_,
                                 0.0, shared_state));
  float cost = -log(1.0 / left_context_phones_.size());
  for (int32 left_context_phone : left_context_phones_) {
    lexicon_fst.AddArc(shared_state,
                       fst::StdArc(left_context_phone, 0, cost, loop_state));
  }
  lexicon_fst.AddArc(
      loop_state, fst::StdArc(phones_.at("#nonterm_end"), nonterminal_end_, 0.0,
                              final_state));
  lexicon_fst.SetFinal(final_state, 0.0);

  // Apparently we only want to add self loops for the first disambiguation
  // symbol.
  std::vector<int32> disambig_syms;
  disambig_syms.push_back(disambig_syms_[0]);
  AddSelfLoops(&lexicon_fst, disambig_syms, disambig_syms);
  fst::ArcSort(&lexicon_fst, fst::OLabelCompare<fst::StdArc>());

  fst::VectorFst<fst::StdArc> grammar_fst;
  grammar_fst.AddState();
  grammar_fst.AddState();
  grammar_fst.AddState();
  grammar_fst.AddState();
  grammar_fst.AddArc(0, fst::StdArc(nonterminal_begin_, /* <eps> */ 0, 0.0, 1));
  grammar_fst.AddArc(2, fst::StdArc(nonterminal_end_, /* <eps> */ 0, 0.0, 3));
  for (int i = 0; i < hints.size(); i++) {
    if (skipped.find(hints[i]) == skipped.end()) {
      grammar_fst.AddArc(
          1, fst::StdArc(i + hint_start_, i + hint_start_, hint_weight_, 2));
    }
  }
  fst::ArcSort(&grammar_fst, fst::ILabelCompare<fst::StdArc>());
  grammar_fst.SetStart(0);
  grammar_fst.SetFinal(3, 0.0);

  std::shared_ptr<const fst::ConstFst<fst::StdArc>> ret(
      CompileGraph(transition_model_, context_dependency_, lexicon_fst,
                   grammar_fst, disambig_syms_, nonterm_phones_offset_));

  return ret;
}

}  // namespace speech_engine
