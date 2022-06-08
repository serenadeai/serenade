#include <boost/algorithm/string.hpp>
#include <iostream>
#include <iterator>
#include <memory>
#include <vector>

#include "io.h"
#include "token_encryption.h"

namespace code_engine {

void TokenIdConverter::LoadTokenMaps(const std::string &vocab_filename) {
  std::unique_ptr<std::istream> strm = FileStream(vocab_filename);

  std::string line;
  int token_index = 2;  // Starts at 2 because 0/1 reserved for marian.
  while (std::getline(*strm, line)) {
    token_to_id_.emplace(line, std::to_string(token_index));
    id_to_token_.emplace(std::to_string(token_index), line);
    token_index++;
  }
}

TokenIdConverter::TokenIdConverter(const std::string &vocab_filename) {
  is_sentencepiece_ = false;
  LoadTokenMaps(vocab_filename);
}

TokenIdConverter::TokenIdConverter(const std::string &vocab_filename,
                                   const std::string &spm_filename) {
  is_sentencepiece_ = true;
  LoadTokenMaps(vocab_filename);
  std::unique_ptr<std::istream> strm = FileStream(spm_filename);

  sp_processor_ = std::make_unique<sentencepiece::SentencePieceProcessor>();
  std::string decrypted_proto(std::istreambuf_iterator<char>(*strm), {});
  absl::string_view decrypted_proto_view(decrypted_proto);

  sp_processor_->LoadFromSerializedProto(decrypted_proto_view);
}

std::string TokenIdConverter::Encode(std::string input) {
  if (is_sentencepiece_) {
    absl::string_view input_view(input);
    std::vector<int> ids;

    sp_processor_->Encode(input_view, &ids);
    std::string result("");
    for (int id : ids) {
      result = result += std::string(" ") + std::to_string(id);
    }
    return result;
  } else {
    std::vector<std::string> tokens;
    boost::split(tokens, input, [](char c) { return c == ' '; });
    std::string result("");
    for (std::string token : tokens) {
      auto id_num = token_to_id_.find(token);
      if (id_num != token_to_id_.end()) {
        result = result += std::string(" ") + id_num->second;
      } else {
        result = result += std::string(" 1");  // Append <unk> value.
      }
    }
    return result;
  }
}

std::string TokenIdConverter::Decode(std::string input) {
  std::vector<std::string> tokens;
  boost::split(tokens, input, [](char c) { return c == ' '; });
  std::string result("");
  for (std::string token : tokens) {
    auto id_num = id_to_token_.find(token);
    if (id_num != id_to_token_.end()) {
      result = result += std::string(" ") + id_num->second;
    } else {
      // The case where the input id doesn't exist in the token list should
      // typically never happen.
      result = result += std::string(" ") + "<UNK>";
    }
  }
  return result;
}

std::vector<std::string> TokenIdConverter::Encode(
    std::vector<std::string> inputs) {
  std::vector<std::string> result;
  for (std::string s : inputs) {
    result.push_back(Encode(s));
  }
  return result;
}

std::vector<std::string> TokenIdConverter::Decode(
    std::vector<std::string> inputs) {
  std::vector<std::string> result;
  for (std::string s : inputs) {
    result.push_back(Decode(s));
  }
  return result;
}

}  // namespace code_engine
