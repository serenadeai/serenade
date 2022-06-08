#include <algorithm>
#include <boost/algorithm/string/predicate.hpp>
#include <boost/compute/detail/lru_cache.hpp>
#include <boost/uuid/uuid.hpp>
#include <boost/uuid/uuid_generators.hpp>
#include <boost/uuid/uuid_io.hpp>
#include <chrono>
#include <memory>
#include <mutex>

#include "base/kaldi-common.h"
#include "crow.h"
#include "feat/resample.h"
#include "fst/fstlib.h"
#include "fstext/fstext-lib.h"
#include "fstext/kaldi-fst-io.h"
#include "fstext/table-matcher.h"
#include "hints.h"
#include "io.h"
#include "lat/confidence.h"
#include "lat/kaldi-lattice.h"
#include "lat/lattice-functions.h"
#include "lattice_ops.h"
#include "lm/const-arpa-lm.h"
#include "nnet3/nnet-utils.h"
#include "online2/online-endpoint.h"
#include "online2/online-nnet2-feature-pipeline.h"
#include "online2/online-nnet3-decoding.h"
#include "online2/online-timing.h"
#include "online2/onlinebin-util.h"
#include "phoneme_converter.h"
#include "recognizer.h"
#include "recognizer_config.h"
#include "rescorer.h"
#include "speech-engine.pb.h"
#include "util/common-utils.h"
#include "util/kaldi-thread.h"

using namespace speech_engine;
using namespace kaldi;
using namespace fst;

class SpeechEngineStream {
  const std::string kEndPostfix = "(spell)";

  const RecognizerConfig& recognizer_config_;
  Rescorer& chunk_rescorer_;
  Rescorer& final_rescorer_;
  std::unique_ptr<Recognizer> previous_recognizer_;
  std::unique_ptr<Recognizer> recognizer_;
  std::vector<std::string> hints_;
  boost::compute::detail::lru_cache<
      std::vector<std::string>,
      std::shared_ptr<const fst::ConstFst<fst::StdArc>>>
      hint_cache_;
  const HintGraphCreator& hint_graph_creator_;
  const fst::SymbolTable& word_symbols_;
  const std::set<std::string>& words_;
  int32 hint_word_start_;
  int32 nonterm_phones_offset_;

  std::string LatticeToTranscript(const std::vector<int32>& alignment,
                                  const std::vector<int32>& words) {
    std::string transcript = "";
    bool previous_is_letters = false;
    for (size_t j = 0; j < words.size(); j++) {
      std::string s = words[j] >= hint_word_start_
                          ? hints_[words[j] - hint_word_start_]
                          : word_symbols_.Find(words[j]);
      bool current_is_letters =
          boost::algorithm::ends_with(s, kEndPostfix) || (s.size() == 1);
      if (j > 0 && !(previous_is_letters && current_is_letters)) {
        transcript += " ";
      }
      if (current_is_letters && s.size() > 1) {
        s = s.substr(0, s.size() - kEndPostfix.size());
      }

      transcript += s;
      previous_is_letters = current_is_letters;
    }

    return transcript;
  }

  void LatticeToAlternatives(const Lattice& lat,
                             AlternativesResponse& response) {
    std::vector<string> transcripts;
    std::vector<Lattice> nbest_lats = Nbest(lat);

    for (size_t i = 0; i < nbest_lats.size(); i++) {
      LatticeWeight weight_with_language_model;
      std::vector<int32> alignment;
      std::vector<int32> words;
      GetLinearSymbolSequence(nbest_lats[i], &alignment, &words,
                              &weight_with_language_model);

      boost::uuids::uuid uuid = boost::uuids::random_generator()();
      Alternative* alternative = response.add_alternatives();
      alternative->set_transcript(LatticeToTranscript(alignment, words));
      alternative->set_transcript_id(boost::uuids::to_string(uuid));

      double cost = ConvertToCost(weight_with_language_model);
      alternative->set_cost(cost);

      std::optional<Lattice> lattice =
          final_rescorer_.ScaleLanguageModelScore(nbest_lats[i], -1.0);
      if (!lattice) {
        return;
      }

      LatticeWeight weight_without_language_model;
      GetLinearSymbolSequence(*lattice, &alignment, &words,
                              &weight_without_language_model);
      double acoustic_cost = ConvertToCost(weight_without_language_model);
      alternative->set_acoustic_cost(acoustic_cost);
      alternative->set_language_model_cost(cost - acoustic_cost);
    }
  }

