import fetch from "electron-fetch";
import { core } from "../gen/core";
import Active from "./active";
import Log from "./log";
import MainWindow from "./windows/main";
import Metadata from "../shared/metadata";
import RendererBridge from "./bridge";
import Settings from "./settings";
import SettingsWindow from "./windows/settings";
import { Endpoint } from "../shared/endpoint";

export default class API {
  constructor(
    private active: Active,
    private bridge: RendererBridge,
    private log: Log,
    private mainWindow: MainWindow,
    private metadata: Metadata,
    private settings: Settings,
    private settingsWindow: () => Promise<SettingsWindow> | undefined
  ) {
    setInterval(() => {
      this.ping(this.settings.getStreamingEndpoint());
    }, 60000);
  }

  private async request(
    url: string,
    data: any,
    requestClass: any,
    responseClass: any,
    timeout?: number
  ): Promise<any> {
    if (!timeout) {
      timeout = 0;
    }

    const endpoint = process.env.ENDPOINT
      ? process.env.ENDPOINT
      : this.settings.getStreamingEndpoint().id == "local"
      ? "https://stream-us-west-2.serenade.ai"
      : `https://${this.settings.getStreamingEndpoint().address}`;

    try {
      const response = await fetch(`${endpoint}${url}`, {
        method: "POST",
        body: requestClass.encode(requestClass.create(data)).finish(),
        headers: {
          "Content-Type": "application/octet-stream",
        },
        timeout,
      });

      const buffer = await response.buffer();
      return responseClass.toObject(responseClass.decode(buffer), { defaults: true });
    } catch (e) {
      this.log.logError(e);
    }
  }

  logEvent(log: string, event: any) {
    if (this.settings.getDisableAnalytics()) {
      return;
    }

    return this.request(
      "/api/event",
      {
        log,
        token: this.settings.getToken(),
        event: JSON.stringify(event),
        clientIdentifier: this.metadata.identifier(
          this.active.app,
          core.Language[this.active.language]
        ),
        dt: Date.now(),
      },
      core.LogEventRequest,
      core.EmptyResponse,
      3000
    );
  }

  logLocalAudio(audio: Buffer, chunkId: string) {
    if (this.settings.getDisableAnalytics()) {
      return;
    }

    return this.request(
      "/api/audio",
      {
        chunkId,
        token: this.settings.getToken(),
        audio: Buffer.from(audio),
      },
      core.LogAudioRequest,
      core.EmptyResponse,
      3000
    );
  }

  logLocalResponse(editorState: any, response: core.ICommandsResponse) {
    if (this.settings.getDisableAnalytics()) {
      return;
    }

    return this.request(
      "/api/response",
      {
        editorState,
        response,
      },
      core.LogResponseRequest,
      core.EmptyResponse,
      3000
    );
  }

  async ping(endpoint: { address: string }, updateRenderer: boolean = true): Promise<number> {
    if (process.env.ENDPOINT) {
      return 1;
    }

    const unreachable = 1000;
    const url = endpoint.address.split(":")[0];
    try {
      const start = Date.now();
      await fetch(`https://${url}/api/status`, { method: "POST", timeout: unreachable });
      const latency = Math.round(Date.now() - start);
      if (updateRenderer) {
        this.bridge.setState({ latency }, [this.mainWindow, this.settingsWindow()]);
      }

      return latency;
    } catch (e) {
      return unreachable;
    }
  }

  async setBestEndpoint(endpoints: Endpoint[]) {
    if (process.env.ENDPOINT) {
      return;
    }

    if (
      this.settings.getStreamingEndpoint().id != "local" ||
      !endpoints.some((e: any) => e.id == this.settings.getStreamingEndpoint().id)
    ) {
      const filtered = endpoints.filter((e) => e.id != "local");
      if (filtered.length > 0) {
        const pings = await Promise.all(filtered.map((e) => this.ping(e, false)));
        const index = Math.max(0, pings.indexOf(Math.min(...pings)));
        this.settings.setStreamingEndpoint(filtered[index].id!);
        this.bridge.setState({ latency: pings[index] }, [this.mainWindow, this.settingsWindow()]);
      }
    }

    this.bridge.setState({ endpoint: this.settings.getStreamingEndpoint() }, [
      this.mainWindow,
      this.settingsWindow(),
    ]);
  }
}
