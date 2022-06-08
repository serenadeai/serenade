#ifndef LIBRARY_PHONEME_CONVERTER_H
#define LIBRARY_PHONEME_CONVERTER_H

#include <sys/stat.h>
#include <sys/types.h>

#include <memory>
#include <string>
#include <vector>

class PhonetisaurusScript;

namespace speech_engine {

class PhonemeConverter {
 private:
  std::unique_ptr<PhonetisaurusScript> decoder_;

 public:
  PhonemeConverter(std::string model_path);
  ~PhonemeConverter();
  std::string CharactersToPhonemes(std::string word);
};

}  // namespace speech_engine

#endif  // LIBRARY_PHONEME_CONVERTER_H
