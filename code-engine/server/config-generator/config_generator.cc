#include <iostream>

#include "common/timer.h"
#include "common/utils.h"
#include "marian.h"
#include "translator/beam_search.h"
#include "translator/translator.h"

int main(int argc, char *argv[]) {
  auto options =
      marian::parseOptions(argc, argv, marian::cli::mode::server, true);
  std::cout << options->asYamlString() << std::endl;
}  // main()
