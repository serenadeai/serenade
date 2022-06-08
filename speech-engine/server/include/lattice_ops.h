#ifndef LIBRARY_LATTICE_OPS_H
#define LIBRARY_LATTICE_OPS_H

#include "lat/kaldi-lattice.h"

namespace speech_engine {

std::optional<kaldi::Lattice> Determinize(kaldi::Lattice lat);

std::vector<kaldi::Lattice> Nbest(const kaldi::Lattice &lat);

}  // namespace speech_engine

#endif  // LIBRARY_LATTICE_OPS_H
