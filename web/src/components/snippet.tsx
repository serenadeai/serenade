import React from "react";
import { Light as SyntaxHighlighter } from "react-syntax-highlighter";
import java from "react-syntax-highlighter/dist/cjs/languages/hljs/java";
import js from "react-syntax-highlighter/dist/cjs/languages/hljs/javascript";
import python from "react-syntax-highlighter/dist/cjs/languages/hljs/python";
import style from "react-syntax-highlighter/dist/cjs/styles/hljs/atom-one-dark";

interface Props {
  code: string;
  language?: string;
}

SyntaxHighlighter.registerLanguage("java", java);
SyntaxHighlighter.registerLanguage("javascript", js);
SyntaxHighlighter.registerLanguage("python", python);

export const Snippet: React.FC<{ code: string; language?: string }> = ({ code, language }) => {
  language = language || "javascript";
  return (
    <pre className={`code-snippet language-${language} my-3 w-fit max-w-full`}>
      <code>
        <SyntaxHighlighter language={language} style={style}>
          {code}
        </SyntaxHighlighter>
      </code>
    </pre>
  );
};
