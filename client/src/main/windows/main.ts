import { app, screen, Menu, MenuItemConstructorOptions, Tray } from "electron";
import * as os from "os";
import * as path from "path";
import App from "../app";
import ChunkManager from "../stream/chunk-manager";
import MiniModeWindow from "./mini-mode";
import Metadata from "../../shared/metadata";
import RendererBridge from "../bridge";
import Settings from "../settings";
import Window from "./window";

export default class MainWindow extends Window {
  private tray?: Tray;
  private windowMovedTimeout?: NodeJS.Timeout;

  public expandedHeight: number = 500;
  public loggedInWidth: number = 275;
  public loggedOutHeight: number = 375;
  public loggedOutWidth: number = 600;
  public miniModeHeight: number = 86;
  public quitInProgress: boolean = false;
  public resizeCallbackEnabled: boolean = true;

  constructor(
    private app: App,
    private metadata: Metadata,
    private settings: Settings,
    private chunkManager: () => ChunkManager | undefined,
    private miniModeWindow: () => MiniModeWindow | undefined,
    private windowsToDestroy: () => (Window | Promise<Window> | undefined)[]
  ) {
    super();
  }

  static async create(
    app: App,
    bridge: RendererBridge,
    metadata: Metadata,
    settings: Settings,
    chunkManager: () => ChunkManager | undefined,
    miniModeWindow: () => MiniModeWindow | undefined,
    windowsToDestroy: () => (Window | Promise<Window> | undefined)[]
  ): Promise<MainWindow> {
    const instance = new MainWindow(
      app,
      metadata,
      settings,
      chunkManager,
      miniModeWindow,
      windowsToDestroy
    );
    await instance.createWindow(bridge, settings);
    return instance;
  }

  private saveBounds() {
    this.settings.setBounds(this.window!.getBounds());
  }

  private windowMoved() {
    if (!this.window) {
      return;
    }

    if (this.windowMovedTimeout) {
      clearTimeout(this.windowMovedTimeout);
    }

    const miniModeWindow = this.miniModeWindow();
    if (miniModeWindow) {
      miniModeWindow.hide();
    }

    this.windowMovedTimeout = global.setTimeout(() => {
      if (!this.window || !this.shown()) {
        return;
      }

      this.saveBounds();
      const miniModeWindow = this.miniModeWindow();
      if (miniModeWindow) {
        miniModeWindow.snapToMain();
        if (this.settings.getMiniMode()) {
          miniModeWindow.show();
        }
      }
    }, 500);
  }

  createMenu() {
    this.updateTray();
    Menu.setApplicationMenu(
      Menu.buildFromTemplate([
        {
          label: "Serenade",
          submenu: [
            { label: `Serenade ${this.metadata.version}`, enabled: false },
            { type: "separator" },
            {
              label: "Quit",
              accelerator: "CommandOrControl+Q",
              click: (_menuItem: any, _browserWindow: any, _event: any) => {
                this.quit();
              },
            },
          ],
        },
        {
          label: "Edit",
          submenu: [
            { role: "undo" },
            { role: "redo" },
            { type: "separator" },
            { role: "cut" },
            { role: "copy" },
            { role: "paste" },
            { role: "delete" },
            { role: "selectAll" },
          ],
        },
        {
          label: "View",
          submenu: [
            ...(process.env.NODE_ENV != "production"
              ? ([
                  { role: "reload" },
                  { role: "forceReload" },
                  { role: "toggleDevTools" },
                  { type: "separator" },
                ] as MenuItemConstructorOptions[])
              : []),
            { role: "resetZoom" },
            { role: "zoomIn" },
            { role: "zoomOut" },
            { type: "separator" },
            { role: "togglefullscreen" },
          ],
        },
        {
          label: "Window",
          submenu: [{ role: "minimize" }, { role: "zoom" }],
        },
      ])
    );
  }

  async createWindow(bridge: RendererBridge, settings: Settings) {
    this.createMenu();
    await super.createWindow(bridge, settings);
    this.resizeToCurrentMode();

    this.window?.on("close", async (e: any) => {
      // suppress errors that might occur while closing the app
      try {
        if (!this.quitInProgress && this.settings.getContinueRunningInTray()) {
          e.preventDefault();
          this.hide(false, true);
          return false;
        }

        for (const e of this.windowsToDestroy()) {
          if (e) {
            (await Promise.resolve(e)).destroy();
          }
        }

        app.quit();
      } catch (e) {}

      return true;
    });

    this.window?.on("minimize", (e: any) => {
      this.hide(true);
    });

    this.window?.on("move", (e: any) => {
      this.windowMoved();
    });

    this.window?.on("resize", (e: any) => {
      if (this.resizeCallbackEnabled) {
        this.windowMoved();
      }
    });

    this.window?.on("show", (e: any) => {
      this.show(true);
    });

    this.window?.on("restore", (e: any) => {
      this.show(true);
    });

    app.on("activate", (_event: any, hasVisibleWindows: boolean) => {
      if (!hasVisibleWindows) {
        this.show();
      }
    });
  }

