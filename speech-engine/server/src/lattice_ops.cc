#include "lattice_ops.h"

using namespace kaldi;

namespace speech_engine {

std::optional<Lattice> Determinize(Lattice lat) {
  Invert(&lat);
  CompactLattice determinized_lat;
  fst::DeterminizeLatticeOptions lat_opts;
  lat_opts.max_mem = 15000000;  // 15 mb
  lat_opts.max_loop = 500000;
  lat_opts.delta = fst::kDelta;

  if (!DeterminizeLattice(lat, &determinized_lat, lat_opts, nullptr)) {
    return std::nullopt;
  }
  Lattice out;
  ConvertLattice(determinized_lat, &out);
  return std::optional<Lattice>(out);
}

std::vector<Lattice> Nbest(const Lattice &lat) {
  Lattice nbest_lat;
  fst::ShortestPath(lat, &nbest_lat, 32);
  std::vector<Lattice> nbest_lats;
  fst::ConvertNbestToVector(nbest_lat, &nbest_lats);
  return nbest_lats;
}

}  // namespace speech_engine
