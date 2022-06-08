import { screen, shell, BrowserWindow } from "electron";
import os from "os";
import path from "path";
import MainWindow from "./main";
import RendererBridge from "../bridge";
import Settings from "../settings";
import { format as formatUrl } from "url";

export default abstract class Window {
  isShown: boolean = false;
  window?: BrowserWindow;

  abstract position(): { x: number; y: number };
  abstract title(): string;
  abstract url(): string;

  async createWindow(bridge: RendererBridge, settings: Settings): Promise<void> {
    return new Promise((resolve) => {
      let icon = "icon_512x512.png";
      if (os.platform() == "win32") {
        icon = "icon.ico";
      }

      const position = this.position();
      this.window = new BrowserWindow({
        icon: path.join(__dirname, "..", "static", "img", icon),
        titleBarStyle: "hidden",
        alwaysOnTop: true,
        show: this.main() || this.transparent(),
        x: position.x,
        y: position.y,
        width: this.width(),
        height: this.height(),
        minWidth: this.minWidth(),
        minHeight: this.minHeight(),
        maxWidth: this.maxWidth(),
        maxHeight: this.maxHeight(),
        title: this.title(),
        frame: false,
        skipTaskbar: !this.main(),
        resizable: !this.transparent(),
        transparent: this.transparent(),
        hasShadow: !this.transparent(),
        webPreferences: {
          contextIsolation: false,
          nodeIntegration: true,
        },
      });

      this.loadURL(this.url());
      this.window.setVisibleOnAllWorkspaces(true);
      this.window.setMenuBarVisibility(false);
      this.window.setAutoHideMenuBar(true);
      this.window.setAlwaysOnTop(true);
      if (!this.main()) {
        this.window.on("close", (e: any) => {
          e.preventDefault();
          this.hide();
        });
      }

      this.window.webContents.on("new-window", (e: any, url: string) => {
        e.preventDefault();
        shell.openExternal(url);
      });

      this.window.once("ready-to-show", () => {
        bridge.updateDarkMode([this]);
        resolve();
      });
    });
  }

  destroy() {
    this.window?.close();
    this.window?.destroy();
    this.isShown = false;
  }

  focus() {
    this.window?.focus();
    this.window?.focusOnWebView();
  }

  height(): number {
    return 500;
  }

  hide() {
    if (!this.shown()) {
      return;
    }

    this.window?.hide();
    this.isShown = false;
  }

  loadURL(url: string) {
    if (!this.window) {
      return;
    }

    if (process.env.NODE_ENV !== "production") {
      this.window.loadURL(`http://localhost:4000#/${url}`);
    } else {
      this.window.loadURL(
        formatUrl({
          pathname: path.join(__dirname, "renderer/index.html"),
          protocol: "file",
          slashes: true,
          hash: url,
        })
      );
    }
  }

  main(): boolean {
    return false;
  }

  maxHeight(): number | undefined {
    return undefined;
  }

  maxWidth(): number | undefined {
    return undefined;
  }

  minHeight(): number | undefined {
    return this.height();
  }

  minWidth(): number | undefined {
    return this.width();
  }

  positionNearMainWindow(mainWindow?: MainWindow): { x: number; y: number } {
    if (!mainWindow || !mainWindow.window) {
      return { x: 0, y: 0 };
    }

    const mainWindowBounds = mainWindow.window.getBounds();
    const currentDisplayArea = screen.getDisplayMatching(mainWindowBounds).workArea;
    let x = mainWindowBounds.x + mainWindowBounds.width;
    let y = mainWindowBounds.y;
    if (x + this.width() > currentDisplayArea.x + currentDisplayArea.width) {
      x = Math.max(currentDisplayArea.x, mainWindowBounds.x - this.width());
    }

    if (y + this.height() > currentDisplayArea.y + currentDisplayArea.height) {
      y = Math.max(
        currentDisplayArea.y,
        currentDisplayArea.y + currentDisplayArea.height - this.height()
      );
    }

    return { x, y };
  }

  send(message: string, data: any) {
    try {
      this.window?.webContents.send(message, data);
    } catch (e) {}
  }

  setSize(height?: number, width?: number) {
    if (!this.window) {
      return;
    }

    const setWidth = width !== undefined ? width : this.window.getSize()[0];
    const setHeight = height !== undefined ? height : this.window.getSize()[1];
    this.window?.setSize(setWidth, setHeight);
  }

  show() {
    const position = this.position();
    let bounds = this.window!.getBounds();
    bounds.x = position.x;
    bounds.y = position.y;

    this.window!.setBounds(bounds);
    this.window!.show();
    this.window!.focus();
    this.isShown = true;
  }

  shown(): boolean {
    return this.isShown;
  }

  transparent(): boolean {
    return false;
  }

  width(): number {
    return 500;
  }
}
