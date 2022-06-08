import { screen } from "electron";
import Bridge from "../bridge";
import MainWindow from "./main";
import Settings from "../settings";
import System from "../execute/system";
import Window from "./window";

export default class TextInputWindow extends Window {
  private previousApplication: string = "";
  private x?: number;
  private y?: number;

  constructor(private mainWindow: MainWindow, private settings: Settings, private system: System) {
    super();
  }

  static async create(
    bridge: Bridge,
    mainWindow: MainWindow,
    settings: Settings,
    system: System
  ): Promise<TextInputWindow> {
    const instance = new TextInputWindow(mainWindow, settings, system);
    await instance.createWindow(bridge, settings);
    return instance;
  }

  async createWindow(bridge: Bridge, settings: Settings) {
    super.createWindow(bridge, settings);

    if (this.window?.setWindowButtonVisibility) {
      this.window?.setWindowButtonVisibility(false);
    }

    this.window?.on("move", (e: any) => {
      this.x = this.window!.getBounds().x;
      this.y = this.window!.getBounds().y;
    });
  }

  height(): number {
    return 44;
  }

  async hide() {
    super.hide();

    if (this.previousApplication) {
      await this.system.focus(this.previousApplication);
    }
  }

  position(): { x: number; y: number } {
    const mainWindowPosition = this.mainWindow.position();
    let result = { x: 0, y: 0 };
    if (this.x !== undefined && this.y !== undefined) {
      result.x = this.x;
      result.y = this.y;
    } else {
      const bounds = screen.getDisplayNearestPoint(mainWindowPosition).workArea;
      result.x = Math.floor(bounds.x + (bounds.width - this.width()) / 2);
      result.y = Math.floor(bounds.y + 0.2 * bounds.height);
    }

    return result;
  }

  async show() {
    const app = await this.system.determineActiveApplication();
    const active = app.split(" ");
    this.previousApplication = active[active.length - 1];

    super.show();
    this.send("focusTextInput", {});
  }

  title(): string {
    return "Serenade Text Input";
  }

  url(): string {
    return "input";
  }

  width(): number {
    return 500;
  }
}
