import { v4 as uuid } from "uuid";
import Active from "../active";
import API from "../api";
import App from "../app";
import Custom from "../ipc/custom";
import Executor from "../execute/executor";
import Log from "../log";
import MainWindow from "../windows/main";
import MiniModeWindow from "../windows/mini-mode";
import Microphone from "./microphone";
import RendererBridge from "../bridge";
import Settings from "../settings";
import Stream from "./stream";
import { Chunk, ChunkQueue } from "./chunk-queue";
import { core } from "../../gen/core";
import { commandTypeToString, isMetaResponse, isValidAlternative } from "../../shared/alternatives";

interface Request {
  requestType: "audio" | "editor" | "endpoint" | "initialize";
  audio?: Buffer;
  chunkId?: string;
  finalize?: boolean;
}

/**
 * When speaking, chunks go through the following states:
 * - onChunkStart: speech is detected, and the leading buffer is sent
 * - onAudio: speech is continuing
 * - onChunkEnd: chunk has ended, so send a finalized endpoint request
 *
 * We need to make sure to handle all of the following cases:
 * - speaking -> endpoint -> response -> silence
 * - speaking -> endpoint -> silence -> response
 * - speaking -> endpoint -> speaking -> response -> endpoint -> response -> silence
 * - speaking -> endpoint -> speaking -> response -> endpoint -> silence -> response
 * - start executing -> speaking -> stop executing -> silence
 * - start executing -> speaking -> silence -> stop executing
 * - revert -> speaking -> response -> silence
 * - revert -> speaking -> silence -> response
 */
export default class ChunkManager {
  private audioSizeForDelayedInitialize: number = 6;
  private buffer: Request[] = [];
  private buffering: boolean = false;
  private deadlineToMakeNewInitializeRequest: number = 0;
  private speaking: boolean = false;
  private timeToWaitBeforeClassifyingAsNoise: number = 200;
  private timeToWaitBeforeStartingNewCommand: number = 5000;

  listening: boolean = false;

  constructor(
    private active: Active,
    private api: API,
    private app: App,
    private bridge: RendererBridge,
    private chunkQueue: ChunkQueue,
    private custom: Custom,
    private executor: Executor,
    private log: Log,
    private mainWindow: MainWindow,
    private microphone: Microphone,
    private miniModeWindow: MiniModeWindow,
    private settings: Settings,
    private stream: Stream
  ) {}

  private async enqueue(request: Request, flush: boolean = true) {
    this.buffer.push(request);
    if (flush) {
      this.flush();
    }
  }

  private async flush() {
    if (this.buffering) {
      return;
    }

    while (this.buffer.length > 0) {
      const request = this.buffer.shift()!;
      if (request.requestType != "audio") {
        this.log.logVerbose(`Flushing ${request.requestType}`);
      }

      await this.send(request);
    }
  }

  private getLogEntry(alternative: core.ICommandsResponseAlternative): any {
    return {
      alternative_id: alternative.alternativeId,
      description: alternative.description,
      transcript: alternative.transcript,
      commands: (alternative.commands || []).map((c: any) => {
        let o: any = {
          type: commandTypeToString(c.type),
        };

        if (c.index > 0) {
          o.index = c.index;
        }

        return o;
      }),
    };
  }

  private getResponse(chunk: Chunk): any {
    if (chunk.reverted && chunk.revertedResponse) {
      return chunk.revertedResponse;
    }

    if (!chunk.reverted && chunk.response) {
      return chunk.response;
    }

    return undefined;
  }

