import { clipboard } from "electron";
import Custom from "./ipc/custom";
import InsertHistory from "./execute/insert-history";
import MainWindow from "./windows/main";
import Metadata from "../shared/metadata";
import MiniModeWindow from "./windows/mini-mode";
import PluginManager from "./ipc/plugin-manager";
import RevisionBoxWindow from "./windows/revision-box";
import Settings from "./settings";
import System from "./execute/system";
import RendererBridge from "./bridge";
import { isValidAlternative } from "../shared/alternatives";
import { filenameToLanguage, languages } from "../shared/languages";
import { plugins } from "../shared/plugins";
import { core } from "../gen/core";

export default class Active {
  private suggestion: string = "";

  app: string = "";
  icon?: string;
  customCommands: any[] = [];
  customHints: any[] = [];
  customWords: any[] = [];
  dictateMode: boolean = false;
  filename: string = "";
  language: core.Language = core.Language.LANGUAGE_DEFAULT;
  languageSwitcherLanguage: core.Language = core.Language.LANGUAGE_NONE;
  sourceAvailable = false;
  refocused = false;

  constructor(
    private bridge: RendererBridge,
    private custom: Custom,
    private revisionBoxWindow: RevisionBoxWindow,
    private insertHistory: InsertHistory,
    private mainWindow: MainWindow,
    private metadata: Metadata,
    private miniModeWindow: MiniModeWindow,
    private pluginManager: PluginManager,
    private settings: Settings,
    private system: System
  ) {
    setInterval(async () => {
      // If we only have empty-tabs in chrome, then our service worker times out
      // and we need to re-focus chrome to trigger a callback that spins up a new one.
      // The logic below tries this once before we consider it a disconnect.
      if (this.firstPartyBrowserPlugins().includes(this.app) && this.pluginInstalled() && !this.pluginConnected()) {
        if (!this.refocused) {
          await this.system.focus("serenade");
          await this.system.focus(this.app);
          this.refocused = true;
          return;
        }
      } else {
        this.refocused = false;
      }
      this.update();
    }, 1000);
  }

  private customCommandMatches(
    command: any,
    app: string,
    filename: string,
    language: core.Language,
    url: string
  ): boolean {
    const applications = command.applications || [];
    const extensions = command.extensions || [];
    const urls = command.urls || [];
    const commandLanguages = command.languages || [];
    return (
      (applications.some((e: string) => app.toLowerCase().includes(e.toLowerCase())) ||
        applications.length == 0) &&
      (extensions.some((e: string) => filename.toLowerCase().endsWith(e.toLowerCase())) ||
        extensions.length == 0) &&
      (urls.some((e: string) => url.toLowerCase().includes(e.toLowerCase())) || urls.length == 0) &&
      (commandLanguages.some((e: string) =>
        Object.values(languages).some(
          (language: any) =>
            language.name.toLowerCase() == e.toLowerCase() ||
            language.extensions.some(
              (extension: string) => extension.toLowerCase() == e.toLowerCase()
            )
        )
      ) ||
        commandLanguages.length == 0)
    );
  }

  private firstPartyPlugins(): string[] {
    return this.firstPartyEditorPlugins()
      .concat(this.firstPartyBrowserPlugins())
      .concat(this.firstPartyTerminalPlugins());
  }

  private firstPartyBrowserPlugins(): string[] {
    return ["chrome", "edge"];
  }

  private firstPartyEditorPlugins(): string[] {
    return ["atom", "jetbrains", "vscode"];
  }

  private firstPartyPluginAvailable(app?: string): boolean {
    app = app || this.app;

    // temporarily remove iterm because the revision box is sufficient
    return this.firstPartyPlugins()
      .filter((e) => e != "iterm")
      .includes(app);
  }

  private firstPartyTerminalPlugins(): string[] {
    return ["hyper", "iterm"];
  }

