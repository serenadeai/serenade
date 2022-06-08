import React from "react";
import { ChevronRightIcon } from "@heroicons/react/solid";
import { Example, examplesByLanguage } from "./examples";
import { Snippet } from "../snippet";
import { Transcript } from "../transcript";
import { useStore } from "../../lib/store";

export const CodeTable: React.FC<{ examples: string[]; title?: string; description?: string }> = ({
  examples,
  title,
  description,
}) => {
  const { state, dispatch } = useStore();

  return (
    <>
      {title ? <h4>{title}</h4> : null}
      {description}
      <table className="my-4 code-table">
        <thead>
          <tr className="text-white bg-slate-600">
            <th className="py-2">Command</th>
            <th>Code</th>
          </tr>
        </thead>
        <tbody>
          {examples.map((e: string, i: number) => {
            let example = examplesByLanguage[state.language][e];
            if (!example) {
              example = examplesByLanguage["All"][e];
            }
            if (!example) {
              return null;
            }

            return (
              <tr key={example[0]} className={i % 2 == 1 ? "bg-gray-100" : ""}>
                <td className="p-2">
                  <Transcript text={example[0]} />
                </td>
                <td className="p-2">
                  {example[1] ? (
                    <pre
                      className="inline-block align-middle"
                      dangerouslySetInnerHTML={{ __html: example[1] }}
                    />
                  ) : null}
                  {example.length == 3 ? (
                    <>
                      <ChevronRightIcon className="w-4 inline-block align-middle" />
                      <pre
                        className="inline-block align-middle"
                        dangerouslySetInnerHTML={{ __html: example[2] }}
                      />
                    </>
                  ) : null}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </>
  );
};