  private async logResponse(response: core.ICommandsResponse) {
    let data: any = {
      token: this.settings.getToken(),
      endpoint_id: response.endpointId,
    };

    if (this.settings.getLogAudio() || this.settings.getLogSource()) {
      data.endpoint = this.settings.getStreamingEndpoint().id;
      data.chunk_ids = response.chunkIds;
      if (response.execute) {
        data.execute = this.getLogEntry(response.execute);
      }

      if (response.alternatives && response.alternatives.length > 0) {
        data.alternatives = response.alternatives.map((e: core.ICommandsResponseAlternative) =>
          this.getLogEntry(e)
        );
      }
    }

    this.api.logEvent(`client.stream.${response.final ? "final" : "partial"}_response`, {
      dt: Date.now(),
      data,
    });

    if (
      response.final &&
      this.settings.getStreamingEndpoint() &&
      this.settings.getStreamingEndpoint().id == "local" &&
      this.settings.getLogSource()
    ) {
      this.api.logLocalResponse(await this.active.getEditorState(), response);
    }
  }

  private reachedSilenceThreshold(chunk: Chunk): boolean {
    const response = this.getResponse(chunk);
    return (
      !!response &&
      chunk.silence >= this.settings.getExecuteSilenceThreshold() * response.silenceThreshold
    );
  }

  private async send(request: Request) {
    if (request.requestType == "initialize") {
      this.startBuffering();
      await this.stream.sendInitializeRequest();
      await this.stopBufferingAndFlush();
    } else if (request.requestType == "audio") {
      this.stream.sendAudioRequest(request.audio!, request.chunkId!);
    } else if (request.requestType == "editor") {
      await this.stream.sendEditorStateRequest();
    } else if (request.requestType == "endpoint") {
      await this.stream.sendEndpointRequest(request.chunkId!, request.finalize!);
    }
  }

  private shouldAppendToPrevious(response: core.ICommandsResponse): boolean {
    if (
      !this.active.pluginConnected() ||
      this.chunkQueue.size() < 2 ||
      this.active.dictateMode ||
      !response ||
      !response.alternatives ||
      response.alternatives.length == 0
    ) {
      return false;
    }

    const current = this.chunkQueue.getIndex(0);
    let previous = null;
    for (let i = 1; i < Math.min(this.chunkQueue.size(), 10); i++) {
      const chunk = this.chunkQueue.getIndex(i);
      if (chunk.executed || chunk.reverted) {
        previous = chunk;
        break;
      }
    }

    if (!previous) {
      return false;
    }

    let result =
      this.active.isFirstPartyEditor() &&
      !current.reverted &&
      Date.now() - Math.max(previous.reverted, previous.executed) <
        this.timeToWaitBeforeStartingNewCommand &&
      !isMetaResponse(response) &&
      response.alternatives.every(
        (e: core.ICommandsResponseAlternative) => !isValidAlternative(e)
      ) &&
      this.startsWithTextPrefix(this.getResponse(previous)) &&
      !this.startsWithTextPrefix(response);

    return !!result;
  }

  private startsWithTextPrefix(response: core.ICommandsResponse): boolean {
    return !!(
      response &&
      response.alternatives &&
      response.alternatives.length > 0 &&
      !!response.alternatives[0].transcript!.match(/^(add|change|dictate|insert|newline|type)/)
    );
  }

