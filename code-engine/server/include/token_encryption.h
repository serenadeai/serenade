#pragma once

#include <sentencepiece_processor.h>

#include <list>
#include <map>
#include <memory>
#include <string>
#include <vector>

namespace code_engine {

class TokenIdConverter {
 public:
  TokenIdConverter(const std::string &vocab_filename);
  TokenIdConverter(const std::string &vocab_filename,
                   const std::string &spm_filename);

 private:
  bool is_sentencepiece_;
  std::map<std::string, std::string> token_to_id_;
  std::map<std::string, std::string> id_to_token_;
  std::unique_ptr<sentencepiece::SentencePieceProcessor> sp_processor_;

 public:
  std::string Encode(std::string input);
  std::string Decode(std::string input);

  std::vector<std::string> Encode(std::vector<std::string> inputs);
  std::vector<std::string> Decode(std::vector<std::string> inputs);

 private:
  void LoadTokenMaps(const std::string &vocab_filename);
};

}  // namespace code_engine
