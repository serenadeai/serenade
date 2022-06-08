#!/usr/bin/env python3

import os
import sys
import click
import shutil
import subprocess

sys.path.append(
    os.path.join(os.getenv("SERENADE_SOURCE_ROOT") or os.path.expanduser("~/serenade"), "scripts")
)
import serenade.config
import serenade.code_engine


def _run_marian(data_dir, model_dir, model, num_batches, gpus):
    if gpus > 0:
        gpu_list = " ".join([str(i) for i in range(gpus)])
        optimizer_delay = round(8 / gpus)
        compute_info = f"--devices {gpu_list} --optimizer-delay {optimizer_delay}"
    else:
        compute_info = "--cpu-threads 1"
    if model == "auto-style":
        vocab_postfix = "yml"
        maybe_dim_vocabs = ""
        maybe_sentencepiece_max_lines = ""
        enc_depth = 8
        dec_depth = 8
        label_smoothing = 0
    elif model == "contextual-language-model":
        vocab_postfix = "spm"
        maybe_dim_vocabs = "--dim-vocabs 10000 10000"
        maybe_sentencepiece_max_lines = "--sentencepiece-max-lines 10000000"
        enc_depth = 6
        dec_depth = 6
        label_smoothing = 0.1
    elif model == "transcript-parser":
        vocab_postfix = "yml"
        maybe_dim_vocabs = ""
        maybe_sentencepiece_max_lines = ""
        enc_depth = 6
        dec_depth = 1
        label_smoothing = 0
    else:
        raise ValueError("Invalid model class input")

    os.chdir(serenade.config.library_path("marian"))
    marian_command = f"""./build/marian \
    --model {model_dir}/model.npz --type transformer \
    --train-sets {data_dir}/train.unk.input {data_dir}/train.unk.output \
    --vocabs {model_dir}/vocab.{vocab_postfix} {model_dir}/vocab.{vocab_postfix} \
    --cost-type ce-mean-words \
    --dim-emb 96 \
    --transformer-dim-ffn 512 \
    --mini-batch-words 10000 \
    --early-stopping 10 \
    --save-freq 5000 --disp-freq 500 \
    --beam-size 2 \
    --log {model_dir}/train.log --valid-log {model_dir}/valid.log \
    --enc-depth {enc_depth} --dec-depth {dec_depth} \
    --transformer-heads 4 \
    --transformer-postprocess-emb d \
    --transformer-postprocess dan \
    --transformer-ffn-activation relu \
    --transformer-dropout 0.1 --label-smoothing {label_smoothing} \
    --learn-rate 0.001 --lr-warmup 4000 --lr-decay-inv-sqrt 4000 --lr-report \
    --optimizer-params 0.9 0.98 1e-09 --clip-norm 0 \
    --sync-sgd --seed 1111 \
    --tied-embeddings-all \
    --valid-metrics ce-mean-words perplexity \
    --valid-sets {data_dir}/valid.small.unk.input {data_dir}/valid.small.unk.output \
    --valid-script-path {serenade.config.library_path("marian", "scripts", "validate.sh")} \
    --valid-translation-output {data_dir}/train.output.output --quiet-translation \
    --valid-mini-batch 64 \
    --valid-freq 5000 \
    {compute_info} \
    --maxi-batch-sort none \
    --max-length 200 \
    --valid-max-length 200 \
    --after-batches {num_batches} {maybe_dim_vocabs} {maybe_sentencepiece_max_lines}
"""
    subprocess.check_call(marian_command, shell=True)


@click.group()
def cli():
    pass


@cli.command()
@click.option(
    "--model",
    type=click.Choice(["auto-style", "contextual-language-model", "transcript-parser"]),
    help="Type of model to train",
)
@click.option("--language", help="Programming language")
@click.option("--gpus", help="Number of GPUs available for training, 0 if CPU only", default=0)
@click.option(
    "--test-mode/--no-test-mode",
    help="Turns on test mode and runs fewer training iterations",
    default=False,
)
def train_model(model, language, gpus, test_mode):
    """Train code engine model"""
    data_path = serenade.code_engine.data_path(model, language)
    prefix = serenade.code_engine.model_path(model, language)

    os.makedirs(prefix, exist_ok=True)

    data_files = os.listdir(data_path)
    for training_file in [
        "train.unk.input",
        "train.unk.output",
        "valid.small.unk.input",
        "valid.small.unk.output",
        f"{language}_{model}_lexicon.txt",
    ]:
        if training_file not in data_files:
            raise FileNotFoundError(f"{training_file} missing from data path {data_path}")

    subprocess.check_call(
        f"cat {data_path}/train.unk.input {data_path}/train.unk.output | {serenade.config.library_path('marian', 'build', 'marian-vocab')} -m 5500 > {prefix}/vocab.yml",
        shell=True,
    )
    if test_mode:
        num_batches = 5000
    else:
        num_batches = 200000
    _run_marian(data_path, prefix, model, num_batches, gpus)

    try:
        shutil.copyfile(
            f"{data_path}/{language}_{model}_lexicon.txt",
            f"{prefix}/{language}_{model}_lexicon.txt",
        )
    except shutil.SameFileError:
        pass
    print(f"The trained model files are located at {prefix}")


if __name__ == "__main__":
    train_model()
