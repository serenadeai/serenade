import * as os from "os";
import Active from "../active";
import InsertHistory from "./insert-history";
import RevisionBoxWindow from "../windows/revision-box";
import System from "./system";
import { core } from "../../gen/core";

interface Operation {
  execute(): void;
  keystrokesCount(): number;
}

class CompositeOperation implements Operation {
  constructor(private operations: Operation[]) {}

  async execute() {
    for (const operation of this.operations) {
      await operation.execute();
    }
  }

  keystrokesCount(): number {
    let total = 0;
    for (const operation of this.operations) {
      total += operation.keystrokesCount();
    }

    return total;
  }
}

class KeylessOperation implements Operation {
  constructor(private inner: () => void) {}

  async execute() {
    this.inner();
  }

  keystrokesCount(): number {
    return 0;
  }
}

class MoveCursor implements Operation {
  constructor(private system: System, private initial: number, private final: number) {}

  async execute() {
    let change = this.final - this.initial;
    if (change > 0) {
      await this.system.pressKey("right", [], change);
    } else if (change < 0) {
      await this.system.pressKey("left", [], -change);
    }
  }

  keystrokesCount(): number {
    return Math.abs(this.final - this.initial);
  }
}

class PressKey implements Operation {
  constructor(
    private system: System,
    private key: string,
    private modifiers: string[],
    private count: number
  ) {}

  async execute() {
    await this.system.pressKey(this.key, this.modifiers, this.count);
  }

  keystrokesCount(): number {
    return this.count;
  }
}

class TypeText implements Operation {
  constructor(
    private active: Active,
    private insertHistory: InsertHistory,
    private system: System,
    private text: string
  ) {}

  async execute() {
    this.insertHistory.add(this.text, this.active.app);
    await this.system.typeText(this.text, this.active.app);
  }

  keystrokesCount(): number {
    return this.text.length;
  }
}

class Substitution {
  constructor(
    private active: Active,
    private insertHistory: InsertHistory,
    private system: System,
    private start: number,
    private stop: number,
    private substitution: string,
    private cursor: number
  ) {}

  operation(): Operation {
    return new CompositeOperation([
      new MoveCursor(this.system, this.cursor, this.stop),
      new PressKey(this.system, "backspace", [], this.stop - this.start),
      new TypeText(this.active, this.insertHistory, this.system, this.substitution),
    ]);
  }

  finalCursor(): number {
    return this.start + this.substitution.length;
  }
}

interface Command {
  apply(state: core.IEditorState): Operation;
  hasExpectedUndoSource(state: core.IEditorState): boolean;
  undo(state: core.IEditorState): Operation;
}

class RevisionBoxDiff implements Command {
  private beforeState?: core.IEditorState;

  constructor(private revisionBoxWindow: RevisionBoxWindow, private command: core.ICommand) {}

  apply(state: core.IEditorState): Operation {
    this.beforeState = state;
    return new KeylessOperation(() => {
      this.revisionBoxWindow.setEditorState({
        source: this.command.source!.toString(),
        cursor: this.command.cursor!,
        cursorEnd: this.command.cursorEnd!,
      });
    });
  }

  undo(state: core.IEditorState): Operation {
    return new KeylessOperation(() => {
      this.revisionBoxWindow.setEditorState({
        source: this.beforeState!.source!.toString(),
        cursor: this.beforeState!.cursor!,
        cursorEnd: 0,
      });
    });
  }

  hasExpectedUndoSource(): boolean {
    return true;
  }
}

class RevisionBoxPress implements Command {
  private beforeState?: core.IEditorState;

  constructor(
    private revisionBoxWindow: RevisionBoxWindow,
    private system: System,
    private command: core.ICommand
  ) {}

  apply(state: core.IEditorState) {
    this.beforeState = state;
    return new PressKey(
      this.system,
      this.command.text!,
      this.command.modifiers!,
      Math.max(1, this.command.index || 0)
    );
  }

  undo(): Operation {
    return new KeylessOperation(() => {
      this.revisionBoxWindow.setEditorState({
        source: this.beforeState!.source!.toString(),
        cursor: this.beforeState!.cursor!,
        cursorEnd: 0,
      });
    });
  }

  hasExpectedUndoSource(): boolean {
    return true;
  }
}

class Insert implements Command {
  private text: string;

  constructor(
    private active: Active,
    private insertHistory: InsertHistory,
    private system: System,
    text: string
  ) {
    this.text = text;
  }

  apply(state: core.IEditorState): Operation {
    return new TypeText(this.active, this.insertHistory, this.system, this.text);
  }

  undo(state: core.IEditorState): Operation {
    return new PressKey(this.system, "backspace", [], this.text.length);
  }

  hasExpectedUndoSource(state: core.IEditorState): boolean {
    return true;
  }
}

class NativeDiff implements Command {
  private beforeCursor: number = -1;
  private beforeSource: string = "";
  private afterSource: string = "";

  constructor(
    private active: Active,
    private insertHistory: InsertHistory,
    private system: System,
    private command: core.ICommand
  ) {}

  private normalize(text: string) {
    return text
      .toLowerCase()
      .trim()
      .replace(/[\u2018\u2019]/g, "'")
      .replace(/[\u201C\u201D]/g, '"')
      .replace(/[\u2013\u2014]/g, "-")
      .replace(/[\u2026]/g, "...");
  }