 public:
  crow::websocket::connection& connection_;

  SpeechEngineStream(crow::websocket::connection& connection,
                     const RecognizerConfig& recognizer_config,
                     Rescorer& chunk_rescorer, Rescorer& final_rescorer,
                     const HintGraphCreator& hint_graph_creator,
                     const fst::SymbolTable& word_symbols,
                     const std::set<std::string>& words, int32 hint_word_start,
                     int32 nonterm_phones_offset)
      : connection_(connection),
        recognizer_config_(recognizer_config),
        chunk_rescorer_(chunk_rescorer),
        final_rescorer_(final_rescorer),
        hint_graph_creator_(hint_graph_creator),
        word_symbols_(word_symbols),
        words_(words),
        hint_word_start_(hint_word_start),
        nonterm_phones_offset_(nonterm_phones_offset),
        hint_cache_(20){};

  void HandleAudioToAlternatives(AudioToAlternativesRequest& request) {
    try {
      if (request.has_audio_request()) {
        std::vector<BaseFloat> audio{
            reinterpret_cast<const int16*>(
                request.audio_request().audio().c_str()),
            reinterpret_cast<const int16*>(
                request.audio_request().audio().c_str() +
                request.audio_request().audio().size())};
        if (recognizer_) {
          recognizer_->ProcessAudio(audio);
        }
      } else if (request.has_init_request()) {
        hints_.resize(0);
        for (std::string hint : request.init_request().hints()) {
          if (words_.find(hint) == words_.end()) {
            hints_.emplace_back(hint);
          }
          std::sort(hints_.begin(), hints_.end());
        }

        std::shared_ptr<const fst::ConstFst<fst::StdArc>> hint_graph;
        if (auto optional_hint_graph = hint_cache_.get(hints_)) {
          hint_graph = *optional_hint_graph;
        } else {
          hint_graph = hint_graph_creator_.Create(hints_);
          hint_cache_.insert(hints_, hint_graph);
        }

        std::vector<std::pair<int32, std::shared_ptr<const ConstFst<StdArc>>>>
            hints_fsts;
        hints_fsts.push_back(
            std::pair<int32, std::shared_ptr<const ConstFst<StdArc>>>(
                nonterm_phones_offset_ + 4, hint_graph));

        previous_recognizer_ = std::move(recognizer_);
        recognizer_ = std::make_unique<Recognizer>(recognizer_config_);
        recognizer_->Init(hints_fsts);
      } else if (request.has_revert_request() && previous_recognizer_) {
        std::swap(recognizer_, previous_recognizer_);
      } else if (request.has_transcripts_request()) {
        if (recognizer_) {
          recognizer_->GetResponse([&](std::optional<Lattice> lattice) {
            AlternativesResponse response;
            if (lattice &&
                (lattice =
                     chunk_rescorer_.ScaleLanguageModelScore(*lattice, -1.0)) &&
                (lattice =
                     final_rescorer_.ScaleLanguageModelScore(*lattice, 1.0))) {
              LatticeToAlternatives(*lattice, response);
            }

            std::string result;
            response.SerializeToString(&result);
            connection_.send_binary(result);
          });
        }
      }
    } catch (const std::exception& e) {
      std::cerr << "AudioToAlternatives exception:" << std::endl
                << e.what() << std::endl;
      throw;
    }
  }
};