  defaultClose(): boolean {
    return false;
  }

  height(): number {
    if (this.settings.getMiniMode()) {
      return this.miniModeHeight;
    }

    return Math.max(this.minHeight(), this.settings.getBounds().height);
  }

  hide(windowAlreadyHidden: boolean = false, removeFromDock: boolean = false) {
    if (!this.isShown) {
      return;
    }

    if (removeFromDock && app.dock) {
      this.window?.setSkipTaskbar(true);
      if (app.dock) {
        app.dock.hide();
      }
    }

    this.isShown = false;
    this.updateTray();

    if (!windowAlreadyHidden) {
      this.window?.minimize();
    }

    const miniModeWindow = this.miniModeWindow();
    if (miniModeWindow) {
      miniModeWindow.show();
      miniModeWindow.snapToMain();
    }

    this.app.clearAlternativesAndShowExamples();
  }

  main(): boolean {
    return true;
  }

  minHeight(): number {
    if (!this.settings.getToken()) {
      return this.loggedOutHeight;
    }

    if (this.settings.getMiniMode()) {
      return this.miniModeHeight;
    }

    return this.expandedHeight;
  }

  minWidth(): number {
    return this.settings.getToken() ? this.loggedInWidth : this.loggedOutWidth;
  }

  position(): { x: number; y: number } {
    let bounds = this.settings.getBounds();
    const currentDisplayArea = screen.getDisplayMatching(bounds).workArea;
    if (
      bounds.x < currentDisplayArea.x ||
      bounds.y < currentDisplayArea.y ||
      bounds.x > currentDisplayArea.x + currentDisplayArea.width ||
      bounds.y > currentDisplayArea.y + currentDisplayArea.height
    ) {
      bounds.x = currentDisplayArea.x;
      bounds.y = currentDisplayArea.y;
    }

    return { x: bounds.x, y: bounds.y };
  }

  resizeToCurrentMode(resetToDefault: boolean = false) {
    if (!this.window) {
      return;
    }

    this.window.setMinimumSize(this.minWidth(), this.minHeight());
    this.window.setMaximumSize(2000, this.settings.getMiniMode() ? this.miniModeHeight : 2000);
    this.window.setSize(
      resetToDefault ? this.minWidth() : this.width(),
      resetToDefault ? this.minHeight() : this.height(),
      true
    );
    this.saveBounds();
  }

  show(windowAlreadyShown: boolean = false) {
    if (this.isShown) {
      return;
    }

    this.window?.setSkipTaskbar(false);
    if (app.dock) {
      app.dock.show();
    }

    this.isShown = true;
    this.updateTray();
    if (!windowAlreadyShown && this.window) {
      if (this.window.isMinimized()) {
        this.window.restore();
      }

      this.window.showInactive();
    }

    const miniModeWindow = this.miniModeWindow();
    if (miniModeWindow) {
      if (this.settings.getMiniMode()) {
        miniModeWindow.show();
      } else {
        miniModeWindow.hide();
      }
    }

    this.app.clearAlternativesAndShowExamples();
  }

  quit() {
    this.quitInProgress = true;

    // suppress errors that might occur while closing the app
    try {
      app.quit();
    } catch (e) {}
  }

  title(): string {
    return "Serenade";
  }

  updateTray() {
    const mac = os.platform() == "darwin";
    let trayIcon = mac ? "/img/MacOSTrayTemplate.png" : "/img/Tray.png";
    if (this.chunkManager() && this.chunkManager()!.listening) {
      trayIcon = mac ? "/img/MacOSTrayListeningTemplate.png" : "/img/TrayListening.png";
    }

    trayIcon = path.join(__dirname, "..", "static", trayIcon);
    if (!this.tray) {
      this.tray = new Tray(trayIcon);
    }

    this.tray.setImage(trayIcon);
    let menu: MenuItemConstructorOptions[] = [];
    menu.push({ label: `Serenade ${this.metadata.version}`, enabled: false });
    menu.push({ type: "separator" });

    if (this.shown()) {
      menu.push({
        label: "Hide Serenade",
        click: (_menuItem: any, _browserWindow: any, _event: any) => {
          this.hide(false);
        },
      });
    } else {
      menu.push({
        label: "Show Serenade",
        click: (_menuItem: any, _browserWindow: any, _event: any) => {
          this.show();
        },
      });
    }

    menu.push({
      label: "Quit",
      click: (_menuItem: any, _browserWindow: any, _event: any) => {
        this.quit();
      },
    });

    this.tray!.setContextMenu(Menu.buildFromTemplate(menu));
    if (!mac) {
      this.tray.setTitle("Serenade");
      if (this.tray.listenerCount("click") == 0) {
        this.tray.on("click", () => {
          this.show();
        });
      }
    }
  }

  url(): string {
    return "";
  }

  width(): number {
    return Math.max(this.minWidth(), this.settings.getBounds().width);
  }
}
