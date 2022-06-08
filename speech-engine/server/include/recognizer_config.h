#ifndef LIBRARY_RECOGNIZER_CONFIG_H
#define LIBRARY_RECOGNIZER_CONFIG_H

#include "feat/resample.h"
#include "fstext/fstext-lib.h"
#include "lat/confidence.h"
#include "lat/lattice-functions.h"
#include "nnet3/nnet-utils.h"
#include "online2/online-endpoint.h"
#include "online2/online-nnet2-feature-pipeline.h"
#include "online2/online-nnet3-decoding.h"
#include "online2/online-timing.h"
#include "online2/onlinebin-util.h"
#include "util/kaldi-thread.h"

namespace speech_engine {

struct RecognizerConfig {
  const kaldi::OnlineNnet2FeaturePipelineInfo &feature_info;
  const kaldi::TransitionModel &transition_model;
  const kaldi::nnet3::DecodableNnetSimpleLoopedInfo &decodable_info;
  const kaldi::nnet3::NnetSimpleLoopedComputationOptions &decodable_opts;
  const kaldi::LatticeFasterDecoderConfig &decoder_opts;
  const std::shared_ptr<const fst::ConstFst<fst::StdArc>> decode_fst;
  const int32 nonterm_phones_offset;
  const kaldi::OnlineEndpointConfig &endpoint_opts;
  const int32 sample_frequency;
  const size_t chunk_length;
};

}  // namespace speech_engine

#endif  // LIBRARY_RECOGNIZER_CONFIG_H
