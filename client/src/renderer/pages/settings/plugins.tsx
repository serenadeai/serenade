import React from "react";
import { connect } from "react-redux";
import { ipcRenderer } from "electron";
import { core } from "../../../gen/core";
import { languages } from "../../../shared/languages";
import { plugins } from "../../../shared/plugins";
import { Row } from "../settings";
import { Select } from "../../components/select";

const stylersForLanguage = {
  [core.Language.LANGUAGE_CPLUSPLUS]: [
    core.StylerType.STYLER_TYPE_EDITOR,
    core.StylerType.STYLER_TYPE_CLANG_GOOGLE,
    core.StylerType.STYLER_TYPE_CLANG_LLVM,
    core.StylerType.STYLER_TYPE_CLANG_WEBKIT,
  ],
  [core.Language.LANGUAGE_CSHARP]: [
    core.StylerType.STYLER_TYPE_EDITOR,
    core.StylerType.STYLER_TYPE_CLANG_MICROSOFT,
  ],
  [core.Language.LANGUAGE_DART]: [core.StylerType.STYLER_TYPE_EDITOR],
  [core.Language.LANGUAGE_GO]: [
    core.StylerType.STYLER_TYPE_EDITOR,
    core.StylerType.STYLER_TYPE_GOFMT,
  ],
  [core.Language.LANGUAGE_HTML]: [
    core.StylerType.STYLER_TYPE_EDITOR,
    core.StylerType.STYLER_TYPE_PRETTIER,
  ],
  [core.Language.LANGUAGE_JAVA]: [
    core.StylerType.STYLER_TYPE_EDITOR,
    core.StylerType.STYLER_TYPE_PRETTIER,
    core.StylerType.STYLER_TYPE_GOOGLE_JAVA_FORMAT,
  ],
  [core.Language.LANGUAGE_JAVASCRIPT]: [
    core.StylerType.STYLER_TYPE_EDITOR,
    core.StylerType.STYLER_TYPE_PRETTIER,
    core.StylerType.STYLER_TYPE_STANDARD,
  ],
  [core.Language.LANGUAGE_KOTLIN]: [
    core.StylerType.STYLER_TYPE_EDITOR,
    core.StylerType.STYLER_TYPE_KTLINT,
  ],
  [core.Language.LANGUAGE_PYTHON]: [
    core.StylerType.STYLER_TYPE_EDITOR,
    core.StylerType.STYLER_TYPE_AUTOPEP8,
    core.StylerType.STYLER_TYPE_BLACK,
    core.StylerType.STYLER_TYPE_YAPF,
  ],
  [core.Language.LANGUAGE_RUST]: [
    core.StylerType.STYLER_TYPE_EDITOR,
    core.StylerType.STYLER_TYPE_RUSTFMT,
  ],
  [core.Language.LANGUAGE_RUBY]: [core.StylerType.STYLER_TYPE_EDITOR],
  [core.Language.LANGUAGE_SCSS]: [
    core.StylerType.STYLER_TYPE_EDITOR,
    core.StylerType.STYLER_TYPE_PRETTIER,
  ],
  [core.Language.LANGUAGE_BASH]: [],
  [core.Language.LANGUAGE_DEFAULT]: [],
  [core.Language.LANGUAGE_NONE]: [],
};

const stylerToString = {
  [core.StylerType.STYLER_TYPE_NONE]: "",
  [core.StylerType.STYLER_TYPE_EDITOR]: "Editor built-in",
  [core.StylerType.STYLER_TYPE_PRETTIER]: "Prettier",
  [core.StylerType.STYLER_TYPE_BLACK]: "Black",
  [core.StylerType.STYLER_TYPE_KTLINT]: "ktlint",
  [core.StylerType.STYLER_TYPE_GOOGLE_JAVA_FORMAT]: "google-java-format",
  [core.StylerType.STYLER_TYPE_AUTOPEP8]: "autopep8",
  [core.StylerType.STYLER_TYPE_YAPF]: "yapf",
  [core.StylerType.STYLER_TYPE_STANDARD]: "Standard",
  [core.StylerType.STYLER_TYPE_CLANG_GOOGLE]: "clang-format (Google)",
  [core.StylerType.STYLER_TYPE_CLANG_LLVM]: "clang-format (LLVM)",
  [core.StylerType.STYLER_TYPE_CLANG_WEBKIT]: "clang-format (WebKit)",
  [core.StylerType.STYLER_TYPE_CLANG_MICROSOFT]: "clang-format (Microsoft)",
  [core.StylerType.STYLER_TYPE_GOFMT]: "gofmt",
  [core.StylerType.STYLER_TYPE_RUSTFMT]: "rustfmt",
};