  async getEditorState(includeClipboard: boolean = false, limited: boolean = false): Promise<any> {
    let reportedState: any = {
      source: "",
      cursor: 0,
      canGetState: false,
      canSetState: false,
    };

    if (this.revisionBoxWindow.shown()) {
      reportedState = {
        ...(await this.revisionBoxWindow.getEditorState()),
        canGetState: true,
        canSetState: true,
      };
    } else if (this.pluginConnected()) {
      const response = await this.pluginManager.sendCommandToApp(this.app, {
        type: core.CommandType.COMMAND_TYPE_GET_EDITOR_STATE,
        limited,
      });

      if (response && response.data && !response.data.error) {
        reportedState = { ...response.data };
      }

      // remove once we update the chrome extension to v2
      if (this.isFirstPartyBrowser() && response && response.data) {
        if (reportedState.filename === "") {
          reportedState.canGetState = true;
          reportedState.canSetState = false;
        }
      }

      // older plugins may not explicitly set canGetState or canSetState, but do set available
      if (reportedState.canGetState === undefined || reportedState.canSetState === undefined) {
        if (reportedState.available !== undefined) {
          // if the plugin sets the available flag, use it
          reportedState.canGetState = reportedState.available;
          reportedState.canSetState = reportedState.available;
        } else {
          // otherwise assume we can get/set the state
          reportedState.canGetState = reportedState.canGetState || true;
          reportedState.canSetState = reportedState.canSetState || true;
        }
      }
    } else if (this.useAccessibilityApi()) {
      const state = await this.system.getEditorStateWithAccessibilityApi();
      reportedState = {
        source: state.source,
        cursor: state.cursor,
        canGetState: !state.error,
        canSetState: false,
      };
    }

    // support legacy plugin versions that don't set this flag
    if (this.system.isTerminal(this.app)) {
      reportedState.canSetState = false;
    }

    if (!reportedState.canGetState) {
      const latestInsert = this.insertHistory.latest(this.app);
      reportedState.source = latestInsert;
      reportedState.cursor = latestInsert.length;
    }

    let customWords = await this.settings.getCustomWords();
    const customWordsFromScripts = this.customWords.filter((e: any) =>
      this.customCommandMatches(e, this.app, this.filename, this.language, url)
    );

    for (const word of Object.values(customWordsFromScripts)) {
      if (word.before && word.after) {
        customWords[word.before] = word.after;
      }
    }

    let styler: any = this.settings.getStylers();
    for (const k of Object.keys(languages)) {
      const language = (k as unknown) as core.Language;
      if (!styler[language]) {
        styler[language] = languages[language]!.styler;
      }
    }

    let url = reportedState.url || "";
    let result: core.IEditorState = {
      token: this.settings.getToken(),
      application: this.revisionBoxWindow.shown() ? "revision-box" : this.app,
      pluginInstalled: this.pluginInstalled() || this.revisionBoxWindow.shown(),
      clientIdentifier: this.metadata.identifier(this.app, this.language),
      nux: !this.settings.getNuxCompleted(),
      autocomplete: this.settings.getEditorAutocomplete(),
      canGetState: reportedState.canGetState,
      canSetState: reportedState.canSetState,
      customHints: (await this.settings.getCustomHints()).concat(
        this.customHints
          .filter(
            (e: any) =>
              e.hint && this.customCommandMatches(e, this.app, this.filename, this.language, url)
          )
          .map((e: any) => e.hint)
      ),
      styler: this.settings.getStylers(),
      dictateMode: this.dictateMode,
      source: Buffer.from(reportedState.source || ""),
      cursor: reportedState.cursor || 0,
      selectionRange: reportedState.selectionRange || { start: 0, stop: 0 },
      filename: reportedState.filename || "",
      files: reportedState.files || [],
      tabs: reportedState.tabs || [""],
      language: this.language,
      logAudio: this.settings.getLogAudio(),
      logSource: this.settings.getLogSource(),
      customWords,
      url,
    };

    // if NUX isn't completed, send the filename to make sure NUX works
    if (!this.settings.getNuxCompleted()) {
      this.bridge.setState(
        {
          filename: reportedState.filename || "",
        },
        [this.mainWindow, this.miniModeWindow]
      );
    }

    if (includeClipboard) {
      result.clipboard = clipboard.readText();
    }

    result.customCommands = this.customCommands
      .filter((e: any) => this.customCommandMatches(e, this.app, this.filename, this.language, url))
      .map((e: any) => {
        let chainable = core.CustomCommandChainable.CUSTOM_COMMAND_CHAINABLE_NONE;
        if (e.chainable == "any" || e.chainable === true) {
          chainable = core.CustomCommandChainable.CUSTOM_COMMAND_CHAINABLE_ANY;
        } else if (e.chainable == "firstOnly") {
          chainable = core.CustomCommandChainable.CUSTOM_COMMAND_CHAINABLE_FIRST_ONLY;
        } else if (e.chainable == "lastOnly") {
          chainable = core.CustomCommandChainable.CUSTOM_COMMAND_CHAINABLE_LAST_ONLY;
        }

        return {
          id: e.id,
          templated: e.templated,
          applications: e.applications || [],
          languages: e.languages || [],
          extensions: e.extensions || [],
          generated: e.generated || "",
          snippetType: e.snippetType || "",
          options: e.options || [],
          urls: e.urls || [],
          transformExamples: e.transformExamples || [],
          transformDescription: e.transformDescription || "",
          chainable,
        };
      });

    return result;
  }

