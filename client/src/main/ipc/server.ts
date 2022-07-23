import WebSocket from "ws";
import Active from "../active";
import Custom from "./custom";
import MainWindow from "../windows/main";
import MiniModeWindow from "../windows/mini-mode";
import PluginManager from "./plugin-manager";
import RendererBridge from "../bridge";
import Stream from "../stream/stream";
import { core } from "../../gen/core";
import Log from "../log";

const maximumIconLength = 20000;

export default class IPCServer {
  private server: WebSocket.Server;

  constructor(
    private active: Active,
    private bridge: RendererBridge,
    private custom: Custom,
    private mainWindow: MainWindow,
    private miniModeWindow: MiniModeWindow,
    private pluginManager: PluginManager,
    private stream: Stream,
    private log: Log
  ) {
    this.server = new WebSocket.Server({ host: "localhost", port: 17373 });
    this.server.on("connection", (websocket) => {
      websocket.on("message", (message) => {
        const request = JSON.parse(typeof message === "string" ? message : message.toString());

        // protocol messages from plugins
        if (request.message == "active") {
          let icon = request.data.icon;

          const iconValid =
            icon == undefined ||
            (typeof icon == "string" &&
              icon.startsWith("data:") &&
              icon.length <= maximumIconLength);

          if (!iconValid) {
            this.log.logVerbose("Plugin provided an app icon that does not adhere to requirements");
            icon = undefined;
          }

          this.pluginManager.updateActive(
            websocket,
            request.data.id,
            request.data.app,
            request.data.match,
            icon
          );
        } else if (request.message == "callback") {
          this.pluginManager.resolve(request.data.callback, request.data.data);
        } else if (request.message == "disconnect") {
          this.pluginManager.removeWebSocket(websocket);
        } else if (request.message == "heartbeat") {
          this.pluginManager.updateHeartbeat(websocket, request.data.id, request.data.app);
        }
        // custom commands messages from custom commands servers
        if (request.message == "customCommands") {
          this.log.logVerbose(
            "Received " +
              request.data.commands.length +
              " commands, " +
              request.data.hints.length +
              " hints, " +
              request.data.words.length +
              " words"
          );
          this.active.customCommands = request.data.commands;
          this.active.customHints = request.data.hints;
          this.active.customWords = request.data.words;
        } else if (request.message == "error") {
          this.bridge.setState(
            {
              scriptError: request.data.error
                .split("\n")
                .filter((e: string) => !e.startsWith("    at"))
                .join("\n")
                .replace(/\n\s*\n/g, "\n"),
            },
            [this.mainWindow, this.miniModeWindow]
          );
        } else if (request.message == "evaluateInPlugin") {
          this.pluginManager.sendCommandToApp(
            this.active.app,
            new core.Command({
              type: core.CommandType.COMMAND_TYPE_EVALUATE_IN_PLUGIN,
              text: request.data.command,
            })
          );
        } else if (request.message == "keepalive") {
          this.custom.clearKeepAliveTimeout();
        } else if (request.message == "sendText") {
          this.stream.sendTextRequest(request.data.text, false);
        } else if (this.active.isFirstPartyBrowser() && this.active.pluginConnected()) {
          if (request.message == "domClick") {
            this.pluginManager.sendCommandToApp(
              this.active.app,
              new core.Command({
                type: core.CommandType.COMMAND_TYPE_DOM_CLICK,
                text: request.data.query,
              })
            );
          } else if (request.message == "domFocus") {
            this.pluginManager.sendCommandToApp(
              this.active.app,
              new core.Command({
                type: core.CommandType.COMMAND_TYPE_DOM_FOCUS,
                text: request.data.query,
              })
            );
          } else if (request.message == "domBlur") {
            this.pluginManager.sendCommandToApp(
              this.active.app,
              new core.Command({
                type: core.CommandType.COMMAND_TYPE_DOM_BLUR,
                text: request.data.query,
              })
            );
          } else if (request.message == "domCopy") {
            this.pluginManager.sendCommandToApp(
              this.active.app,
              new core.Command({
                type: core.CommandType.COMMAND_TYPE_DOM_COPY,
                text: request.data.query,
              })
            );
          } else if (request.message == "domScroll") {
            this.pluginManager.sendCommandToApp(
              this.active.app,
              new core.Command({
                type: core.CommandType.COMMAND_TYPE_DOM_SCROLL,
                text: request.data.query,
              })
            );
          }
        }

        if (request.message == "customCommands" || request.message == "error") {
          this.custom.connect(websocket);
        }
      });

      websocket.on("error", (e) => {
        console.log(e);
        this.pluginManager.removeWebSocket(websocket);
      });

      websocket.on("close", () => {
        this.pluginManager.removeWebSocket(websocket);
      });
    });
  }

  stop() {
    this.server.close();
  }
}
