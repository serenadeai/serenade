#!/usr/bin/env python3

import click
import os.path
import sys

sys.path.append(
    os.path.join(os.getenv("SERENADE_SOURCE_ROOT") or os.path.expanduser("~/serenade"), "scripts")
)
import serenade.config
import serenade.repositories.github


@click.group()
def main():
    """Manage source code repositories for training"""
    pass


@main.command()
@click.argument("languages", nargs=-1)
@click.option("--url", default=f"{serenade.config.base_url}/repositories")
def create_small_repository(languages, url):
    """Create a small repository of sampled files"""
    for language in languages:
        serenade.repositories.create_small_repository(language, url)


@main.command()
@click.argument("languages", nargs=-1)
@click.option(
    "--limit",
    type=int,
    default=None,
    help="Starting limit (for continuing a previous crawl)",
)
@click.option(
    "--page-count",
    type=int,
    default=10,
    help="Number of pages of repositories to crawl",
)
@click.option("--page-size", type=int, default=100, help="Number of repositories per page")
@click.option("--rounds", type=int, default=20, help="Number of rounds to apply")
def crawl(languages, limit, page_count, page_size, rounds):
    """Crawl and download repositories from GitHub"""
    for language in languages:
        serenade.repositories.github.crawl(
            language,
            rounds,
            page_count,
            page_size,
            limit,
        )


@main.command()
@click.argument("languages", nargs=-1)
@click.option(
    "--small-repository/--no-small-repository",
    default=False,
    help="Download the small repository rather than the full repository",
)
@click.option("--url", default=f"{serenade.config.base_url}/repositories")
def download(languages, small_repository, url):
    """Download existing repositories"""
    for language in languages:
        serenade.repositories.download(language, url, small_repository)


if __name__ == "__main__":
    main()
