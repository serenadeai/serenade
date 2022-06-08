import { ipcMain, shell, systemPreferences } from "electron";
import { autoUpdater } from "electron-updater";
import { v4 as uuid } from "uuid";
import * as path from "path";
import * as os from "os";
import Active from "./active";
import API from "./api";
import App from "./app";
import ChunkManager from "./stream/chunk-manager";
import Custom from "./ipc/custom";
import LanguageSwitcherWindow from "./windows/language-switcher";
import Local from "./ipc/local";
import MainWindow from "./windows/main";
import Microphone from "./stream/microphone";
import MiniModeWindow from "./windows/mini-mode";
import NUX from "./nux";
import PluginManager from "./ipc/plugin-manager";
import RendererBridge from "./bridge";
import RevisionBoxWindow from "./windows/revision-box";
import Settings from "./settings";
import SettingsWindow from "./windows/settings";
import Stream from "./stream/stream";
import TextInputWindow from "./windows/text-input";
import Window from "./windows/window";
import { core } from "../gen/core";

export default class RendererProcessEventHandlers {
  constructor(
    private active: Active,
    private app: App,
    private api: API,
    private bridge: RendererBridge,
    private chunkManager: ChunkManager,
    private custom: Custom,
    private revisionBoxWindow: RevisionBoxWindow,
    private local: Local,
    private mainWindow: MainWindow,
    private microphone: Microphone,
    private miniModeWindow: MiniModeWindow,
    private nux: NUX,
    private pluginManager: PluginManager,
    private settings: Settings,
    private stream: Stream,
    private languageSwitcherWindow: () => Promise<LanguageSwitcherWindow> | undefined,
    private settingsWindow: () => Promise<SettingsWindow> | undefined,
    private textInputWindow: () => Promise<TextInputWindow> | undefined
  ) {
    ipcMain.on("accessibilityPermission", () => {
      this.bridge.setState(
        {
          accessibilityPermission: systemPreferences.isTrustedAccessibilityClient
            ? systemPreferences.isTrustedAccessibilityClient(true)
            : true,
        },
        [this.mainWindow, this.miniModeWindow]
      );
    });

    ipcMain.on("closeLanguages", async (_event: any, _data: any) => {
      const languageSwitcherWindow = this.languageSwitcherWindow();
      if (languageSwitcherWindow) {
        (await languageSwitcherWindow).hide();
      }
    });

    ipcMain.on("forward", (_event: any, data: any) => {
      this.pluginManager.sendResponseToApp(this.active.app, data);
    });

    ipcMain.on("generateToken", (_event: any, data: any) => {
      this.settings.setToken(uuid());
    });

    ipcMain.on("hideTextInput", async () => {
      const textInputWindow = this.textInputWindow();
      if (textInputWindow) {
        (await textInputWindow).hide();
      }
    });

    ipcMain.on("loadTutorial", (_event: any, data: { name: string; resize?: boolean }) => {
      this.nux.load(data.name);
      this.resetNux();
      if (data.resize) {
        this.mainWindow.resizeToCurrentMode(true);
      }

      this.bridge.setState({ loggedIn: true }, [this.mainWindow, this.miniModeWindow]);
    });

    ipcMain.on("microphonePermission", () => {
      if (systemPreferences.askForMediaAccess !== undefined) {
        systemPreferences.askForMediaAccess("microphone").then((data) => {
          this.bridge.setState(
            {
              microphonePermission: data,
            },
            [this.mainWindow]
          );
        });
      }
    });

    ipcMain.on("nuxBack", () => {
      this.nux.back();
    });

    ipcMain.on("nuxNext", () => {
      this.nux.next();
    });

    ipcMain.on("openCustomCommands", () => {
      shell.openPath(path.join(this.settings.path(), "scripts", "custom.js"));
    });

    ipcMain.on("openLogDirectory", () => {
      shell.openPath(path.join(os.homedir(), ".serenade"));
    });

    ipcMain.on("openURL", (_event: any, data: string) => {
      shell.openExternal(data);
    });

    ipcMain.on("reloadCustomCommands", () => {
      this.custom.reload();
    });

    ipcMain.on("restart", () => {
      autoUpdater.quitAndInstall();
    });

    ipcMain.on("revisionBoxState", (_event: any, data: any) => {
      this.revisionBoxWindow.onGetEditorState(data);
    });

    ipcMain.on("sendTextRequest", (_event: any, data: any) => {
      this.stream.sendTextRequest(data.text, data.includeAlternatives);
    });

    ipcMain.on("setLanguage", (_event: any, language: core.Language) => {
      this.active.languageSwitcherLanguage = language;
      this.active.update(true);
    });

    ipcMain.on("setMiniModeWindowHeight", (_event: any, data: any) => {
      if (this.settings.getMiniMode()) {
        this.miniModeWindow.setHeight(data.height || 0);
      }
    });

    ipcMain.on("setNuxCompleted", async (_event: any, completed: boolean) => {
      if (this.settings.getNuxCompleted() == completed) {
        return;
      }

      if (completed) {
        this.nux.complete();
      } else {
        this.resetNux();
      }
    });

    ipcMain.on("setSettings", async (_event: any, data: any) => {
      if (data.animations !== undefined) {
        this.settings.setAnimations(data.animations);
        this.bridge.setState(
          {
            animations: data.animations,
          },
          [this.settingsWindow()]
        );
      }

      if (data.chunkSilenceThreshold !== undefined) {
        this.settings.setChunkSilenceThreshold(data.chunkSilenceThreshold);
        this.bridge.setState(
          {
            chunkSilenceThreshold: data.chunkSilenceThreshold,
          },
          [this.settingsWindow()]
        );
      }

      if (data.chunkSpeechThreshold !== undefined) {
        this.settings.setChunkSpeechThreshold(data.chunkSpeechThreshold);
        this.bridge.setState(
          {
            chunkSpeechThreshold: data.chunkSpeechThreshold,
          },
          [this.settingsWindow()]
        );
      }

      if (data.clipboardInsert !== undefined) {
        this.settings.setClipboardInsert(data.clipboardInsert);
        this.bridge.setState(
          {
            clipboardInsert: data.clipboardInsert,
          },
          [this.settingsWindow()]
        );
      }

      if (data.continueRunningInTray !== undefined) {
        this.settings.setContinueRunningInTray(data.continueRunningInTray);
        this.bridge.setState(
          {
            continueRunningInTray: data.continueRunningInTray,
          },
          [this.mainWindow, this.settingsWindow()]
        );
      }

      if (data.darkMode !== undefined) {
        this.settings.setDarkMode(data.darkMode);
        this.app.updateDarkModeForAllWindows();
      }

      if (data.disableSuggestions !== undefined) {
        this.settings.setDisableSuggestions(data.disableSuggestions);
        this.bridge.setState(
          {
            disableSuggestions: data.disableSuggestions,
          },
          [this.settingsWindow()]
        );
      }

      if (data.editorAutocomplete !== undefined) {
        this.settings.setEditorAutocomplete(data.editorAutocomplete);
        this.bridge.setState(
          {
            editorAutocomplete: data.editorAutocomplete,
          },
          [this.settingsWindow()]
        );
      }

      if (data.endpoint !== undefined) {
        this.chunkManager.toggle(false);
        this.settings.setStreamingEndpoint(data.endpoint);
        this.bridge.setState(
          {
            endpoint: this.settings.getStreamingEndpoint(),
          },
          [this.mainWindow, this.settingsWindow()]
        );

        this.api.ping(this.settings.getStreamingEndpoint());
      }

      if (data.executeSilenceThreshold !== undefined) {
        this.settings.setExecuteSilenceThreshold(data.executeSilenceThreshold);
        this.bridge.setState(
          {
            executeSilenceThreshold: data.executeSilenceThreshold,
          },
          [this.settingsWindow()]
        );
      }

      if (data.logAudio !== undefined) {
        this.settings.setLogAudio(data.logAudio);
        this.bridge.setState(
          {
            logAudio: data.logAudio,
          },
          [this.mainWindow, this.settingsWindow()]
        );
      }

      if (data.logSource !== undefined) {
        this.settings.setLogSource(data.logSource);
        this.bridge.setState(
          {
            logSource: data.logSource,
          },
          [this.mainWindow, this.settingsWindow()]
        );
      }

      if (data.microphone !== undefined && data.microphone.id != this.settings.getMicrophone().id) {
        this.microphone.changeMicrophone({
          id: data.microphone.id,
          name: data.microphone.name,
        });

        this.bridge.setState(
          {
            microphones: this.microphone.microphones(),
          },
          [this.settingsWindow()]
        );
      }

      if (data.minimizedPosition !== undefined) {
        this.settings.setMinimizedPosition(data.minimizedPosition);
        this.bridge.setState(
          {
            minimizedPosition: data.minimizedPosition,
          },
          [this.mainWindow, this.miniModeWindow, this.settingsWindow()]
        );
      }

      if (data.miniMode !== undefined) {
        this.app.clearAlternativesAndShowExamples();
        this.settings.setMiniMode(data.miniMode);
        this.bridge.setState(
          {
            miniMode: data.miniMode,
          },
          [this.mainWindow, this.miniModeWindow, this.settingsWindow()]
        );

        setImmediate(() => {
          this.app.toggleMiniMode(data.miniMode);
        });
      }

      if (data.useMiniModeFewerAlternatives !== undefined) {
        this.settings.setUseMiniModeFewerAlternatives(data.useMiniModeFewerAlternatives);
        this.bridge.setState(
          {
            useMiniModeFewerAlternatives: data.useMiniModeFewerAlternatives,
          },
          [this.miniModeWindow, this.settingsWindow()]
        );
      }

      if (data.miniModeFewerAlternativesCount !== undefined) {
        this.settings.setMiniModeFewerAlternativesCount(data.miniModeFewerAlternativesCount);
        this.bridge.setState(
          {
            miniModeFewerAlternativesCount: data.miniModeFewerAlternativesCount,
          },
          [this.miniModeWindow, this.settingsWindow()]
        );
      }

      if (data.useMiniModeHideTimeout !== undefined) {
        this.settings.setUseMiniModeHideTimeout(data.useMiniModeHideTimeout);
        this.bridge.setState(
          {
            useMiniModeHideTimeout: data.useMiniModeHideTimeout,
          },
          [this.miniModeWindow, this.settingsWindow()]
        );
      }

      if (data.miniModeHideTimeout !== undefined) {
        this.settings.setMiniModeHideTimeout(data.miniModeHideTimeout);
        this.bridge.setState(
          {
            miniModeHideTimeout: data.miniModeHideTimeout,
          },
          [this.miniModeWindow, this.settingsWindow()]
        );
      }

      if (data.miniModeReversed !== undefined) {
        this.settings.setMiniModeReversed(data.miniModeReversed);
        this.bridge.setState(
          {
            miniModeReversed: data.miniModeReversed,
          },
          [this.miniModeWindow, this.settingsWindow()]
        );
      }

      if (data.pushToTalk !== undefined) {
        this.settings.setPushToTalk(data.pushToTalk);
        this.bridge.setState(
          {
            pushToTalk: data.pushToTalk,
          },
          [this.settingsWindow()]
        );
      }

      if (data.showRevisionBox !== undefined) {
        this.settings.setShowRevisionBox(data.showRevisionBox);
        this.bridge.setState(
          {
            showRevisionBox: data.showRevisionBox,
          },
          [this.settingsWindow()]
        );
      }

      if (data.stylers !== undefined) {
        this.settings.setStylers(data.stylers);
        this.bridge.setState(
          {
            stylers: data.stylers,
          },
          [this.settingsWindow()]
        );
      }

      if (data.textInputKeybinding !== undefined) {
        this.settings.setTextInputKeybinding(data.textInputKeybinding);
        this.bridge.setState(
          {
            textInputKeybinding: data.textInputKeybinding,
          },
          [this.settingsWindow()]
        );
      }

      if (data.useVerboseLogging !== undefined) {
        this.settings.setUseVerboseLogging(data.useVerboseLogging);
        this.bridge.setState(
          {
            useVerboseLogging: data.useVerboseLogging,
          },
          [this.settingsWindow()]
        );
      }
    });

    ipcMain.on("setSettingsPage", (_event: any, settingsPage: string) => {
      this.bridge.setState(
        {
          settingsPage,
        },
        [this.settingsWindow()]
      );
    });

    ipcMain.on("setWindowState", async (_event: any, data: { state: string; url: string }) => {
      const settingsWindow = this.settingsWindow();
      const languageSwitcherWindow = this.languageSwitcherWindow();

      let window: Window = this.mainWindow;
      if (data.url.includes("minimode")) {
        window = this.miniModeWindow;
      } else if (data.url.includes("settings") && settingsWindow) {
        window = await settingsWindow;
      } else if (data.url.includes("revision")) {
        window = this.revisionBoxWindow;
      } else if (data.url.includes("languages") && languageSwitcherWindow) {
        window = await languageSwitcherWindow;
      }

      if (data.state == "minimize") {
        window.window?.minimize();
      } else if (data.state == "maximize") {
        window.window?.maximize();
      } else if (data.state == "unmaximize") {
        window.window?.unmaximize();
      } else if (data.state == "close") {
        window.window?.close();
      }

      if (window == this.mainWindow) {
        this.miniModeWindow.snapToMain();
        this.active.showSuggestionIfNeeded();
      }
    });

    ipcMain.on("showLanguageSwitcher", async () => {
      const languageSwitcherWindow = this.languageSwitcherWindow();
      if (languageSwitcherWindow) {
        (await languageSwitcherWindow).show();
      }
    });

    ipcMain.on("showNuxHint", async () => {
      this.bridge.setState(
        {
          nuxHintShown: true,
        },
        [this.mainWindow, this.miniModeWindow]
      );
    });

    ipcMain.on("showSettingsWindow", async () => {
      const settingsWindow = this.settingsWindow();
      if (settingsWindow) {
        (await settingsWindow).show();
      }
    });

    ipcMain.on("startLocal", () => {
      this.local.start();
    });

    ipcMain.on("stopLocal", () => {
      this.local.stop();
      this.bridge.setState(
        {
          localRunning: false,
        },
        [this.mainWindow]
      );
    });

    ipcMain.on("toggleChunkManager", (_event: any, listening: boolean) => {
      this.chunkManager.toggle(listening);
    });

    ipcMain.on("toggleDictateMode", async (_event: any, _data: any) => {
      this.active.dictateMode = !this.active.dictateMode;
      this.active.update(true);
    });
  }

  private resetNux() {
    this.settings.setNuxStep(0);
    this.settings.setNuxCompleted(false);
    this.app.clearAlternativesAndShowExamples();
    this.bridge.setState(
      {
        nuxCompleted: false,
      },
      [this.mainWindow, this.miniModeWindow]
    );
  }
}
