#include <include/PhonetisaurusScript.h>
#include <include/util.h>

#include <sstream>

#include "phoneme_converter.h"

std::vector<std::string> tokenize_utf8_string(std::string* utf8_string,
                                              std::string* delimiter) {
  char* str = (char*)utf8_string->c_str();  // utf-8 string
  char* str_i = str;                        // string iterator
  char* str_j = str;
  char* end = str + strlen(str) + 1;  // end iterator
  std::vector<std::string> string_vec;
  if (delimiter->compare("") != 0) string_vec.push_back("");

  do {
    str_j = str_i;
    utf8::uint32_t code = utf8::next(str_i, end);  // get 32 bit code
    if (code == 0) continue;
    int start = strlen(str) - strlen(str_j);
    int end = strlen(str) - strlen(str_i);
    int len = end - start;

    if (delimiter->compare("") == 0) {
      string_vec.push_back(utf8_string->substr(start, len));
    } else {
      if (delimiter->compare(utf8_string->substr(start, len)) == 0)
        string_vec.push_back("");
      else
        string_vec[string_vec.size() - 1] += utf8_string->substr(start, len);
    }
  } while (str_i < end);

  return string_vec;
}

std::vector<int> tokenize2ints(string* testword, string* sep,
                               const SymbolTable* syms) {
  std::vector<std::string> tokens = tokenize_utf8_string(testword, sep);
  std::vector<int> entry;
  for (unsigned int i = 0; i < tokens.size(); i++) {
    int label = syms->Find(tokens[i]);
    if (label != -1) {
      entry.push_back(label);
    }
  }

  return entry;
}

namespace speech_engine {

PhonemeConverter::PhonemeConverter(std::string model_path)
    : decoder_(new PhonetisaurusScript(model_path, "")) {}

PhonemeConverter::~PhonemeConverter() = default;

std::string PhonemeConverter::CharactersToPhonemes(std::string word) {
  PathData path_data = decoder_->Phoneticize(
      word, /* nbest */ 1, /* beam */ 3000, /* thresh */ 99.0,
      /* write_fsts */ false, /* accumulate */ false, /* pmass */ 0.0)[0];
  std::stringstream ss;
  for (int j = 0; j < path_data.Uniques.size(); j++) {
    ss << decoder_->osyms_->Find(path_data.Uniques[j]);
    if (j < path_data.Uniques.size() - 1) {
      ss << " ";
    }
  }
  return ss.str();
}

}  // namespace speech_engine
