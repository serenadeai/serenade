#!/usr/bin/env python3

import click
import collections
import enchant
import os.path
import requests
import shutil
import random
import shutil
import subprocess
import sys

from datetime import datetime
from collections import namedtuple

sys.path.append(
    os.path.join(os.getenv("SERENADE_SOURCE_ROOT") or os.path.expanduser("~/serenade"), "scripts")
)


import serenade.config
import serenade.repositories
import serenade.speech_engine

LanguageConfig = namedtuple(
    "LanguageConfig", ["file_list_limit", "sentence_sample_size", "use_small_repo"]
)

_sampling_language_config = {
    "bash": LanguageConfig(file_list_limit=100, sentence_sample_size=1000000, use_small_repo=False),
    "cplusplus": LanguageConfig(
        file_list_limit=5, sentence_sample_size=1000000, use_small_repo=False
    ),
    "csharp": LanguageConfig(file_list_limit=5, sentence_sample_size=1000000, use_small_repo=False),
    "dart": LanguageConfig(file_list_limit=5, sentence_sample_size=1000000, use_small_repo=False),
    "default": LanguageConfig(
        file_list_limit=100, sentence_sample_size=5000000, use_small_repo=False
    ),
    "go": LanguageConfig(file_list_limit=5, sentence_sample_size=1000000, use_small_repo=False),
    "html": LanguageConfig(file_list_limit=5, sentence_sample_size=1000000, use_small_repo=False),
    "java": LanguageConfig(file_list_limit=10, sentence_sample_size=5000000, use_small_repo=False),
    "javascript": LanguageConfig(
        file_list_limit=10, sentence_sample_size=5000000, use_small_repo=False
    ),
    "kotlin": LanguageConfig(file_list_limit=5, sentence_sample_size=1000000, use_small_repo=False),
    "python": LanguageConfig(
        file_list_limit=10, sentence_sample_size=5000000, use_small_repo=False
    ),
    "ruby": LanguageConfig(file_list_limit=5, sentence_sample_size=1000000, use_small_repo=False),
    "rust": LanguageConfig(file_list_limit=5, sentence_sample_size=1000000, use_small_repo=False),
    "scss": LanguageConfig(file_list_limit=5, sentence_sample_size=1000000, use_small_repo=False),
}

_test_language_config = {
    "java": LanguageConfig(file_list_limit=2, sentence_sample_size=5000, use_small_repo=True),
    "python": LanguageConfig(file_list_limit=2, sentence_sample_size=5000, use_small_repo=True),
}

_excluded_words_urls = [
    "https://raw.githubusercontent.com/coffee-and-fun/google-profanity-words/main/data/list.txt",
    "https://raw.githubusercontent.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words/master/en",
]

_letter_to_pronunciation = {
    "a": "ey1",
    "b": "b iy1",
    "c": "s iy1",
    "d": "d iy1",
    "e": "iy1",
    "f": "eh1 f",
    "g": "jh iy1",
    "h": "ey1 ch",
    "i": "ay1",
    "j": "jh ey1",
    "k": "k ey1",
    "l": "eh1 l",
    "m": "eh1 m",
    "n": "eh1 n",
    "o": "ow1",
    "p": "p iy1",
    "q": "k y uw1",
    "r": "aa1 r",
    "s": "eh1 s",
    "t": "t iy1",
    "u": "y uw1",
    "v": "v iy1",
    "w": "d ah1 b ah0 l y uw0",
    "x": "eh1 k s",
    "y": "w ay1",
    "z": "z iy1",
}


def _add_short_invalid(file):
    with open(file, "a") as f:
        for _ in range(2000000):
            f.write("<s> </s>\n")


