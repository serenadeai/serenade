import React from "react";

export const codeSelectors: string[] = [
  "argument list",
  "argument",
  "annotation",
  "assert",
  "assignment",
  "assignment value",
  "assignment variable",
  "attribute",
  "attribute value",
  "attribute name",
  "attribute text",
  "body",
  "break",
  "case",
  "catch",
  "class",
  "close tag",
  "comment",
  "comment text",
  "condition",
  "constructor",
  "content",
  "continue",
  "declaration",
  "decorator",
  "dictionary",
  "element",
  "else if",
  "else",
  "entry",
  "enum",
  "except",
  "export",
  "extends",
  "field",
  "finally",
  "for",
  "function",
  "generator",
  "if",
  "import",
  "include",
  "interface",
  "implementation",
  "keyword argument",
  "keyword parameter",
  "key",
  "lambda",
  "line",
  "list",
  "member",
  "method",
  "mixin",
  "modifier",
  "namespace",
  "object",
  "open tag",
  "parameter list",
  "parameter value",
  "parameter",
  "parent",
  "property",
  "prototype",
  "return",
  "return type",
  "return value",
  "ruleset",
  "set",
  "setter",
  "statement",
  "string",
  "string text",
  "struct",
  "switch",
  "tag",
  "throw",
  "trait",
  "tuple",
  "try",
  "type",
  "with",
  "with alias",
  "with item",
  "while",
  "value",
  "variable",
];

export const textSelectors: string[] = [
  "all",
  "block",
  "character",
  "file",
  "letter",
  "line",
  "number",
  "phrase",
  "word",
];