  async attemptToEvaluateChunk(chunk: Chunk): Promise<any> {
    if (this.chunkQueue.size() == 0) {
      this.log.logVerbose(`Attempt to evaluate chunk, but empty chunk queue`);
      return;
    }

    const current = this.chunkQueue.getIndex(0);
    this.log.logVerbose(
      `Attempt to evaluate chunk\n  chunk.id: ${chunk.id}\n  chunk.executed: ${
        chunk.executed
      }\n  chunk.reverted: ${
        chunk.reverted
      }\n  chunk.response: ${!!chunk.response}\n  chunk.silence: ${
        chunk.silence
      } (${this.reachedSilenceThreshold(chunk)})\n  current.id: ${
        current.id
      }\n  current.audioSize: ${current.audioSize}`
    );

    if (!chunk.reverted && chunk.executed) {
      this.log.logVerbose(`Not executing chunk ${chunk.id}: already executed`);
      return;
    }

    if (chunk.id != current.id) {
      this.log.logVerbose(`Not executing chunk ${chunk.id}: new chunk started`);
      return;
    }

    if (!chunk.reverted && !chunk.response) {
      this.log.logVerbose(`Not executing chunk ${chunk.id}: no final response yet`);
      return;
    }

    if (chunk.reverted && !chunk.revertedResponse) {
      this.log.logVerbose(`Not executing chunk ${chunk.id}: no reverted response yet`);
      return;
    }

    if (!this.reachedSilenceThreshold(chunk)) {
      this.log.logVerbose(`Not executing chunk ${chunk.id}: waiting for silence`);
      return;
    }

    // nothing to execute means noise, so send an initialize request
    if (
      !chunk.reverted &&
      chunk.response &&
      (!chunk.response.alternatives || chunk.response.alternatives.length == 0) &&
      !chunk.response.execute
    ) {
      this.log.logVerbose(`Not executing chunk ${chunk.id}: no alternatives or execute`);
      this.deadlineToMakeNewInitializeRequest =
        chunk.audioSize < this.audioSizeForDelayedInitialize
          ? Date.now() + this.timeToWaitBeforeClassifyingAsNoise
          : 0;
      return;
    }

    if (chunk.response && chunk.response.final && this.shouldAppendToPrevious(chunk.response)) {
      this.log.logVerbose(`Appending to previous ${chunk.id}`);
      chunk.reverted = Date.now();
      chunk.executed = 0;
      chunk.silence = 0;
      this.stream.sendAppendToPreviousRequest();
      this.enqueue({ requestType: "endpoint", chunkId: chunk.id, finalize: true });
      return;
    }

    this.log.logVerbose(`Setting partial to false`);
    this.bridge.setState(
      {
        partial: false,
      },
      [this.mainWindow, this.miniModeWindow]
    );

    this.log.logVerbose(`Executing chunk ${chunk.id}`);
    this.deadlineToMakeNewInitializeRequest = 0;
    chunk.executed = Date.now();
    this.startBuffering();
    await this.executor.execute(this.getResponse(chunk));
    await this.stopBufferingAndFlush();
  }

  async onCommandsResponse(response: core.ICommandsResponse) {
    const chunk = this.chunkQueue.getChunk(response.chunkId!);
    if (!chunk) {
      this.log.logVerbose(`No chunk found for ${response.chunkId!}`);
      return;
    }

    this.log.logVerbose(
      `Received ${response.final ? "final" : "partial"} response for ${chunk.id}: [${(
        response.alternatives || []
      )
        .map((e: any) => e.transcript)
        .join(", ")}]`
    );

    if (response.final) {
      response = await this.executor.postProcessResponse(response);
      if (chunk.reverted) {
        chunk.revertedResponse = response;
      } else {
        chunk.response = response;
      }
    }

    if (!this.shouldAppendToPrevious(response)) {
      const partial = !chunk.executed && (!response.final || !this.reachedSilenceThreshold(chunk));
      if (!isMetaResponse(response) && response.alternatives && response.alternatives.length > 0) {
        this.log.logVerbose(`Setting partial = ${partial}`);
        this.bridge.setState(
          {
            partial,
          },
          [this.mainWindow, this.miniModeWindow]
        );

        if (partial) {
          response = this.executor.truncateAlternativesIfNeeded(response);
          this.executor.showAlternativesIfPresent(response);
        }
      }
    }

    await this.logResponse(response);
    if (response.final) {
      await this.attemptToEvaluateChunk(chunk);
    }
  }

