#!/usr/bin/env python3

import glob
import os
import sys
import yaml


def _bool(value):
    return str(bool(value)).lower()


def _format_source(source, cursor_indicator):
    return source.replace(cursor_indicator, "").replace("\n", "\\n").replace('"', '\\"')


def _generate_class(language):
    tests = {}
    with open(f"src/test/resources/{language}.yaml") as f:
        tests = yaml.load(f, Loader=yaml.SafeLoader)

    valid = True
    if tests.get("validate", False):
        valid = _validate(language, tests["tests"])

    filename = tests.get("filename", "file.py")
    suite = tests["suite"]
    methods = "\n".join(
        _generate_method(language, filename, name, test) for name, test in tests["tests"].items()
    )

    source = f"""
package core.gen;

import core.BaseServiceTest;
import core.gen.rpc.CommandType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class {suite}Test extends BaseServiceTest {{
{methods}
}}
"""

    path = f"src/test/java/core/gen/{suite}Test.java"
    os.makedirs("src/test/java/core/gen/", exist_ok=True)
    with open(path, "w") as f:
        f.write(source)

    return valid


def _generate_method(language, filename, name, test):
    if test.get("notSupportedInLanguage", False):
        return ""

    if not test.get("transcript") and not test.get("object") and not test.get("addAndType"):
        print(f"Invalid {language} test: {name}")
        sys.exit(1)

    cursor_indicator = test.get("cursor_indicator", "<>")
    before_source = _format_source(test.get("before", ""), cursor_indicator)
    before_cursor = max(0, test.get("before", "").find(cursor_indicator))
    after_source = _format_source(test.get("after", ""), cursor_indicator)
    filename = test.get("filename", filename)
    transcript = test.get("transcript", None)
    object = test.get("object", None)
    add_and_type = test.get("addAndType", False)
    description = _format_source(test.get("description", ""), cursor_indicator)
    allow_second_alternative = test.get("allowSecondAlternative", False)
    type = test.get("type", None)
    call = ""

    if transcript:
        # type means we're only looking at the type of the command
        if type:
            call = f'assertCommandType("{before_source}", {before_cursor}, "{transcript}", "{filename}", {type});'

        # description and no after source means test description only
        elif description and not after_source:
            call = f'assertDescription("{before_source}", {before_cursor}, "{transcript}", "{filename}", "{description}");'

        # two cursor indicators means we're selecting a range
        elif test["after"].count(cursor_indicator) == 2 or transcript.startswith("select"):
            after_cursor_start = test["after"].find(cursor_indicator)
            after_cursor_end = test["after"].find(cursor_indicator, after_cursor_start + 1) - len(
                cursor_indicator
            )
            if after_cursor_end < 0:
                after_cursor_end = len(test["after"].replace(cursor_indicator, ""))

            call = f'assertSelectedRange("{before_source}", {before_cursor}, "{transcript}", "{filename}", {after_cursor_start}, {after_cursor_end});'

        # one or zero cursor indicators means we're performing a diff
        else:
            after_cursor = test["after"].find(cursor_indicator)
            if after_cursor == -1:
                after_cursor = len(test["after"])

            call = f'assertStringsMatch("{before_source}", {before_cursor}, "{transcript}", "{filename}", "{after_source}", {after_cursor}, "{description}", {_bool(allow_second_alternative)});'

    elif object and test["after"].count(cursor_indicator) == 2:
        after_cursor_start = test["after"].find(cursor_indicator)
        after_cursor_end = test["after"].find(cursor_indicator, after_cursor_start + 1) - len(
            cursor_indicator
        )

        if after_cursor_end < 0:
            after_cursor_end = len(test["after"].replace(cursor_indicator, ""))

        call += f'\nassertSelectedRange("{before_source}", {before_cursor}, "select {object}", "{filename}", {after_cursor_start}, {after_cursor_end});'
        call += f'\nassertStringsMatch("{before_source}", {before_cursor}, "go to {object}", "{filename}", "{after_source}", {after_cursor_start}, "", {_bool(allow_second_alternative)});'

    elif add_and_type:
        after_cursor = test["after"].find(cursor_indicator)
        call += f'\nassertStringsMatch("{before_source}", {before_cursor}, "add {add_and_type}", "{filename}", "{after_source}\\n", {after_cursor}, "{description}", {_bool(allow_second_alternative)});'
        call += f'\nassertStringsMatch("{before_source}", {before_cursor}, "type {add_and_type}", "{filename}", "{after_source}", {after_cursor}, "{description}", {_bool(allow_second_alternative)});'

    return f"  @Test\n  public void {name}() {{\n    {call}\n  }}\n"


def _validate(language, tests):
    required = []
    with open(f"src/test/resources/required.yaml") as f:
        required = yaml.load(f, Loader=yaml.SafeLoader)["tests"]

    missing = []
    for required_test in required:
        if required_test not in tests:
            missing.append(required_test)

    if missing:
        print(f"\033[31mERROR\033[0m {language}")
        for e in missing:
            print(f"\033[33mMISSING\033[0m {e}")

    return len(missing) == 0


if __name__ == "__main__":
    valid = True
    os.chdir(os.path.dirname(os.path.realpath(__file__)) + "/../")
    for path in glob.iglob("src/test/resources/*.yaml"):
        if "required" in path:
            continue

        if not _generate_class(path[path.rindex("/") + 1 : path.index(".")]):
            valid = False

    if not valid:
        print("")
        print(f"==================================================")
        print(f"Some test files are invalid! See above for errors.")
        print(f"==================================================")
        print("")
        sys.exit(1)
