import glob
import os
import random
import re
import subprocess
import shutil

import serenade.config
import serenade.packages

allowed_non_ascii = set([169, 180, 181, 228, 920, 931, 8216, 8217, 8220, 8221, 65292])

# repositories that passed heuristic checks but still messed things up for various reasons
excluded_repositories = set(
    [
        "https://github.com/NLPIR-team/NLPIR",
        "https://github.com/eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee/eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee",
        "https://github.com/matrix-org/synapse",
        "https://github.com/DefinitelyTyped/DefinitelyTyped",
        "https://github.com/NativeScript/NativeScript",
        "https://github.com/OUCMachineLearning/OUCML",
        "https://github.com/521xueweihan/HelloGitHub",
        "https://github.com/testerSunshine/12306",
        "https://github.com/fxsjy/jieba",
        "https://github.com/tonybeltramelli/pix2code",
        "https://github.com/Yorko/mlcourse.ai",
        "https://github.com/fluentpython/example-code",
        "https://github.com/bjmashibing/InternetArchitect",
        "https://github.com/DuGuQiuBai/Java",
        "https://github.com/liuawen/Learning-Java",
        "https://github.com/harvic/harvic_blg_share",
        "https://github.com/staresgroup/NJU-SE-GraduateEntrance",
        "https://github.com/sunnyandgood/bigdata",
        "https://github.com/leelovejava/cloud2020",
        "https://github.com/iqiyi/Qigsaw",
        "https://github.com/cangwang/modulebus",
        "https://github.com/wnagzihxa1n/CTF-Mobile",
        "https://github.com/kirill-grouchnikov/radiance",
        "https://github.com/shineware/KOMORAN",
        "https://github.com/h1287324781/coding2017",
        "https://github.com/loveincode/java-multi-thread-programming",
        "https://github.com/sunjincheng121/know_how_know_why",
        "https://github.com/habibi07/ImgEffects",
        "https://github.com/flankerhqd/JAADAS",
        "https://github.com/shihyu/DesignPatternExample",
        "https://github.com/szluyu99/Data_Structure_Note",
        "https://github.com/code-hunter/Answer",
        "https://github.com/raysonfang/jdk1.8-source-analysis",
        "https://github.com/doctorrm/WechatMiniProgram-shopping-mall",
        "https://github.com/chenshouyin/DevNote",
        "https://github.com/Hironsan/HotPepperGourmetDialogue",
        "https://github.com/scutan90/DeepLearning-500-questions",
        "https://github.com/the1812/Bilibili-Evolved",
        "https://github.com/dataarts/3-dreams-of-black",
        "https://github.com/jtleek/dataanalysis",
        "https://github.com/cwilso/MIDIDrums",
        "https://github.com/jtleek/modules",
        "https://github.com/dataarts/webgl-globe",
        "https://github.com/diercan/PSSC-2019",
        "https://github.com/StephenGrider/MeteorCasts",
        "https://github.com/walkingtree/sample-projects",
        "https://github.com/wizawu/tyrian",
    ]
)

permissive_licenses = [
    "0bsd",
    "afl-3.0",
    "apache-2.0",
    "bsd-2-clause",
    "bsd-3-clause",
    "bsd-3-clause-clear",
    "bsd-4-clause",
    "bsl-1.0",
    "cc-by-4.0",
    "cc-by-sa-4.0",
    "cc0-1.0",
    "ecl-2.0",
    "isc",
    "mit",
    "mit-0",
    "ncsa",
    "ofl-1.1",
    "postgresql",
    "unlicense",
    "upl-1.0",
    "wtfpl",
    "zlib",
]


repositories_path = serenade.config.library_path("repositories")
extensions = {
    "c": [".c", ".h"],
    "cplusplus": [".c", ".h", ".cc", ".cpp", ".hpp", ".cxx", ".hxx"],
    "csharp": [".cs"],
    "dart": [".dart"],
    "default": [".md"],
    "java": [".java"],
    "javascript": [".js", ".jsx", ".ts", ".tsx"],
    "go": [".go"],
    "html": [".html"],
    "kotlin": [".kt"],
    "python": [".py"],
    "ruby": [".rb"],
    "rust": [".rs"],
    "scss": [".css", ".scss"],
    "typescript": [".ts", ".tsx"],
}

languages = set(extensions.keys()) - {"c"} | {"bash"}


def include_file(language, file):
    if language in extensions:
        return any(file.endswith(e) for e in extensions[language])

    # bash scripts often have no file extension, so include them
    # (other tests will make sure this doesn't include binaries)
    if language == "bash":
        # exclude all uppercase files like COPYING and LICENSE
        if os.path.basename(file).upper() == os.path.basename(file):
            return False

        return file.endswith(".sh") or os.path.splitext(file)[1] == ""

    return False