const PluginLink: React.FC<{
  title: string;
  subtitle: string;
  link: string;
  installed: boolean;
}> = ({ title, subtitle, link, installed }) => (
  <Row
    title={title}
    subtitle={subtitle}
    action={
      installed ? (
        <span className="primary-button" style={{ background: "#9ca3af" }}>
          Installed
        </span>
      ) : (
        <a className="primary-button" href={link} target="_blank">
          Install
        </a>
      )
    }
  />
);

const PluginsComponent: React.FC<{ installed: string[]; stylers: any }> = ({
  installed,
  stylers,
}) => (
  <div className="px-4">
    <h2 className="text-lg font-light">Plugins</h2>
    <PluginLink
      title="VS Code"
      subtitle="Install Serenade for the VS Code editor"
      link={plugins.vscode.url}
      installed={installed.some((e: string) => e == "vscode")}
    />
    <PluginLink
      title="JetBrains"
      subtitle="Install Serenade for JetBrains editors"
      link={plugins.jetbrains.url}
      installed={installed.some((e: string) => e == "intellij" || e == "jetbrains")}
    />
    <PluginLink
      title="Atom"
      subtitle="Install Serenade for the Atom editor"
      link={plugins.atom.url}
      installed={installed.some((e: string) => e == "atom")}
    />
    <PluginLink
      title="Chrome"
      subtitle="Install Serenade for the Chrome browser"
      link={plugins.chrome.url}
      installed={installed.some((e: string) => e == "chrome")}
    />
    <PluginLink
      title="Edge"
      subtitle="Install Serenade for the Edge browser"
      link={plugins.edge.url}
      installed={installed.some((e: string) => e == "edge")}
    />
    <PluginLink
      title="Hyper"
      subtitle="Install Serenade for the Hyper terminal"
      link={plugins.hyper.url}
      installed={installed.some((e: string) => e == "hyper")}
    />
    <PluginLink
      title="iTerm2"
      subtitle="Install Serenade for the iTerm2 terminal"
      link={plugins.iterm.url}
      installed={installed.some((e: string) => e == "iterm" || e == "iterm2")}
    />
    <h2 className="text-lg font-light mt-4">Custom Commands</h2>
    <Row
      title="Edit Custom Commands"
      subtitle="Create custom snippets and automations"
      action={
        <button
          className="primary-button"
          onClick={(e: React.MouseEvent) => {
            e.preventDefault();
            ipcRenderer.send("openCustomCommands");
          }}
        >
          Edit
        </button>
      }
    />
    <h2 className="text-lg font-light mt-4">Styler Settings</h2>
    {!stylers
      ? null
      : [
          core.Language.LANGUAGE_JAVASCRIPT,
          core.Language.LANGUAGE_PYTHON,
          core.Language.LANGUAGE_HTML,
          core.Language.LANGUAGE_SCSS,
          core.Language.LANGUAGE_JAVA,
          core.Language.LANGUAGE_CPLUSPLUS,
          core.Language.LANGUAGE_CSHARP,
          core.Language.LANGUAGE_RUST,
          core.Language.LANGUAGE_GO,
          core.Language.LANGUAGE_RUBY,
          core.Language.LANGUAGE_DART,
          core.Language.LANGUAGE_KOTLIN,
        ].map((language: core.Language) => (
          <Row
            key={languages[language]!.name}
            title={languages[language]!.name}
            action={
              <div className="w-40 ml-auto">
                <Select
                  items={stylersForLanguage[language].map(
                    (e: core.StylerType) => stylerToString[e]
                  )}
                  value={
                    stylerToString[
                      (stylers[language] || languages[language]!.styler) as core.StylerType
                    ]
                  }
                  onChange={(value: any) => {
                    let styler: any = stylerToString[core.StylerType.STYLER_TYPE_EDITOR];
                    for (const k of Object.keys(stylerToString)) {
                      if (stylerToString[parseInt(k, 10) as core.StylerType] == value) {
                        styler = k;
                        break;
                      }
                    }

                    stylers[language] = styler;
                    ipcRenderer.send("setSettings", { stylers });
                  }}
                />
              </div>
            }
          />
        ))}
  </div>
);

export const Plugins = connect((state: any) => ({
  installed: state.plugins,
  stylers: state.stylers,
}))(PluginsComponent);
