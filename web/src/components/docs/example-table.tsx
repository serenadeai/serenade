import React from "react";
import { Transcript } from "../transcript";

export const ExampleTable: React.FC<{ rows: any[]; title?: string; description?: string }> = ({
  rows,
  title,
  description,
}) => {
  const syntax = rows[0].length == 3;
  return (
    <>
      {title ? <h4>{title}</h4> : null}
      {description}
      <table className="my-4">
        <thead>
          <tr className="text-white bg-slate-600">
            <th className="py-2">Command</th>
            <th>Description</th>
            {syntax ? <th>Examples</th> : null}
          </tr>
        </thead>
        <tbody>
          {rows.map((e: any[], i: number) =>
            syntax ? (
              <tr key={e[0]} className={i % 2 == 1 ? "bg-gray-100" : ""}>
                <td className="p-2">
                  <div>
                    <pre className="whitespace-pre-wrap">{e[0]}</pre>
                  </div>
                </td>
                <td className="p-2">{e[1]}</td>
                <td className="p-2">
                  {e[2].map((t: any) => (
                    <span key={t} className="mr-1 inline-block">
                      <Transcript text={t} />
                    </span>
                  ))}
                </td>
              </tr>
            ) : (
              <tr key={e[1]} className={i % 2 == 1 ? "bg-gray-100" : ""}>
                <td className="p-2">
                  {e[0].map((t: any) => (
                    <span key={t} className="mr-1 inline-block">
                      <Transcript text={t} />
                    </span>
                  ))}
                </td>
                <td className="p-2">{e[1]}</td>
              </tr>
            )
          )}
        </tbody>
      </table>
    </>
  );
};
