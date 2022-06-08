import React from "react";
import { GradientButton } from "./buttons";

export const LargeCodeTranscript: React.FC<{ text: string }> = ({ text }) => (
  <code className="text-md mt-2 block">
    <Transcript text={text} innerStyleOverrides={{ padding: "0.5rem 1rem" }} />
  </code>
);

export const Transcript: React.FC<{
  innerStyleOverrides?: { [k: string]: any };
  outerStyleOverrides?: { [k: string]: any };
  text: string;
}> = ({ innerStyleOverrides, outerStyleOverrides, text }) => (
  <GradientButton
    light={true}
    text={text}
    innerStyleOverrides={innerStyleOverrides}
    outerStyleOverrides={outerStyleOverrides}
  />
);
