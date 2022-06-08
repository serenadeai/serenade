import React from "react";

const PluginButton: React.FC<{ image: string; name: string }> = ({ image, name }) => (
  <span
    className="inline-block rounded-full mr-3"
    style={{
      background: "linear-gradient(#a46dff 3.88%, #ff8388 40.34%, #eec14d 70.02%, #82c0ff 100%)",
      padding: "3px",
    }}
  >
    <span className="bg-white text-slate-600 text-base font-medium px-4 py-2 inline-block rounded-full">
      <img className="inline-block mr-3" src={image} alt={name} style={{ width: "28px" }} />
      <span style={{ verticalAlign: "-1px" }}>{name}</span>
    </span>
  </span>
);

const Row: React.FC<{ index: number }> = ({ children, index }) => {
  const durations = ["60s", "58s", "65s", "62s", "53s"];
  return (
    <div
      className="overflow-x-hidden pb-3"
      style={{
        whiteSpace: "nowrap",
        width: "fit-content",
      }}
    >
      {[1, 2, 3].map((e: number) => (
        <div
          key={e}
          className="inline-block"
          style={{
            animationDuration: durations[index],
            animationName: "marquee",
            animationTimingFunction: "linear",
            animationIterationCount: "infinite",
          }}
        >
          {children}
        </div>
      ))}
    </div>
  );
};

export const PluginGrid: React.FC = () => {
  return (
    <div className="overflow-x-hidden">
      <Row index={0}>
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/python.svg" name="Python" />
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/vscode.svg" name="VS Code" />
        <PluginButton
          image="https://cdn.serenade.ai/web/img/icons/javascript.svg"
          name="JavaScript"
        />
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/chrome.svg" name="Chrome" />
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/markdown.svg" name="Markdown" />
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/intellij.svg" name="IntelliJ" />
      </Row>
      <Row index={1}>
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/jupyter.svg" name="Jupyter" />
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/html.svg" name="HTML" />
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/slack.svg" name="Slack" />
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/hyper.svg" name="Hyper" />
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/java.svg" name="Java" />
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/discord.svg" name="Discord" />
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/atom.svg" name="Atom" />
      </Row>
      <Row index={2}>
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/cpp.svg" name="C / C++" />
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/github.svg" name="GitHub" />
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/jira.svg" name="JIRA" />
        <PluginButton
          image="https://cdn.serenade.ai/web/img/icons/typescript.svg"
          name="TypeScript"
        />
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/gitlab.svg" name="GitLab" />
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/pycharm.svg" name="PyCharm" />
      </Row>
      <Row index={3}>
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/iterm2.png" name="iTerm2" />
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/colab.png" name="Colab" />
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/css.svg" name="CSS" />
        <PluginButton
          image="https://cdn.serenade.ai/web/img/icons/android-studio.svg"
          name="Android Studio"
        />
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/leetcode.png" name="LeetCode" />
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/replit.svg" name="Repl.it" />
      </Row>
      <Row index={4}>
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/dart.svg" name="Dart" />
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/bash.svg" name="Bash" />
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/gmail.svg" name="Gmail" />
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/linear.svg" name="Linear" />
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/sass.svg" name="Sass" />
        <PluginButton image="https://cdn.serenade.ai/web/img/icons/webstorm.svg" name="WebStorm" />
      </Row>
    </div>
  );
};
