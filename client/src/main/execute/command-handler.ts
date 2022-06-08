import { clipboard } from "electron";
import { shell } from "electron";
import Active from "../active";
import App from "../app";
import ChunkManager from "../stream/chunk-manager";
import Custom from "../ipc/custom";
import Executor from "./executor";
import LanguageSwitcherWindow from "../windows/language-switcher";
import MainWindow from "../windows/main";
import NativeCommands from "./native-commands";
import NUX from "../nux";
import RendererBridge from "../bridge";
import RevisionBoxWindow from "../windows/revision-box";
import Settings from "../settings";
import Stream from "../stream/stream";
import System from "./system";
import { core } from "../../gen/core";

export default class CommandHandler {
  constructor(
    private active: Active,
    private app: App,
    private bridge: RendererBridge,
    private chunkManager: ChunkManager,
    private custom: Custom,
    private executor: Executor,
    private mainWindow: MainWindow,
    private nativeCommands: NativeCommands,
    private nux: NUX,
    private revisionBoxWindow: RevisionBoxWindow,
    private settings: Settings,
    private stream: Stream,
    private system: System,
    private languageSwitcherWindow: () => Promise<LanguageSwitcherWindow> | undefined
  ) {}

  private clearPending() {
    this.executor.clearPending();
    this.app.clearAlternativesAndShowExamples();
  }

  async COMMAND_TYPE_BACK(_data: core.ICommand): Promise<any> {
    this.nux.back(true);
  }

  async COMMAND_TYPE_CALLBACK(data: core.ICommand): Promise<any> {
    const command = this.active.customCommands.filter((e: any) => e.templated == data.path)[0];
    const state = await this.active.getEditorState();
    state.source = Buffer.from(data.source || "");
    state.cursor = data.cursor || 0;

    this.stream.sendCallbackRequest({
      type: data.callbackType,
      text: data.text,
    });
  }

  async COMMAND_TYPE_CANCEL(_data: core.ICommand): Promise<any> {
    this.clearPending();
    if (this.revisionBoxWindow.shown()) {
      this.revisionBoxWindow.hide("cancel");
    }
  }

  async COMMAND_TYPE_CLICK(data: core.ICommand): Promise<any> {
    // Click buttons in system dialogs. This is overloaded for clicks
    // via the Chrome plugin as well, but one of the actions will always
    // be a no-op since only one of Chrome or system dialog can be in
    // focus at a given time.
    if (data.path) {
      await this.system.clickButton(data.path);
    } else {
      const button = data.text || "left";
      await this.system.click(button);
    }
  }

  async COMMAND_TYPE_CLIPBOARD(data: core.ICommand): Promise<any> {
    await this.stream.sendEditorStateRequest(true);
    this.stream.sendCallbackRequest({
      type: core.CallbackType.CALLBACK_TYPE_PASTE,
      text: data.direction || "",
    });
  }

  async COMMAND_TYPE_COPY(data: core.ICommand): Promise<any> {
    clipboard.writeText(data.text || "");
  }

  async COMMAND_TYPE_CUSTOM(data: core.ICommand): Promise<any> {
    this.custom.execute(data.customCommandId!, data.replacements!);
  }

  async COMMAND_TYPE_DIFF(data: core.ICommand): Promise<any> {
    const state = await this.active.getEditorState();
    const trigger = this.settings.revisionBoxTrigger(this.active.app);
    if ((!state.canSetState && trigger == "auto") || trigger == "always") {
      await this.revisionBoxWindow.show();
    }

    if (this.revisionBoxWindow.shown()) {
      await this.nativeCommands.applyRevisionBoxDiff(data);
    } else if (!state.canSetState) {
      await this.nativeCommands.applyNativeDiff(data);
    }
  }

  async COMMAND_TYPE_FOCUS(data: core.ICommand): Promise<any> {
    await this.system.focus(data.text!);
  }

  async COMMAND_TYPE_HIDE_REVISION_BOX(data: any): Promise<any> {
    this.revisionBoxWindow.hide(data.text);
  }

  async COMMAND_TYPE_INSERT(data: core.ICommand): Promise<any> {
    await this.nativeCommands.applyInsert(data.source || data.text || "");
  }

  async COMMAND_TYPE_LANGUAGE_MODE(data: core.ICommand): Promise<any> {
    this.active.languageSwitcherLanguage = data.language!;
    this.bridge.setState(
      {
        languageSwitcherLanguage: data.language!,
      },
      [this.mainWindow, this.languageSwitcherWindow()]
    );
  }

  async COMMAND_TYPE_LAUNCH(data: core.ICommand): Promise<any> {
    await this.system.launch(data.text!);
  }

  async COMMAND_TYPE_OPEN_IN_BROWSER(data: core.ICommand): Promise<any> {
    await shell.openExternal(data.path!);
  }

  async COMMAND_TYPE_PAUSE(_data: core.ICommand): Promise<any> {
    this.clearPending();
    this.chunkManager.toggle(false);
  }

  async COMMAND_TYPE_PRESS(data: core.ICommand): Promise<any> {
    if (this.revisionBoxWindow.shown()) {
      await this.nativeCommands.applyRevisionBoxPress(data);
    } else {
      await this.system.pressKey(data.text!, data.modifiers!, Math.max(1, data.index || 0));
    }
  }

  async COMMAND_TYPE_REDO(_data: core.ICommand): Promise<any> {
    const state = await this.active.getEditorState();
    if (this.nativeCommands.needsUndoStack(state)) {
      await this.nativeCommands.redo(state);
    }
  }

  async COMMAND_TYPE_RUN(data: core.ICommand): Promise<any> {
    await this.nativeCommands.applyNativeDiff(data);
    await this.system.pressKey("enter");
  }

  async COMMAND_TYPE_QUIT(data: core.ICommand): Promise<any> {
    await this.system.quit(data.text!);
  }

  async COMMAND_TYPE_SELECT(data: core.ICommand): Promise<any> {
    if (this.revisionBoxWindow.shown()) {
      this.nativeCommands.applyRevisionBoxDiff(data);
    }
  }

  async COMMAND_TYPE_SHOW_REVISION_BOX(data: any): Promise<any> {
    this.revisionBoxWindow.show(data.text);
  }

  async COMMAND_TYPE_START_DICTATE(_data: any): Promise<any> {
    this.active.dictateMode = true;
    this.bridge.setState(
      {
        dictateMode: true,
      },
      [this.mainWindow]
    );
  }

  async COMMAND_TYPE_STOP_DICTATE(_data: any): Promise<any> {
    this.active.dictateMode = false;
    this.bridge.setState(
      {
        dictateMode: false,
      },
      [this.mainWindow]
    );
  }

  async COMMAND_TYPE_UNDO(_data: core.ICommand): Promise<any> {
    if (!this.settings.getNuxCompleted()) {
      await this.nux.showCurrentStep();
      return;
    }

    const state = await this.active.getEditorState();
    if (this.nativeCommands.needsUndoStack(state)) {
      await this.nativeCommands.undo(state);
    }
  }

  async COMMAND_TYPE_USE(data: core.ICommand): Promise<any> {
    const state = await this.active.getEditorState();
    if (this.nativeCommands.useNeedsUndo) {
      await this.nativeCommands.undo(state);
    }

    await this.executor.executePending(data.index! - 1);
  }
}
