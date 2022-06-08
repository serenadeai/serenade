import { screen } from "electron";
import MainWindow from "./main";
import MiniModeWindow from "./mini-mode";
import RendererBridge from "../bridge";
import Settings from "../settings";
import System from "../execute/system";
import Window from "./window";

export default class RevisionBoxWindow extends Window {
  private promises: any = {};
  private previousApplication: string = "";
  private previousClipboardContents: string = "";

  constructor(
    private mainWindow: MainWindow,
    private miniModeWindow: MiniModeWindow,
    private settings: Settings,
    private system: System
  ) {
    super();
  }

  static async create(
    bridge: RendererBridge,
    mainWindow: MainWindow,
    miniModeWindow: MiniModeWindow,
    settings: Settings,
    system: System
  ): Promise<RevisionBoxWindow> {
    const instance = new RevisionBoxWindow(mainWindow, miniModeWindow, settings, system);
    await instance.createWindow(bridge, settings);
    return instance;
  }

  getEditorState(): Promise<any> {
    return new Promise((resolve) => {
      const id = Math.random().toString();
      this.promises[id] = resolve;
      this.send("getRevisionBoxState", { id });
    });
  }

  height(): number {
    return 300;
  }

  async hide(action: string = "") {
    if (!this.shown()) {
      return;
    }

    const state = await this.getEditorState();
    this.system.setClipboard(state.source);
    this.setEditorState({ source: "", cursor: 0, cursorEnd: 0 }, true);
    super.hide();

    if (this.previousApplication) {
      await this.system.focus(this.previousApplication);

      if (action == "send" || action == "close") {
        if (state.source) {
          await this.system.paste(this.previousApplication);
        } else {
          await this.system.pressKey("backspace");
        }

        if (action == "send") {
          await this.system.pressKey("enter");
        }

        this.system.setClipboard(this.previousClipboardContents);
      }
    }
  }

  onGetEditorState(state: { id: string; source: string; cursor: number; cursorEnd: number }) {
    if (!this.promises[state.id]) {
      return;
    }

    this.promises[state.id]({
      source: state.source,
      cursor: state.cursor,
      cursorEnd: state.cursorEnd,
    });

    this.promises[state.id] = undefined;
  }

  position(): { x: number; y: number } {
    const position = this.settings.getMinimizedPosition();
    if (
      !this.window ||
      !this.mainWindow.window ||
      !this.miniModeWindow.window ||
      this.mainWindow.shown() ||
      position == "window"
    ) {
      return super.positionNearMainWindow(this.mainWindow);
    }

    const bounds = this.window!.getBounds();
    const mainBounds = this.mainWindow.window!.getBounds();
    const miniModeBounds = this.miniModeWindow.window!.getBounds();
    const display = screen.getDisplayMatching(mainBounds).workArea;
    if (position == "top-left") {
      return {
        x: display.x + miniModeBounds.width,
        y: display.y,
      };
    } else if (position == "top-right") {
      return {
        x: display.x + display.width - miniModeBounds.width - bounds.width,
        y: display.y,
      };
    } else if (position == "bottom-right") {
      return {
        x: display.x + display.width - miniModeBounds.width - bounds.width,
        y: display.y + display.height - bounds.height,
      };
    }

    return {
      x: display.x + miniModeBounds.width,
      y: display.y + display.height - bounds.height,
    };
  }

  setEditorState(
    state: { source: string; cursor: number; cursorEnd: number },
    allEditors: boolean = true
  ) {
    this.send("setRevisionBoxState", { ...state, allEditors });
  }

  async show(action: string = "") {
    const app = await this.system.determineActiveApplication();
    const active = app.split(" ");
    this.previousApplication = active[active.length - 1];
    this.previousClipboardContents = this.system.getClipboard();

    let source = "";
    if (action == "clipboard") {
      source = await this.system.getClipboard();
    } else if (action == "selection") {
      await this.system.copy();
      source = await this.system.getClipboard();
    } else if (action == "all" && !this.system.isTerminal(app)) {
      await this.system.selectAll();
      await this.system.copy();
      source = await this.system.getClipboard();
    }

    this.setEditorState({ source, cursor: source.length, cursorEnd: 0 });
    super.show();
    setTimeout(() => {
      this.focus();
      this.send("focusRevisionBox", {});
    }, 200);
  }

  title(): string {
    return "Serenade Revision Box";
  }

  url(): string {
    return "revision";
  }

  width(): number {
    return 500;
  }
}
