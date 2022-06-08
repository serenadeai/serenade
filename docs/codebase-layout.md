# Codebase Layout

Serenade consists of a few different online services as well as several offline training pipelinees for models.

## Online Services

### core

`core` is the main Serenade application, which is written in Java 14. Some core packages include:

- `ast`: The Abstract Syntax Tree abstraction that's used for various Serenade commands, like `go to function` or `delete parameter`.
- `closeness`: Utility methods for determining which objects are closest to the cursor.
- `codeengine`: Utility methods for making calls to the `code-engine` service.
- `commands`: Code manipulation command implementations.
- `converter`: Language-specific implementations to convert from tree-sitter parse trees to Serenade ASTs.
- `evaluator`: Handles the evaluation of transcripts into Serenade commands.
- `exception`: Serenade exception classes.
- `fixedsource`: Utilities for dealing with tree-sitter parse errors.
- `formattedtext`: The non-ML implementation of text formatting commands, which is used for commands like `system <text>`.
- `language`: Utilities for creating language-specific classes, like converters and selectors.
- `metadata`: Protobuf wrapper classes that add metadata that's used within `core`, but not relevant to other services (and thus the protobuf).
- `parser`: Parses code from strings to tree-sitter trees and then Serenade ASTs.
- `selector`: AST traversal methods and language-specific implementations thereof.
- `server`: HTTP server endpoints.
- `snippet`: Language-specific collections of `add` commands.
- `streaming`: Websocket server endpoints.
- `util`: Utility classes and core concepts used throughout the codebase, like `Range` and `Whitespace`.
- `visitor`: Transcript tree visitor classes

### code-engine

`code-engine` is the C++ implementation of the transcript-to-code engine. `crow` is used for HTTP serving, and `marian` is used to serve our models.

`code-engine` exposes the following endpoints:

- `/api/rescore`: Serves the contextual language model, which is used to rerank alternatives.
- `/api/translate`: Serves the auto-style model, which is used to produce code alternatives.

### speech-engine

`speech-engine` is the C++ implementation of the speech-to-transcript engine. `crow` is used for HTTP serving, and models are served using Kaldi.

`speech-engine` exposes a single Websocket endpoint, `/stream/`. `core` streams audio data over this endpoint, and `speech-engine` responds with a list of transcripts when speech has finished (i.e., and endpoint request is sent).

## Offline Services

### CorpusGen

`corpusgen` is the offline service that generates the synthetic data used to train auto-style models, transcript-parser models, contextual language models, and speech language models. More details can be found [here](generating-data.md).

### scripts/serenade/code_engine

`code_engine` scripts are used to train auto-style models, contextual language models, and transcript-parser models. Instructions for training these models can be found [here](training-models.md), and a details about the model architecture can be found [here](model-architecture.md).

### scripts/serenade/speech_engine

`speech_engine` scripts are used to train Kaldi language models. Instructions for training these models are [here](training-models.md), and details about the speech engine system are [here](model-architecture.md).

### grammarflattener

`grammarflattener` is a post-processing step that's performed on generated tree-sitter grammars in order to make them suitable for use with Serenade.

### replayer

`replayer` is used to calculate metrics for various ML models.