def _get_sampled_file_list(language, count):
    repositories = os.listdir(f"{repositories_path}/{language}")
    cached = {}
    included = set()

    while len(included) < count:
        repository = random.randrange(len(repositories))
        if repository not in cached:
            cached[repository] = [
                e[len(f"{repositories_path}/{language}/") :]
                for e in glob.glob(
                    f"{repositories_path}/{language}/{repositories[repository]}/**/*",
                    recursive=True,
                )
                if os.path.isfile(e) and include_file(language, e)
            ]

        if len(cached[repository]) == 0:
            continue

        file = cached[repository][random.randrange(len(cached[repository]))]
        included.add(file)

    return list(included)


def create_small_repository(language, url):
    if not os.path.exists(f"{repositories_path}/{language}"):
        download(language, url)
    sampled_file_list = _get_sampled_file_list(language, 1000)
    small_repository = f"{repositories_path}/{language}/small-repository/{language}"
    subprocess.check_call(["rm", "-rf", small_repository])
    os.makedirs(small_repository, exist_ok=True)
    for file in sampled_file_list:
        destination = f"{small_repository}/{os.path.dirname(file)}"
        os.makedirs(destination, exist_ok=True)
        try:
            shutil.copy(
                f"{repositories_path}/{language}/{file}",
                destination,
            )
        except:
            pass
    subprocess.check_call(
        f"cd {repositories_path}/{language}/small-repository && tar czf {language}-small.tar.gz {language}",
        shell=True,
    )


def download(language, url, use_small_repository=False):
    os.makedirs(repositories_path, exist_ok=True)
    if language == "javascript":
        languages = ["javascript", "typescript"]
    else:
        languages = [language]

    for language in languages:
        local_name = f"{language}{'-small' if use_small_repository else ''}"
        if (
            local_name in os.listdir(repositories_path)
            and len(os.listdir(os.path.join(repositories_path, local_name))) > 0
        ):
            continue

        os.makedirs(os.path.join(repositories_path, local_name), exist_ok=True)
        file_name = f"{language}{'-small' if use_small_repository else ''}.tar.gz"
        local_file_name = f"{local_name}.tar.gz"
        serenade.packages.download(
            f"{url}/{language}/{file_name}",
            f"{repositories_path}/{local_file_name}",
        )

        os.system(
            f"cd {repositories_path} && tar xf {local_file_name} -C {local_name} --strip-components 1 && rm {local_file_name}"
        )


def generate_file_list(language, count, data_path, test_mode):
    result = []
    language_path = f"{repositories_path}/{language}{'-small' if test_mode else ''}"
    if len(os.listdir(language_path)) == 0:
        raise FileNotFoundError(f"No repositories found in {language_path}.")

    for repository in glob.glob(f"{language_path}/*"):
        included = 0
        files = glob.glob(f"{repository}/**", recursive=True)
        random.shuffle(files)
        for file in files:
            if included >= count:
                break

            if not is_ascii(file) or os.path.isdir(file):
                continue

            for e in excluded_paths(language):
                if e in file.lower():
                    continue

            with open(file, "r") as f:
                try:
                    if not is_ascii(f.read()):
                        continue
                except:
                    continue

            if len(re.findall("github[.]com-.*", file)) == 1:
                result.append(file)
                included += 1

    with open(
        serenade.config.library_path(data_path, f"{language}-file-list.txt"),
        "w",
    ) as f:
        for file in result:
            f.write(f"{file}\n")


def excluded_keywords(language):
    return set(
        e
        for e in [
            "adult",
            "algorithm",
            "blog",
            "book",
            "china",
            "chinese",
            "course",
            "cream",
            "dataset",
            "documentation",
            "docs",
            "icons",
            "jdk",
            "geoip",
            "homework",
            "mirror",
            "model",
            "naughty",
            "nsfw",
            "nude",
            "porn",
            "xx",
            "zoo",
            "zh",
            "zx",
        ]
        if e
        not in {
            "scss": ["blog", "docs"],
            "english": ["blog", "docs", "book", "course", "documentation"],
        }.get(language, {})
    )


def excluded_paths(language):
    return set(
        [
            ".git",
            ".gradle",
            ".idea",
            "bootstrap",
            "bower_components",
            "build",
            "bundle",
            "codepoint",
            "config",
            "dist",
            "docs",
            "dockerfile",
            "encoding",
            "font",
            "gemfile",
            "i18n",
            "icon",
            "img",
            "jquery",
            "lang",
            "lib",
            "locale",
            "lodash",
            "makefile",
            "min.js",
            "node_modules",
            "npm",
            "out",
            "pack.js",
            "releases",
            "store",
            "svg",
            "test",
            "typeface",
            "typing",
            "vagrantfile",
            "vendor",
            "yarn",
        ]
    ) - set(
        {
            "dart": ["lib"],
            "default": ["docs"],
            "scss": ["bootstrap", "docs", "font", "icon", "out"],
        }.get(language, [])
    )


def is_ascii(s):
    return all(ord(c) < 128 or ord(c) in allowed_non_ascii for c in s)
