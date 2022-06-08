#!/usr/bin/env python3

import glob
import os
import shutil
import sys
import yaml

cursor_sigil = "<>"


def format_source(source):
    return source.replace(cursor_sigil, "").replace("\n", "\\n").replace('"', '\\"')


def generate_class(tests):
    suite = tests["suite"]
    filename = tests.get("filename", "test.py")
    methods = "\n".join(
        generate_method(name, test, filename) for name, test in tests["tests"].items()
    )

    source = f"""
package corpusgen.gen;

import org.junit.jupiter.api.Test;
import corpusgen.mapping.BaseFullContextMappingGeneratorTest;
import corpusgen.mapping.Snippets;
import core.gen.rpc.Language;
import core.codeengine.Resolver;
import toolbelt.languages.LanguageDeterminer;

public class {suite}CommandsTest extends BaseFullContextMappingGeneratorTest {{

{methods}
}}
"""

    path = f"corpusgen/src/test/java/corpusgen/gen/{suite}CommandsTest.java"
    with open(path, "w") as f:
        f.write(source)

    os.system(f"prettier --write {path} > /dev/null 2>&1")


def generate_method(name, test, default_filename):
    test = dict(test)
    transcript = test.get("transcript", "")
    filename = test.get("filename", default_filename)
    if (
        test.get("skip", False)
        or test.get("skipMappingGeneratorTest", False)
        or "after" not in test
    ):
        return ""

    before_source = format_source(test.get("before", ""))
    after_source = format_source(test["after"])
    generation_source = after_source
    if "mappingGeneratorSource" in test:
        generation_source = format_source(test["mappingGeneratorSource"])
    before_cursor = max(test.get("before", "").find(cursor_sigil), 0)
    cursor = test.get("after", "").find(cursor_sigil)
    if cursor == -1:
        cursor = len(test["after"])
    if "addAndType" in test:
        transcript = "add " + test["addAndType"]
    elif not (transcript.startswith("add") or transcript.startswith("insert")):
        return ""

    return f"""
  @Test
  public void {name}() {{
    assertMappingsContain("{before_source}", {before_cursor}, "{generation_source}", language("{filename}"), "{transcript}", "{after_source}", {cursor});
  }}
    """


if __name__ == "__main__":
    os.chdir(os.path.dirname(os.path.realpath(__file__)) + "/../../")
    shutil.rmtree("corpusgen/src/test/java/corpusgen/gen")
    os.makedirs("corpusgen/src/test/java/corpusgen/gen/", exist_ok=True)
    for service in ["core", "corpusgen"]:
        for path in glob.iglob(service + "/src/test/resources/*.yaml"):
            module = path[path.rindex("/") + 1 : path.index(".")]
            if module in ["required"]:
                continue

            tests = {}
            with open(path) as f:
                tests = yaml.load(f, Loader=yaml.SafeLoader)

            if not tests.get("skipAllMappingGeneratorTests"):
                generate_class(tests)
