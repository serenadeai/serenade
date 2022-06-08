# Speech and Code Engine Architectures

## Motivation

When we started out building Serenade, we wanted to avoid building our own speech engine. Most of the work in developing Serenade went into building a system to process the transcripts once you have them, as opposed to generating the transcripts from audio. Our initial prototypes used cloud APIs like the Google Speech API.

Unfortunately these APIs had a few problems that led us to build our own speech engine. Among these were latency, privacy concerns, and cost. A decent amount of work went into improving latency. In particular, getting to our goal of < 150ms required special management of state that most voice applications don’t need because latency is less of a constraint. Besides that, the bulk of the work went into mitigating the largest issue: accuracy.

### Accuracy and Metrics

Some ASR leaderboards suggest the models used in these cloud APIs had [reached human-level accuracy in 2017](https://github.com/syhw/wer_are_we). However, these accuracy benchmarks are highly context- and domain-sensitive. Consider the phrase "triple equals". Sending this audio to the Google Speech API came back with something like "AAA, Kohl's". This is arguably a good transcript to guess, especially if the person is mumbling or not speaking clearly, since people speaking to voice assistants (the typical use-case for these APIs) are generally more likely to mention places they buy products from than programming syntax. Similar issues came up with other programming and Serenade-specific words and phrases like "getenv", "instanceof", "dedent", and "system dot out dot println".

This sensitivity to the use case may not be able to be overcome by just increasing the data volume. State-of-the-art models trained on 10k+ hours of general-purpose data often do significantly worse on the LibriSpeech audiobook data benchmark than models only trained on the much smaller (1k hours) LibriSpeech dataset. We noticed the same pattern with our dataset.

When building a new Serenade-specific speech engine, we tried to use domain-specific metrics just like we used domain-specific data. We ultimately landed on recall@1, recall@5, and recall@10, measuring how often the correct transcript is the most likely transcript, in the top 5, and in the top 10, respectively. This was inspired by the "alternatives" mechanic in Serenade, where the most likely transcript is executed, but the user is able to easily revert that change to apply another displayed alternative. In this workflow, it's great if the first result is correct, but it is essential that the correct interpretation is in the top 5-10 options otherwise.

Throughout the iterating process, we often discovered approaches that improved recall@5 and recall@10 a lot, while not affecting recall@1, and vice-versa. Had we focused solely on improving word error-rates, we may have missed some of these opportunities.

### Context

Another salient aspect of the voice coding domain is the rich context: we know what file you’re in, we know its structure, we know the variables in scope, we know where your cursor is, etc. In the same way that Siri is really good at recognizing names by using your contact list, we wanted our speech engine to be able to exploit that context.

### Data Availability

Another unique feature of the voice coding domain is that while there is not much audio data, there is a large amount of potential text data. There are millions of lines of open-source code, though they're not in a form that can be naively leveraged by a speech engine. There's also a lot of text data of people discussing code in places like StackOverflow and GitHub.

## Our Solution

To leverage all of the available text data and make use of context, we focused our speech engine development on two things: figuring out how to generate an English corpus from the raw code data available and picking a speech engine architecture best suited to leverage a large corpus of English text.

### Generating a Corpus

To generate the English corpus, we created a system called CorpusGen (a more detailed outline of the system can be found in [Generating Data](generating-data.md)). This system takes comments and code crawled from GitHub and uses them to generate a random sample of transcripts that could be used to generate that code using Serenade commands. In short, it converts millions of code fragments like "System.out.println" to millions of english transcripts like "system dot out dot println".

### Speech Engine Architecture

The speech engine itself consists of two phases that work together to convert a stream of audio into a transcript. The first phase uses a customized [Kaldi](https://kaldi-asr.org/) system. The second phase is a re-ranking step by a transformer-based language model that takes the context into account.

Kaldi is a Hybrid Hidden Markov Model (Hybrid-HMM) speech engine, which is roughly decomposed into the following three models:

- A language model to predict the likelihood of a given word sequence.
- A pronunciation lexicon, which maps a word into a phone sequence.
- An acoustic model which models the probability a phone sequence would produce the sound that's heard.

This decomposition means we're able to separately leverage our large volume of text-only to train the pronunciation and language models. While end-to-end models do better on leaderboards, we noticed that a lot of the industry still uses hybrid models for similar practical reasons. We still experimented with end-to-end models and our findings are discussed in a later section.

We use the transcripts generated by CorpusGen to train a language model. Then we interpolate this language model with another language model trained on 10 hours of hand-labeled user data. The former is intended to model the tail of the distribution where we have less data (infrequent commands) and is intended to model the head of the distribution (most commonly used commands).

To generate the pronunciation lexicon, we started with the CMU pronunciation dictionary. We then labeled the most frequent words that appear in the corpus but aren’t yet in the lexicon. We hand-labeled the top 100 such words from each programming language, and used [a G2P model](https://github.com/AdolfVonKleist/Phonetisaurus) to automatically label the rest. The speech engine also uses this G2P model at inference time to include words that we see in the users file that aren’t in the static lexicon.

To train the acoustic model, we used the standard [Kaldi LibriSpeech recipe](https://github.com/kaldi-asr/kaldi/tree/master/egs/librispeech) but added noise and reverb augmentation. Originally the augmentation was intended to just help with background noise, but it improved accuracy a decent amount overall (possibly by 5xing the amount of data). Another acoustic model we considered was the model open-sourced by [Appen](https://github.com/Appen/UHV-OTS-Speech). The model showed a small increase in accuracy as well as improved robustness to noise, so we may switch to it in the future.

We have also done some preliminary work fine-tuning our Kaldi acoustic model but have yet to see a significant increase in accuracy. It's possible that there are still some potential wins here, but it might require a more exhaustive hyperparameter sweep or further tweaking of our fine-tuning scripts generally. We also suspect a lot of the potential gains from fine-tuning kaldi were captured by the interpolated language model mentioned above.

The transformer language model is an encoder-decoder model that takes the code context as input and uses it to provide a score for the transcript. Kaldi gives a clean decomposition of acoustic and language model scores, so we subtract the Kaldi language model score from the transformer language model score to avoid double counting. With this final score, we re-rank the top 10 results that come back from Kaldi. Anecdotally this seems to help close the gap between recall@1 and recall@10.

### Code Engine Architecture

While the speech engine turns audio into a ranked list of transcripts, many of those transcripts will describe source code, and so we need to translate natural language descriptions of code into actual source code. The code engine models solve this task.

Serenade has three transformer models:

* `contextual-language-model`, which scores the likelihood of an English transcript given the current context
* `transcript-parser`, which converts English transcripts into Serenade command markup
* `auto-style`, which converts code context and English transcripts into tokenized code

These problems can be thought of as translation tasks, converting sequences from a source language (English) to a target language (tokenized code or command markup; see the tokenizer section of the [Generating Data](generating-data.md) for more information).

Our models all have an encoder-decoder architecture, with different numbers of encoding and decoding layers for each model. The models all needed to be small enough to run Serenade Pro consumer hardware. We tried to sizes that were as large as possible while keeping the overall latency of Serenade under 300ms.

### Experiments with End-to-End Models

We also experimented with "end-to-end" frameworks like [wav2letter](https://github.com/flashlight/wav2letter) and [wenet](https://github.com/wenet-e2e/wenet). These types of models return a single score for a given transcript, as opposed to a decomposed score like Kaldi does. A popular way to integrate text data into these predictions is called shallow fusion, which takes a weighted sum of the language model score and the speech model score. Since the speech engine model already has an "internal language model", this is considered "double-counting", but it works in practice.

Shallow fusion is often an improvement over just the end-to-end model, but wasn't effective enough with our heavy reliance on text-data. When compared to swapping out language model in Kaldi, shallow fusion only showed a fraction of the gains.

While shadow-fusion alone didn’t work, our production Kaldi model allowed us to gather more data, and we re-evaluated these models with fine-tuning based on audio data. We mainly invested in fine-tuning the GigaSpeech WeNet model, but we saw similar results with the pre-trained models from wav2letter. We focused on WeNet because the framework was the most production-ready and it had a SOTA model trained on a diverse 10k hour dataset.

For this fine-tuning experiment, we labeled 10 hours of production audio data, making sure to avoid skewing towards any particular type of user, demographic, and use-case. The overall metrics were very close to those of our Kaldi model, but with better accuracy in non-coding contexts and worse accuracy coding contexts. The fine-tuned model had a slight edge with recall@1, but recall@5 was the same, and anecdotally this edge went away during transformer re-ranking. Intuitively this makes sense, since the WeNet model already has a transformer re-ranking step built-in.

We also tried using WeNet’s "contextual biasing" to account for rare words in the user's file, but no reasonable score boost was large enough to get those rare words to come through. This effectively meant that we weren't able to match the performance of the real-time G2P system that we have in the Kaldi system.

That said, we still think there’s a lot of promise here. For example, a lot of these issues might be solved by a larger set of (opt-in) in-domain training data. Another idea is to make architectural changes to the WeNet model to include context so that the re-ranking step isn’t necessary.

### Summary & Results

| Metric    | Value |
| --------- | ----- |
| Recall@1  | 0.913 |
| Recall@5  | 0.971 |
| Recall@10 | 0.977 |

The table above provides our most recent recall metrics. These are a step-function improvement in accuracy from the first phase of the system alone. At the same time, this is still just the beginning. We've identified few of the ways the system can be improved, and we're excited to see where things go from here.
