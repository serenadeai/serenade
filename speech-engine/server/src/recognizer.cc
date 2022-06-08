#include "base/kaldi-common.h"
#include "decoder/grammar-fst.h"
#include "fst/fstlib.h"
#include "fstext/kaldi-fst-io.h"
#include "fstext/table-matcher.h"
#include "recognizer.h"
#include "util/common-utils.h"

using namespace kaldi;

namespace speech_engine {

Recognizer::Recognizer(const RecognizerConfig &config)
    : config_(config),
      adaptation_state_(config_.feature_info.ivector_extractor_info){};

void Recognizer::Init(
    std::vector<
        std::pair<int32, std::shared_ptr<const fst::ConstFst<fst::StdArc>>>>
        hint_fsts) {
  hint_fsts_ = hint_fsts;
  feature_pipeline_ =
      std::make_unique<kaldi::OnlineNnet2FeaturePipeline>(config_.feature_info);

  silence_weighting_ = std::make_unique<kaldi::OnlineSilenceWeighting>(
      config_.transition_model, config_.feature_info.silence_weighting_config,
      config_.decodable_opts.frame_subsampling_factor);

  decode_fst_ = std::make_unique<fst::ConstGrammarFst>(
      config_.nonterm_phones_offset, config_.decode_fst, hint_fsts_);

  decoder_ = std::make_unique<
      kaldi::SingleUtteranceNnet3DecoderTpl<fst::ConstGrammarFst>>(
      config_.decoder_opts, config_.transition_model, config_.decodable_info,
      *decode_fst_, feature_pipeline_.get());

  feature_pipeline_->SetAdaptationState(adaptation_state_);
  decoder_->Checkpoint();
  current_chunk_.clear();
  audio_size_ = 0;
}

void Recognizer::Accept() {
  if (current_chunk_.size() != 0) {
    audio_size_ += current_chunk_.size();
    SubVector<BaseFloat> chunk(current_chunk_.data(), current_chunk_.size());
    feature_pipeline_->AcceptWaveform(kModelFrequency, chunk);
  }
}

void Recognizer::UpdateIvectorAndAdvance() {
  if (silence_weighting_->Active() &&
      feature_pipeline_->IvectorFeature() != NULL) {
    std::vector<std::pair<int32, BaseFloat>> delta_weights;
    silence_weighting_->ComputeCurrentTraceback(decoder_->Decoder());
    silence_weighting_->GetDeltaWeights(feature_pipeline_->NumFramesReady(),
                                        &delta_weights);
    feature_pipeline_->IvectorFeature()->UpdateFrameWeights(delta_weights);
  }

  decoder_->AdvanceDecoding();
}

void Recognizer::Rollback() {
  feature_pipeline_->RevertInputFinished();
  decoder_->Rollback();
  decoder_->AdvanceDecoding();
}

std::optional<Lattice> Recognizer::GetLattice(bool final) {
  if (decoder_->NumFramesDecoded() == 0) {
    std::cerr << "Decoded no frames." << std::endl;
    return std::nullopt;
  }

  CompactLattice clat;
  decoder_->GetLattice(final, &clat);
  if (clat.NumStates() == 0) {
    return std::nullopt;
  }

  Lattice lat;
  ConvertLattice(clat, &lat);
  return lat;
}

void Recognizer::ProcessAudio(const std::vector<kaldi::BaseFloat> &data) {
  current_chunk_.insert(current_chunk_.end(), data.begin(),
                        data.begin() + data.size());
  if (current_chunk_.size() < config_.chunk_length) {
    return;
  }
  Accept();
  UpdateIvectorAndAdvance();
  if (current_chunk_.size() != 0) {
    decoder_->Checkpoint();
  }

  current_chunk_.clear();
}

void Recognizer::GetResponse(
    std::function<void(std::optional<kaldi::Lattice>)> handle_response) {
  Accept();
  if (audio_size_ == 0) {
    std::cerr << "Can't decode empty audio." << std::endl;
    handle_response(std::nullopt);
    return;
  }

  feature_pipeline_->InputFinished();
  UpdateIvectorAndAdvance();
  decoder_->FinalizeDecoding();

  // The rest of this function takes 50-100ms. We call this callback here
  // since it doesn't affect the response. There might be ways of cutting
  // down the time, but there might always be a non-neglible amount of
  // work here.
  handle_response(GetLattice(true));

  feature_pipeline_->GetAdaptationState(&adaptation_state_);
  Rollback();
  current_chunk_.clear();
}

}  // namespace speech_engine
