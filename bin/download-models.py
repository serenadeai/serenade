#!/usr/bin/env python3

import boto3
import click
import os.path
import requests
import shutil
import sys
import tarfile

sys.path.append(
    os.path.join(os.getenv("SERENADE_SOURCE_ROOT") or os.path.expanduser("~/serenade"), "scripts")
)
import serenade.config
import serenade.packages


def _flatten(data, prefix=""):
    result = []
    for k, v in data.items():
        key = f"{prefix}{k}/"
        if isinstance(v, dict):
            result.extend(_flatten(v, key).items())
        else:
            result.append((key[:-1], v))

    return dict(result)


@click.command()
@click.option(
    "--url",
    default=f"{serenade.config.base_url}/models",
    help="URL or S3 path where models are stored",
)
def main(url):
    """Download the models specified in config/models.yaml"""
    models = _flatten(serenade.config.models())
    for path, model in models.items():
        version_file = serenade.config.library_path("models", path, "v")
        if os.path.exists(version_file):
            with open(version_file) as f:
                version = f.read().strip()
                if version == model:
                    continue

        archive = serenade.config.library_path("models", path, f"{model}.tar.gz")
        if not os.path.exists(archive):
            shutil.rmtree(os.path.dirname(archive), ignore_errors=True)
            os.makedirs(os.path.dirname(archive), exist_ok=True)
            click.echo(f"Downloading model: {path}/{model}")
            serenade.packages.download(f"{url}/{path}/{model}.tar.gz", archive)

        with tarfile.open(archive, mode="r:gz") as f:
            def is_within_directory(directory, target):
                
                abs_directory = os.path.abspath(directory)
                abs_target = os.path.abspath(target)
            
                prefix = os.path.commonprefix([abs_directory, abs_target])
                
                return prefix == abs_directory
            
            def safe_extract(tar, path=".", members=None, *, numeric_owner=False):
            
                for member in tar.getmembers():
                    member_path = os.path.join(path, member.name)
                    if not is_within_directory(path, member_path):
                        raise Exception("Attempted Path Traversal in Tar File")
            
                tar.extractall(path, members, numeric_owner=numeric_owner) 
                
            
            safe_extract(f, os.path.dirname(archive))

        os.remove(archive)


if __name__ == "__main__":
    main()
