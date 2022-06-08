#include <dirent.h>

#include <boost/algorithm/string.hpp>
#include <boost/algorithm/string/join.hpp>
#include <boost/algorithm/string/regex.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/regex.hpp>
#include <iostream>
#include <list>
#include <memory>
#include <mutex>

#include "code-engine.pb.h"
#include "common/timer.h"
#include "common/utils.h"
#include "core.pb.h"
#include "crow.h"
#include "io.h"
#include "marian.h"
#include "token_encryption.h"
#include "translator/beam_search.h"
#include "translator/translator.h"

// must be included last
#include "rescorer_extension.h"

using namespace code_engine;
using namespace core;

RescoringResponse HandleRescore(
    RescoringRequest& request,
    marian::Ptr<RescoreService<marian::Rescorer>>& service,
    TokenIdConverter& converter) {
  RescoringResponse response;

  std::map<std::string, std::vector<std::string>> input_to_outputs;
  for (int i = 0; i < request.alternative_size(); i++) {
    input_to_outputs[request.alternative(i).input_sentence()].push_back(
        request.alternative(i).output_sentence());
  }

  // create input/output lines in the n-best dataset format. the order
  // will be related to the scores that we get back, so also construct a
  // map that lets us go from input/output pair to score index.
  std::vector<std::string> input_lines;
  std::vector<std::string> output_lines;
  std::map<std::pair<std::string, std::string>, int> score_index;
  int index = 0;
  for (auto const& entry : input_to_outputs) {
    std::string input = entry.first;
    std::vector<std::string> outputs = converter.Encode(entry.second);
    for (int i = 0; i < outputs.size(); i++) {
      output_lines.push_back(std::to_string(input_lines.size()) + " ||| " +
                             outputs[i] + " ||| 0 ||| 0 ");
      score_index[std::make_pair(input, outputs[i])] = index++;
    }

    input_lines.push_back(input);
  }

  std::map<long, float> scores =
      service->run_rescore(boost::join(converter.Encode(input_lines), " \n "),
                           boost::join(output_lines, " \n "));

  // use the score indexing created above so that we can preserve the
  // same order of the alternatives as they were specified in the call.
  for (int i = 0; i < request.alternative_size(); i++) {
    RescoringAlternative* alternative = response.add_alternative();
    std::string input = request.alternative(i).input_sentence();
    std::string output = request.alternative(i).output_sentence();
    alternative->set_score(
        scores[score_index[std::make_pair(input, converter.Encode(output))]]);
    alternative->set_input_sentence(input);
    alternative->set_output_sentence(output);
  }

  return response;
}

TranslationResponse HandleTranslate(
    TranslationRequest& request,
    marian::Ptr<marian::TranslateService<marian::BeamSearch>>& service,
    TokenIdConverter& converter) {
  TranslationResponse response;
  std::vector<std::string> raw_inputs(request.input_sentence().begin(),
                                      request.input_sentence().end());
  if (raw_inputs.size() == 0) {
    return response;
  }

  std::string inputs = boost::join(converter.Encode(raw_inputs), "\n");
  std::string output_string = service->run(inputs);
  std::vector<std::string> output_lines;
  boost::split(output_lines, output_string, [](char c) { return c == '\n'; });
  std::string prefix = "";
  TranslationOutput* output;
  for (std::string output_line : output_lines) {
    std::vector<std::string> output_line_sections;
    boost::split_regex(output_line_sections, output_line,
                       boost::regex(" \\|\\|\\| "));
    if (prefix != output_line_sections[0]) {
      output = response.add_output();
      prefix = output_line_sections[0];
    }

    TranslationAlternative* alternative = output->add_alternative();
    alternative->set_sentence(converter.Decode(output_line_sections[1]));
    alternative->set_score(
        boost::lexical_cast<double>(output_line_sections[4]));
  }

  return response;
}

std::string LanguageToString(Language language) {
  if (language == LANGUAGE_JAVA) {
    return "java";
  } else if (language == LANGUAGE_PYTHON) {
    return "python";
  } else if (language == LANGUAGE_JAVASCRIPT) {
    return "javascript";
  } else if (language == LANGUAGE_SCSS) {
    return "scss";
  } else if (language == LANGUAGE_HTML) {
    return "html";
  } else if (language == LANGUAGE_DART) {
    return "dart";
  } else if (language == LANGUAGE_KOTLIN) {
    return "kotlin";
  } else if (language == LANGUAGE_CPLUSPLUS) {
    return "cplusplus";
  } else if (language == LANGUAGE_BASH) {
    return "bash";
  } else if (language == LANGUAGE_DEFAULT) {
    return "default";
  } else if (language == LANGUAGE_CSHARP) {
    return "csharp";
  } else if (language == LANGUAGE_GO) {
    return "go";
  } else if (language == LANGUAGE_RUST) {
    return "rust";
  } else if (language == LANGUAGE_RUBY) {
    return "ruby";
  }

  return "";
}

