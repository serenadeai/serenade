import App from "../app";
import Local from "../ipc/local";
import Microphone from "../stream/microphone";
import MainWindow from "./main";
import MiniModeWindow from "./mini-mode";
import RendererBridge from "../bridge";
import Settings from "../settings";
import Window from "./window";

declare var __static: string;

export default class SettingsWindow extends Window {
  constructor(
    private app: App,
    private local: Local,
    private mainWindow: MainWindow,
    private microphone: Microphone,
    private miniModeWindow: MiniModeWindow,
    private settings: Settings
  ) {
    super();
  }

  static async create(
    app: App,
    bridge: RendererBridge,
    local: Local,
    mainWindow: MainWindow,
    microphone: Microphone,
    miniModeWindow: MiniModeWindow,
    settings: Settings
  ): Promise<SettingsWindow> {
    const instance = new SettingsWindow(
      app,
      local,
      mainWindow,
      microphone,
      miniModeWindow,
      settings
    );
    await instance.createWindow(bridge, settings);
    return instance;
  }

  height(): number {
    return 400;
  }

  hide() {
    super.hide();

    this.app.registerPushToTalk();
    this.microphone.unregister("settings");
  }

  position(): { x: number; y: number } {
    return this.positionNearMainWindow(this.mainWindow);
  }

  async show() {
    this.microphone.register("settings", () => {});
    this.app.sendAllSettings(this.local, this.microphone, this.miniModeWindow, this.settings, [
      this,
    ]);

    super.show();
  }

  title(): string {
    return "Serenade Settings";
  }

  url(): string {
    return "settings";
  }

  width(): number {
    return 450;
  }
}
