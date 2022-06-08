#!/usr/bin/env python3

import os
import subprocess
import sys

os.chdir(os.path.join(os.path.dirname(os.path.realpath(__file__)), ".."))

if len(sys.argv) == 1:
    subprocess.check_call("python3 ./bin/build.py", shell=True)

env = {**os.environ, **{"npm_config_arch": "x64"}}
subprocess.check_call("npm run dev", env=env, shell=True)
