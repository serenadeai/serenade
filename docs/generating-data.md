# Generating Data with CorpusGen

All of the models used by Serenade use an offline service called CorpusGen to process raw source code data into the source and target text sequences that used to train our models. You most likely will not have to run CorpusGen directly in order to train models, but we this overview is helpful for modifying the generated data for your own experiments.

## Overview and Rationale

CorpusGen works by sampling parts of source code and using a series of hand-crafted heuristics to randomly generate English transcripts that would produce those code snippets. Generating transcripts from source code is fairly straightforward to do with such a rules-based system, and doing so allows us to use real code to inform the model.

To get a better sense of the benefits for taking our approach, consider two other approaches. One alternative is to use a rule-based system to parse the transcripts directly and not to use data at all. The main issue with this is that transcripts can be ambiguous. Consider "list of string elements". Should this result in `List<String> elements` or `list("elements")`? Both are possible interpretations, depending on the context.

Another approach could be to use same machine learning models, but to use annotated data instead of synthetic data. This approach could certainly complement our approach, since both approaches output data in the same format. The main issue it faces when used by itself is that it's too expensive to label enough data to provide strong accuracy gaurantees for the core Serenade commands across a variety of languages.

## Tokenization

Most CorpusGen operations start by tokenizing raw source code files. Camel, pascal, snake, and kebob cased words are split into their component parts (i.e. `helloWorld`, `HelloWorld`, `hello_world`, and `hello-world` are all treated as `hello` and `world`. The lower case version of each component word is treated as a separate token, all punctuation marks are their own tokens, and we include some all-caps special tokens to represent formatting and spacing. The special tokens we use are:

- `SP`: spaces
- `I`: indent (including the new line)
- `D`: dedent (including the new line)
- `NL`: new line without change in indentation
- `C`: indicates the next token is capitalized
- `A`: indicates the next token is in all caps
- `CRSR`: location of the cursor

This tokenization means the model doesn't have to learn the mapping from words to their capitalized versions. The indent/dedent tokens make the tokenization invariant to the current indentation level, which helps with how well the model is later able to generalize in empty files or different contexts.

As an example, this Java line

```java
List<String> elements = new ArrayList<>();
```

would be tokenized as

```
C list < C string > SP elements SP = SP new SP C array C list < > ( ) ;
```

## CorpusGen Mappings

CorpusGen has four modes: `mapping`, `text`, `lexicon`, and `unknowns`. The bulk of the work involves commands related to `mapping` mode.

This mode creates source and target pairs of text sequences corresponding to the desired sequences for the models.

There are two types of mappings: "insert" mappings and "add" mappings. These reflect the "insert" and "add" commands used in Serenade, where "insert" is used for inserting formatted strings of text and "add" is used to add larger code snippets. For both types of mappings, the CorpusGen `MappingGenerator` randomly samples the raw source code and then approximately generates the natural language command that would produce that source code.

Running CorpusGen in mapping mode for the `transcript-parser` adds an additional step. There, we use the insert mappings to produce new mappings from command transcripts to transcript parse trees.

### Mapping Types

#### Insert Mappings

Insert mappings are obtained by iterating through each token in the raw source code files and using them to update a list of code tokens and a list of phrase tokens. The code tokens match the raw source code. The phrase tokens are determined by hand-crafted conversion rules, with some randomness to generate multiple ways of generating the code.

In order to split large source code files into reasonable command-sized chunks, we set a small "stop" probability to be sampled at each iteration. When that property is true, the lists of code tokens and phrase tokens are added to the running list of mappings and then reset. The process continues with the remaining unprocessed tokens until all data files are processed.

#### Add Mappings

Add mappings (sometimes referred to as snippets) sample nodes from the abstract syntax tree (AST) of the source code, rather than individual tokens. Within the node, the code tokens are converted to phrase tokens just like they are for insert mappings. Add mappings also include a reference to the "snippet container"—the parent AST node of the sampled node. For example, if we are adding an argument to a function signature, the snippet container would be an `ArgumentList`.

#### Transcript Parser

The transcript parser model uses the phrase token lists from the insert mappings to get examples of formatted text string phrases (like "camel case hello world"). A separate module (`Chainer`) produces random chains of commands based on a grammar of command rules, and uses this generated formatted text to fill in the portions of the transcripts that accept formatted text. We discuss the details of `Chainer` below.

### Leading Context and Alpha Context

Both types of mappings sometimes include leading context and "alpha subsequence context". Leading context is just a list of the tokens that precede the code tokens of the mapping. Alpha subsequence context is the sequences of alphanumeric tokens that appear elsewhere in the code. Both of these serve to help the model disambiguate or reweight ambiguous results based on the source code that is in your editor while using Serenade.

Some mappings that are generated do not include any context. This is done to ensure the model can still produce good predictions when we start a new file.

### Mapping Example

We'll show an example group of mappings for the following Python source file:

```python
def factorial(number):
    if number <= 1:
	    return 1
    return number * factorial(number - 1)
```

Note that the formatting here is just for illustration—you can get a sense for the true API by reading the source for `FullContextMapping`:

```python
mapping1:
  leading_context: ""
  phrase_tokens: "def factorial of number colon"
  output_tokens: "def SP factorial ( number ) :"

mapping2:
  leading_context: "NL def SP factorial ( number ) : I"
  phrase_tokens: "if"
  output_tokens: "if SP C true CRSR : I pass"

mapping3:
  leading_context: "number ) : I if SP"
  phrase_tokens: "number is less than or equal to one"
  output_tokens: "number SP < = SP 1"

mapping4:
  leading_context: "< = SP 1 : I"
  phrase_tokens: "return one"
  output_tokens: "return SP 1"

add_mapping1:
  leading_context: ""
  snippet_container: "StatementList"
  phrase_tokens: "function factorial"
  output_tokens: "def SP factorial CRSR ( ) : I pass"

add_mapping2:
  leading_context: "NL def SP factorial ("
  snippet_container: "ParameterList"
  phrase_tokens: "parameter number"
  output_tokens: "number CRSR"
```

#### Source and Target Format Examples

The mapping objects are converted into text that is formatted differently depending on the model we want to use the data to train.

##### `auto-style` Insert Mappings

Source (input): `CTX {leading_context} ENG {phrase_tokens}`

Target (output): `{output_tokens}`

##### `auto-style` Add Mappings

Source: `CTX {leading_context} SNIP_{snippet_container} {phrase_tokens}`

Target: `{output_tokens}`

##### `contextual-language-model`

Source: `{leading_context}`

Target: `{phrase_tokens}`

Since the `transcript-parser` mappings are slightly different, we present the example in the section explaining [transcript parsing](#commandgenerator-chainer-and-transcript-parse-trees)).

## Other CorpusGen Modes

### `text`

This mode is used to generate data for training the speech engine language model

`text` mode is similar to `mapping` mode for the `transcript-parser`. It uses the formatted text from the insert mapping transcripts to generate examples commands. The major difference is that it also adds filler words, like "um" and "uh" to the transcripts.

### `lexicon`

`lexicon` mode generates a list of all of the most frequently used tokens for all code engine model data sets. Marian transformer models expect the training data to have a fixed vocabulary size, so this mode is used to determine which words are added to that vocabulary

### `unknowns`

For words that do not make it into the lexicon, we use `unknowns` to replace them in the generated data sets with a special “UNK” token indicating that they are outside of the established vocabulary.

## `CommandGenerator`, `Chainer`, and Transcript Parse Trees

The `transcript-parser` model is slightly different from the other code engine models. For this model, we extract just the formatted text strings from the insert mappings. We then run `CommandGenerator`, which takes an [ANTLR grammar](https://www.antlr.org/) of Serenade commands and randomly samples paths through that grammar. Some of the nodes (rules) in the grammar call for formatted text; when we reach one of those nodes, we add one of the formatted text strings we obtained from the insert mapping.

The reasoning behind using formatted text from insert mappings (rather than, say, random text) is similar to our rationale for generating commands from code. By using text simulated from real source code, we can resolve ambiguities by ensuring that the distribution of these ambiguities are accurately represented in our training set to properly guide the model.

The result from running `CommandGenerator` is a series of parse trees (paths sampled from the grammar) and the corresponding English transcripts associated with each tree. Some examples (using formatted text from the Python example [above](#mapping-example)), are given here:

```
command1:
  transcript: "replace all factorial of number minus with return one"
  parse_tree:
    <command>
      <changeAll>
        replace all
        <formattedText> factorial of number minus </formattedText>
        with
        <formattedText> return one </formattedText>
      </changeAll>
    </command>

command2:
  transcript: "left two"
  parse_tree:
    <command>
      <arrowKeyWithQuantifier>
        <arrowKeyDirection> left </arrowKeyDirection>
        <numberRange1To10> two </numberRange1To10>
      </arrowKeyWithQuantifier>
    </command>

command3:
  transcript: "after positional parameter def factorial of number colon"
  parse_tree:
    <command>
      <goTo>
        <preposition> after </preposition>
        <positionSelection>
          <namedPositionSelection>
            <namedSelectionObject>
              <positionalParameterNamedObject> positional parameter </positionalParameterNamedObject>
            </namedSelectionObject>
            <formattedText> def factorial of number colon </formattedText>
          </namedPositionSelection>
        </positionSelection>
      </goTo>
    </command>
...
```

Serenade is also capable of chaining multiple commands together (e.g. "go to line 5, add function foo"). To support this feature in the transcript parser, we provide examples of these chained commands in the training data. These chains are produced by `Chainer`, which takes the list of commands generated by `CommandGenerator` and combines some fraction of commands that can be chained together. The command markup for these final parse trees looks like

```
<main>
  <commandChain>
    <command>
      # command 1
    </command>
    <command>
      # command 2
    </command>
    ...
    <command>
      # command n
    </command>
  </commandChain>
</main>
```

The final results, then, from running CorpusGen in `mapping` mode for the `transcript-parser` model are source files containing the transcripts of all of the sampled chains and target files containing all of the markup, flattened into one line per chain.

## CorpusGen Tests

If you modify CorpusGen, you can test your changes against the CorpusGen unit tests with

```bash
./scripts/serenade/bin/run.py --tests 'gradle corpusgen:test'
```

These tests effectively check the inverse of the core unit tests. The `core` tests include an example transcript, source code before the change, and the expected source code after the change. With the CorpusGen tests, we process the desired source code change through mapping mode and make sure that the transcript is included in the list of transcripts that are generated from that code change.
