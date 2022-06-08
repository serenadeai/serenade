#!/usr/bin/env python3

import os.path
import subprocess

root = os.path.join(os.path.dirname(os.path.realpath(__file__)), "..")

os.chdir(root)
env = {**os.environ, **{"npm_config_arch": "x64"}}
subprocess.check_call("npm install", env=env, shell=True)

os.chdir(
    os.path.join(
        root,
        "static",
        "custom-commands-server",
    )
)
subprocess.check_call("npm install", env=env, shell=True)

os.chdir(root)
subprocess.check_call(
    "npx uglifyjs static/custom-commands-server/serenade-custom-commands-server.js -c -m -o static/custom-commands-server/serenade-custom-commands-server.min.js",
    shell=True
)

os.makedirs("src/gen", exist_ok=True)
if not os.path.exists("src/gen/core.js") or os.path.getmtime(
    "../toolbelt/src/main/proto/core.proto"
) > os.path.getmtime("src/gen/core.js"):
    subprocess.check_call(
        "npx pbjs -t static-module -o src/gen/core.js -w commonjs ../toolbelt/src/main/proto/core.proto",
        shell=True
    )

    subprocess.check_call("npx pbts -o src/gen/core.d.ts src/gen/core.js", shell=True)

subprocess.check_call("npm run build", env=env, shell=True)