def _apply_g2p(words):
    g2p_model = serenade.config.library_path("lm", "tmp", "g2p-model")
    missing_lexicon = serenade.config.library_path("lm", "tmp", "missing-lexicon")
    missing_words = serenade.config.library_path("lm", "tmp", "missing.txt")

    os.makedirs(g2p_model, exist_ok=True)
    with open(missing_words, "w") as f:
        f.write("\n".join(words).lower())

    subprocess.check_call(
        f'cp {serenade.config.library_path("models", "speech-engine", "g2p", "model.fst")} {os.path.join(g2p_model, "model.fst")} && \
        bash -c "source {serenade.config.library_path("kaldi", "tools", "env.sh")} && \
        rm -rf {missing_lexicon} && \
        cd {serenade.config.library_path("kaldi", "egs", "wsj", "s5")} && \
        ./steps/dict/apply_g2p_phonetisaurus.sh --nbest 1 {missing_words} {g2p_model} {missing_lexicon}"',
        shell=True,
    )

    result = []
    with open(os.path.join(missing_lexicon, "lexicon.lex"), "r") as f:
        for line in f:
            line = line.lower().split()
            result.append([line[0]] + line[2:])

    return result


def _create_const_arpa(lm, output):
    print(f"Creating const arpa {output} from {lm}...")

    subprocess.check_call(
        [
            serenade.config.source_path(
                "scripts",
                "serenade",
                "speech_engine",
                "training",
                "create-const-arpa.sh",
            ),
            serenade.config.library_path("kaldi"),
            lm,
            serenade.speech_engine.output_path(),
            output,
        ]
    )


def _filter_vocabulary(corpus_file, filtered_file, words_file):
    allow_list = set()
    with open(words_file, "r", encoding="utf-8") as words:
        for row in words:
            allow_list.add(row.split()[0])

    with open(corpus_file, "r") as corpus:
        with open(filtered_file, "w") as out_file:
            for line in corpus:
                words = [w for w in line.strip().split(" ") if w != ""]
                out_file.write(
                    " ".join(w if w in allow_list else "#nonterm:hint" for w in words) + "\n"
                )


def _finalize_corpus(languages):
    with open(f"{serenade.config.library_path('lm', 'corpus.txt')}", "w") as output:
        for language in languages:
            path = _language_path(language)
            with open(f"{path}/{language}-corpus.txt") as f:
                output.write(f"{f.read()}\n")


def _generate_corpus(language, sample_size=None):
    path = _language_path(language)

    print("Generating corpus from formatted text...")
    subprocess.check_call(
        f"cat {path}/{language}-file-list.txt | \
        {serenade.config.source_path('corpusgen', 'build', 'install', 'corpusgen', 'bin', 'corpusgen')} \
            {language} text {path}/{language}-corpus-unsampled.txt",
        shell=True,
    )

    if sample_size > 0:
        subprocess.check_call(
            f"cat {path}/{language}-corpus-unsampled.txt | shuf -n{sample_size} > {path}/{language}-corpus.txt",
            shell=True,
        )
    else:
        subprocess.check_call(
            f"mv {path}/{language}-corpus-unsampled.txt {path}/{language}-corpus.txt",
            shell=True,
        )


def _generate_kaldi_graph(lm, lexicon, acoustic_model):
    print(f"Generating Kaldi graph...")
    subprocess.check_call(
        [
            serenade.config.source_path(
                "scripts",
                "serenade",
                "speech_engine",
                "training",
                "lm-to-kaldi-model.sh",
            ),
            serenade.config.library_path("kaldi"),
            serenade.speech_engine.output_path(),
            lm,
            lexicon,
            serenade.config.source_path("scripts", "serenade", "speech_engine", "training"),
            acoustic_model,
        ]
    )


def _generate_lexicon(languages):
    lexicon = set()
    hand_labeled_lexicon = [
        s.split()
        for s in subprocess.check_output(
            f"cat {serenade.config.source_path('scripts', 'serenade', 'speech_engine', 'lexicon', '*')}",
            shell=True,
        )
        .decode(sys.stdout.encoding)
        .lower()
        .strip()
        .split("\n")
    ]

    lexicon.update(map(tuple, hand_labeled_lexicon))
    lexicon.update(map(tuple, _spelled_words_lexicon()))

    for language in languages:
        words = set(s[0] for s in lexicon)
        lexicon.update(
            map(
                tuple,
                _apply_g2p(
                    _missing_words(
                        words,
                        os.path.join(_language_path(language), f"{language}-corpus.txt"),
                        1000,
                    ),
                ),
            )
        )

    words = set(s[0] for s in lexicon)
    vocab = _vocab(serenade.config.library_path("lm", "corpus.txt"), words)
    lexicon = [list(s) for s in sorted(lexicon)]

    output = serenade.speech_engine.intermediate_path()
    os.makedirs(output, exist_ok=True)
    with open(os.path.join(output, "lexicon.txt"), "w") as f:
        print("!sil SIL", file=f)
        print("nspc SPN", file=f)
        for s in lexicon:
            print(" ".join([s[0]] + [p.upper() for p in s[1:]]), file=f)

    with open(os.path.join(output, "vocab.txt"), "w") as f:
        print("#nonterm:hint", file=f)
        for s in vocab:
            print(s, file=f)


