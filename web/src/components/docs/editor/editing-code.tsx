import React from "react";
import { LargeCodeTranscript, Transcript } from "../../transcript";
import { Link } from "../../link";
import { CodeTable } from "../code-table";

export const Intro: React.FC = () => (
  <>
    <p>
      In this section, we'll take a look at voice commands you can use for editing existing code,
      like <Transcript text="change" />, and <Transcript text="delete" />, as well as more powerful
      refactoring commands.
    </p>
  </>
);

export const Changing: React.FC = () => (
  <>
    <p>
      The <Transcript text="change" /> command selects the nearest match to the cursor and replaces
      it with some text.
    </p>
    <p>
      <LargeCodeTranscript text="change <selector> to <code>" />
    </p>
    <CodeTable examples={["changeObject", "changeText", "rename"]} />
    <p>You can also copy, cut, and paste code.</p>
    <CodeTable examples={["copy", "cut", "select", "paste"]} />
  </>
);

export const Deleting: React.FC = () => (
  <>
    <p>
      You can delete code with the <Transcript text="delete" /> command. Below are just some
      examples of <Transcript text="delete" /> commands—you can use any{" "}
      <Link to="#code-selectors">selector</Link>. All <Transcript text="delete" /> commands have the
      same form:
    </p>
    <p>
      <LargeCodeTranscript text="delete <selector>" />
    </p>
    <CodeTable examples={["deleteLine", "deleteObject", "deleteRange", "deleteToEndpoint"]} />
  </>
);

export const Refactoring: React.FC = () => (
  <>
    <p>
      Serenade supports a variety of commands for refactoring code. All refactoring commands have
      the same form:
    </p>
    <LargeCodeTranscript text="<operation> <selector>" />
    <CodeTable
      examples={[
        "comment",
        "uncomment",
        "indent",
        "dedent",
        "surround",
        "duplicateBlock",
        "duplicateFunction",
        "shift",
        "move",
        "joinLines",
      ]}
    />
  </>
);
