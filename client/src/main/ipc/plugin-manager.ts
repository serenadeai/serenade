import { v4 as uuid } from "uuid";
import WebSocket from "ws";
import Settings from "../settings";
import { core } from "../../gen/core";
import { commandTypeToString } from "../../shared/alternatives";

export type Plugin = {
  id: string;
  app: string;
  lastHeartbeat: number;
  lastActive: number;
  websocket: WebSocket;
  match?: string;
  icon?: string;
};

export default class PluginManager {
  private plugins: Plugin[] = [];
  private promises: { [id: string]: any } = {};

  constructor(private settings: Settings) {
    setInterval(() => {
      this.removePluginsWhere((e) => Date.now() - e.lastHeartbeat > 5 * 60 * 1000);
    }, 60 * 1000);
  }

  private removePluginsWhere(predicate: (e: Plugin) => boolean) {
    let result: Plugin[] = [];
    for (let plugin of this.plugins) {
      if (predicate(plugin)) {
        plugin.websocket.close();
      } else {
        result.push(plugin);
      }
    }
    this.plugins = result;
  }

  private updatePlugin(websocket: WebSocket, id: string, app: string, match?: string, icon?: string) {
    const plugin = this.fromId(id);
    if (plugin) {
      plugin.websocket = websocket;

      // only update the icon if it has a value. an empty string clears the
      // custom icon
      if (icon != undefined) {
        plugin.icon = icon;
      }
    } else {
      if (app == "intellij") {
        app = "jetbrains";
      }
      this.settings.setPluginInstalled(app);
      this.plugins.push({
        id,
        app,
        websocket,
        match,
        icon,
        lastActive: Date.now(),
        lastHeartbeat: Date.now(),
      });
    }
  }

  fromApp(app: string): Plugin | null {
    // search for exact app name matches first, then look for each plugin's match field
    let result = this.plugins.filter((e) => e.app == app);
    if (result.length == 0) {
      result = this.plugins.filter((e) => e.match && app.match(new RegExp(e.match, "i")) != null);
    }

    if (result.length == 0) {
      return null;
    }

    result.sort((a, b) => b.lastActive - a.lastActive);
    return result[0];
  }

  fromId(id: string): Plugin | null {
    const result = this.plugins.filter((e) => e.id == id);
    if (result.length == 0) {
      return null;
    }

    return result[0];
  }

  resolve(callback: string, value: any) {
    if (!this.promises[callback]) {
      return;
    }

    this.promises[callback](value);
    delete this.promises[callback];
  }

  async sendResponseToApp(app: string, response: any): Promise<any> {
    // create a deep copy, since we're mutating state
    let data: any = JSON.parse(JSON.stringify(response));

    // replace enum numerical values with strings, so plugins don't need the protobuf
    if (data.alternatives) {
      for (let alternative of data.alternatives) {
        if (alternative.commands) {
          for (let command of alternative.commands) {
            command.type = commandTypeToString(command.type!);
          }
        }
      }
    }
    if (data.execute && data.execute.commands) {
      for (let command of data.execute.commands) {
        command.type = commandTypeToString(command.type!);
      }
    }

    // for backwards compatibility with previous format
    if (data.alternatives) {
      data.alternativesList = data.alternatives;
    }
    if (data.execute && data.execute.commands) {
      data.execute.commandsList = data.execute.commands;
    }

    const callback = uuid();
    const plugin = this.fromApp(app);
    if (!plugin || plugin.websocket.readyState !== 1) {
      return Promise.resolve(null);
    }
    const websocket = plugin!.websocket;
 
    websocket.send(JSON.stringify({ message: "response", data : {
      callback,
      response: data,
    } }));
    return new Promise((resolve) => {
      this.promises[callback] = resolve;
      setTimeout(() => {
        if (this.promises[callback]) {
          this.promises[callback](null);
          delete this.promises[callback];
          this.removeWebSocket(websocket);
        }
      }, 3000);
    });
  }

  sendCommandToApp(app: string, command: core.ICommand): Promise<any> {
    return this.sendResponseToApp(app, {
      execute: {
        commandsList: [command],
        commands: [command],
      },
    });
  }

  updateActive(websocket: WebSocket, id: string, app: string, match?: string, icon?: string) {
    this.updatePlugin(websocket, id, app, match, icon);
    this.fromId(id)!.lastActive = Date.now();
  }

  updateHeartbeat(websocket: WebSocket, id: string, app: string) {
    this.updatePlugin(websocket, id, app);
    this.fromId(id)!.lastHeartbeat = Date.now();
  }

  removeWebSocket(websocket: WebSocket) {
    this.removePluginsWhere((e) => e.websocket === websocket);
  }
}
