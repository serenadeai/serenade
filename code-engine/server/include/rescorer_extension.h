#include <sstream>
#include <string>

#include "common/config.h"
#include "common/options.h"
#include "data/batch_generator.h"
#include "data/corpus.h"
#include "data/corpus_nbest.h"
#include "marian.h"
#include "models/costs.h"
#include "models/model_task.h"
#include "rescorer/rescorer.h"
#include "rescorer/score_collector.h"
#include "text_nbest_input.h"
#include "training/scheduler.h"
#include "training/validator.h"

// Copied/followed closely from
// https://github.com/marian-nmt/marian-dev/blob/3f155730d4224a76f0edbf4d29b1ea47b4aa1a18/src/rescorer/rescorer.h

template <class Model>

class RescoreService : public marian::ModelServiceTask {
 private:
  marian::Ptr<marian::Options> options_;
  std::vector<marian::Ptr<marian::ExpressionGraph>> graphs_;
  std::vector<marian::Ptr<Model>> models_;
  std::vector<marian::Ptr<marian::Vocab>> srcVocabs_;

 public:
  virtual ~RescoreService() {}

  RescoreService(marian::Ptr<marian::Options> options) : options_(options) {
    auto vocabPaths = options_->get<std::vector<std::string>>("vocabs");
    std::vector<int> maxVocabs = options_->get<std::vector<int>>("dim-vocabs");

    for (size_t i = 0; i < vocabPaths.size(); ++i) {
      marian::Ptr<marian::Vocab> vocab =
          marian::New<marian::Vocab>(options_, i);
      vocab->load(vocabPaths[i], maxVocabs[i]);
      srcVocabs_.emplace_back(vocab);
    }

    options_->set("n-best", true);
    options_->set("inference", true);
    options_->set("shuffle", "none");
    options_->set("cost-type",
                  "ce-rescore");  // indicates that to keep separate
                                  // per-batch-item scoresForSummary

    auto devices = marian::Config::getDevices(options_);

    for (auto device : devices) {
      auto graph = marian::New<marian::ExpressionGraph>(true);

      auto precison =
          options_->get<std::vector<std::string>>("precision", {"float32"});
      graph->setDefaultElementType(
          marian::typeFromString(precison[0]));  // only use first type, used
                                                 // for parameter type in graph
      graph->setDevice(device);

      graph->reserveWorkspaceMB(options_->get<size_t>("workspace"));
      graphs_.push_back(graph);
    }

    auto modelFile = options_->get<std::string>("model");
    models_.resize(graphs_.size());
    marian::ThreadPool pool(graphs_.size(), graphs_.size());
    for (size_t i = 0; i < graphs_.size(); ++i) {
      pool.enqueue(
          [=](size_t j) {
            models_[j] = marian::New<Model>(options_);
            models_[j]->load(graphs_[j], modelFile);
          },
          i);
    }
  }

  std::string run(const std::string &input) override {
    // This needs to be overridden, but we actually need a version with
    // different-shaped output
    return "test";
  }

  std::map<long, float> run_rescore(const std::string &translate_inputs,
                                    const std::string &translate_outputs) {
    std::vector<std::string> texts{translate_inputs, translate_outputs};

    auto corpus =
        marian::New<marian::data::TextNBestInput>(texts, srcVocabs_, options_);

    auto batchGenerator =
        marian::New<marian::BatchGenerator<marian::data::TextNBestInput>>(
            corpus, options_);
    batchGenerator->prepare();

    float sumLoss = 0;
    size_t sumWords = 0;
    size_t sumSamples = 0;
    size_t batchId = 1;  // start at the second cpu since translation is
                         // happening on the first.

    std::map<long, float> output_scores;

    std::mutex smutex;
    {
      marian::ThreadPool pool(graphs_.size(), graphs_.size());

      for (auto batch : *batchGenerator) {
        auto task = [=, &sumLoss, &sumWords, &sumSamples, &smutex,
                     &output_scores](size_t id) {
          thread_local marian::Ptr<marian::ExpressionGraph> graph;
          thread_local marian::Ptr<Model> builder;

          if (!graph) {
            graph = graphs_[id % graphs_.size()];
            builder = models_[id % graphs_.size()];
          }

          // @TODO: normalize by length as in normalize
          // Once we have Frank's concept of ce-sum with sample size by words we
          // will return a pair here which will make it trivial to report all
          // variants.
          auto dynamicLoss = builder->build(graph, batch);

          graph->forward();

          // get loss
          std::vector<float> scoresForSummary;
          dynamicLoss->loss(scoresForSummary);
          std::vector<float> sentScores(scoresForSummary);

          std::unique_lock<std::mutex> lock(smutex);
          for (auto s : scoresForSummary) sumLoss += s;
          sumWords += batch->back()->batchWords();
          sumSamples += batch->size();

          for (size_t i = 0; i < batch->size(); ++i) {
            output_scores.emplace(
                (long)batch->getSentenceIds()[i],  // report logProb while score
                -1.f * sentScores[i]               // is CE, hence negate
            );
          }
        };

        pool.enqueue(task, batchId++);
      }
    }

    return output_scores;
  }
};
