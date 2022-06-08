#include "text_nbest_input.h"
#include "common/utils.h"

namespace marian {
namespace data {

int numFromNbest(const std::string& line) {
  auto fields = utils::split(line, " ||| ", true);
  ABORT_IF(fields.size() < 4,
           "Too few fields ({}) in line \"{}\", is this a correct n-best list?",
           fields.size(),
           line);
  return std::stoi(fields[0]);
}

std::string lineFromNbest(const std::string& line) {
  auto fields = utils::split(line, " ||| ", true);
  ABORT_IF(fields.size() < 4,
           "Too few fields ({}) in line \"{}\", is this a correct n-best list?",
           fields.size(),
           line);
  return fields[1];
}

TextNBestIterator::TextNBestIterator() : pos_(-1), tup_(0) {}
TextNBestIterator::TextNBestIterator(TextNBestInput& corpus) : corpus_(&corpus), pos_(0), tup_(corpus_->next()) {}

void TextNBestIterator::increment() {
  tup_ = corpus_->next();
  pos_++;
}

bool TextNBestIterator::equal(TextNBestIterator const& other) const {
  return this->pos_ == other.pos_ || (this->tup_.empty() && other.tup_.empty());
}

const SentenceTuple& TextNBestIterator::dereference() const {
  return tup_;
}

TextNBestInput::TextNBestInput(std::vector<std::string> inputs,
                               std::vector<Ptr<Vocab>> vocabs,
                               Ptr<Options> options)
    : DatasetBase(inputs, options),
      vocabs_(vocabs),
      maxLength_(options_->get<size_t>("max-length")),
      maxLengthCrop_(options_->get<bool>("max-length-crop")) {
  // Note: inputs are automatically stored in the inherited variable named paths_, but these are
  // texts not paths!
  for(const auto& text : paths_)
    files_.emplace_back(new std::istringstream(text));
}

void TextNBestInput::addWordsToSentenceTuple(const std::string& line,
                                         size_t batchIndex,
                                         SentenceTuple& tup) const {
  // This turns a string in to a sequence of numerical word ids. Depending
  // on the vocabulary type, this can be non-trivial, e.g. when SentencePiece
  // is used.
  Words words = vocabs_[batchIndex]->encode(line, /*addEOS =*/ true, inference_);

  ABORT_IF(words.empty(), "Empty input sequences are presently untested");

  if(maxLengthCrop_ && words.size() > maxLength_) {
    words.resize(maxLength_);
    if(true)
      words.back() = vocabs_[batchIndex]->getEosId();
  }

  tup.push_back(words);
}

SentenceTuple TextNBestInput::next() {
  bool cont = true;
  while(cont) {
    // get index of the current sentence
    size_t curId = pos_;
    pos_++;

    // fill up the sentence tuple with sentences from all input files
    SentenceTuple tup(curId);

    std::string line;
    lastLines_.resize(files_.size() - 1);
    size_t last = files_.size() - 1;

    if(io::getline(*files_[last], line)) {
      int curr_num = numFromNbest(line);
      std::string curr_text = lineFromNbest(line);

      for(size_t i = 0; i < last; ++i) {
        if(curr_num > lastNum_) {
          ABORT_IF(!std::getline(*files_[i], lastLines_[i]),
                   "Too few lines in input {}",
                   i);
        }
        addWordsToSentenceTuple(lastLines_[i], i, tup);
      }
      addWordsToSentenceTuple(curr_text, last, tup);
      lastNum_ = curr_num;
    }

    // continue only if each input file provides an example
    size_t expectedSize = files_.size();

    cont = tup.size() == expectedSize;

    // continue if all sentences are no longer than maximum allowed length
    if(cont && std::all_of(tup.begin(), tup.end(), [=](const Words& words) {
         return words.size() > 0 && words.size() <= maxLength_;
       }))
      return tup;
  }
  return SentenceTuple(0);
}

}  // namespace data
}  // namespace marian
