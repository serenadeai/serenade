import React from "react";
import { LargeCodeTranscript, Transcript } from "../../transcript";
import { Link } from "../../link";
import { CodeTable } from "../code-table";
import { ExampleTable } from "../example-table";

export const Intro: React.FC = () => (
  <>
    <p>Serenade has two different commands for writing code.</p>
    <ul>
      <li>
        <Transcript text="insert" /> inserts code right at your cursor, which is helpful for
        inserting text onto an existing line.
      </li>
      <li>
        <Transcript text="add" /> moves your cursor intelligently based on what you say (e.g., "add
        parameter"), and is used for writing new statements and larger blocks of code, like
        functions and classes.
      </li>
    </ul>
    <p>
      As you'll see, Serenade uses machine learning under the hood to handle many formatting details
      for you, so you don't have to manually dictate symbols and formatting details like underscores
      vs. camel case formatting. For instance, if you have a variable <code>foo_bar</code> in scope
      and you say <Transcript text="insert foo bar" />, Serenade will automatically format your code
      as <code>foo_bar</code>, so you don't have to specify the underscore.
    </p>
  </>
);

export const Insert: React.FC = () => (
  <>
    <p>
      The <Transcript text="insert" /> command will insert code at the current cursor position.{" "}
      <Transcript text="insert" /> is useful for appending text to an existing line of code or
      inserting text into the middle of a line.
    </p>
    <p>
      <Transcript text="insert" /> commands look like:
    </p>
    <p>
      <LargeCodeTranscript text="insert (above | below)? <code>" />
    </p>
    <CodeTable examples={["insertSimple", "insertEnclosures", "insertAbove"]} />
  </>
);

export const Add: React.FC = () => (
  <>
    <p>
      <Transcript text="add" /> is used for writing new lines or blocks of code. This command will
      intelligently position your cursor and handle boilerplate for you. For instance,{" "}
      <Transcript text="add parameter foo" /> will move your cursor to the nearest parameter list,
      and then create a new parameter.
    </p>
    <p>
      Below are just some examples of <Transcript text="add" /> commands—you can use any{" "}
      <Link to="#code-selectors">selector</Link>. All <Transcript text="add" /> commands have the
      same form:
    </p>
    <p>
      <LargeCodeTranscript text="add <selector> <code>" />
    </p>
    <CodeTable
      examples={[
        "addFunction",
        "addClass",
        "addStatement",
        "addElseIf",
        "addImport",
        "addArgument",
        "addComment",
        "addDecorator",
        "addEnum",
        "addIf",
        "addLambda",
        "addMethod",
        "addProperty",
        "addWhile",
      ]}
    />
  </>
);

export const RawText: React.FC = () => (
  <>
    <p>
      <Transcript text="system" /> inserts text wherever your cursor has focus, even if no Serenade
      plugin is present. For instance, you can use <Transcript text="system" /> to type into VS
      Code's "Find" dialog box rather than the main text editor. <Transcript text="system" /> also
      doesn't do much formatting for you, which can be useful if you want to drop down to a
      lower-level and manually specify all formatting, spacing, etc.
    </p>
    <CodeTable examples={["systemSimple"]} />
  </>
);
