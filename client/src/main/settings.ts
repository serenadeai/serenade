import { screen } from "electron";
import { jsonc } from "jsonc";
import * as fs from "fs-extra";
import * as os from "os";
import * as path from "path";
import Microphone from "./stream/microphone";
import { core } from "../gen/core";
import { Endpoint } from "../shared/endpoint";
const { devices } = require("speech-recorder");

export type Theme = "light" | "dark" | "system";

export default class Settings {
  private loaded = false;
  private systemData: any = {};
  private userData: any = {};
  private wordsData: any = {};
  private wordsLastLoad: number = 0;

  constructor() {
    this.setInstalled(true);
  }

  private createIfNotExists(file: string) {
    fs.mkdirpSync(this.path());
    if (!fs.existsSync(file)) {
      fs.closeSync(fs.openSync(file, "w"));
    }
  }

  private dataForFile(file: string): any {
    if (file == "user") {
      return this.userData;
    } else if (file == "system") {
      return this.systemData;
    } else if (file == "words") {
      return this.wordsData;
    }
  }

  private get(file: string, key: string, defaultValue?: any): any {
    if (!this.loaded) {
      this.load();
      this.loaded = true;
    }

    let data = this.dataForFile(file);
    if (data[key] === undefined) {
      return defaultValue;
    }

    return data[key];
  }

  private load() {
    this.systemData = {};
    this.userData = {};
    this.wordsData = {};

    this.createIfNotExists(this.systemFile());
    this.createIfNotExists(this.userFile());
    this.createIfNotExists(this.wordsFile());

    const systemFileContent = fs.readFileSync(this.systemFile()).toString();
    if (systemFileContent) {
      this.systemData = JSON.parse(systemFileContent);
    }

    const userFileContent = fs.readFileSync(this.userFile()).toString();
    if (userFileContent) {
      this.userData = JSON.parse(userFileContent);
    }

    const wordsFileContent = fs.readFileSync(this.wordsFile()).toString();
    if (wordsFileContent) {
      try {
        this.wordsData = jsonc.parse(wordsFileContent);
      } catch (e) {}
    }
  }

  private save() {
    this.createIfNotExists(this.systemFile());
    this.createIfNotExists(this.userFile());

    fs.writeFileSync(this.systemFile(), JSON.stringify(this.systemData, null, 2));
    fs.writeFileSync(this.userFile(), JSON.stringify(this.userData, null, 2));
  }

  private set(file: string, key: string, value: any) {
    if (!this.loaded) {
      this.load();
      this.loaded = true;
    }

    let data = this.dataForFile(file);
    data[key] = value;
    this.save();
  }

  private systemFile(): string {
    return path.join(this.path(), "serenade.json");
  }

  private userFile(): string {
    return path.join(this.path(), "settings.json");
  }

  private wordsFile(): string {
    return path.join(this.path(), "words.json");
  }

  revisionBoxTrigger(app: string): string {
    const data = this.getShowRevisionBox();
    if (app) {
      for (const k of Object.keys(data)) {
        if (app.includes(k)) {
          if (data[k] === true) {
            return "auto";
          } else if (data[k] === false) {
            return "never";
          }

          return data[k];
        }
      }
    }

    return data["default"] || data.all_apps || "never";
  }

  getAnimations(): boolean {
    return this.get("user", "animations", false);
  }

  getBounds(): any {
    const result = this.get("system", "bounds", { x: 0, y: 0, width: 0, height: 0 });
    const display = screen.getDisplayNearestPoint({ x: result.x, y: result.y });
    if (result.x < display.workArea.x || result.x > display.workArea.x + display.workArea.width) {
      result.x = display.workArea.x;
    }
    if (result.y < display.workArea.y || result.y > display.workArea.y + display.workArea.height) {
      result.y = display.workArea.y;
    }

    return result;
  }

  getChunkSilenceThreshold(): number {
    return this.get("user", "chunk_silence_threshold", 0.3);
  }

  getChunkSpeechThreshold(): number {
    return this.get("user", "chunk_speech_threshold", 0.3);
  }

  getClipboardInsert(): boolean {
    return this.get("user", "clipboard_insert", true);
  }

  getContinueRunningInTray(): boolean {
    return this.get("user", "continue_running_in_tray", false);
  }

  async getCustomHints(): Promise<string[]> {
    await this.loadWordsFileIfNeeded();
    const result = this.get("words", "hints", []);
    if (Array.isArray(result) && result.every((item) => typeof item === "string")) {
      return result;
    }

    return [];
  }

  async getCustomWords(): Promise<any> {
    await this.loadWordsFileIfNeeded();
    const result = this.get("words", "words", {});
    if (
      Object.keys(result).every((key) => typeof key === "string" && typeof result[key] === "string")
    ) {
      return result;
    }

    return {};
  }

