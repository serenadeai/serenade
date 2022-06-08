#ifndef LIBRARY_RECOGNIZER_H
#define LIBRARY_RECOGNIZER_H

#include <functional>
#include <optional>

#include "base/kaldi-types.h"
#include "decoder/grammar-fst.h"
#include "online2/online-ivector-feature.h"
#include "online2/online-nnet2-feature-pipeline.h"
#include "online2/online-nnet3-decoding.h"
#include "recognizer.h"
#include "recognizer_config.h"

namespace speech_engine {

class Recognizer {
 private:
  const int32 kModelFrequency = 16000;

  // for audio lattice.
  RecognizerConfig config_;
  kaldi::OnlineIvectorExtractorAdaptationState adaptation_state_;
  std::unique_ptr<kaldi::OnlineNnet2FeaturePipeline> feature_pipeline_;
  std::unique_ptr<kaldi::OnlineSilenceWeighting> silence_weighting_;
  std::unique_ptr<kaldi::SingleUtteranceNnet3DecoderTpl<fst::ConstGrammarFst>>
      decoder_;
  std::unique_ptr<fst::ConstGrammarFst> decode_fst_;
  std::vector<
      std::pair<int32, std::shared_ptr<const fst::ConstFst<fst::StdArc>>>>
      hint_fsts_;
  std::vector<kaldi::BaseFloat> current_chunk_;
  int32 audio_size_ = 0;

  void Accept();
  void UpdateIvectorAndAdvance();
  void Rollback();
  std::optional<kaldi::Lattice> GetLattice(bool final);

 public:
  Recognizer(const RecognizerConfig &config);
  void Init(std::vector<
            std::pair<int32, std::shared_ptr<const fst::ConstFst<fst::StdArc>>>>
                hints_fsts);
  void ProcessAudio(const std::vector<kaldi::BaseFloat> &data);
  void GetResponse(
      std::function<void(std::optional<kaldi::Lattice>)> handle_response);
};

}  // namespace speech_engine

#endif  // LIBRARY_RECOGNIZER_H
