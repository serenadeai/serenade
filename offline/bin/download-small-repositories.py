#!/usr/bin/env python3

import click
import os
import sys

sys.path.append(
    os.path.join(os.getenv("SERENADE_SOURCE_ROOT") or os.path.expanduser("~/serenade"), "scripts")
)
import serenade.config
import serenade.repositories


@click.command()
@click.option(
    "--url",
    default=f"{serenade.config.base_url}/repositories",
    help="URL or S3 path where repositories are stored",
)
def main(url):
    """Download repositories for tests"""
    for language in [
        "cplusplus",
        "csharp",
        "go",
        "html",
        "java",
        "javascript",
        "python",
        "ruby",
        "rust",
        "scss",
    ]:
        serenade.repositories.download(language, url, True)


if __name__ == "__main__":
    main()
