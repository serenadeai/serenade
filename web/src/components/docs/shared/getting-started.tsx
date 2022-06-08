import React from "react";
import { LargeCodeTranscript, Transcript } from "../../transcript";
import { Link } from "../../link";
import { ExampleTable } from "../example-table";

export const Intro: React.FC = () => (
  <>
    <p>
      Welcome to the Serenade docs! Here, you'll find an overview of everything Serenade can do,
      along with examples. First, let's get Serenade installed on your device.
    </p>
  </>
);

export const Installation: React.FC = () => (
  <>
    <p>
      Serenade is freely available for macOS, Windows, and Linux. To download Serenade, head to{" "}
      <Link to="/download">https://serenade.ai/download.</Link>
    </p>
    <p>
      After installation, Serenade will walk you through installing plugins for supported
      applications, like VS Code, Chrome, and Hyper. Serenade will then guide you through
      interactive tutorials to practice voice coding.
    </p>
    <p>
      Serenade can be configured to use cloud-based speech recognition or local speech recognition.
      To change this setting, just head to Settings > Server. Regardless of your setting, you can
      also opt into sharing your data with Serenade in order to help improve Serenade's
      speech-to-code machine learning models.
    </p>
  </>
);

export const Setup: React.FC = () => (
  <>
    <video controls className="max-w-lg py-4 w-full">
      <source src="https://cdn.serenade.ai/web/video/javascript-intro.mp4#t=0.1" type="video/mp4" />
    </video>
    <p>
      Serenade floats above all your other windows, so you can keep it side-by-side with other
      applications, like your code editor. You can toggle Serenade by clicking the Listening switch
      or pressing Alt+Space. Then, as you speak, you'll see a list of transcripts in the Serenade
      window.
    </p>
    <p>
      Sometimes, Serenade isn't sure what you said, so you'll see a few different options. The first
      one will be used automatically, but to use a different option (and undo the first one), just
      say the number you want to use instead. For instance, to use the second option, just say{" "}
      <Transcript text="two" />. If none of the options are right, you can just say{" "}
      <Transcript text="undo" />.
    </p>
  </>
);

export const Basics: React.FC = () => (
  <>
    <p>As you'll see throughout these docs, most Serenade voice commands have the same form:</p>
    <p>
      <LargeCodeTranscript text="<action> <selector>" />
    </p>
    <p>
      The <code>action</code> is something you want to do to your code. Common actions include{" "}
      <Transcript text="add" /> to add a new line of code, <Transcript text="change" /> to edit
      code, and <Transcript text="delete" /> to remove code.
    </p>
    <p>
      A <code>selector</code> is a block of code to operate on. Some selectors are text-based, like{" "}
      <Transcript text="line" /> or <Transcript text="word" />. Even more powerful selectors are
      code-based, which enable you to reference parts of your code, including{" "}
      <Transcript text="function" />, <Transcript text="class" />, and{" "}
      <Transcript text="return value" />. For a complete list of selectors, see the{" "}
      <Link to="#code-selectors">Reference section</Link>.
    </p>
    <p>
      Serenade commands simply combine an action and a selector:{" "}
      <Transcript text="add function hello" />, <Transcript text="change parameter to number" />,{" "}
      and <Transcript text="copy lines five to ten" />.
    </p>
    <p>
      You can also chain commands together without pausing. For instance, you can say{" "}
      <Transcript text="save focus terminal" /> to save your current file and then focus your
      terminal, or <Transcript text="start of class add method hello" /> to add a new method at the
      start of a class.
    </p>
    <p>
      Finally, you can also specify how many times a command should be executed. For instance{" "}
      <Transcript text="indent three times" /> will run the indent command three times.
    </p>
  </>
);

export const CommonCommands: React.FC = () => (
  <>
    <p>
      Below is a compact list of commonly-used Serenade commands that you can use as a reference or
      cheat sheet as you're learning voice coding.
    </p>
    <ExampleTable
      rows={[
        [
          "go to <selector>",
          "Move your cursor around a file",
          ["go to line fifty", "go to next function"],
        ],
        [
          "add <code>",
          "Add a new statement or construct",
          ["add return false", "add class exception"],
        ],
        ["insert <code>", "Insert text at your cursor", ["insert foo of bar plus baz"]],
        [
          "change <old> to <new>",
          "Change existing text",
          ["change hello to goodbye", "change return value to false"],
        ],
        ["delete <selector>", "Delete text", ["delete foo bar", "delete next function"]],
        [
          "copy/paste <selector>",
          "Copy, cut, paste",
          ["copy method", "cut previous two words", "paste"],
        ],
        ["indent/dedent", "Change the indentation level of code", ["indent block", "dedent if"]],
        ["save", "Save the current file", ["save"]],
        ["undo", "Undo the last operation", ["undo"]],
        ["open <text>", "Open a new file", ["open react.js"]],
        ["(next | previous) tab", "Switch tabs in your editor", ["next tab", "previous tab"]],
        ["focus <text>", "Switch apps", ["focus code", "focus chrome"]],
      ]}
    />
  </>
);
