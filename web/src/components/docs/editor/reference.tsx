import React, { useEffect, useState } from "react";
import { Transcript } from "../../transcript";
import { ExampleTable } from "../example-table";
import { FixedColumnTable } from "../fixed-column-table";
import * as data from "../data";

export const Intro: React.FC = () => (
  <>
    <p>
      This section has a few difference references that you can use as cheat sheets while learning
      Serenade.
    </p>
  </>
);

export const CodeSelectors: React.FC = () => {
  const [mobile, setMobile] = useState(false);
  useEffect(() => {
    setMobile(window.innerWidth < 992);
  }, []);

  return (
    <>
      <p>
        Code selectors can be used to describe a block of code you want to reference, like a
        function, argument, or class. You can use any of these selectors in commands like{" "}
        <Transcript text="go to" />, <Transcript text="delete" />, <Transcript text="change" />, or
        any command that contains a selector.
      </p>
      <p>The following selectors work for supported languages:</p>
      <FixedColumnTable columns={mobile ? 2 : 4} items={data.codeSelectors} />
    </>
  );
};

export const SelectorModifiers: React.FC = () => (
  <>
    <p>
      Below is a list of common modifiers you can use with any selector above. Modifiers can also be
      combined (e.g., <Transcript text="delete next two words" />
      ).
    </p>
    <ExampleTable
      rows={[
        ["next | previous", "The next or previous instance", ["next function", "previous class"]],
        ["first | second | ...", "The index in a list", ["second parameter", "first word"]],
        ["one | two | ...", "The index in a list", ["line five", "argument two"]],
        ["start | end", "The start or end", ["start of line", "end of method"]],
        ["<number> <objects>", "Multiple objects at once", ["three words", "four functions"]],
        ["X to Y", "A range of instances", ["lines five to ten", "words one to three"]],
        [
          "to start, to end",
          "From the cursor to the start or end",
          ["to end of next word", "to previous line"],
        ],
        [
          "<object> <name>",
          "The object with the given name",
          ["go to foo bar", "function factorial", "class dog"],
        ],
        ["phrase", "Escaped, literal text", ["phrase function"]],
      ]}
    />
  </>
);

export const TextSelectors: React.FC = () => (
  <>
    <p>
      Text selectors can be used to describe a block of text you want to reference, independent of
      any programming language. You can use any of these selectors in commands like{" "}
      <Transcript text="go to" />, <Transcript text="delete" />, <Transcript text="change" />, or
      any command that contains a selector.
    </p>
    <p>
      The following selectors work in any file, regardless of whether or not Serenade supports the
      language:
    </p>
    <FixedColumnTable columns={3} items={data.textSelectors} />
  </>
);
