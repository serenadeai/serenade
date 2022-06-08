#include <fstream>
#include <memory>
#include <sstream>

#include "io.h"

namespace code_engine {

std::unique_ptr<std::istream> FileStream(const std::string &filename) {
  std::ifstream ifs(filename);
  std::string content((std::istreambuf_iterator<char>(ifs)),
                      (std::istreambuf_iterator<char>()));
  std::unique_ptr<std::istream> is =
      std::make_unique<std::istringstream>(content);
  return is;
}

}  // namespace code_engine