export const symbolAndKeyData: {
  transcripts: string[];
  literal: string;
  key?: boolean;
  single?: boolean;
  enclosure?: boolean;
}[] = [
  { transcripts: ["plus"], literal: "+", single: true, key: true },
  { transcripts: ["dash", "minus"], literal: "-", single: true, key: true },
  { transcripts: ["star", "times"], literal: "*", single: true, key: true },
  { transcripts: ["slash", "divided by"], literal: "/", single: true, key: true },
  { transcripts: ["less than or equal to"], literal: "<=", single: true },
  { transcripts: ["less than"], literal: "<", single: true, key: true },
  { transcripts: ["greater than or equal to"], literal: ">=", single: true },
  { transcripts: ["greater than"], literal: ">", single: true, key: true },
  { transcripts: ["not equal"], literal: "!=", single: true },
  { transcripts: ["double equal"], literal: "==", single: true },
  { transcripts: ["triple equal"], literal: "===", single: true },
  { transcripts: ["equal"], literal: "=", single: true, key: true },
  { transcripts: ["left shift"], literal: "<<", single: true },
  { transcripts: ["right shift"], literal: ">>", single: true },
  { transcripts: ["and"], literal: "&&", single: true },
  { transcripts: ["or"], literal: "||", single: true },
  { transcripts: ["comma"], literal: ",", single: true, key: true },
  { transcripts: ["colon"], literal: ":", single: true, key: true },
  { transcripts: ["dot", "period"], literal: ".", single: true, key: true },
  { transcripts: ["underscore"], literal: "_", single: true, key: true },
  { transcripts: ["semicolon"], literal: ";", single: true, key: true },
  { transcripts: ["bang", "exclam"], literal: "!", single: true, key: true },
  { transcripts: ["question mark"], literal: "?", single: true, key: true },
  { transcripts: ["tilde"], literal: "~", single: true, key: true },
  { transcripts: ["percent", "mod"], literal: "%", single: true, key: true },
  { transcripts: ["at"], literal: "@", single: true, key: true },
  { transcripts: ["dollar"], literal: "$", single: true, key: true },
  { transcripts: ["right arrow"], literal: "->", single: true },
  { transcripts: ["arrow"], literal: "->", single: true },
  { transcripts: ["space"], literal: " ", single: true },
  { transcripts: ["backslash"], literal: "\\", single: true, key: true },
  { transcripts: ["hash"], literal: "#", single: true, key: true },
  { transcripts: ["caret"], literal: "^", single: true, key: true },
  { transcripts: ["ampersand"], literal: "&", single: true, key: true },
  { transcripts: ["backtick"], literal: "`", single: true, key: true },
  { transcripts: ["pipe"], literal: "|", single: true, key: true },
  { transcripts: ["left brace"], literal: "{", single: true, key: true },
  { transcripts: ["right brace"], literal: "}", single: true, key: true },
  { transcripts: ["left bracket"], literal: "[", single: true, key: true },
  { transcripts: ["right bracket"], literal: "]", single: true, key: true },
  { transcripts: ["single quote"], literal: "'", single: true, key: true },
  { transcripts: ["quote", "double quote"], literal: '"', single: true, key: true },
  { transcripts: ["braces"], literal: "{ }", enclosure: true },
  { transcripts: ["brackets"], literal: "[ ]", enclosure: true },
  { transcripts: ["comparators", "angle brackets"], literal: "< >", enclosure: true },
  { transcripts: ["quotes", "string"], literal: '" "', enclosure: true },
  { transcripts: ["single quotes"], literal: "' '", enclosure: true },
  { transcripts: ["triple quotes"], literal: '""" """', enclosure: true },
  { transcripts: ["of", "parens"], literal: "( )", enclosure: true },
  { transcripts: ["underscores"], literal: "_ _", enclosure: true },
  { transcripts: ["double underscores"], literal: "__ __", enclosure: true },
  { transcripts: ["tab"], literal: "<tab>", key: true },
  { transcripts: ["enter", "return"], literal: "<enter>", key: true },
  { transcripts: ["space"], literal: "<space>", key: true },
  { transcripts: ["delete"], literal: "<delete>", key: true },
  { transcripts: ["backspace"], literal: "<backspace>", key: true },
  { transcripts: ["up"], literal: "<up>", key: true },
  { transcripts: ["down"], literal: "<down>", key: true },
  { transcripts: ["left"], literal: "<left>", key: true },
  { transcripts: ["right"], literal: "<right>", key: true },
  { transcripts: ["escape"], literal: "<escape>", key: true },
  { transcripts: ["pageup"], literal: "<pageup>", key: true },
  { transcripts: ["pagedown"], literal: "<pagedown>", key: true },
  { transcripts: ["home"], literal: "<home>", key: true },
  { transcripts: ["end"], literal: "<end>", key: true },
  { transcripts: ["caps"], literal: "<caps lock>", key: true },
  { transcripts: ["shift"], literal: "<shift>", key: true },
  { transcripts: ["command"], literal: "<command>", key: true },
  { transcripts: ["control"], literal: "<control>", key: true },
  { transcripts: ["alt"], literal: "<alt>", key: true },
  { transcripts: ["option"], literal: "<option>", key: true },
  { transcripts: ["win", "windows"], literal: "<windows>", key: true },
  { transcripts: ["function", "fn"], literal: "<fn>", key: true },
  { transcripts: ["f1"], literal: "<F1>", key: true },
  { transcripts: ["f2"], literal: "<F2>", key: true },
  { transcripts: ["f3"], literal: "<F3>", key: true },
  { transcripts: ["f4"], literal: "<F4>", key: true },
  { transcripts: ["f5"], literal: "<F5>", key: true },
  { transcripts: ["f6"], literal: "<F6>", key: true },
  { transcripts: ["f7"], literal: "<F7>", key: true },
  { transcripts: ["f8"], literal: "<F8>", key: true },
  { transcripts: ["f9"], literal: "<F9>", key: true },
  { transcripts: ["f10"], literal: "<F10>", key: true },
  { transcripts: ["f11"], literal: "<F11>", key: true },
  { transcripts: ["f12"], literal: "<F12>", key: true },
];

export const enclosures = () => {
  return symbolAndKeyData.filter((e) => e.enclosure);
};

export const keys = () => {
  return symbolAndKeyData.filter((e) => e.key);
};

export const symbols = () => {
  return symbolAndKeyData.filter((e) => e.single);
};

export const table = (items, columns = 2, header = "Symbol") => {
  let thead = [];
  for (let i = 0; i < columns; i++) {
    thead.push(<th key={"t" + i}>Transcript</th>);
    thead.push(<th key={"h" + i}>{header}</th>);
  }

  let rows: any[][] = Array.from({ length: Math.ceil(items.length / columns) }, () => []);
  for (let i = 0; i < rows.length; i++) {
    for (let j = 0; j < columns; j++) {
      const item = items[i * columns + j];
      if (item) {
        rows[i].push(
          <td key={"t" + i + j} className="p-2">
            {item.transcripts.join(", ")}
          </td>
        );
        rows[i].push(
          <td key={"l" + i + j} className="p-2">
            <code>{item.literal}</code>
          </td>
        );
      }
    }
  }

  return (
    <table className="my-4">
      <thead>
        <tr className="text-white bg-slate-600">{thead}</tr>
      </thead>
      <tbody>
        {rows.map((row, i) => (
          <tr key={i} className={i % 2 == 1 ? "bg-gray-100" : ""}>
            {row}
          </tr>
        ))}
      </tbody>
    </table>
  );
};