def _generate_ngram_model(corpus, output, vocab=None, **kwargs):
    _require_srilm()
    print(f"Generating {kwargs['order']}-gram language model from {corpus}...")

    if vocab is not None:
        print(f"Using vocab from {vocab}")
    else:
        print(f"No vocab provided")

    args = ["ngram-count", "-text", corpus, "-order", kwargs["order"]]
    for i in range(2, kwargs["order"] + 1):
        key = f"gt{i}min"
        args += [f"-{key}", kwargs[key]]

    args += [
        "-kndiscount",
        "-interpolate",
    ]

    if vocab is not None:
        args += ["-vocab", vocab]
    args += ["-lm", output]

    subprocess.check_call(
        [
            serenade.config.library_path(
                "kaldi", "tools", "srilm", "bin", "i686-m64", "ngram-count"
            ),
            *[str(arg) for arg in args],
        ]
    )


def _language_path(language):
    return serenade.config.library_path("lm", f"{language}-data")


def _missing_words(allowlist, corpus_path, size):
    count = collections.Counter()
    total = 0
    with open(corpus_path, "r") as f:
        for row in f:
            for word in row.split():
                total += 1
                if word not in allowlist:
                    count[word] += 1

    print(f"Missing words for {corpus_path}")
    print("Most common missing:", count.most_common(100))
    print("Total missing:", sum(count.values()))
    print("Total words:", total)
    return [e[0] for e in count.most_common(size)]


def _mix_ngram_models(base_model, mix_model, output, **kwargs):
    _require_srilm()
    print(f"Mixing {kwargs['order']}-gram models from {base_model} and {mix_model}...")

    subprocess.check_call(
        [
            serenade.config.library_path("kaldi", "tools", "srilm", "bin", "i686-m64", "ngram"),
            *[
                str(arg)
                for arg in [
                    "-order",
                    kwargs["order"],
                    "-lm",
                    base_model,
                    "-mix-lm",
                    mix_model,
                    "-lambda",
                    kwargs["lambda_"],
                    "-write-lm",
                    output,
                ]
            ],
        ]
    )


def _require_srilm():
    if not os.path.exists(serenade.config.library_path("kaldi", "tools", "srilm", "bin")):
        print("You need to install SRILM by running kaldi/tools/install_srilm.sh")
        sys.exit(1)


def _spelled_words_lexicon():
    spelling_path = serenade.config.source_path("scripts", "serenade", "speech_engine", "spelling")

    spelled_words = (
        subprocess.check_output(
            f"cat {os.path.join(spelling_path, 'acronyms', '*')} {os.path.join(spelling_path, 'abbreviations', '*')}",
            shell=True,
        )
        .decode(sys.stdout.encoding)
        .strip()
        .split("\n")
    )

    result = []
    for spelled_word in spelled_words + list(_letter_to_pronunciation.keys()):
        spelled_word = spelled_word.strip().lower()
        if spelled_word.isalpha():
            result.append(
                tuple(
                    [spelled_word + "(spell)"]
                    + list(_letter_to_pronunciation[c] for c in spelled_word)
                )
            )

    return result