  apply(state: core.IEditorState): Operation {
    this.beforeCursor = state.cursor!;
    this.beforeSource = state.source!.toString();
    this.afterSource = this.command.source!.toString();

    let operations: Operation[] = [];
    let cursor = state.cursor!;
    for (let i = this.command.changes!.length - 1; i >= 0; i--) {
      const change = this.command.changes![i];
      let substitution = new Substitution(
        this.active,
        this.insertHistory,
        this.system,
        change.start!,
        change.stop!,
        change.substitution!,
        cursor
      );
      cursor = substitution.finalCursor();
      operations.push(substitution.operation());
    }

    operations.push(new MoveCursor(this.system, cursor, this.command.cursor!));
    return new CompositeOperation(operations);
  }

  undo(state: core.IEditorState): Operation {
    let operations: Operation[] = [];
    let cursor = state.cursor!;
    for (let i = 0; i < this.command.changes!.length; i++) {
      const change = this.command.changes![i];
      const stop = change.start! + change.substitution!.length;
      if (!state.canSetState) {
        cursor = stop;
      }

      const substitution = new Substitution(
        this.active,
        this.insertHistory,
        this.system,
        change.start!,
        stop,
        this.beforeSource.substring(change.start!, change.stop!),
        cursor
      );

      cursor = substitution.finalCursor();
      operations.push(substitution.operation());
    }

    operations.push(new MoveCursor(this.system, this.beforeCursor, cursor));
    return new CompositeOperation(operations);
  }

  hasExpectedUndoSource(state: core.IEditorState): boolean {
    return (
      !state.canSetState ||
      this.normalize(state.source!.toString()) == this.normalize(this.afterSource)
    );
  }
}

export default class NativeCommands {
  private maxUndoStackSize = 20;
  private nextCommandIndex: number = 0;
  private undoStack: Command[] = [];

  public maxKeystrokes = 250;
  public useNeedsUndo: boolean = false;

  constructor(
    private active: Active,
    private insertHistory: InsertHistory,
    private revisionBoxWindow: RevisionBoxWindow,
    private system: System
  ) {}

  private async applyCommand(command: Command) {
    const state = await this.active.getEditorState();
    if (command.apply(state).keystrokesCount() >= this.maxKeystrokes) {
      return;
    }

    this.useNeedsUndo = true;
    await (await command.apply(state)).execute();
    this.undoStack.splice(this.nextCommandIndex, this.undoStack.length - this.nextCommandIndex + 1);

    this.undoStack.push(command);
    while (this.undoStack.length > this.maxUndoStackSize) {
      this.undoStack.shift();
    }

    this.nextCommandIndex = this.undoStack.length;
  }

  async applyInsert(text: string) {
    await this.applyCommand(new Insert(this.active, this.insertHistory, this.system, text));
  }

  async applyNativeDiff(command: core.ICommand) {
    await this.applyCommand(new NativeDiff(this.active, this.insertHistory, this.system, command));
  }

  async applyRevisionBoxDiff(command: core.ICommand) {
    await this.applyCommand(new RevisionBoxDiff(this.revisionBoxWindow, command));
  }

  async applyRevisionBoxPress(command: core.ICommand) {
    await this.applyCommand(new RevisionBoxPress(this.revisionBoxWindow, this.system, command));
  }

  canRedo() {
    return this.nextCommandIndex < this.undoStack.length;
  }

  canUndo(state: core.IEditorState): boolean {
    return (
      this.nextCommandIndex - 1 >= 0 &&
      this.undoStack[this.nextCommandIndex - 1].hasExpectedUndoSource(state)
    );
  }

  diffKeystrokesCount(state: core.IEditorState, command: core.ICommand) {
    return new NativeDiff(this.active, this.insertHistory, this.system, command)
      .apply(state)
      .keystrokesCount();
  }

  insertKeystrokesCount(state: core.IEditorState, text: string) {
    return new Insert(this.active, this.insertHistory, this.system, text)
      .apply(state)
      .keystrokesCount();
  }

  needsUndoStack(state: core.IEditorState): boolean {
    return !state.canSetState || this.revisionBoxWindow.shown();
  }

  async redo(state: core.IEditorState) {
    if (
      this.nextCommandIndex < this.undoStack.length &&
      this.undoStack[this.nextCommandIndex].apply(state).keystrokesCount() < this.maxKeystrokes
    ) {
      await this.undoStack[this.nextCommandIndex].apply(state).execute();
      this.nextCommandIndex++;
    }
  }

  redoKeystrokesCount(state: core.IEditorState) {
    return this.undoStack[this.nextCommandIndex].apply(state).keystrokesCount();
  }

  async undo(state: core.IEditorState) {
    if (
      this.nextCommandIndex - 1 >= 0 &&
      this.undoStack[this.nextCommandIndex - 1].hasExpectedUndoSource(state) &&
      this.undoStack[this.nextCommandIndex - 1].undo(state).keystrokesCount() < this.maxKeystrokes
    ) {
      this.nextCommandIndex--;
      await this.undoStack[this.nextCommandIndex].undo(state).execute();
    }
  }

  undoKeystrokesCount(state: core.IEditorState) {
    return this.undoStack[this.nextCommandIndex - 1].undo(state).keystrokesCount();
  }
}
