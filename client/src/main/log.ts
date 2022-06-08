import * as fs from "fs-extra";
import * as os from "os";
import * as path from "path";
import Settings from "./settings";

export default class Log {
  private errorStream?: fs.WriteStream;
  private verboseStream?: fs.WriteStream;

  constructor(private settings: Settings) {}

  logError(e: any) {
    if (!this.errorStream) {
      this.errorStream = fs.createWriteStream(path.join(os.homedir(), ".serenade", "error.log"));
    }

    console.error(e);
    this.errorStream.write(`${e.stack}\n`);
  }

  logVerbose(message: string, includeDate: boolean = true) {
    if (!this.settings.getUseVerboseLogging()) {
      return;
    }

    if (!this.verboseStream) {
      this.verboseStream = fs.createWriteStream(
        path.join(os.homedir(), ".serenade", "verbose.log")
      );
    }

    const data = `${includeDate ? Date.now() + " " : ""}${message}`;
    console.log(data);
    this.verboseStream.write(`${data}\n`);
  }
}
