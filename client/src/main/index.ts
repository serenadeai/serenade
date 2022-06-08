import child_process from "child_process";
import { app, Notification } from "electron";
import { autoUpdater } from "electron-updater";
import * as fs from "fs";
import * as os from "os";
import * as path from "path";
import App from "./app";
import Metadata from "../shared/metadata";

let instance: App | null = null;
const lock = app.requestSingleInstanceLock();

if (os.platform() == "win32") {
  app.commandLine.appendSwitch("wm-window-animations-disabled");
}

// Workaround for latest react-router-dom to work in production webpack:
// https://github.com/ReactTraining/react-router/issues/6726#issuecomment-691701535
process.env["NODE_" + "ENV"] = "production";

process.on("uncaughtException", (e) => {
  if (instance && instance.log) {
    instance.log.logError(e);
  } else {
    console.error(e);
  }
});

if (!lock) {
  app.quit();
} else {
  app.on("second-instance", () => {
    if (instance) {
      instance.show();
    }
  });

  app.on("will-quit", () => {
    if (instance) {
      instance.quit();
    }
  });

  app.on("ready", async () => {
    // transparent windows need a slight delay to render correctly
    // https://github.com/electron/electron/issues/2170
    setTimeout(async () => {
      app.setName("Serenade");
      app.setAsDefaultProtocolClient("serenade");
      instance = await App.create();
      autoUpdater.checkForUpdatesAndNotify();
    }, 500);
  });
}
