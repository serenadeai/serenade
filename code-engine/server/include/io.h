#ifndef LIBRARY_IO_H
#define LIBRARY_IO_H

#include <memory>

namespace code_engine {

std::unique_ptr<std::istream> FileStream(const std::string& filename);

}  // namespace code_engine

#endif  // LIBRARY_IO_H