  onAudio(audio: any, silence: number) {
    const current = this.chunkQueue.getIndex(0);
    if (!current) {
      return;
    }
    current.silence = silence;
    if (this.speaking) {
      current.audioSize++;
      this.enqueue({ requestType: "audio", audio: Buffer.from(audio.buffer), chunkId: current.id });

      // we want to send non-final endpoint requests (aka partials) every so often when it seems like a long
      // command is being spoken, but we're not near the end of it (at which point an endpoint request
      // will be sent anyway), in order to trade off a responsive UI with not overloading the server
      if (
        current.audioSize > 0 &&
        current.audioSize % (current.audioSize < 66 ? 15 : 66) == 0 &&
        current.silence < 4
      ) {
        this.enqueue({ requestType: "endpoint", chunkId: current.id, finalize: false });
      }
    }

    let silenceThreshold: number;
    if (!current.reverted && current.response) {
      silenceThreshold = current.response.silenceThreshold!;
    } else if (current.reverted && current.revertedResponse) {
      silenceThreshold = current.revertedResponse.silenceThreshold!;
    } else {
      return;
    }
    if (
      current.silence == Math.ceil(this.settings.getExecuteSilenceThreshold() * silenceThreshold)
    ) {
      this.log.logVerbose(`Silence hit for ${current.id}`);
      this.attemptToEvaluateChunk(current);
    }
  }

  async onChunkEnd() {
    this.speaking = false;
    this.bridge.setState(
      {
        speaking: false,
      },
      [this.mainWindow]
    );

    // if the settings window is opened and then listening is started, we can get a chunk end
    // without a corresponding chunk start, so make sure a chunk actually exists
    const current = this.chunkQueue.getIndex(0);
    if (!current) {
      return;
    }

    this.log.logVerbose(`Chunk end for ${current.id}`);
    this.enqueue({ requestType: "editor" }, false);
    this.enqueue({ requestType: "endpoint", chunkId: current.id, finalize: true });
  }

  async onChunkStart(audio: any) {
    const id = uuid();
    this.chunkQueue.add(id);
    this.log.logVerbose(`Chunk start for ${id}`);

    if (!this.speaking) {
      this.bridge.setState(
        {
          speaking: true,
        },
        [this.mainWindow]
      );
    }

    // if one chunk comes down as noise, and another chunk is started within the threshold, then don't blow away
    // the server-side state, and keep going on the current command
    if (this.deadlineToMakeNewInitializeRequest < Date.now()) {
      this.deadlineToMakeNewInitializeRequest = Number.MAX_SAFE_INTEGER;
      this.enqueue({ requestType: "initialize" }, false);
    } else {
      this.enqueue({ requestType: "editor" }, false);
    }

    this.speaking = true;
    this.enqueue({ requestType: "audio", audio: Buffer.from(audio.buffer), chunkId: id });
  }

  startBuffering() {
    this.log.logVerbose("Buffering started");
    this.buffering = true;
  }

  async stopBufferingAndFlush() {
    this.log.logVerbose("Buffering stopped");
    this.buffering = false;
    await this.flush();
  }

  async toggle(listening?: boolean) {
    if (listening === undefined) {
      listening = !this.listening;
    }

    this.listening = listening;
    this.bridge.setState(
      {
        listening,
        partial: false,
        speakingVolume: 0,
        suggestion: "",
        statusText: listening ? "Listening" : "Paused",
      },
      [this.mainWindow, this.miniModeWindow]
    );

    this.log.logVerbose(`Toggling listening to ${listening}`);
    setTimeout(async () => {
      this.mainWindow.updateTray();
      if (listening) {
        this.startBuffering();
        this.microphone.register("chunk-manager", (data: any) => {
          if (data.event == "chunk_start") {
            this.onChunkStart(data.audio);
          } else if (data.event == "audio") {
            this.onAudio(data.audio, data.consecutiveSilence);
          } else if (data.event == "chunk_end") {
            this.onChunkEnd();
          }
        });

        await this.stream.connect(this, this.custom, this.executor);
        this.stopBufferingAndFlush();
      } else {
        this.microphone.unregister("chunk-manager");
        this.stream.sendDisableRequest();
        this.stream.disconnect();
        this.app.clearAlternativesAndShowExamples();
        this.chunkQueue.clear();
        this.deadlineToMakeNewInitializeRequest = 0;
        this.buffer = [];
        this.buffering = false;
        this.speaking = false;
      }
    }, 1);
  }
}
