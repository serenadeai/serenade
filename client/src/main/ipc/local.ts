import fetch from "electron-fetch";
import * as child_process from "child_process";
import * as fs from "fs-extra";
import * as os from "os";
import * as path from "path";
import * as semver from "semver";
import Log from "../log";
import MainWindow from "../windows/main";
import Metadata from "../../shared/metadata";
import RendererBridge from "../bridge";
import Settings from "../settings";
const commandExists = require("command-exists");

type RunnableService = "core" | "speech-engine" | "code-engine";

export default class Local {
  private processes: { [key in RunnableService]?: child_process.ChildProcess } = {};
  private logStreams: { [key in RunnableService]?: fs.WriteStream } = {};
  private pollingInterval?: NodeJS.Timeout;
  private started: boolean = false;

  constructor(
    private bridge: RendererBridge,
    private log: Log,
    private mainWindow: MainWindow,
    private metadata: Metadata,
    private settings: Settings
  ) {}

  private captureOutput(service: RunnableService, child: child_process.ChildProcess) {
    if (this.logStreams[service]) {
      return;
    }

    const stream = fs.createWriteStream(path.join(this.settings.path(), `${service}.log`));
    child.stdout!.pipe(stream);
    child.stderr!.pipe(stream);
    this.logStreams[service] = stream;
  }

  private killAll() {
    for (const e of Object.values(this.processes)) {
      if (e) {
        this.killProcess(e);
      }
    }

    for (const e of Object.values(this.logStreams)) {
      if (e) {
        e.end();
      }
    }

    this.processes = {};
    this.logStreams = {};
    this.pkill("serenade-speech-engine");
    this.pkill("serenade-code-engine");
    this.pkill("serenade-core");
    this.pkill("run-pro");
  }

  private killProcess(child?: child_process.ChildProcess) {
    if (child) {
      child.kill("SIGTERM");
    }
  }

  private pkill(name: string) {
    try {
      if (os.platform() == "win32") {
        child_process.spawnSync("wsl.exe", ["pkill", "-f", name]);
      } else {
        child_process.spawnSync("pkill", ["-f", name]);
      }
    } catch (e) {}
  }

  private stopPolling() {
    if (this.pollingInterval) {
      clearInterval(this.pollingInterval);
      this.pollingInterval = undefined;
    }
  }

  pollUntilRunning() {
    if (this.pollingInterval) {
      return;
    }

    this.bridge.setState(
      {
        localLoading: true,
      },
      [this.mainWindow]
    );

    this.pollingInterval = global.setInterval(async () => {
      // speech-engine is always the last to load, so poll until it's ready
      try {
        const response = await fetch("http://localhost:17202/api/status");
        if (await response.json()) {
          this.stopPolling();
          this.bridge.setState(
            {
              localLoading: false,
            },
            [this.mainWindow]
          );
        }
      } catch (e) {}
    }, 1000);
  }

  requiresNewerMac() {
    return os.platform() == "darwin" && semver.lt(os.release(), "20.0.0");
  }

  async requiresWsl() {
    return os.platform() == "win32" && !(await commandExists("wsl.exe"));
  }

  async start() {
    if (this.started || (await this.requiresWsl())) {
      return;
    }

    this.started = true;
    this.killAll();
    this.pollUntilRunning();
    let speechEngineModels = path.join(__dirname, "..", "static", "local", "speech-engine-models");
    let codeEngineModels = path.join(__dirname, "..", "static", "local", "code-engine-models");
    if (os.platform() == "win32") {
      speechEngineModels =
        "/" +
        child_process
          .spawnSync("wsl.exe", [
            "wslpath",
            "-a",
            "'" + speechEngineModels.replace("\\", "\\\\") + "'",
          ])
          .stdout.toString()
          .trim();
      codeEngineModels =
        "/" +
        child_process
          .spawnSync("wsl.exe", [
            "wslpath",
            "-a",
            "'" + codeEngineModels.replace("\\", "\\\\") + "'",
          ])
          .stdout.toString()
          .trim();
    }

    // here and below: WSL doesn't deal well with paths, so set the cwd to be the same as the binary
    this.processes["speech-engine"] = child_process.spawn(
      os.platform() == "win32" ? "wsl.exe" : "./run-pro",
      os.platform() == "win32" ? ["./run-pro", speechEngineModels] : [speechEngineModels],
      {
        cwd: path.join(__dirname, "..", "static", "local", "speech-engine"),
        shell: true,
        windowsHide: true,
      }
    );
    this.captureOutput("speech-engine", this.processes["speech-engine"]);

    this.processes["code-engine"] = child_process.spawn(
      os.platform() == "win32" ? "wsl.exe" : "./run-pro",
      os.platform() == "win32" ? ["./run-pro", codeEngineModels] : [codeEngineModels],
      {
        cwd: path.join(__dirname, "..", "static", "local", "code-engine"),
        shell: true,
        windowsHide: true,
      }
    );
    this.captureOutput("code-engine", this.processes["code-engine"]);

    this.processes["core"] = child_process.spawn(
      os.platform() == "win32" ? "wsl.exe" : "./run-pro",
      os.platform() == "win32" ? ["./run-pro"] : [],
      {
        cwd: path.join(__dirname, "..", "static", "local", "core", "bin"),
        shell: true,
        windowsHide: true,
      }
    );
    this.captureOutput("core", this.processes["core"]);
  }

  stop() {
    this.started = false;
    this.stopPolling();
    this.killAll();
    this.bridge.setState(
      {
        localLoading: false,
      },
      [this.mainWindow]
    );
  }
}
