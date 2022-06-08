import React, { useState } from "react";
import classNames from "classnames";
import { GradientButton } from "./buttons";
import { Snippet } from "./snippet";

export const CustomCommands: React.FC<{ snippets: any[] }> = ({ snippets }) => {
  const [shown, setShown] = useState(0);

  const snippet = (i: number, code: string) =>
    shown != i ? null : <Snippet key={i} code={code} />;

  const button = (i: number, text: string) => (
    <div className="inline-block mb-5 mr-4 md:mb-3 md:mr-3">
      <GradientButton
        key={i}
        light={i != shown}
        text={text}
        outerStyleOverrides={{
          fontSize: "1.125rem",
          lineHeight: "1.75rem",
        }}
        onClick={(e: React.MouseEvent) => {
          e.preventDefault();
          setShown(i);
        }}
      />
    </div>
  );

  return (
    <div className="mx-auto">
      <div className="mx-auto text-center">{snippets.map((e) => button(e.id, e.title))}</div>
      <div className="mx-auto w-max max-w-full text-sm -mt-2 md:mt-0 md:text-xl">
        {snippets.map((e) => snippet(e.id, e.content))}
      </div>
    </div>
  );
};
