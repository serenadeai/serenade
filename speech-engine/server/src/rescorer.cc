#include "rescorer.h"

#include "lat/kaldi-lattice.h"
#include "lat/lattice-functions.h"
#include "lattice_ops.h"

using namespace kaldi;

namespace speech_engine {

class ConstArpaFstWithHints
    : public fst::DeterministicOnDemandFst<fst::StdArc> {
 public:
  ConstArpaFstWithHints(const kaldi::ConstArpaLm& lm, int32 hint_word_start,
                        int32 hint_placeholder, float hint_weight)
      : inner_(lm),
        hint_word_start_(hint_word_start),
        hint_placeholder_(hint_placeholder),
        hint_weight_(hint_weight) {}

  fst::StdArc::StateId Start() { return inner_.Start(); }

  fst::StdArc::Weight Final(StateId s) { return inner_.Final(s); }

  bool GetArc(fst::StdArc::StateId s, fst::StdArc::Label ilabel,
              fst::StdArc* oarc) {
    if (ilabel >= hint_word_start_) {
      bool result = inner_.GetArc(s, hint_placeholder_, oarc);
      if (result) {
        oarc->weight = fst::StdArc::Weight(oarc->weight.Value() + hint_weight_);
      }
      return result;
    }
    return inner_.GetArc(s, ilabel, oarc);
  }

 private:
  kaldi::ConstArpaLmDeterministicFst inner_;
  int32 hint_word_start_;
  int32 hint_placeholder_;
  float hint_weight_;
};

Rescorer::Rescorer(const ConstArpaLm& const_arpa, int32 hint_word_start,
                   int32 hint_placeholder, float hint_weight)
    : const_arpa_(const_arpa),
      hint_word_start_(hint_word_start),
      hint_placeholder_(hint_placeholder),
      hint_weight_(hint_weight) {}

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
  ConstArpaFstWithHints const_arpa_fst(const_arpa_, hint_word_start_,
                                       hint_placeholder_, hint_weight_);

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
