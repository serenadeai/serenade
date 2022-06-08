import builtins
import glob
import math
import os.path
import random
import re
import requests
import shutil
import subprocess
import time
import re
import zipfile

import serenade.config
import serenade.repositories


def _crawl_page(
    language,
    sort,
    round,
    page_size,
    page,
    limit,
    repository_count,
    start,
):
    max_size = 250000
    if limit is not None and limit <= 0:
        return (0, 0)

    if language == "default":
        q = ""
    else:
        query_languages = [language]
        if language == "cplusplus":
            query_languages = ["cpp"]
        elif language == "csharp":
            query_languages = ["c%23"]
        elif language == "scss":
            query_languages = ["scss", "html", "css"]
        elif language == "javascript":
            query_languages = ["javascript", "typescript"]
        q = "+".join(f"language:{e}" for e in query_languages)

    if limit:
        lower_limit = min(10 ** math.floor(math.log10(limit)), max(limit - 1000, 0))
        q += f" {sort}:{lower_limit}..{limit}"
    else:
        q += f" {sort}:>1"

    search_url = f"https://api.github.com/search/repositories?q={q}&sort={sort}&per_page={page_size}&page={page + 1}"
    print(f"\nListing {search_url} ...")
    response = requests.get(search_url, headers=_headers())

    if response.status_code != 200:
        print(response.text)
        return False

    data = response.json()
    for i, repo in enumerate(data["items"]):
        print(f"\nRuntime: {builtins.round((time.time() - start) / 60)} minutes")
        url = repo["html_url"]
        if sort == "stars":
            limit = repo["stargazers_count"]
        elif sort == "forks":
            limit = repo["forks_count"]

        if repo["size"] > max_size:
            print(f"Skipping (repository too big) {url}")
            continue

        # exclude repos that are forks, since they're probably duplicates
        if repo["fork"]:
            print(f"Skipping (fork) {url}")
            continue

        if url in serenade.repositories.excluded_repositories:
            print(f"Skipping (excluded repository) {url}")
            continue

        if repo["license"] is None:
            print(f"Skipping (no license) {url}")
            continue

        if repo["license"]["key"] not in serenade.repositories.permissive_licenses:
            print(f"Skipping (non-permissive license: {repo['license']['key']}) {url}")
            continue

        if any(e in url.lower() for e in serenade.repositories.excluded_keywords(language)):
            print(f"Skipping (excluded keyword) {url}")
            continue

        print(
            f"Downloading (Sort: {sort}, Round: {round + 1}, Page: {page + 1}, Repo: {i + 1}) {url} ..."
        )

        repository_count += 1
        archive = ""
        try:
            directory = f"{serenade.repositories.repositories_path}/{language}/{url.replace('https://', '').replace('http://', '').replace('/', '-')}"
            if os.path.exists(directory):
                print(f"Skipping (already processed) {url}")
                continue

            default_branch = repo["default_branch"]
            archive = f"{directory}/archive.zip"
            os.makedirs(directory, exist_ok=True)
            with requests.get(
                f"{url}/archive/{default_branch}.zip", allow_redirects=True, stream=True
            ) as r:
                r.raise_for_status()
                with open(archive, "wb") as f:
                    for chunk in r.iter_content(chunk_size=8192):
                        f.write(chunk)

            print(f"Extracting {url} ...")
            with zipfile.ZipFile(archive, "r") as f:
                f.extractall(directory)
        except:
            print(f"Skipping (download error) {url}")
            if os.path.exists(archive):
                os.remove(archive)
            continue

        if os.path.exists(archive):
            os.remove(archive)

        # remove files not matching the language or likely to have bad data
        non_ascii_path = False
        for file in glob.iglob(f"{directory}/**", recursive=True):
            # skip any repository with non-ascii filenames, which is unlikely to be English
            valid = serenade.repositories.is_ascii(file)
            if not valid:
                non_ascii_path = True
                break

            if valid and any(
                e in file.lower()[len(directory) :]
                for e in serenade.repositories.excluded_paths(language)
            ):
                valid = False

            if os.path.isfile(file) and valid:
                valid = os.path.getsize(file) < 10000000
                if serenade.repositories.include_file(language, file):
                    valid = True

                if valid:
                    data = ""
                    with open(file, "r") as f:
                        try:
                            data = f.read()
                        except:
                            valid = False

                    if valid and data and serenade.repositories.is_ascii(data):
                        # don't count docstrings against files, because those can be long
                        data = re.sub(
                            r"\"{3}(.*?)\"{3}",
                            "",
                            data,
                            flags=re.M | re.S,
                        )

                        # ignore files that have long strings or a lot of strings
                        strings = re.findall(r"\"(.+?)\"", data) + re.findall(r"'(.+?)'", data)
                        if len(strings) > 300 or any(len(e) > 300 for e in strings):
                            valid = False
                    else:
                        valid = False

            if not valid:
                if os.path.isfile(file):
                    os.remove(file)
                elif os.path.isdir(file):
                    shutil.rmtree(file)

        if non_ascii_path:
            print(f"Skipping (non-ASCII path) {url}")
            shutil.rmtree(directory)
            continue

        # glob doesn't include entries that start with .,
        # so just remove them all after processing the repository
        hidden = []
        total_size = 0
        for walk_root, walk_directories, walk_files in os.walk(directory):
            for walk_directory in walk_directories:
                if walk_directory.startswith("."):
                    hidden.append(os.path.join(walk_root, walk_directory))
            for walk_file in walk_files:
                walk_file_path = os.path.join(walk_root, walk_file)
                if walk_file.startswith("."):
                    hidden.append(walk_file_path)
                elif not os.path.islink(walk_file_path):
                    total_size += os.path.getsize(walk_file_path)

        if total_size > max_size * 1000:
            print(f"Skipping (extracted repository too big) {url}")
            shutil.rmtree(directory)
            continue

        for e in hidden:
            if os.path.isfile(e):
                os.remove(e)
            elif os.path.isdir(e):
                shutil.rmtree(e)

        try:
            if language == "default":
                _crawl_comments(repo, directory)
        except:
            print(f"Skipping (problem getting comments) {url}")
            continue

    return (limit, repository_count)


