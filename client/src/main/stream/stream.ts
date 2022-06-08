import WebSocket from "ws";
import { v4 as uuid } from "uuid";
import Active from "../active";
import API from "../api";
import ChunkManager from "./chunk-manager";
import Custom from "../ipc/custom";
import Executor from "../execute/executor";
import Log from "../log";
import Settings from "../settings";
import { core } from "../../gen/core";

export default class Stream {
  private isConnected: boolean = false;
  private lastActivity: number = 0;
  private keepAliveTimeout?: NodeJS.Timeout;
  private loggingBuffer: Buffer[] = [];
  private coreSocket?: WebSocket;

  constructor(
    private active: Active,
    private api: API,
    private log: Log,
    private settings: Settings
  ) {
    // disconnect after an hour with no commands
    setInterval(() => {
      if (this.connected() && Date.now() > this.lastActivity + 3600000) {
        this.disconnect();
        return;
      }
    }, 300000);

    setInterval(() => {
      if (!this.connected()) {
        return;
      }

      this.log.logVerbose("Sending keepalive");
      this.send(this.coreSocket, {
        keepAliveRequest: {},
      });

      this.keepAliveTimeout = global.setTimeout(() => {
        this.disconnect();
      }, 3000);
    }, 30000);
  }

  private send(socket: WebSocket | undefined, data: any) {
    if (!this.connected() || !socket || socket.readyState != WebSocket.OPEN) {
      return;
    }

    socket!.send(core.EvaluateRequest.encode(core.EvaluateRequest.create(data)).finish());
  }

  async connect(chunkManager: ChunkManager, custom: Custom, executor: Executor): Promise<boolean> {
    this.lastActivity = Date.now();
    if (this.connected()) {
      return Promise.resolve(true);
    }

    return new Promise<boolean>((resolve, reject) => {
      this.coreSocket = new WebSocket(
        `${
          (process.env.ENDPOINT && process.env.ENDPOINT.startsWith("https")) ||
          (!process.env.ENDPOINT && this.settings.getStreamingEndpoint().id != "local")
            ? "wss"
            : "ws"
        }://${
          process.env.ENDPOINT
            ? process.env.ENDPOINT.replace("https://", "").replace("http://", "")
            : this.settings.getStreamingEndpoint().address
        }/stream/`
      );

      this.coreSocket.on("open", () => {
        this.log.logVerbose("Stream connected");
        this.isConnected = true;
        resolve(true);
      });

      this.coreSocket.on("message", (data: any) => {
        const response = core.EvaluateResponse.toObject(core.EvaluateResponse.decode(data), {
          defaults: true,
        });

        if (response.commandsResponse) {
          this.lastActivity = Date.now();
          if (response.commandsResponse.textResponse) {
            this.onTextCommandsResponse(custom, executor, response.commandsResponse);
          } else {
            this.onCommandsResponse(chunkManager, response.commandsResponse);
          }
        } else if (response.keepAliveResponse) {
          if (this.keepAliveTimeout) {
            clearTimeout(this.keepAliveTimeout);
          }
        }
      });

      this.coreSocket.on("close", () => {
        // an idle timeout might trigger close but not error, so reset the state to be safe
        // this callback is also triggered by toggling chunk manager
        this.disconnect();
      });

      this.coreSocket.on("error", (e) => {
        chunkManager.toggle(false);
        this.log.logError(e);
      });
    });
  }

  connected(): boolean {
    return this.isConnected;
  }

  disconnect() {
    if (!this.connected()) {
      return;
    }

    this.log.logVerbose("Stream disconnected");
    this.isConnected = false;
    this.coreSocket?.close();
    this.loggingBuffer = [];
    this.coreSocket = undefined;
  }

  onCommandsResponse(chunkManager: ChunkManager, response: core.ICommandsResponse) {
    chunkManager.onCommandsResponse(response);
  }

  async onTextCommandsResponse(
    custom: Custom,
    executor: Executor,
    response: core.ICommandsResponse
  ) {
    response = await executor.postProcessResponse(response);
    await executor.execute(response);
    custom.send("callback", {
      transcript: response.execute?.transcript,
    });
  }

  sendAppendToPreviousRequest() {
    this.send(this.coreSocket, {
      appendToPreviousRequest: {},
    });
  }

  sendAudioRequest(audio: Buffer, chunkId: string) {
    if (
      this.settings.getStreamingEndpoint() &&
      this.settings.getStreamingEndpoint().id == "local" &&
      this.settings.getLogAudio()
    ) {
      this.loggingBuffer.push(Buffer.from(audio));
    }

    this.send(this.coreSocket, {
      audioRequest: {
        audio: Buffer.from(audio),
        chunkId,
      },
    });
  }

  sendCallbackRequest(callbackRequest: core.ICallbackRequest) {
    this.log.logVerbose(`Sending callback request: ${callbackRequest.type}`);
    this.send(this.coreSocket, {
      callbackRequest,
    });
  }

  sendDisableRequest() {
    this.send(this.coreSocket, {
      disableRequest: {},
    });
  }

  async sendEditorStateRequest(clipboard: boolean = false, editorState?: any): Promise<any> {
    this.log.logVerbose("Sending editor state");
    if (!editorState) {
      editorState = await this.active.getEditorState(clipboard);
    }

    this.send(this.coreSocket, {
      editorStateRequest: {
        editorState,
      },
    });
  }

  async sendEndpointRequest(chunkId: string, finalize: boolean) {
    if (
      this.settings.getStreamingEndpoint() &&
      this.settings.getStreamingEndpoint().id == "local" &&
      this.settings.getLogAudio() &&
      this.loggingBuffer.length > 0 &&
      finalize
    ) {
      this.api.logLocalAudio(Buffer.concat(this.loggingBuffer), chunkId);
      this.loggingBuffer = [];
    }

    const endpointId = uuid();
    this.log.logVerbose(
      `Sending ${finalize ? "final" : "partial"} endpoint request for ${chunkId}`
    );
    this.send(this.coreSocket, {
      endpointRequest: {
        chunkId,
        finalize,
        endpointId,
      },
    });
  }

  async sendInitializeRequest(): Promise<any> {
    this.log.logVerbose("Sending initialize request");
    this.send(this.coreSocket, {
      initializeRequest: {
        editorState: await this.active.getEditorState(),
      },
    });
  }

  async sendTextRequest(text: string, includeAlternatives: boolean) {
    this.log.logVerbose(`Sending text request: ${text}, ${includeAlternatives}`);
    await this.sendInitializeRequest();
    this.send(this.coreSocket, {
      textRequest: {
        text,
        includeAlternatives,
      },
    });
  }
}
