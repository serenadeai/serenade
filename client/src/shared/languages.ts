import { core } from "../gen/core";
import bash from "../../static/img/bash.png";
import csharp from "../../static/img/csharp.png";
import c from "../../static/img/c.png";
import dart from "../../static/img/dart.png";
import go from "../../static/img/go.png";
import html from "../../static/img/html.png";
import java from "../../static/img/java.png";
import javascript from "../../static/img/javascript.png";
import kotlin from "../../static/img/kotlin.png";
import python from "../../static/img/python.png";
import ruby from "../../static/img/ruby.png";
import rust from "../../static/img/rust.png";
import css from "../../static/img/css.png";

interface LanguageConfiguration {
  extensions: string[];
  name: string;
  icon: string;
  styler: core.StylerType;
}

export const languages: { [key in core.Language]?: LanguageConfiguration } = {
  [core.Language.LANGUAGE_BASH]: {
    extensions: ["bash", "sh"],
    icon: bash,
    name: "Bash",
    styler: core.StylerType.STYLER_TYPE_EDITOR,
  },
  [core.Language.LANGUAGE_CSHARP]: {
    extensions: ["cs", "csharp"],
    icon: csharp,
    name: "C#",
    styler: core.StylerType.STYLER_TYPE_CLANG_MICROSOFT,
  },
  [core.Language.LANGUAGE_CPLUSPLUS]: {
    extensions: ["cpp", "cc", "cxx", "c++", "hpp", "hh", "hxx", "h++", "c", "h", "cplusplus"],
    icon: c,
    name: "C/C++",
    styler: core.StylerType.STYLER_TYPE_CLANG_GOOGLE,
  },
  [core.Language.LANGUAGE_DART]: {
    extensions: ["dart"],
    icon: dart,
    name: "Dart",
    styler: core.StylerType.STYLER_TYPE_EDITOR,
  },
  [core.Language.LANGUAGE_DEFAULT]: {
    extensions: ["json", "md", "rst", "toml", "txt", "yaml", "yml"],
    icon: "",
    name: "Text",
    styler: core.StylerType.STYLER_TYPE_EDITOR,
  },
  [core.Language.LANGUAGE_GO]: {
    extensions: ["go"],
    icon: go,
    name: "Go",
    styler: core.StylerType.STYLER_TYPE_GOFMT,
  },
  [core.Language.LANGUAGE_HTML]: {
    extensions: ["html", "svelte", "vue", "xml", "xaml"],
    icon: html,
    name: "HTML",
    styler: core.StylerType.STYLER_TYPE_PRETTIER,
  },
  [core.Language.LANGUAGE_JAVA]: {
    extensions: ["java"],
    icon: java,
    name: "Java",
    styler: core.StylerType.STYLER_TYPE_PRETTIER,
  },
  [core.Language.LANGUAGE_JAVASCRIPT]: {
    extensions: ["js", "jsx", "ts", "tsx", "typescript"],
    icon: javascript,
    name: "JavaScript",
    styler: core.StylerType.STYLER_TYPE_PRETTIER,
  },
  [core.Language.LANGUAGE_KOTLIN]: {
    extensions: ["kt"],
    icon: kotlin,
    name: "Kotlin",
    styler: core.StylerType.STYLER_TYPE_KTLINT,
  },
  [core.Language.LANGUAGE_PYTHON]: {
    extensions: ["py"],
    icon: python,
    name: "Python",
    styler: core.StylerType.STYLER_TYPE_BLACK,
  },
  [core.Language.LANGUAGE_RUBY]: {
    extensions: ["rb"],
    icon: ruby,
    name: "Ruby",
    styler: core.StylerType.STYLER_TYPE_EDITOR,
  },
  [core.Language.LANGUAGE_RUST]: {
    extensions: ["rs"],
    icon: rust,
    name: "Rust",
    styler: core.StylerType.STYLER_TYPE_RUSTFMT,
  },
  [core.Language.LANGUAGE_SCSS]: {
    extensions: ["css", "scss", "less"],
    icon: css,
    name: "CSS/SCSS",
    styler: core.StylerType.STYLER_TYPE_PRETTIER,
  },
};

export const filenameToLanguage = (filename: string): core.Language => {
  for (const language of Object.keys(languages)) {
    const k: core.Language = (language as unknown) as core.Language;
    if (
      languages[k] &&
      languages[k]!.extensions.some((e: string) => filename.toLowerCase().endsWith("." + e))
    ) {
      return k;
    }
  }

  return core.Language.LANGUAGE_DEFAULT;
};
