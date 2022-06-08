import { screen } from "electron";
import MainWindow from "./main";
import RendererBridge from "../bridge";
import Settings from "../settings";
import Window from "./window";

export default class MiniModeWindow extends Window {
  private currentHeight: number = 0;
  private ignoreMouseEvents: boolean = false;
  private offset: number = 5;

  constructor(
    private bridge: RendererBridge,
    private mainWindow: MainWindow,
    private settings: Settings
  ) {
    super();
  }

  private setIgnoreMouseEvents(ignoreMouseEvents: boolean) {
    if (this.ignoreMouseEvents == ignoreMouseEvents) {
      return;
    }

    this.ignoreMouseEvents = ignoreMouseEvents;
  }

  static async create(
    bridge: RendererBridge,
    mainWindow: MainWindow,
    settings: Settings
  ): Promise<MiniModeWindow> {
    const instance = new MiniModeWindow(bridge, mainWindow, settings);
    await instance.createWindow(bridge, settings);
    return instance;
  }

  async createWindow(bridge: RendererBridge, settings: Settings) {
    super.createWindow(bridge, settings);

    if (this.window?.setWindowButtonVisibility) {
      this.window?.setWindowButtonVisibility(false);
    }
  }

  height(): number {
    return this.currentHeight;
  }

  minHeight(): number {
    return 0;
  }

  minWidth(): number {
    return 275;
  }

  position(): { x: number; y: number } {
    return this.positionNearMainWindow(this.mainWindow);
  }

  setHeight(height: number) {
    // enforce a maximum size on the window so it scrolls
    height = Math.min(height, 700);
    height = Math.max(height, 39);
    if (!this.shown() || !this.window || height == this.height()) {
      return;
    }

    this.currentHeight = height;
    this.window.setSize(this.width(), this.height());
    this.snapToMain();

    // ensure there aren't any clickable artifacts from a height of zero, since windows enforces
    // a minimum window height of 39px
    this.setIgnoreMouseEvents(this.height() < 40);
  }

  shouldPlaceAboveMain(): boolean {
    const mainBounds = this.mainWindow.window?.getBounds();
    if (!mainBounds) {
      return false;
    }

    if (
      !this.mainWindow.shown() &&
      (this.settings.getMinimizedPosition() == "bottom-left" ||
        this.settings.getMinimizedPosition() == "bottom-right")
    ) {
      return true;
    }

    // since height can be zero when nothing is displayed, assume a minimum height to avoid
    // thinking we can fit below when we can't once alternatives come in
    const height = Math.max(300, this.height());
    const display = screen.getDisplayMatching(mainBounds).workArea;
    const fitsAbove = mainBounds.y - 2 * this.offset - height > display.y;
    const fitsBelow =
      mainBounds.y + mainBounds.height + 2 * this.offset + height < display.y + display.height;
    return fitsAbove && !fitsBelow;
  }

  show() {
    if (this.shown()) {
      return;
    }

    this.window?.showInactive();
    this.isShown = true;
  }

  snapToMain() {
    if (!this.mainWindow.window) {
      return;
    }

    const mainBounds = this.mainWindow.window!.getBounds();
    const display = screen.getDisplayMatching(mainBounds).workArea;
    let x: number = mainBounds.x;
    let y: number = this.shouldPlaceAboveMain()
      ? mainBounds.y - this.offset - this.height()
      : mainBounds.y + mainBounds.height + this.offset;

    if (!this.mainWindow.shown()) {
      const position = this.settings.getMinimizedPosition();
      if (position == "window") {
        y = this.shouldPlaceAboveMain() ? display.y + display.height - this.height() : mainBounds.y;
      } else if (position == "top-left") {
        x = display.x;
        y = display.y;
      } else if (position == "top-right") {
        x = display.x + display.width - mainBounds.width;
        y = display.y;
      } else if (position == "bottom-right") {
        x = display.x + display.width - mainBounds.width;
        y = display.y + display.height - this.height();
      } else if (position == "bottom-left") {
        x = display.x;
        y = display.y + display.height - this.height();
      }
    }

    if (this.window) {
      const bounds = this.window.getBounds();
      if (
        bounds.x != x ||
        bounds.y != y ||
        bounds.width != mainBounds.width ||
        bounds.height != this.height()
      ) {
        this.window.setBounds({
          x,
          y,
          width: mainBounds.width,
          height: this.height(),
        });

        this.bridge.setState(
          {
            miniModeBottomUp: this.shouldPlaceAboveMain(),
          },
          [this]
        );
      }
    }
  }

  title(): string {
    return "Serenade";
  }

  transparent(): boolean {
    return true;
  }

  url(): string {
    return "minimode";
  }

  width(): number {
    return this.minWidth();
  }
}
