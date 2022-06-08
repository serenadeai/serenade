import Active from "../active";
import MainWindow from "./main";
import RendererBridge from "../bridge";
import Settings from "../settings";
import Window from "./window";

export default class LanguageSwitcherWindow extends Window {
  constructor(
    private active: Active,
    private bridge: RendererBridge,
    private mainWindow: MainWindow
  ) {
    super();
  }

  static async create(
    active: Active,
    bridge: RendererBridge,
    mainWindow: MainWindow,
    settings: Settings
  ): Promise<LanguageSwitcherWindow> {
    const instance = new LanguageSwitcherWindow(active, bridge, mainWindow);
    await instance.createWindow(bridge, settings);
    return instance;
  }

  height(): number {
    return 455;
  }

  maxHeight(): number {
    return this.height();
  }

  maxWidth(): number {
    return this.width();
  }

  position(): { x: number; y: number } {
    return this.positionNearMainWindow(this.mainWindow);
  }

  async show() {
    this.bridge.setState(
      {
        languageSwitcherLanguage: this.active.languageSwitcherLanguage,
      },
      [this]
    );

    super.show();
  }

  title(): string {
    return "Serenade Languages";
  }

  url(): string {
    return "languages";
  }

  width(): number {
    return 200;
  }
}