  getDarkMode(): Theme {
    return this.get("user", "dark_mode", "system");
  }

  getDisableAnalytics(): boolean {
    return this.get("user", "disable_analytics", false);
  }

  getDisableSuggestions(): boolean {
    return this.get("user", "disable_suggestions", false);
  }

  getDisableAutoUpdate(): boolean {
    return this.get("system", "disable_auto_update", false);
  }

  getEditorAutocomplete(): boolean {
    return this.get("user", "autocomplete", false);
  }

  getExecuteSilenceThreshold(): number {
    return this.get("user", "execute_silence_threshold", 1);
  }

  getLogAudio(): boolean {
    // support legacy setting
    const legacy = this.get("user", "local_logging_opt_out", undefined);
    if (legacy === true) {
      this.set("user", "log_audio", false);
      return false;
    }

    return this.get("user", "log_audio", false);
  }

  getLogSource(): boolean {
    // support legacy setting
    const legacy = this.get("user", "local_logging_opt_out", undefined);
    if (legacy === true) {
      this.set("user", "log_source", false);
      return false;
    }

    return this.get("user", "log_source", false);
  }

  getMicrophone(): any {
    // use the microphone from settings only if the microphone at that index has a matching name.
    // microphones can re-order and different microphones can have the same name,
    // so only set to a non-default microphone if it matches both name and index.
    const data = this.get("system", "microphone", Microphone.systemDefaultMicrophone);
    if (data.id != Microphone.systemDefaultMicrophone.id) {
      const active = devices().filter((e: any) => e.id == data.id);
      if (active.length != 1 || active[0].name != data.name) {
        this.setMicrophone(Microphone.systemDefaultMicrophone);
      }
    }

    return this.get("system", "microphone", Microphone.systemDefaultMicrophone);
  }

  getMinimizedPosition(): string {
    return this.get(
      "user",
      "minimized_position",
      os.platform() == "win32" ? "bottom-right" : "top-right"
    );
  }

  getMiniMode(): boolean {
    return this.get("user", "mini_mode", true);
  }

  getMiniModeFewerAlternativesCount(): number {
    return this.get("user", "mini_mode_fewer_alternatives_count", 5);
  }

  getMiniModeHideTimeout(): number {
    return this.get("user", "mini_mode_timeout_value", 5);
  }

  getMiniModeReversed(): boolean {
    return this.get("user", "mini_mode_reversed", true);
  }

  getPlugins(): string[] {
    return this.get("system", "plugins", []);
  }

  getPluginInstalled(plugin: string): boolean {
    return this.getPlugins().includes(plugin);
  }

  getNuxCompleted(): boolean {
    return this.get("system", "nux_completed", false);
  }

  getNuxStep(): number {
    return this.get("system", "nux_step", 0);
  }

  getNuxTutorialName(): string {
    return this.get("system", "nux_tutorial_name", "");
  }

  getPasteKeys(app?: string): { key: string; modifiers: string[] } {
    const data = this.get("user", "paste_override", {
      "gnome-terminal": { key: "v", modifiers: ["control", "shift"] },
    });

    if (app) {
      for (const k of Object.keys(data)) {
        if (app.includes(k)) {
          return { key: data[k].key, modifiers: data[k].modifiers };
        }
      }
    }

    return { key: "v", modifiers: os.platform() == "darwin" ? ["command"] : ["control"] };
  }

  getPushToTalk(): string {
    return this.get("user", "push_to_talk", "Alt+Space");
  }

  getShowRevisionBox(): any {
    return this.get("user", "show_revision_box", { all_apps: false });
  }

  getStreamingEndpoint(): Endpoint {
    const endpoints = this.getStreamingEndpoints();
    const endpoint = this.get("system", "streaming_endpoint", "us-west-2");
    return endpoints.filter((e: Endpoint) => e.id == endpoint)[0];
  }

  getStreamingEndpoints(): Endpoint[] {
    return this.get("system", "streaming_endpoints", [
      {
        id: "us-west-2",
        name: "US West Coast",
        address: "stream-us-west-2.serenade.ai",
      },
      {
        id: "us-east-1",
        name: "US East Coast",
        address: "stream-us-east-1.serenade.ai",
      },
      {
        id: "eu-west-2",
        name: "Europe",
        address: "stream-eu-west-2.serenade.ai",
      },
      {
        id: "local",
        name: "Local",
        address: "localhost:17200",
      },
    ]);
  }

  getStylers(): any {
    return this.get("user", "stylers", {});
  }

  getTextInputKeybinding(): string {
    return this.get("user", "text_input_keybinding", "Ctrl+Alt+Space");
  }

  getToken(): string {
    return this.get("system", "token", "");
  }

