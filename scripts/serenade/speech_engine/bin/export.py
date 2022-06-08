#!/usr/bin/env python3

import click
import hashlib
import os
import shutil
import subprocess
import tarfile
import time

import serenade.config
import serenade.speech_engine


@click.command()
def export():
    version = hashlib.sha1(str(time.time()).encode("utf-8")).hexdigest()
    os.chdir(serenade.speech_engine.output_path())

    subprocess.check_call(
        "grep nonterm_bos data/lang/phones.txt | awk '{print $2}' > nonterm_phones_offset.int",
        shell=True,
    )

    with open("v", "w") as f:
        f.write(version)

    tarball = serenade.config.library_path("models", "speech-engine", "export", f"{version}.tar.gz")
    os.makedirs(os.path.dirname(tarball), exist_ok=True)
    with tarfile.open(tarball, "w:gz") as tar:
        tar.add("v", "v")
        tar.add("nonterm_phones_offset.int", "nonterm_phones_offset.int")
        tar.add("new/graph/phones/disambig.int", "graph/phones/disambig.int")
        tar.add("new/graph/words.txt", "graph/words.txt")
        tar.add("new/tree", "tree")
        tar.add("new/const_arpa", "lang/const_arpa")
        tar.add("new/final_const_arpa", "lang/final_const_arpa")
        tar.add("new/phones.txt", "lang/phones.txt")
        tar.add("new/final.mdl", "final.mdl")
        tar.add("new/graph/HCLG.fst", "graph/HCLG.fst")
        tar.add("new/ivector_extractor/final.dubm", "ivector_extractor/final.dubm")
        tar.add("new/ivector_extractor/final.ie", "ivector_extractor/final.ie")
        tar.add("new/ivector_extractor/final.mat", "ivector_extractor/final.mat")
        tar.add(
            "new/ivector_extractor/global_cmvn.stats",
            "ivector_extractor/global_cmvn.stats",
        )
        tar.add(
            "data/lang/phones/left_context_phones.int",
            "lang/phones/left_context_phones.int",
        )
        tar.add("data/local/dict/lexicon.txt", "lang/lexicon.txt")

    print(f"Exported model to {tarball}")


if __name__ == "__main__":
    export()