void LoadModels(
    Model model, std::vector<Language>& languages, std::string& include,
    std::map<std::pair<Model, Language>, std::mutex>& mutexes,
    std::map<std::pair<Model, Language>, std::unique_ptr<TokenIdConverter>>&
        converters,
    std::map<std::pair<Model, Language>,
             marian::Ptr<marian::TranslateService<marian::BeamSearch>>>&
        translate_services,
    std::map<std::pair<Model, Language>,
             marian::Ptr<RescoreService<marian::Rescorer>>>& rescore_services) {
  for (Language language : languages) {
    std::string path = include + std::string("/export/");
    if (model == MODEL_AUTO_STYLE) {
      path += std::string("/auto-style/");
    } else if (model == MODEL_CONTEXTUAL_LANGUAGE_MODEL) {
      path += std::string("/contextual-language-model/");
    } else if (model == MODEL_TRANSCRIPT_PARSER) {
      path += std::string("/transcript-parser/");
    }
    path += LanguageToString(language) + std::string("/");

    std::unique_ptr<std::istream> config_in =
        FileStream(path + std::string("config.yml"));
    std::ostringstream sstr;
    sstr << config_in->rdbuf();

    std::string config = std::string(sstr.str());
    if (model == MODEL_AUTO_STYLE || model == MODEL_TRANSCRIPT_PARSER) {
      config +=
          std::string("models:\n  - ") + path + std::string("model.npz\n");
    } else {
      config += std::string("model:\n  ") + path + std::string("model.npz\n");
    }

    config += std::string("vocabs:\n  - ") + path +
              std::string("exposed_vocab.yml\n") + std::string("  - ") + path +
              std::string("exposed_vocab.yml\n");

    auto options = marian::New<marian::Options>();
    options->parse(config);
    if (model == MODEL_AUTO_STYLE || model == MODEL_TRANSCRIPT_PARSER) {
      translate_services[std::make_pair(model, language)] =
          marian::New<marian::TranslateService<marian::BeamSearch>>(options);
      converters[std::make_pair(model, language)] =
          std::make_unique<TokenIdConverter>(path + std::string("tokens.txt"));
    } else {
      rescore_services[std::make_pair(model, language)] =
          marian::New<RescoreService<marian::Rescorer>>(options);
      converters[std::make_pair(model, language)] =
          std::make_unique<TokenIdConverter>(path + std::string("tokens.txt"),
                                             path + std::string("vocab.spm"));
    }

    mutexes[std::make_pair(model, language)];
  }
}

int main(int argc, char* argv[]) {
  std::string include = std::getenv("CODE_ENGINE_MODELS");

  std::map<std::pair<Model, Language>, std::mutex> mutexes;
  std::map<std::pair<Model, Language>, std::unique_ptr<TokenIdConverter>>
      converters;

  std::map<std::pair<Model, Language>,
           marian::Ptr<marian::TranslateService<marian::BeamSearch>>>
      translate_services;

  std::map<std::pair<Model, Language>,
           marian::Ptr<RescoreService<marian::Rescorer>>>
      rescore_services;

  std::vector<Language> transcript_parser_models = {LANGUAGE_DEFAULT};
  std::vector<Language> auto_style_models = {
      LANGUAGE_JAVA, LANGUAGE_PYTHON,  LANGUAGE_JAVASCRIPT, LANGUAGE_SCSS,
      LANGUAGE_HTML, LANGUAGE_DART,    LANGUAGE_KOTLIN,     LANGUAGE_CPLUSPLUS,
      LANGUAGE_BASH, LANGUAGE_DEFAULT, LANGUAGE_CSHARP,     LANGUAGE_GO,
      LANGUAGE_RUST, LANGUAGE_RUBY};

  LoadModels(MODEL_AUTO_STYLE, auto_style_models, include, mutexes, converters,
             translate_services, rescore_services);
  LoadModels(MODEL_CONTEXTUAL_LANGUAGE_MODEL, auto_style_models, include,
             mutexes, converters, translate_services, rescore_services);
  LoadModels(MODEL_TRANSCRIPT_PARSER, transcript_parser_models, include,
             mutexes, converters, translate_services, rescore_services);

  try {
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

    CROW_ROUTE(app, "/api/rescore")
        .methods("POST"_method)([&](const crow::request& request) {
          RescoringRequest data;

          data.ParseFromString(request.body);
          std::string result;

          RescoringResponse rescoring_response;
          if (rescore_services.find(std::make_pair(
                  data.model(), data.language())) != rescore_services.end()) {
            std::lock_guard<std::mutex> _(
                mutexes[std::make_pair(data.model(), data.language())]);
            rescoring_response = HandleRescore(
                data,
                rescore_services[std::make_pair(data.model(), data.language())],
                *converters[std::make_pair(data.model(), data.language())]
                     .get());
          }

          rescoring_response.SerializeToString(&result);
          return result;
        });

    CROW_ROUTE(app, "/api/translate")
        .methods("POST"_method)([&](const crow::request& request) {
          TranslationRequest data;
          data.ParseFromString(request.body);

          TranslationResponse translation_response;
          if (translate_services.find(std::make_pair(
                  data.model(), data.language())) != translate_services.end()) {
            std::lock_guard<std::mutex> _(
                mutexes[std::make_pair(data.model(), data.language())]);
            translation_response = HandleTranslate(
                data,
                translate_services[std::make_pair(data.model(),
                                                  data.language())],
                *converters[std::make_pair(data.model(), data.language())]
                     .get());
          }

          std::string result;
          translation_response.SerializeToString(&result);
          return result;
        });

    app.loglevel(crow::LogLevel::Error);
    app.port(17203).server_name("").multithreaded().run();

  } catch (const std::exception& e) {
    std::cerr << "code-engine exception: " << e.what() << std::endl;
  }
}  // main()