def _vocab(corpus_path, lexicon_words):
    english_dictionary = enchant.Dict("en_US")

    corpus_words = set()
    with open(corpus_path, "r", encoding="utf-8") as f:
        for row in f:
            corpus_words.update(row.strip().split())

    excluded_words = set()
    excluded_words.update(
        line.strip().lower()
        for url in _excluded_words_urls
        for line in requests.get(url).text.split("\n")
        if all(ord(c) < 128 for c in line) and " " not in line.strip()
    )

    result = set()
    for lexicon_word in lexicon_words:
        if not lexicon_word or lexicon_word in excluded_words:
            continue
        if lexicon_word in corpus_words or (
            lexicon_word.isalpha() and english_dictionary.check(lexicon_word)
        ):
            result.add(lexicon_word)

    return result


@click.group()
def cli():
    pass


@cli.command()
@click.option("--test-mode/--no-test-mode", help="Generate fewer data points", default=False)
def generate_dataset(test_mode):
    """Generate data for training"""
    path = serenade.config.library_path("lm")
    shutil.rmtree(path)
    os.makedirs(path)

    config = _test_language_config if test_mode else _sampling_language_config
    for language in config:
        language_config = config[language]
        language_path = _language_path(language)
        os.makedirs(language_path, exist_ok=True)
        if f"{language}-corpus.txt" in os.listdir(language_path):
            print(
                f"Training data for {language} already exists in {language_path}. Not regenerating."
            )
            continue

        serenade.repositories.generate_file_list(
            language,
            1 if test_mode else language_config.file_list_limit,
            language_path,
            test_mode,
        )
        _generate_corpus(language, language_config.sentence_sample_size)

    _finalize_corpus(list(config.keys()))


@cli.command()
@click.option("--test-mode/--no-test-mode", help="Generate fewer data points", default=False)
@click.option(
    "--url",
    help="URL for source code repositories",
    default=f"{serenade.config.base_url}/repositories",
)
def train_model(test_mode, url):
    """Train speech engine language model"""
    path = serenade.config.library_path("lm")
    os.makedirs(path, exist_ok=True)
    config = _test_language_config if test_mode else _sampling_language_config

    for language in list(config.keys()):
        serenade.repositories.download(language, url, test_mode)

    _generate_lexicon(list(config.keys()))

    _filter_vocabulary(
        serenade.config.library_path("lm", "corpus.txt"),
        serenade.speech_engine.intermediate_path("corpus-filtered.txt"),
        serenade.speech_engine.intermediate_path("vocab.txt"),
    )

    _add_short_invalid(
        serenade.speech_engine.intermediate_path("corpus-filtered.txt"),
    )

    _generate_ngram_model(
        serenade.speech_engine.intermediate_path("corpus-filtered.txt"),
        serenade.speech_engine.intermediate_path("lm-corpus-filtered.arpa"),
        serenade.speech_engine.intermediate_path("vocab.txt"),
        order=3,
        gt2min=2,
        gt3min=5,
    )

    _mix_ngram_models(
        serenade.speech_engine.intermediate_path("lm-corpus-filtered.arpa"),
        serenade.speech_engine.intermediate_path("user-corpus.arpa"),
        serenade.speech_engine.intermediate_path("mixed.arpa"),
        order=3,
        lambda_=0.6,
    )

    _generate_kaldi_graph(
        serenade.speech_engine.intermediate_path("mixed.arpa"),
        serenade.speech_engine.intermediate_path("lexicon.txt"),
        serenade.config.library_path("models", "speech-engine", "acoustic-model"),
    )

    _create_const_arpa(
        serenade.speech_engine.intermediate_path("mixed.arpa"),
        "const_arpa",
    )

    _generate_ngram_model(
        serenade.speech_engine.intermediate_path("corpus-filtered.txt"),
        serenade.speech_engine.intermediate_path("final.arpa"),
        serenade.speech_engine.intermediate_path("vocab.txt"),
        order=4,
        gt2min=2,
        gt3min=4,
        gt4min=5,
    )

    _mix_ngram_models(
        serenade.speech_engine.intermediate_path("final.arpa"),
        serenade.config.library_path(
            "models", "speech-engine", "user-language-model", "model.arpa"
        ),
        serenade.speech_engine.intermediate_path("final-mixed.arpa"),
        order=4,
        lambda_=0.6,
    )

    _create_const_arpa(
        serenade.speech_engine.intermediate_path("mixed.arpa"),
        "final_const_arpa",
    )


if __name__ == "__main__":
    cli()
