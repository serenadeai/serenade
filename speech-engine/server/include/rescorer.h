#ifndef LIBRARY_RESCORER_H
#define LIBRARY_RESCORER_H

#include "base/kaldi-types.h"
#include "lat/compose-lattice-pruned.h"
#include "lat/kaldi-lattice.h"
#include "lm/const-arpa-lm.h"

namespace speech_engine {

class Rescorer {
 private:
  const kaldi::ConstArpaLm &const_arpa_;

 public:
  Rescorer(const kaldi::ConstArpaLm &const_arpa);

  std::optional<kaldi::Lattice> ScaleLanguageModelScore(
      kaldi::Lattice lat, kaldi::BaseFloat lm_scale);
};

}  // namespace speech_engine

#endif  // LIBRARY_RESCORER_H
