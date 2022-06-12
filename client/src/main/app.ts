import { globalShortcut, nativeTheme } from "electron";
import Active from "./active";
import API from "./api";
import { ChunkQueue } from "./stream/chunk-queue";
import ChunkManager from "./stream/chunk-manager";
import CommandHandler from "./execute/command-handler";
import Custom from "./ipc/custom";
import Executor from "./execute/executor";
import InsertHistory from "./execute/insert-history";
import IPCServer from "./ipc/server";
import LanguageSwitcherWindow from "./windows/language-switcher";
import Local from "./ipc/local";
import Log from "./log";
import MainWindow from "./windows/main";
import Metadata from "../shared/metadata";
import Microphone from "./stream/microphone";
import MiniModeWindow from "./windows/mini-mode";
import NativeCommands from "./execute/native-commands";
import NUX from "./nux";
import PluginManager from "./ipc/plugin-manager";
import RendererBridge from "./bridge";
import RendererProcessEventHandlers from "./events";
import RevisionBoxWindow from "./windows/revision-box";
import Settings from "./settings";
import SettingsWindow from "./windows/settings";
import Stream from "./stream/stream";
import System from "./execute/system";
import TextInputWindow from "./windows/text-input";
import Window from "./windows/window";
import * as examples from "./examples";
const { SpeechRecorder } = require("speech-recorder");

export default class App {
  private bridge?: RendererBridge;
  private chunkManager?: ChunkManager;
  private custom?: Custom;
  private executor?: Executor;
  private ipcServer?: IPCServer;
  private languageSwitcherWindow?: Promise<LanguageSwitcherWindow>;
  private local?: Local;
  private mainWindow?: MainWindow;
  private microphone?: Microphone;
  private miniModeWindow?: MiniModeWindow;
  private revisionBoxWindow?: RevisionBoxWindow;
  private settingsWindow?: Promise<SettingsWindow>;
  private stream?: Stream;
  private textInputWindow?: Promise<TextInputWindow>;

  private previousShouldUseDarkColors?: boolean;

  public log?: Log;
  public settings?: Settings;

