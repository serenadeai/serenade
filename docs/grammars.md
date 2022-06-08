# Grammars

Serenade uses a few different grammars to parse spoken commands and code. This document gives a summary of how each is used by the system.

## Command Grammar

The command grammar is used to define the set of valid Serenade commands. It's important to note that this grammar is *not* a deterministic list of what can be said to Serenade. Instead, this grammar is used to train a `transcript-parser` model, which is ultimately used to parse spoken commands. For more on how this works, see [Generating Data](generating-data.md).

The command grammar is defined using the [ANTLR](https://www.antlr.org/) grammar format. It's split between a lexer and a parser, located in:

    core/src/main/antlr/CommandLexer.g4.hbs
    core/src/main/antlr/CommandParser.g4.hbs

## Language Grammars

Language grammars are used to parse source code files into Abstract Syntax Trees (ASTs). Serenade uses [tree-sitter](https://tree-sitter.github.io/tree-sitter/) to parse source code into an immutable tree, then applies a few transformations in order to produce a mutable tree with nodes defined in the `core.ast` package.

When you build Serenade, versioned tree-sitter grammars are automatically downloaded to `~/libserenade/tree-sitter`. The versions for each language grammar are defined in `config/languages.yaml`.

If you'd like to make changes to the tree-sitter grammar and test them with Serenade, follow these steps:

1. Clone the language you'd like to modify to `~/libserenade/dev-tree-sitter`.
2. Run `npm i` to install the necessary dependencies.
3. Several grammars have dependencies on other grammars and require additional setup:
    1. If you are making changes to TypeScript:
        1. Clone `tree-sitter-javascript`
        2. In `tree-sitter-javascript`, run `npm install && npm run build && npm link`
        3. In `tree-sitter-typescript`, run `npm link tree-sitter-javascript`
    2. If you are making changes to C++:
        1. Clone `tree-sitter-c`
        2. In `tree-sitter-c`, run `npm install && npm run build && npm link`
        3. In `tree-sitter-cpp`, run `npm link tree-sitter-c`
4. Make changes to the grammar file
5. Compile grammar changes by running `npm run build`
6. Clean and build `core`, specifying that you want to use the repositories in `dev-tree-sitter` rather than the versioned repositories:
    1. `gradle core:clean`
    2. `DEV_TREE_SITTER=1 gradle core:installd`

When you're done, you can submit a PR to the corresponding language repository.
