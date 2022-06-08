#!/usr/bin/env python3

import atexit
import click
import copy
import os.path
import psutil
import socket
import subprocess
import sys
import time

sys.path.append(
    os.path.join(os.getenv("SERENADE_SOURCE_ROOT") or os.path.expanduser("~/serenade"), "scripts")
)
import serenade.config

command_subprocesses = []
service_subprocesses = []
runnable_services = [
    "speech-engine",
    "code-engine",
    "core",
    "corpusgen",
    "grammarflattener",
    "offline",
    "auth",
]


def _kill(subprocesses):
    for parent in subprocesses:
        if psutil.pid_exists(parent.pid):
            parent = psutil.Process(parent.pid)
            if parent.is_running():
                for child in parent.children(recursive=True):
                    if child.is_running():
                        child.kill()
            parent.kill()


def _kill_all(signal=None, frame=None):
    _kill(service_subprocesses + command_subprocesses)
    sys.exit(1)


def _wait_for_port(port, host="localhost", timeout=15):
    start = time.perf_counter()
    while True:
        try:
            with socket.create_connection((host, port), timeout=timeout):
                break
        except OSError as e:
            time.sleep(0.01)
            if time.perf_counter() - start >= timeout:
                raise TimeoutError(f"{host}:{port} timed out") from e


@click.command()
@click.option(
    "--build/--no-build",
    default=True,
    help="Build services before running",
)
@click.option(
    "commands",
    "--command",
    multiple=True,
    help="Commands to run after services are running",
)
@click.option(
    "--config",
    default=serenade.config.source_path("config", "services.yaml"),
    help="Services config file",
)
@click.option("environment", "--env", help="Environment to run services in")
@click.option("--log", help="Log level")
@click.option(
    "run_services",
    "--services/--no-services",
    help="Run services before executing a command",
    default=True,
)
@click.option(
    "services",
    "--service",
    type=click.Choice(runnable_services),
    multiple=True,
    default=["speech-engine", "code-engine", "core"],
    help="Services to run",
)
@click.option(
    "tests",
    "--tests",
    multiple=True,
    help="Run a test command",
)
def main(build, commands, config, environment, log, run_services, services, tests):
    """Run Serenade services and commands"""

    global command_subprocesses, service_subprocesses

    if build:
        subprocess.check_call(
            f"cd {serenade.config.source_path()} && gradle installd",
            shell=True,
        )

    docker = (
        os.path.exists("/proc/1/cpuset")
        and "docker"
        in subprocess.check_output("cat /proc/1/cpuset", shell=True)
        .decode(sys.stdout.encoding)
        .strip()
    )
    if not environment:
        log = log or "info"
        environment = "docker" if docker else "local"
    log = log or "trace"

    env = {**os.environ, **serenade.config.services(environment)}
    if tests:
        log = "error"
        commands += tests
        services = list(set(services) - set(["speech-engine"]))
        env["SERENADE_TEST"] = "1"

    if run_services and services:
        git_directory = serenade.config.source_path(".git")
        version_file = serenade.config.source_path("VERSION")
        env["LOG_LEVEL"] = log
        if os.path.exists("/proc/1/cpuset"):
            env["CONTAINER_ID"] = (
                subprocess.check_output("cut -c9-20 < /proc/1/cpuset", shell=True)
                .decode(sys.stdout.encoding)
                .strip()
            )

        if os.path.exists(git_directory):
            env["GIT_COMMIT"] = (
                subprocess.check_output(
                    f"git --git-dir {git_directory} rev-parse HEAD",
                    shell=True,
                )
                .decode(sys.stdout.encoding)
                .strip()
            )
        elif os.path.exists(version_file):
            with open(version_file) as f:
                env["GIT_COMMIT"] = f.read().strip()

        for service in services:
            service_env = copy.deepcopy(env)
            service_env["SERVICE"] = service
            if service == "code-engine":
                service_subprocesses.append(
                    subprocess.Popen(
                        "code-engine/server/build/code-engine/serenade-code-engine",
                        env=service_env,
                        shell=True,
                    )
                )
            elif service == "speech-engine":
                service_subprocesses.append(
                    subprocess.Popen(
                        "speech-engine/server/build/speech-engine/serenade-speech-engine",
                        env=service_env,
                        shell=True,
                    )
                )
            else:
                service_subprocesses.append(
                    subprocess.Popen(
                        f"{service}/build/install/{service}/bin/{service}",
                        env=service_env,
                        shell=True,
                    )
                )

        if "speech-engine" in services:
            _wait_for_port(env["SPEECH_ENGINE_PORT"])
        if "code-engine" in services:
            _wait_for_port(env["CODE_ENGINE_PORT"])
        if "core" in services:
            _wait_for_port(env["CORE_PORT"])

    if commands:
        command_subprocesses = [
            subprocess.Popen(command, env=env, shell=True) for command in commands
        ]

        [e.wait() for e in command_subprocesses]
        _kill_all()
    else:
        [e.wait() for e in service_subprocesses]


if __name__ == "__main__":
    atexit.register(_kill_all)
    try:
        main()
    except KeyboardInterrupt:
        sys.exit(1)
