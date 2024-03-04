# Training Models

This document explains how to train the models that are used by Serenade. If you're interesting in learning more about how these models work, you can read [Model Architecture](model-architecture.md), and if you're interesting in learning more about the data used to train these models, you can read [Generating Data](generating-data.md).

## Setup

Make sure all of the dependencies for training models are installed and built. You can do so by following the instructions in [Building](building-client.md)—make sure you do *not* use the `--minimal` flag or `serenade-minimal` Docker image (which are used only for running Serenade).

Ensure that you have plenty of disk space. The language model for the speech engine is trained on all of the source code data available in all programming languages supported by Serenade, totaling to about 50 GB.

## Speech Engine

You can train a speech engine model by running:

```bash
scripts/serenade/speech_engine/bin/train.py generate-dataset [--test-mode]
scripts/serenade/speech_engine/bin/train.py train-model [--test-mode]
scripts/serenade/speech_engine/bin/export.py
```

The optional test-mode flag uses a much smaller subset of the raw data and is intended to be used just to ensure that all of the necessary dependencies have been built and all paths are set up correctly.

## Code Engine

The transformer training code is optimized for use with GPUs, so you'll likely want access to at least one GPU. We've found that with 4 GPUs, training a model for one programming language takes about 4 hours. The data generation and export steps, however, do not require a GPU to run. If you decide to use separate machines for data generation and model training (which will likely save on costs), be sure to copy the data with the same relative paths to the Serenade library root directories on both machines before and after running the training steps.

You can train a code engine model by running:

```bash
# GPU not required
scripts/serenade/code_engine/bin/generate_dataset.py --model={MODEL_TYPE} --language={LANGUAGE} [--test-mode]
# GPU recommended
scripts/serenade/code_engine/train.py --model={MODEL_TYPE} --language={LANGUAGE} --gpus={NUMBER OF GPUS TO USE} [--test-mode]
# GPU not required
scripts/serenade/code_engine/export.py
```

As with the speech engine model, the optional test-mode flag in the first two steps uses a smaller subset of the raw data and can be useful for ensuring that all dependencies are built.

The model type provided must be one of `auto-style`, `contextual-language-model`, or `transcript-parser`. The language is the programming language the model is to be used for.

## Using Models

Once you've trained a model, move the generated tarball to the corresponding path in `~/libserenade/models` (e.g., `models/code-engine/export/auto-style/python/<hash>.tar.gz` for the Python auto-style model). Then, update `config/models.yaml` with the hash contained in the tarball filename. When Serenade is rebuilt, your new models will be used. See `bin/download-models.py` for how this works.

## How Training Works

### Speech Engine

The generate dataset step:

- Downloads all of the source code data for all supported languages
- Downloads the pre-trained acoustic model
- Runs CorpusGen in `text` mode to produce a corpus of text commands for each language based on the raw source code for that language
- Combines all of the language corpora into one final corpus

The train step:

- Creates a lexicon of words from the generated along with their ARPABET pronunciations
- Combines the [CMU Pronunciation dictionary](https://github.com/cmusphinx/cmudict) with a number of handle-labeled programming specific words
- Includes pronunciations for letters to support spelling and abbreviations
- The most common 1000 words that appear in the corpus that are not in this lexicon are run through the [Phonetisaurus Grapheme to Phoneme tool](https://github.com/AdolfVonKleist/Phonetisaurus) to get their pronunciations
- Filters the vocabulary for bad words and non-ASCII characters
- Adds silence and noise phones
- Creates a series of ngram models using the SRI LM tool built in to Kaldi
- Builds a Kaldi FST graph (approximately based on the [WSJ Kaldi example](https://github.com/kaldi-asr/kaldi/tree/master/egs/wsj)) using the ngram models and the pre-trained acoustic model
- Converts the newly trained language models into the ConstArpa format

The export step:

- Generates a tarball of all of the necessary model files

### Code Engine

The generate dataset step:

- Downloads the raw source code data that will be used as our training data
- Runs CorpusGen in mapping mode to turn raw source code data into source/target files for the transformer model
- Splits the data into train, test, and validation subsets

The train step:

- Finds for the generated data in `$SERENADE_LIBRARY_ROOT/code-engine-training/data/{MODEL_TYPE}/{LANGUAGE}`
- Generates vocabulary to be used by Marian
- Runs Marian with the proper configuration for the given model type and language

The export step:

- Builds a mapping between token IDs and vocabulary words that is used by the code engine server
- Creates a tarball containing all the necessary model files so that the new model can be used after modifying your models.py file to use the version string of the new model.
