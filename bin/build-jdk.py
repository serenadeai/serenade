#!/usr/bin/env python3

import os
import shutil
import subprocess
import sys

os.chdir(os.path.dirname(os.path.realpath(__file__)) + "/../")
shutil.rmtree("build/distributions/jdk", ignore_errors=True)

# to get this list, run:
# jdeps --multi-release 14 -cp 'core/build/install/core/lib/*' --ignore-missing-deps --print-module-deps core/build/install/core/lib/core.jar
subprocess.check_call(
    [
        "jlink",
        "--no-header-files",
        "--no-man-pages",
        "--compress=2",
        "--strip-debug",
        "--add-modules",
        "java.base,java.compiler,java.desktop,java.management,java.naming,java.net.http,java.security.jgss,java.sql,jdk.unsupported",
        "--output",
        "build/distributions/jdk",
    ]
)

# replace symlinks with original files
subprocess.check_call(
    "cp -RL build/distributions/jdk/legal build/distributions/jdk/legal2 && rm -rf build/distributions/jdk/legal && mv build/distributions/jdk/legal2 build/distributions/jdk/legal && chmod -R 755 build/distributions/jdk/legal",
    shell=True,
)

subprocess.check_call(
    ["tar", "-C", "build/distributions", "-czf", "build/distributions/jdk.tar.gz", "jdk"]
)

shutil.rmtree("build/distributions/jdk", ignore_errors=True)