int main(int argc, char* argv[]) {
  REGISTER_FST(ConstFst, StdArc);
  try {
    typedef kaldi::int32 int32;
    typedef kaldi::int64 int64;

    std::string include = std::getenv("SPEECH_ENGINE_MODELS");

    // feature_opts includes configuration for the iVector adaptation,
    // as well as the basic features.
    nnet3::NnetSimpleLoopedComputationOptions decodable_opts;
    LatticeFasterDecoderConfig decoder_opts;
    OnlineEndpointConfig endpoint_opts;

    int32 nonterm_phones_offset =
        ReadIntegerFile(include + "/export/nonterm_phones_offset.int");

    float chunk_length_secs = 0.5;
    int32 sample_frequency = 16000;
    decodable_opts.frame_subsampling_factor = 3;
    decodable_opts.acoustic_scale = 1.0;
    decoder_opts.beam = 15.0;
    decoder_opts.max_active = 7000;
    decoder_opts.min_active = 200;
    decoder_opts.lattice_beam = 8.0;

    // setup audio to lattice
    OnlineNnet2FeaturePipelineInfo feature_info;

    feature_info.use_ivectors = true;
    feature_info.ivector_extractor_info.online_cmvn_iextractor = false;
    feature_info.ivector_extractor_info.num_cg_iters = 15;
    feature_info.ivector_extractor_info.use_most_recent_ivector = true;
    feature_info.ivector_extractor_info.greedy_ivector_extractor = false;
    feature_info.ivector_extractor_info.splice_opts.left_context = 3;
    feature_info.ivector_extractor_info.splice_opts.right_context = 3;
    feature_info.ivector_extractor_info.num_gselect = 5;
    feature_info.ivector_extractor_info.min_post = 0.025;
    feature_info.ivector_extractor_info.posterior_scale = 0.1;
    feature_info.ivector_extractor_info.max_remembered_frames = 1000;
    feature_info.ivector_extractor_info.max_count = 100;
    feature_info.ivector_extractor_info.ivector_period = 10;
    ReadKaldiObjectFile(include + "/export/ivector_extractor/final.mat",
                        &feature_info.ivector_extractor_info.lda_mat);
    ReadKaldiObjectFile(include + "/export/ivector_extractor/global_cmvn.stats",
                        &feature_info.ivector_extractor_info.global_cmvn_stats);
    ReadKaldiObjectFile(include + "/export/ivector_extractor/final.dubm",
                        &feature_info.ivector_extractor_info.diag_ubm);
    ReadKaldiObjectFile(include + "/export/ivector_extractor/final.ie",
                        &feature_info.ivector_extractor_info.extractor);

    feature_info.mfcc_opts.use_energy = false;
    feature_info.mfcc_opts.mel_opts.num_bins = 40;
    feature_info.mfcc_opts.mel_opts.low_freq = 20;
    feature_info.mfcc_opts.mel_opts.high_freq = -400;
    feature_info.mfcc_opts.num_ceps = 40;

    TransitionModel transition_model;
    nnet3::AmNnetSimple am_nnet;
    {
      bool binary;
      std::unique_ptr<std::istream> is =
          KaldiFileStream(include + "/export/final.mdl", &binary);
      transition_model.Read(*is, binary);
      am_nnet.Read(*is, binary);
      SetBatchnormTestMode(true, &(am_nnet.GetNnet()));
      SetDropoutTestMode(true, &(am_nnet.GetNnet()));
      nnet3::CollapseModel(nnet3::CollapseModelConfig(), &(am_nnet.GetNnet()));
    }

    nnet3::DecodableNnetSimpleLoopedInfo decodable_info{decodable_opts,
                                                        &am_nnet};

    const std::shared_ptr<const ConstFst<StdArc>> decode_fst(
        ReadConstFstFile(include + "/export/graph/HCLG.fst"));

    fst::SymbolTable* word_symbols = NULL;
    bool binary;
    std::unique_ptr<std::istream> is =
        KaldiFileStream(include + "/export/graph/words.txt", &binary);
    if (!(word_symbols = fst::SymbolTable::ReadText(*is, "words.txt"))) {
      std::cerr << "Could not read symbol table from file" << std::endl;
      return 2;
    }

    fst::SymbolTableIterator it = fst::SymbolTableIterator(*word_symbols);
    std::set<std::string> words;
    while (!it.Done()) {
      words.insert(it.Symbol());
      it.Next();
    }
    int32 hint_word_start = word_symbols->NumSymbols();

    // setup language model rescoring
    ConstArpaLm chunk_const_arpa;
    ConstArpaLm final_const_arpa;
    ReadKaldiObjectFile(include + "/export/lang/const_arpa", &chunk_const_arpa);
    ReadKaldiObjectFile(include + "/export/lang/final_const_arpa",
                        &final_const_arpa);
    Rescorer chunk_rescorer{chunk_const_arpa};
    Rescorer final_rescorer{final_const_arpa};

    RecognizerConfig recognizer_config{
        feature_info,
        transition_model,
        decodable_info,
        decodable_opts,
        decoder_opts,
        decode_fst,
        nonterm_phones_offset,
        endpoint_opts,
        sample_frequency,
        static_cast<size_t>(chunk_length_secs * sample_frequency)};

    std::string phoneme_model_path = include + "/g2p/model.fst";
    PhonemeConverter phoneme_converter{phoneme_model_path};
    ContextDependency context_dependency;
    ReadKaldiObjectFile(include + "/export/tree", &context_dependency);
    std::vector<int32> disambig_syms;
    ReadIntegerVectorFile(include + "/export/graph/phones/disambig.int",
                          &disambig_syms);
    SortAndUniq(&disambig_syms);
    HintGraphCreator hint_graph_creator{
        phoneme_converter,
        transition_model,
        context_dependency,
        disambig_syms,
        hint_word_start,
        nonterm_phones_offset,
        include + "/export/lang/lexicon.txt",
        include + "/export/lang/phones.txt",
        include + "/export/lang/phones/left_context_phones.int"};

    std::mutex mtx;
    std::vector<std::shared_ptr<SpeechEngineStream>> streams;
    crow::SimpleApp app;

    CROW_ROUTE(app, "/api/status")
    ([] {
      std::string container_id = "";
      if (std::getenv("CONTAINER_ID") != nullptr) {
        container_id = std::getenv("CONTAINER_ID");
      }
      std::string commit_id = "";
      if (std::getenv("GIT_COMMIT") != nullptr) {
        commit_id = std::getenv("GIT_COMMIT");
      }

      return "{\"status\":\"ok\",\"c\":\"" + container_id + "\",\"g\":\"" +
             commit_id + "\"}";
    });

    CROW_ROUTE(app, "/stream/")
        .websocket()
        .onopen([&](crow::websocket::connection& connection) {
          std::lock_guard<std::mutex> _(mtx);
          auto stream =
              std::find_if(streams.begin(), streams.end(),
                           [&](std::shared_ptr<SpeechEngineStream>& e) {
                             return &e->connection_ == &connection;
                           });

          if (stream != streams.end()) {
            return;
          }

          streams.push_back(std::make_shared<SpeechEngineStream>(
              connection, recognizer_config, chunk_rescorer, final_rescorer,
              hint_graph_creator, *word_symbols, words, hint_word_start,
              nonterm_phones_offset));
        })
        .onclose([&](crow::websocket::connection& connection,
                     const std::string& reason) {
          std::lock_guard<std::mutex> _(mtx);
          streams.erase(
              std::remove_if(streams.begin(), streams.end(),
                             [&](std::shared_ptr<SpeechEngineStream>& e) {
                               return &e->connection_ == &connection;
                             }),
              streams.end());
        })
        .onmessage([&](crow::websocket::connection& connection,
                       const std::string& data, bool is_binary) {
          std::vector<std::shared_ptr<SpeechEngineStream>>::iterator stream;
          {
            std::lock_guard<std::mutex> _(mtx);
            stream = std::find_if(streams.begin(), streams.end(),
                                  [&](std::shared_ptr<SpeechEngineStream>& e) {
                                    return &e->connection_ == &connection;
                                  });

            if (stream == streams.end()) {
              return;
            }
          }

          AudioToAlternativesRequest request;
          request.ParseFromString(data);
          (*stream)->HandleAudioToAlternatives(request);
        });

    app.loglevel(crow::LogLevel::Error);
    app.port(17202).server_name("").multithreaded().run();
  } catch (const std::exception& e) {
    std::cerr << "speech-engine exception: " << e.what() << std::endl;
  }
}  // main()