  static async create() {
    const instance = new App();

    // these windows are created after the main window is shown, so all callers await
    // promises to ensure they're initialized
    let languageSwitcherWindow: Promise<LanguageSwitcherWindow> | undefined = undefined;
    let settingsWindow: Promise<SettingsWindow> | undefined = undefined;
    let textInputWindow: Promise<TextInputWindow> | undefined = undefined;

    // these windows are needed by the main window (which is created first) so they can be
    // destroyed when the app is quit, but otherwise are always initialized before they
    // are used, so they are not promises like the above
    let revisionBoxWindow: RevisionBoxWindow | undefined = undefined;
    let miniModeWindow: MiniModeWindow | undefined = undefined;

    const chunkQueue = new ChunkQueue();
    const insertHistory = new InsertHistory();
    const metadata = new Metadata();
    const settings = (instance.settings = new Settings());
    const bridge = (instance.bridge = new RendererBridge(settings));
    const system = new System(settings);
    const log = (instance.log = new Log(settings));
    instance.updateDarkModeForAllWindows();

    const custom = (instance.custom = await Custom.create(settings));
    const mainWindow = (instance.mainWindow = await MainWindow.create(
      instance,
      bridge,
      metadata,
      settings,
      () => instance.chunkManager,
      () => miniModeWindow,
      () => [
        revisionBoxWindow,
        miniModeWindow,
        languageSwitcherWindow,
        settingsWindow,
        textInputWindow,
      ]
    ));

    miniModeWindow = instance.miniModeWindow = await MiniModeWindow.create(
      bridge,
      mainWindow,
      settings
    );

    mainWindow.show();
    if (settings.getMiniMode()) {
      miniModeWindow.snapToMain();
      miniModeWindow.show();
    }

    revisionBoxWindow = instance.revisionBoxWindow = await RevisionBoxWindow.create(
      bridge,
      mainWindow,
      miniModeWindow,
      settings,
      system
    );

    nativeTheme.on("updated", () => {
      // this seems to be triggered more often than it changes, so we cache the value here
      if (
        settings.getDarkMode() != "system" ||
        nativeTheme.shouldUseDarkColors === instance.previousShouldUseDarkColors
      ) {
        return;
      }

      instance.updateDarkModeForAllWindows();
      instance.previousShouldUseDarkColors = nativeTheme.shouldUseDarkColors;
    });

    const pluginManager = new PluginManager(settings);
    const microphone = (instance.microphone = new Microphone(
      bridge,
      mainWindow,
      settings,
      () => settingsWindow
    ));

    const active = new Active(
      bridge,
      custom,
      revisionBoxWindow,
      insertHistory,
      mainWindow,
      metadata,
      miniModeWindow,
      pluginManager,
      settings,
      system
    );

    const nativeCommands = new NativeCommands(active, insertHistory, revisionBoxWindow, system);
    const api = new API(active, bridge, log, mainWindow, metadata, settings, () => settingsWindow);
    const stream = (instance.stream = new Stream(active, api, log, settings));
    const local = (instance.local = new Local(bridge, log, mainWindow, metadata, settings));
    const nux = new NUX(
      active,
      instance,
      bridge,
      mainWindow,
      miniModeWindow,
      pluginManager,
      settings
    );

    instance.ipcServer = new IPCServer(
      active,
      bridge,
      custom,
      mainWindow,
      miniModeWindow,
      pluginManager,
      stream,
      log
    );

    await custom.start();
    const executor = (instance.executor = new Executor(
      active,
      api,
      bridge,
      insertHistory,
      log,
      mainWindow,
      miniModeWindow,
      nativeCommands,
      nux,
      pluginManager,
      revisionBoxWindow,
      settings,
      stream,
      system,
      () => commandHandler
    ));

    const chunkManager: ChunkManager = (instance.chunkManager = new ChunkManager(
      active,
      api,
      instance,
      bridge,
      chunkQueue,
      custom,
      executor,
      log,
      mainWindow,
      microphone,
      miniModeWindow,
      settings,
      stream
    ));

    const commandHandler: CommandHandler = new CommandHandler(
      active,
      instance,
      bridge,
      chunkManager,
      custom,
      executor,
      mainWindow,
      nativeCommands,
      nux,
      revisionBoxWindow,
      settings,
      stream,
      system,
      () => languageSwitcherWindow
    );

    new RendererProcessEventHandlers(
      active,
      instance,
      api,
      bridge,
      chunkManager,
      custom,
      revisionBoxWindow,
      local,
      mainWindow,
      microphone,
      miniModeWindow,
      nux,
      pluginManager,
      settings,
      stream,
      () => languageSwitcherWindow,
      () => settingsWindow,
      () => textInputWindow
    );

    // users will see an onboarding step to change these default values before using the product
    if (!settings.getToken()) {
      settings.setLogAudio(true);
      settings.setLogSource(true);
    }

    instance.sendAllSettings(local, microphone, miniModeWindow, settings, [
      mainWindow,
      miniModeWindow,
    ]);

    if (settings.getStreamingEndpoint() && settings.getStreamingEndpoint().id == "local") {
      local.start();
    } else {
      await api.setBestEndpoint(settings.getStreamingEndpoints());
    }

    instance.registerPushToTalk();
    bridge.setState({ loggedIn: !!settings.getToken(), listening: false }, [
      mainWindow,
      miniModeWindow,
    ]);
    instance.clearAlternativesAndShowExamples();
    nux.showIfNeeded();

    languageSwitcherWindow = instance.languageSwitcherWindow = LanguageSwitcherWindow.create(
      active,
      bridge,
      mainWindow,
      settings
    );

    textInputWindow = instance.textInputWindow = TextInputWindow.create(
      bridge,
      mainWindow,
      settings,
      system
    );

    settingsWindow = instance.settingsWindow = SettingsWindow.create(
      instance,
      bridge,
      local,
      mainWindow,
      microphone,
      miniModeWindow,
      settings
    );

    return instance;
  }

  clearAlternativesAndShowExamples() {
    if (!this.bridge || !this.mainWindow || !this.settings) {
      return;
    }

    let alternatives: any = examples.random(5).map((e: string) => ({
      description: e,
      example: true,
    }));

    // don't show suggestions when in NUX, since it's confusing to have the app telling you to say
    // different things at once, or when in mini/minimized mode, where they get in the way
    if (
      !this.settings.getToken() ||
      !this.settings.getNuxCompleted() ||
      !this.mainWindow.shown() ||
      this.settings.getMiniMode()
    ) {
      alternatives = [];
    }

    this.bridge.setState(
      {
        alternatives,
        highlighted: [],
      },
      [this.mainWindow, this.miniModeWindow]
    );
  }

  pushToTalkPressed() {
    this.chunkManager!.toggle();
  }

