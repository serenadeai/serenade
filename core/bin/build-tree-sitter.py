#!/usr/bin/env python3

import click
import os.path
import platform
import subprocess
import sys

sys.path.append(
    os.path.join(os.getenv("SERENADE_SOURCE_ROOT") or os.path.expanduser("~/serenade"), "scripts")
)
import serenade.config


@click.command()
@click.argument("output")
def main(output):
    """Update tree-sitter repositories and build shared library"""
    languages = serenade.config.languages()
    os.makedirs(serenade.config.library_path("tree-sitter"), exist_ok=True)
    dev_tree_sitter = bool(os.getenv("DEV_TREE_SITTER"))

    updated = False
    cwd = os.getcwd()
    for language, data in {**languages["languages"], **languages["libraries"]}.items():
        path = serenade.config.library_path(
            "tree-sitter", data.get("path", data["repository"].split("/")[-1])
        )
        if not os.path.exists(path):
            updated = True
            subprocess.check_call(
                f"git clone --recursive https://github.com/{data['repository']} {path}",
                shell=True,
            )
        os.chdir(path)
        head = subprocess.check_output("git rev-parse HEAD", shell=True).decode("utf-8").strip()

        if head != data["commit"]:
            updated = True
            subprocess.check_call(
                f"git reset --hard && git fetch && git checkout {data['commit']}",
                shell=True,
            )
    os.chdir(cwd)

    if (
        not updated
        and os.path.exists(output + (".dylib" if platform.system() == "Darwin" else ".so"))
        and not dev_tree_sitter
    ):
        sys.exit(0)

    paths = []
    prefix = serenade.config.library_path("tree-sitter")
    dev_prefix = serenade.config.library_path("dev-tree-sitter")
    for language, data in languages["languages"].items():
        path = data.get("path", data["repository"].split("/")[-1])
        if data.get("grammar"):
            path += f"/{data['grammar']}"

        path = (
            f"{dev_prefix}/{path}"
            if dev_tree_sitter and os.path.exists(f"{dev_prefix}/{path}")
            else f"{prefix}/{path}"
        )
        paths.append(path)

        subprocess.check_call(
            [
                serenade.config.source_path(
                    "grammarflattener",
                    "build",
                    "install",
                    "grammarflattener",
                    "bin",
                    "grammarflattener",
                ),
                path,
                language,
                serenade.config.source_path("core", "src", "main", "resources", "grammars"),
            ]
        )

    os.makedirs(os.path.dirname(output), exist_ok=True)
    subprocess.check_call(
        [
            f"{dev_prefix}/java-tree-sitter/build.py"
            if dev_tree_sitter and os.path.exists(f"{dev_prefix}/java-tree-sitter")
            else f"{prefix}/java-tree-sitter/build.py",
            "-o",
            output,
        ]
        + (["-a", "x86_64"] if platform.system() == "Darwin" else [])
        + paths,
    )


if __name__ == "__main__":
    main()
