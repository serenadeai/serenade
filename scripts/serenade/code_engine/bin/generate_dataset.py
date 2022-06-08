#!/usr/bin/env python3

import click
import shlex
import os
import subprocess
import sys

sys.path.append(
    os.path.join(os.getenv("SERENADE_SOURCE_ROOT") or os.path.expanduser("~/serenade"), "scripts")
)

import serenade.config
import serenade.code_engine
import serenade.repositories


def _run_corpusgen(args, optional_pipe_in=None):
    # optional_pipe_in should be something like the cat command on a directory, as a single string
    # args should be the remaining arguments to corpusgen, as a single string
    if optional_pipe_in:
        initial_pipe = subprocess.Popen(
            shlex.split(optional_pipe_in), stdout=subprocess.PIPE
        ).stdout
    else:
        initial_pipe = None

    corpusgen_command = f"{serenade.config.source_path('corpusgen', 'build', 'install', 'corpusgen', 'bin', 'corpusgen')} {args}"
    subprocess.check_call(
        corpusgen_command,
        stdin=initial_pipe,
        shell=True,
    )


def _split_file_list(prefix):
    language = prefix.split("/")[-1]
    with open(
        f"{prefix}/code-engine-{language}-file-list-errors.txt",
        "w",
    ) as error_output:
        for split in ["train", "test", "valid"]:
            with open(
                f"{prefix}/code-engine-{language}-file-list-{split}.txt",
                "w",
            ) as output:
                for filename in open(f"{prefix}/{language}-file-list.txt", "r"):
                    filename = filename.strip()
                    try:
                        # The filelist can potentially have badly formatted filenames due to spaces and other weird characters.
                        postfix = filename.split("github.com-")[1]
                        if postfix.startswith("a"):
                            if split == "test":
                                print(filename, file=output)
                        elif postfix.startswith("c"):
                            if split == "valid":
                                print(filename, file=output)
                        else:
                            if split == "train":
                                print(filename, file=output)
                    except:
                        print(filename, file=error_output)


def _generate_dataset(prefix, language, model, vocab_size, test_mode):
    if test_mode:
        count = 1
    elif language == "cplusplus":
        count = 15
    elif language == "javascript":
        count = 25
    elif model == "transcript-parser":
        count = 10
    else:
        count = 100

    os.makedirs(prefix, exist_ok=True)
    serenade.repositories.generate_file_list(language, count, prefix, test_mode)

    _split_file_list(prefix)

    # Note that this part will periodically throw errors due to parsing errors on the source files
    # due to special characters and otherwise unparsable code.
    for split in ["test", "valid", "train"]:
        _run_corpusgen(
            optional_pipe_in=f"cat {prefix}/code-engine-{language}-file-list-{split}.txt",
            args=f"{language} mapping {model} {prefix}/{language}-data-{split}.input {prefix}/{language}-data-{split}.output",
        )

    print("Extracting lexicon from the training set")
    lexicon_path = f"{prefix}/{language}_{model}_lexicon.txt"
    _run_corpusgen(
        optional_pipe_in=f"cat {prefix}/{language}-data-train.input {prefix}/{language}-data-train.output",
        args=f"{language} lexicon {model} {vocab_size} {lexicon_path}",
    )

    print("Adding tokens not in the lexicon as unknowns")
    # WARNING: This part will fail if this model/language pair does not exist in CodeEngineLexicon.java, since the corpusgen build won't work.
    for split in ["test", "valid", "train"]:
        _run_corpusgen(
            args=f"{language} unknowns {model} {lexicon_path} {prefix}/{language}-data-{split}.input {prefix}/{language}-data-{split}.output {prefix}/{split}.unk.input {prefix}/{split}.unk.output"
        )

    print("Generating a smaller validation set")
    subprocess.check_call(
        f"cat {prefix}/valid.unk.input | shuf -n 100000 --random-source={prefix}/valid.unk.input > {prefix}/valid.small.unk.input",
        shell=True,
    )
    subprocess.check_call(
        f"cat {prefix}/valid.unk.output | shuf -n 100000 --random-source={prefix}/valid.unk.input > {prefix}/valid.small.unk.output",
        shell=True,
    )


@click.command()
@click.option(
    "--model",
    type=click.Choice(["auto-style", "contextual-language-model", "transcript-parser"]),
    help="Type of model to generate training data for",
)
@click.option("--language", help="Programming language")
@click.option(
    "--test-mode/--no-test-mode",
    help="Turns on test mode and generates fewer data",
    default=False,
)
@click.option(
    "--url",
    help="URL for source code repositories",
    default=f"{serenade.config.base_url}/repositories",
)
def generate_dataset(model, language, test_mode, url):
    """Generate data for training a code engine model"""
    serenade.repositories.download(language, url, test_mode)
    if model == "auto-style":
        vocab_size = 5000
    elif model == "contextual-language-model":
        vocab_size = 10000
    elif model == "transcript-parser":
        vocab_size = 3000
    else:
        raise ValueError(f"Invalid model class ({model}) passed in!")

    os.chdir(serenade.config.source_path())
    prefix = serenade.code_engine.data_path(model, language)
    _generate_dataset(prefix, language, model, vocab_size, test_mode)
    print(f"Finished generating data. Data path for model training is {prefix}")


if __name__ == "__main__":
    generate_dataset()