def _crawl_comments(repository, directory):
    if not os.path.exists(directory):
        os.makedirs(directory)

    for kind, url in {
        "issue_comments": repository["issue_comment_url"],
        "comments": repository["comments_url"],
    }.items():
        content = []
        for page in range(1, 11):
            print(f"Crawling {kind} page {page}")
            response = requests.get(
                url.replace("{/number}", f"?per_page=100&page={page}"),
                headers=_headers(),
            )

            if response.status_code != 200:
                print("Hit rate limit, sleeping")
                time.sleep(60)
                break

            data = response.json()
            if len(data) == 0:
                break

            for e in data:
                if e.get("body"):
                    content.append(e["body"])

        with open(f"{directory}/{kind}.md", "w") as f:
            f.write("\n".join(content))


def _headers():
    return {"Authorization": "Token " + serenade.config.services()["GITHUB_API_KEY"]}


def crawl(language, rounds, page_count, page_size, limit=None):
    sorts = ["stars"]
    repository_count = 0
    start = time.time()
    for sort in sorts:
        lowest_limit = limit if limit else 1e6
        for round in range(rounds):
            for page in range(page_count):
                result = _crawl_page(
                    language,
                    sort,
                    round,
                    page_size,
                    page,
                    limit,
                    repository_count,
                    start,
                )

                if not result:
                    print("Hit rate limit, sleeping")
                    time.sleep(60)
                    continue

                new_limit, repository_count = result
                lowest_limit = min(new_limit, lowest_limit)

            limit = max(0, lowest_limit)

    print(f"Found {repository_count} repositories")

    print("Removing empty directories...")
    subprocess.check_call(
        f"find {serenade.repositories.repositories_path}/{language} -type d -empty -delete",
        shell=True,
    )

    print("Creating archive...")
    subprocess.check_call(
        f"cd {serenade.repositories.repositories_path} && rm -f {language}.tar.gz && tar czf {language}.tar.gz {language}",
        shell=True,
    )