  quit() {
    this.local?.stop();
    this.custom?.stop();
    this.microphone?.stop();
    this.ipcServer?.stop();
  }

  registerPushToTalk() {
    globalShortcut.unregisterAll();

    if (this.settings!.getPushToTalk()) {
      try {
        globalShortcut.register(this.settings!.getPushToTalk(), () => {
          this.pushToTalkPressed();
        });
      } catch (e) {}
    }

    if (this.settings!.getTextInputKeybinding()) {
      try {
        globalShortcut.register(this.settings!.getTextInputKeybinding(), async () => {
          if (!this.textInputWindow) {
            return;
          }

          const textInputWindow = await this.textInputWindow;
          if (textInputWindow.shown()) {
            textInputWindow.hide();
          } else {
            this.stream!.connect(this.chunkManager!, this.custom!, this.executor!);
            textInputWindow.show();
          }
        });
      } catch (e) {}
    }
  }

  async sendAllSettings(
    local: Local,
    microphone: Microphone,
    miniModeWindow: MiniModeWindow,
    settings: Settings,
    windows: (Window | Promise<Window> | undefined)[]
  ) {
    this.bridge!.setState(
      {
        animations: settings.getAnimations(),
        chunkSilenceThreshold: settings.getChunkSilenceThreshold(),
        chunkSpeechThreshold: settings.getChunkSpeechThreshold(),
        clipboardInsert: settings.getClipboardInsert(),
        darkMode: settings.getDarkMode(),
        disableSuggestions: settings.getDisableSuggestions(),
        editorAutocomplete: settings.getEditorAutocomplete(),
        endpoint: settings.getStreamingEndpoint(),
        endpoints: settings.getStreamingEndpoints(),
        executeSilenceThreshold: settings.getExecuteSilenceThreshold(),
        logAudio: settings.getLogAudio(),
        logSource: settings.getLogSource(),
        microphones: microphone.microphones(),
        minimizedPosition: settings.getMinimizedPosition(),
        miniMode: settings.getMiniMode(),
        miniModeBottomUp: miniModeWindow.shouldPlaceAboveMain(),
        miniModeFewerAlternativesCount: settings.getMiniModeFewerAlternativesCount(),
        miniModeHideTimeout: settings.getMiniModeHideTimeout(),
        miniModeReversed: settings.getMiniModeReversed(),
        nuxCompleted: settings.getNuxCompleted(),
        nuxTutorial: settings.getNuxTutorialName(),
        plugins: settings.getPlugins(),
        pushToTalk: settings.getPushToTalk(),
        requiresNewerMac: local.requiresNewerMac(),
        requiresWsl: await local.requiresWsl(),
        showRevisionBox: settings.getShowRevisionBox(),
        stylers: settings.getStylers(),
        textInputKeybinding: settings.getTextInputKeybinding(),
        useMiniModeFewerAlternatives: settings.getUseMiniModeFewerAlternatives(),
        useMiniModeHideTimeout: settings.getUseMiniModeHideTimeout(),
        useVerboseLogging: settings.getUseVerboseLogging(),
      },
      windows ? windows : [this.mainWindow, this.miniModeWindow, this.settingsWindow]
    );
  }

  show() {
    this.mainWindow!.show();
  }

  async toggleMiniMode(enabled: boolean) {
    this.settings?.setMiniMode(enabled);
    this.mainWindow?.resizeToCurrentMode(true);
    this.clearAlternativesAndShowExamples();
    this.bridge?.setState(
      {
        miniMode: enabled,
      },
      [this.mainWindow, this.miniModeWindow, this.settingsWindow]
    );

    if (enabled) {
      this.miniModeWindow?.show();
    } else {
      this.miniModeWindow?.hide();
    }

    this.miniModeWindow?.snapToMain();
  }

  async updateDarkModeForAllWindows() {
    if (this.settings) {
      const darkMode = this.settings.getDarkMode();
      if (nativeTheme.themeSource != darkMode) {
        nativeTheme.themeSource = darkMode;
      }

      if (this.bridge) {
        this.bridge.setState(
          {
            darkMode,
            darkTheme:
              darkMode == "dark" || (darkMode == "system" && nativeTheme.shouldUseDarkColors),
          },
          [
            this.mainWindow,
            this.miniModeWindow,
            this.languageSwitcherWindow,
            this.revisionBoxWindow,
            this.settingsWindow,
            this.textInputWindow,
          ]
        );
      }
    }
  }
}
