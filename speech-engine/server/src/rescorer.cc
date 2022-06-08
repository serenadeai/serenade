#include "lat/kaldi-lattice.h"
#include "lat/lattice-functions.h"
#include "lattice_ops.h"
#include "rescorer.h"

using namespace kaldi;

namespace speech_engine {

Rescorer::Rescorer(const ConstArpaLm &const_arpa) : const_arpa_(const_arpa) {}

std::optional<Lattice> Rescorer::ScaleLanguageModelScore(Lattice lat,
                                                         BaseFloat lm_scale) {
  if (lm_scale == 0.0) {
    return lat;
  }

  CompactLattice clat;
  ConvertLattice(lat, &clat);
  fst::ScaleLattice(fst::GraphLatticeScale(1.0 / lm_scale), &clat);

  // Wraps the ConstArpaLm format language model into FST. We re-create it
  // for each lattice to prevent memory usage increasing with time.
  ConstArpaLmDeterministicFst const_arpa_fst(const_arpa_);

  // Composes lattice with language model.
  CompactLattice composed_clat;
  ArcSort(&clat, fst::OLabelCompare<CompactLatticeArc>());
  ComposeCompactLatticeDeterministic(clat, &const_arpa_fst, &composed_clat);

  Lattice composed_lat;
  ConvertLattice(composed_clat, &composed_lat);
  if (auto ret = Determinize(composed_lat)) {
    fst::ScaleLattice(fst::GraphLatticeScale(lm_scale), &(*ret));
    return ret;
  }
  return std::nullopt;
}

}  // namespace speech_engine
