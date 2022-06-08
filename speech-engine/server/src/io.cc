#include "io.h"

using namespace fst;
using namespace kaldi;

namespace speech_engine {

std::unique_ptr<std::istream> KaldiFileStream(const std::string &filename,
                                              bool *binary) {
  std::ifstream ifs(filename);
  std::string content((std::istreambuf_iterator<char>(ifs)),
                      (std::istreambuf_iterator<char>()));
  std::unique_ptr<std::istream> is =
      std::make_unique<std::istringstream>(content);
  kaldi::InitKaldiInputStream(*is, binary);
  return is;
}

void ReadIntegerVectorFile(const std::string &filename,
                           std::vector<int32> *list) {
  std::ifstream ifs(filename);
  std::string content((std::istreambuf_iterator<char>(ifs)),
                      (std::istreambuf_iterator<char>()));
  std::istringstream is = std::istringstream(content);
  int32 i;
  list->clear();
  while (!(is >> i).fail()) list->push_back(i);
  is >> std::ws;
}

int32 ReadIntegerFile(const std::string &filename) {
  std::vector<int32> list;
  ReadIntegerVectorFile(filename, &list);
  return list[0];
}

std::shared_ptr<const fst::ConstFst<fst::StdArc>> ReadConstFstFile(
    std::string filename) {
  fst::Fst<fst::StdArc> *fst = ReadFstKaldiGenericFile(filename);
  fst::ConstFst<fst::StdArc> *const_fst =
      dynamic_cast<fst::ConstFst<fst::StdArc> *>(fst);
  if (!const_fst) {
    const_fst = new fst::ConstFst<fst::StdArc>(*fst);
    delete fst;
  }
  return std::shared_ptr<const fst::ConstFst<fst::StdArc>>(const_fst);
}

Fst<StdArc> *ReadFstKaldiGenericFile(std::string filename) {
  std::ifstream ifs(filename);
  std::string content((std::istreambuf_iterator<char>(ifs)),
                      (std::istreambuf_iterator<char>()));
  std::istringstream is = std::istringstream(content);

  fst::FstHeader hdr;
  // Read FstHeader which contains the type of FST
  if (!hdr.Read(is, filename)) {
    KALDI_ERR << "Reading FST: error reading FST header from " << filename;
  }
  // Check the type of Arc
  if (hdr.ArcType() != fst::StdArc::Type()) {
    KALDI_ERR << "FST with arc type " << hdr.ArcType() << " is not supported.";
  }
  // Read the FST
  FstReadOptions ropts("<unspecified>", &hdr);
  Fst<StdArc> *fst = Fst<StdArc>::Read(is, ropts);
  if (!fst) {
    KALDI_ERR << "Could not read fst from " << filename;
  }
  return fst;
}

}  // namespace speech_engine