  getUseAccessibilityApi(): string[] {
    return this.get(
      "user",
      "use_accessibility_api",
      os.platform() == "darwin" ? ["slack", "textedit"] : []
    );
  }

  getUseMiniModeHideTimeout(): boolean {
    return this.get("user", "mini_mode_timeout", false);
  }

  getUseMiniModeFewerAlternatives(): boolean {
    return this.get("user", "mini_mode_fewer_alternatives", false);
  }

  getUseVerboseLogging(): boolean {
    return this.get("user", "verbose_logging", false);
  }

  async loadWordsFileIfNeeded() {
    const modified = (await fs.stat(this.wordsFile())).mtime.getTime();
    if (modified > this.wordsLastLoad) {
      this.load();
      this.wordsLastLoad = modified;
    }
  }

  path() {
    return `${os.homedir()}/.serenade`;
  }

  setAnimations(animations: boolean) {
    this.set("user", "animations", animations);
  }

  setBounds(bounds: { x: number; y: number; width: number; height: number }) {
    this.set("system", "bounds", bounds);
  }

  setChunkSilenceThreshold(threshold: number) {
    return this.set("user", "chunk_silence_threshold", threshold);
  }

  setChunkSpeechThreshold(threshold: number) {
    return this.set("user", "chunk_speech_threshold", threshold);
  }

  setClipboardInsert(clipboardInsert: boolean) {
    return this.set("user", "clipboard_insert", clipboardInsert);
  }

  setContinueRunningInTray(continueRunningInTray: boolean) {
    this.set("user", "continue_running_in_tray", continueRunningInTray);
  }

  setDarkMode(darkMode: string) {
    this.set("user", "dark_mode", darkMode);
  }

  setDisableSuggestions(disableSuggestions: boolean) {
    return this.set("user", "disable_suggestions", disableSuggestions);
  }

  setEditorAutocomplete(autocomplete: boolean) {
    this.set("user", "autocomplete", autocomplete);
  }

  setExecuteSilenceThreshold(threshold: number) {
    return this.set("user", "execute_silence_threshold", threshold);
  }

  setInstalled(installed: boolean) {
    this.set("system", "installed", installed);
  }

  setLogAudio(logAudio: boolean) {
    this.set("user", "log_audio", logAudio);
  }

  setLogSource(logSource: boolean) {
    this.set("user", "log_source", logSource);
  }

  setMicrophone(microphone: any) {
    return this.set("system", "microphone", microphone);
  }

  setMinimizedPosition(position: string) {
    this.set("user", "minimized_position", position);
  }

  setMiniMode(miniMode: boolean) {
    this.set("user", "mini_mode", miniMode);
  }

  setMiniModeFewerAlternativesCount(fewerAlternativesCount: number) {
    this.set("user", "mini_mode_fewer_alternatives_count", fewerAlternativesCount);
  }

  setMiniModeHideTimeout(timeout: number) {
    this.set("user", "mini_mode_timeout_value", timeout);
  }

  setMiniModeReversed(reversed: boolean) {
    return this.set("user", "mini_mode_reversed", reversed);
  }

  setNuxCompleted(completed: boolean) {
    this.set("system", "nux_completed", completed);
  }

  setNuxStep(step: number) {
    this.set("system", "nux_step", step);
  }

  setNuxTutorialName(name: string) {
    this.set("system", "nux_tutorial_name", name);
  }

  setPluginInstalled(plugin: string) {
    let data = this.dataForFile("system");
    if (!data.plugins) {
      data.plugins = [];
    }

    if (!data.plugins.includes(plugin)) {
      data.plugins.push(plugin);
    }
  }

  setPushToTalk(pushToTalk: string) {
    this.set("user", "push_to_talk", pushToTalk);
  }

  setShowRevisionBox(data: any): any {
    this.set("user", "show_revision_box", {
      ...this.getShowRevisionBox(),
      ...data,
    });
  }

  setStreamingEndpoint(endpoint: string) {
    this.set("system", "streaming_endpoint", endpoint);
  }

  setStylers(stylers: any) {
    this.set("user", "stylers", stylers);
  }

  setTextInputKeybinding(textInputKeybinding: string) {
    this.set("user", "text_input_keybinding", textInputKeybinding);
  }

  setToken(token: string) {
    this.set("system", "token", token);
  }

  setUseMiniModeFewerAlternatives(fewerAlternatives: boolean) {
    this.set("user", "mini_mode_fewer_alternatives", fewerAlternatives);
  }

  setUseMiniModeHideTimeout(timeout: boolean) {
    this.set("user", "mini_mode_timeout", timeout);
  }

  setUseVerboseLogging(verboseLogging: boolean) {
    this.set("user", "verbose_logging", verboseLogging);
  }
}
