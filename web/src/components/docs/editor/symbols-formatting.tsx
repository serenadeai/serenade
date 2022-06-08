import React from "react";
import { Transcript } from "../../transcript";
import { CodeTable } from "../code-table";
import { ExampleTable } from "../example-table";
import * as data from "../data";

export const Intro: React.FC = () => (
  <>
    <p>
      Serenade's speech-to-code engine handles many symbols and formatting details automatically,
      and you can also manually specify text styles and symbols.
    </p>
  </>
);

export const Symbols: React.FC = () => (
  <>
    <p>
      When speaking any command, you can also include symbols. Here's a list of all of the symbols
      supported by Serenade:
    </p>
    <div style={{ overflowX: "scroll" }}>{data.table(data.symbols(), 3)}</div>
    <p>
      You can also enclose text in symbols like braces, brackets, and parens. Here's a list of all
      of the enclosure symbols supported by Serenade:
    </p>
    {data.table(data.enclosures(), 2)}
    <p>
      If you want to use the literal text representation of a symbol (e.g., the word{" "}
      <code>dash</code> rather than the <code>-</code> character, you can escape it by saying{" "}
      <Transcript text="escape" />.
    </p>
    <p>
      Below are just a few examples of commands using symbols—you can use any of the symbols above.
    </p>
    <CodeTable examples={["addSymbol", "insertEnclosure", "insertEscape"]} />
  </>
);

export const TextFormatting: React.FC = () => (
  <>
    <p>
      To specify camel case, underscores, and so on, you can specify a style in any{" "}
      <Transcript text="insert" />, <Transcript text="add" />, etc. command. Below is a list of text
      styles:
    </p>
    <ExampleTable
      rows={[
        [["lowercase"], "sing song"],
        [["camel"], "singSong"],
        [["underscores", "snake"], "sing_song"],
        [["pascal"], "SingSong"],
        [["all caps"], "SING_SONG"],
        [["dashes"], "sing-song"],
        [["one word"], "singsong"],
      ]}
    />
    <p>
      When writing code with <Transcript text="add" /> and <Transcript text="insert" />, you can
      specify text styles:
    </p>
    <CodeTable examples={["addFormatting", "insertFormatting"]} />
    <p>You can also style existing text by describing a style, followed by a selector to change:</p>
    <CodeTable examples={["textStyleSimple", "textStyleMultiple"]} />
  </>
);
