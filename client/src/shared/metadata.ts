import * as os from "os";

export default class Metadata {
  version = "2.0.1";

  identifier(application: string, language: any): string {
    return JSON.stringify({
      os: {
        platform: os.platform(),
        release: os.release(),
      },
      version: this.version,
      application,
      language,
    });
  }
}
