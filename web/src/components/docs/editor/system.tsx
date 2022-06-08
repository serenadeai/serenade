import React from "react";
import { ExampleTable } from "../example-table";
import { Link } from "../../link";
import { Subsubheading } from "../headings";
import { LargeCodeTranscript, Transcript } from "../../transcript";

export const Intro: React.FC = () => (
  <>
    <p>
      Serenade doesn't just work for applications that have a native Serenade plugin—you can control
      any application with voice using Serenade. No matter what application you're using, you can
      use commands like <Transcript text="system" /> to insert text, <Transcript text="press" /> to
      press keys (to trigger shortcuts), and <Transcript text="focus" /> to switch apps.
    </p>
  </>
);

export const ApplicationControl: React.FC = () => (
  <>
    <ExampleTable
      rows={[
        [["pause", "stop listening"], "Pause Serenade"],
        [["repeat", "repeat change"], "Repeat the last command matching some text"],
        [["copy"], "Copy to the clipboard"],
        [["cut"], "Cut to the clipboard"],
        [["paste"], "Paste from the clipboard"],
        [["new tab", "create tab"], "Create a new tab"],
        [["close tab"], "Close the active tab"],
        [["first tab", "second tab", "tab one", "tab two"], "Switch tabs"],
        [["undo"], "Trigger an undo"],
        [["redo"], "Trigger an redo"],
        [["focus chrome", "focus slack"], "Bring an app to the foreground"],
        [["launch atom", "launch discord"], "Launch an app"],
        [["close terminal", "close zoom"], "Close an app"],
      ]}
    />
  </>
);

export const RevisionBox: React.FC = () => (
  <>
    <p>
      Serenade uses a combination of plugins and OS-level accessibility APIs to manipulate text no
      matter what application you're using. However, some applications don't properly implement
      accessibility APIs, and so Serenade can't read their text fields. When you're using one of
      these applications, you can use Serenade's Revision Box to format your text. Inside of the
      Revision Box, all of Serenade's commands will work properly, and when you're done, Serenade
      can copy the text in the Revision Box back into the application you had focused.
    </p>
    <p>
      The Revision Box will open automatically when Serenade can't read the current field (if
      enabled in settings). You can also say <Transcript text="revision box" /> to open the revision
      box yourself. Or, say <Transcript text="edit" /> to copy the current text field into the
      revision box.
    </p>
    <p>
      When Revision Box is open, you can say <Transcript text="close" /> to close and and ts
      contents into the field you were editing. You can also say <Transcript text="enter" /> to also
      press enter after inserting (e.g., for sending a message) or <Transcript text="copy" /> to
      copy the text to the clipboard rather than inserting it.
    </p>
    <p>
      For more information on customizing the behavior of the revision box, see{" "}
      <Link to="/docs/api#revision-box">the API docs</Link>
    </p>
    .
    <ExampleTable
      rows={[
        [["revision box"], "Open an empty revision box"],
        [["edit"], "Open a revision box with the current text field"],
        [["copy"], "Close the revision box and copy its contents to the clipboard"],
        [["close"], "Close the revision box and paste its contents into the current text field"],
        [["enter"], "Same as close, but also press enter afterward"],
      ]}
    />
  </>
);

export const EditorIntegrations: React.FC = () => (
  <>
    <ExampleTable
      rows={[
        [["save"], "Save the current file"],
        [["style file", "format"], "Format the current file"],
        [["open index dot js"], "Open a file"],
        [["add breakpoint"], "Create a breakpoint"],
        [["remove breakpoint"], "Remove a breakpoint"],
        [["toggle breakpoint"], "Toggle a breakpoint"],
        [["start debugging"], "Start a debugger session"],
        [["stop debugging"], "Stop the current debugger session"],
        [["step into", "step out", "step over"], "Move the debugger step-by-step"],
        [["continue"], "Continue the debugger to the next breakpoint"],
      ]}
    />
  </>
);

export const KeyboardMouse: React.FC = () => (
  <>
    <ExampleTable
      rows={[
        [["press enter", "press control tab", "press command k"], "Press a key combination"],
        [["system gmail dot com"], "Type a string using keystrokes"],
        [["run make", "run npm test"], "Type a string and press enter"],
        [["click", "left click", "right click"], "Trigger a mouse click"],
      ]}
    />
  </>
);

export const Modes: React.FC = () => (
  <>
    <p>
      Serenade has a few different configurable modes to change how your voice commands are
      interpreted.
    </p>
    <Subsubheading title="Command/Dictate Mode" />
    <p>
      Serenade's default mode is Command Mode. In this mode, if you say something that isn't a valid
      Serenade command, Serenade will show an x next to the command and won't do anything. To enable
      command mode, just say <Transcript text="command mode" />.
    </p>
    <p>
      Sometimes, like when dictating a longer block of text, you want Serenade to simply type out
      everything you're saying, rather than listen for other commands. Serenade's Dictate Mode does
      just that. To enable Dictate Mode, just say <Transcript text="dictate mode" />, and then
      Serenade will type out everything you say, rather than listening for valid commands. Some
      commands, like <Transcript text="undo" />, <Transcript text="repeat" />, and
      <Transcript text="stop listening" />, will also be presented as alternatives so you don't have
      to leave Dictate Mode to use them. To get back to normal mode, just say{" "}
      <Transcript text="command mode" />.
    </p>
    <Subsubheading title="Language Modes" />
    <p>
      By default, Serenade will auto-detect which programming language is being used based on your
      editor's filename or active language setting. Sometimes, you might want to override this
      behavior and specify which language Serenade should use. To do so, simply say{" "}
      <Transcript text="python mode" />, <Transcript text="javascript mode" />, and so on. To get
      re-enable auto-detection, say <Transcript text="auto mode" />.
    </p>
  </>
);