  isFirstPartyBrowser(app?: string): boolean {
    app = app || this.app;
    return this.pluginConnected() && this.firstPartyBrowserPlugins().includes(app);
  }

  isFirstPartyEditor(app?: string): boolean {
    app = app || this.app;
    return this.pluginConnected() && this.firstPartyEditorPlugins().includes(app);
  }

  pluginConnected(): boolean {
    return this.pluginManager.fromApp(this.app) != null;
  }

  pluginInstalled(): boolean {
    return this.settings.getPluginInstalled(this.app);
  }

  showSuggestionIfNeeded() {
    if (
      !this.settings.getToken() ||
      !this.settings.getNuxCompleted() ||
      this.settings.getDisableSuggestions()
    ) {
      return;
    }

    if (!this.mainWindow.shown()) {
      if (this.suggestion) {
        this.suggestion = "";
        this.bridge.setState(
          {
            suggestion: "",
          },
          [this.mainWindow, this.miniModeWindow]
        );
      }

      return;
    }

    let suggestion = "";
    if (this.firstPartyPluginAvailable()) {
      if (this.pluginInstalled() && !this.pluginConnected()) {
        suggestion = `<p>${plugins[this.app]!.name} plugin disconnected. Try restarting ${
          plugins[this.app]!.name
        } or reinstalling the plugin.<div style="margin-top: 0.8rem"><a class="primary-button" href="${
          plugins[this.app]!.url
        }" target="_blank" style="font-size: 0.82rem">Install</a></div>`;
      } else if (!this.pluginInstalled()) {
        suggestion = `<p>A plugin is available for ${
          plugins[this.app]!.name
        }.</p><div style="margin-top: 0.8rem"><a class="primary-button" href="${
          plugins[this.app]!.url
        }" target="_blank" style="font-size: 0.82rem">Install</a></div>`;
      }
    }

    // don't re-send the same suggestion we're already showing
    if (suggestion == this.suggestion) {
      return;
    }

    this.suggestion = suggestion;
    this.bridge.setState(
      {
        suggestion,
      },
      [this.mainWindow, this.miniModeWindow]
    );
  }

  async update(force: boolean = false) {
    let app = await this.system.determineActiveApplication();

    // some UI controls in Serenade will take the focus off the active app, so we want to keep
    // sending commands to the last app to be active that isn't Serenade
    if (app == "serenade") {
      app = this.app;
    }

    const plugin = this.pluginManager.fromApp(app);
    if (plugin) {
      app = plugin.app;
    }

    if (!app) {
      return;
    }

    const editorState = app == "serenade" ? null : await this.getEditorState(false, true);
    let filename = "";
    let sourceAvailable = false;
    if (editorState) {
      filename = editorState.filename || "";
      sourceAvailable = editorState.canSetState;
    }

    // make sure terminal uses the Bash model
    if (this.system.isTerminal(app)) {
      filename = "terminal.sh";
    }

    const language =
      this.languageSwitcherLanguage != core.Language.LANGUAGE_NONE
        ? this.languageSwitcherLanguage
        : filenameToLanguage(filename);

    const icon = plugin?.icon;

    const send =
      force ||
      app != this.app ||
      icon != this.icon ||
      filename != this.filename ||
      language != this.language ||
      sourceAvailable != this.sourceAvailable;

    this.app = app;
    this.icon = icon;
    this.filename = filename;
    this.language = language;
    this.sourceAvailable = sourceAvailable;

    if (send) {
      this.showSuggestionIfNeeded();
      this.bridge.setState(
        {
          app: this.app,
          icon: this.icon,
          dictateMode: this.dictateMode,
          filename: this.filename,
          firstPartyPluginAvailable: this.firstPartyPluginAvailable(),
          language: this.language,
          languageSwitcherLanguage: this.languageSwitcherLanguage,
          pluginConnected: this.pluginConnected(),
          pluginInstalled: this.pluginInstalled(),
          sourceAvailable: this.sourceAvailable,
        },
        [this.mainWindow]
      );

      this.custom.send("contextChanged", {
        application: app,
        filename,
        language: languages[language] ? languages[language]!.name : "",
      });
    }
  }

  useAccessibilityApi() {
    return (
      !this.pluginConnected() &&
      this.settings.getUseAccessibilityApi().some((e) => this.app.includes(e))
    );
  }
}
