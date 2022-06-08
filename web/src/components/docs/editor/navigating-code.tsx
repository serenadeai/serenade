import React from "react";
import { LargeCodeTranscript, Transcript } from "../../transcript";
import { Link } from "../../link";
import { CodeTable } from "../code-table";

export const Intro: React.FC = () => (
  <>
    <p>
      The <Transcript text="go to" /> command moves your cursor around the file. With this command,
      you can jump to any text in your file (e.g., <Transcript text="go to random" />
      ), or any <Link to="#code-selectors">selector</Link> (e.g., <Transcript text="go to class" />
      ). Since the <Transcript text="go to" /> command is so common, if you just say a selector,
      like <Transcript text="next function" /> or <Transcript text="second parameter" />, Serenade
      will implicitly use the <Transcript text="go to" /> command.
    </p>
    <p>
      Below are just some examples of <Transcript text="go to" /> commands—you can use any{" "}
      <Link to="#code-selectors">selector</Link>. All <Transcript text="go to" /> commands have the
      same form:
    </p>
    <p>
      <LargeCodeTranscript text="go to <selector>" />
    </p>
    <CodeTable examples={["goToLine", "goToText", "goToIndex", "goToObject"]} />
    <p>
      To move your cursor to the literal text matching a selector name, rather than the selector
      itself, you can say <Transcript text="go to phrase" />. For instance, you might want to move
      your cursor to the literal word "parameter" rather than a parameter to a function, so you
      could say <Transcript text="go to phrase parameter" />. This is similar to how{" "}
      <Transcript text="escape" /> works in <Transcript text="add" /> and{" "}
      <Transcript text="insert" /> commands.
    </p>
    <p>
      Finally, you can move your cursor by speaking a direction: <Transcript text="up" />,
      <Transcript text="down" />,
      <Transcript text="left" />, and <Transcript text="right" />.
    </p>
  </>
);
