#!/usr/bin/env python3

import os
import re
import time
import yaml
import click
import codecs
import shutil
import hashlib
import tarfile
import subprocess
import sentencepiece
import serenade.config
import serenade.code_engine


def _generate_config(
    beam_size,
    max_length,
    max_length_factor,
    work_path,
    use_n_best,
    use_word_scores,
    vocab_file,
):
    # All inputs need to be strings
    config = subprocess.check_output(
        [
            serenade.config.source_path(
                "code-engine", "server", "build", "code-engine", "config-generator"
            ),
            "--mini-batch",
            "64",
            "--maxi-batch",
            "100",
            "--beam-size",
            beam_size,
            "--max-length",
            max_length,
            "--max-length-factor",
            max_length_factor,
            "--cpu-threads",
            "2",
            "--n-best",
            use_n_best,
            "--models",
            f"{work_path}/model.npz",
            "--vocabs",
            f"{work_path}/{vocab_file }",
            f"{work_path}/{vocab_file}",
            "--word-scores",
            use_word_scores,
        ],
        universal_newlines=True,
    )

    with open(f"{work_path}/config.yml", "w") as file:
        for line in config:
            file.write(line)


def _create_vocab_to_id_token_map(vocab_in, out_path):
    _, encrypted_tokens, exposed_map = get_vocab_to_id_map(vocab_in)

    with open(os.path.join(out_path, "tokens.txt"), "w") as f:
        for token in encrypted_tokens:
            if "\n" in token:
                raise ValueError("Input tokens should not have newlines in them.")
            f.write(token + "\n")

    with open(os.path.join(out_path, "exposed_vocab.yml"), "w") as f:
        yaml.dump(exposed_map, f, sort_keys=False)


def _create_sentencepiece_to_id_token_map(model_in, out_path):
    sp_model = sentencepiece.SentencePieceProcessor(model_file=model_in)
    encrypted_tokens = []
    exposed_map = {}

    num = 0
    while True:
        try:
            token = sp_model.id_to_piece(num)
            # 0 and 1 are reserved for </s> and <unk> internal to marian for non-sentencepiece models
            if num <= 1:
                exposed_map[token] = num
            else:
                exposed_map[str(num)] = num
                encrypted_tokens.append(str(token))
        except IndexError:
            break
        num += 1

    with open(os.path.join(out_path, "tokens.txt"), "w") as f:
        for token in encrypted_tokens:
            if "\n" in token:
                raise ValueError("Input tokens should not have newlines in them.")
            f.write(token + "\n")

    with open(os.path.join(out_path, "exposed_vocab.yml"), "w") as f:
        yaml.dump(exposed_map, f, sort_keys=False)


def export(model, language, version):
    model_path = serenade.code_engine.model_path(model, language)
    working_path = serenade.config.library_path("tmp", "code-engine", model, language, version)

    # allow many more tokens than the original transcript, required for transcript parsing.
    max_length = "75"
    max_length_factor = "3"
    if model == "auto-style":
        vocab_file = "vocab.yml"
        use_n_best = "true"
        use_word_scores = "true"
        beam_size = "2"
    elif model == "transcript-parser":
        vocab_file = "vocab.yml"
        use_n_best = "true"
        use_word_scores = "true"
        beam_size = "3"
        max_length = "150"
        max_length_factor = "20"
    elif model == "contextual-language-model":
        # Models using sentencepiece instead should go down this path.
        max_length = "1000"
        vocab_file = "vocab.spm"
        use_n_best = "false"
        use_word_scores = "false"
        beam_size = "2"
    else:
        raise ValueError(f"Unsupported model type {model}")

    if os.path.isdir(working_path):
        shutil.rmtree(working_path)
    os.makedirs(working_path)
    unchanged_files = [
        vocab_file,
        f"{language}_{model}_lexicon.txt",
        "model.npz",
    ]
    for file in unchanged_files:
        shutil.copyfile(os.path.join(model_path, file), os.path.join(working_path, file))

    _generate_config(
        beam_size,
        max_length,
        max_length_factor,
        working_path,
        use_n_best,
        use_word_scores,
        vocab_file,
    )

    if vocab_file == "vocab.yml":
        _create_vocab_to_id_token_map(os.path.join(working_path, vocab_file), working_path)
    else:
        _create_sentencepiece_to_id_token_map(os.path.join(working_path, vocab_file), working_path)

    with open(os.path.join(working_path, "v"), "w") as f:
        f.write(version)

    export_files = unchanged_files + [
        "config.yml",
        "tokens.txt",
        "exposed_vocab.yml",
        "v",
    ]
    tarball = serenade.config.library_path(
        "models", "code-engine", "export", model, language, f"{version}.tar.gz"
    )
    os.makedirs(os.path.dirname(tarball), exist_ok=True)
    with tarfile.open(tarball, "w:gz") as tar:
        for file in export_files:
            tar.add(os.path.join(working_path, file), file.replace("-", "_"))

    print(f"Exported model to {tarball}")


def get_vocab_to_id_map(file):
    yaml_dict_line_regex = r"(.*): ([0-9]+)"
    quote_remove_regex = r"\"(.+)\""

    with codecs.open(file, "r", encoding="utf-8", errors="replace") as f:
        yaml_lines = f.read().splitlines()

    vocab = {}
    for line in yaml_lines:
        m = re.match(yaml_dict_line_regex, line)
        # Check for special cases -- remove surrounding quotes, convert \" to ".
        quotes_m = re.match(quote_remove_regex, m.group(1))
        if quotes_m:
            if quotes_m.group(1) == '\\"':
                vocab['"'] = m.group(2)
            else:
                vocab[quotes_m.group(1)] = m.group(2)
        else:
            vocab[m.group(1)] = m.group(2)

    encrypted_tokens = []
    exposed_map = {}

    for token, num in vocab.items():
        if int(num) <= 1:
            # 0 and 1 are reserved for </s> and <unk> internal to marian for non-sentencepiece models
            exposed_map[token] = num
        else:
            exposed_map[num] = num
            encrypted_tokens.append(token)

    return vocab, encrypted_tokens, exposed_map


@click.command()
@click.option(
    "--model",
    type=click.Choice(["auto-style", "contextual-language-model", "transcript-parser"]),
    help="Type of model to train",
)
@click.option("--language", help="Programming language")
def export_model(model, language):
    """Export model for usage in code engine"""
    version = hashlib.sha1(str(time.time()).encode("utf-8")).hexdigest()
    export(model, language, version)


if __name__ == "__main__":
    export_model()
