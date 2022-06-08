#ifndef LIBRARY_IO_H
#define LIBRARY_IO_H

#include <fst/fstlib.h>

#include <memory>

#include "base/io-funcs.h"

namespace speech_engine {

std::unique_ptr<std::istream> KaldiFileStream(const std::string &filename,
                                              bool *binary);

void ReadIntegerVectorFile(const std::string &filename,
                           std::vector<int32> *list);

int32 ReadIntegerFile(const std::string &filename);

template <class C>
void ReadKaldiObjectFile(const std::string &filename, C *c) {
  bool binary;
  std::unique_ptr<std::istream> is = KaldiFileStream(filename, &binary);
  c->Read(*is, binary);
}

std::shared_ptr<const fst::ConstFst<fst::StdArc>> ReadConstFstFile(
    std::string filename);

fst::Fst<fst::StdArc> *ReadFstKaldiGenericFile(std::string filename);

}  // namespace speech_engine

#endif  // LIBRARY_IO_H
