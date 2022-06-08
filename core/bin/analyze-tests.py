#!/usr/bin/env python3

import click
import glob
import os.path
import sys
import yaml

sys.path.append(
    os.path.join(os.getenv("SERENADE_SOURCE_ROOT") or os.path.expanduser("~/serenade"), "scripts")
)
import serenade.config


@click.command()
@click.argument("languages", nargs=-1)
def main(languages):
    """Analyze language-specific YAML tests for potential issues"""
    languages = [f"src/test/resources/{e}.yaml" for e in languages] or sorted(
        glob.glob("src/test/resources/*.yaml")
    )
    with open(f"src/test/resources/required.yaml") as r:
        required = yaml.load(r, Loader=yaml.SafeLoader)["tests"]
        for language in languages:
            with open(language) as f:
                data = yaml.load(f, Loader=yaml.SafeLoader)
                if not data.get("validate"):
                    continue

                tests = data["tests"]
                print(f"\033[36m{data['suite']}\033[0m")
                print("===")
                for name in required:
                    if name not in tests.keys():
                        print(f"\033[31mMISSING\033[0m {name}")
                for name, test in tests.items():
                    if test.get("notSupportedInLanguage"):
                        print(f"\033[33mSKIPPED\033[0m {name}")
                for name, test in tests.items():
                    if test.get("allowSecondAlternative"):
                        print(f"\033[35mLAX\033[0m {name}")
                for name, test in tests.items():
                    if name not in required:
                        print(f"\033[32mEXTRA\033[0m {name}")

            print("")


if __name__ == "__main__":
    os.chdir(serenade.config.source_path("core"))
    main()
