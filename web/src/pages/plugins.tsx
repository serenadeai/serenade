import React from "react";
import classNames from "classnames";
import { Link } from "gatsby";
import { Group } from "../components/block";
import { Gray } from "../components/pages";

const PluginGroup: React.FC<{ columns: number; description: string; title: string }> = ({
  children,
  columns,
  description,
  title,
}) => (
  <Group title={title} description={description}>
    <div
      className={classNames("grid gap-8", {
        "grid-cols-3": columns == 3,
        "grid-cols-5": columns == 5,
      })}
    >
      {children}
    </div>
  </Group>
);

const Plugin: React.FC<{ image: string; link?: string; name: string }> = ({
  image,
  link,
  name,
}) => (
  <div className="mx-auto text-center pb-4">
    <img className="block mx-auto text-center pb-2" src={image} alt={name} width={60} />
    <h5 className="text-xl font-medium pb-4">{name}</h5>
    {link ? (
      <Link to={link} className="primary-button">
        Download
      </Link>
    ) : null}
  </div>
);

const Plugins = () => (
  <Gray>
    <div className="max-w-screen-md mx-auto pt-6">
      <h1 className="font-bold text-4xl pb-2">Supported Applications</h1>
      <h3 className="font-light text-xl">
        Serenade integrates with editors, terminals, web browsers, and more in order to enable voice
        control across your entire workflow.
      </h3>
    </div>
    <div className="max-w-screen-md mx-auto">
      <PluginGroup
        title="Plugins"
        description="Serenade plugins enable deeper integrations with your editor, bringing all of Serenade's functionality to supported tools."
        columns={3}
      >
        <Plugin
          image="https://cdn.serenade.ai/web/img/icons/atom.svg"
          link="https://atom.io/packages/serenade"
          name="Atom"
        />
        <Plugin
          image="https://cdn.serenade.ai/web/img/icons/vscode.svg"
          link="https://marketplace.visualstudio.com/items?itemName=serenade.serenade"
          name="VS Code"
        />
        <Plugin
          image="https://cdn.serenade.ai/web/img/icons/intellij.svg"
          link="/install#jetbrains"
          name="JetBrains"
        />
        <Plugin
          image="https://cdn.serenade.ai/web/img/icons/chrome.svg"
          link="https://chrome.google.com/webstore/detail/bgfbijeikimjmdjldemlegooghdjinmj"
          name="Chrome"
        />
        <Plugin
          image="https://cdn.serenade.ai/web/img/icons/edge.svg"
          link="https://chrome.google.com/webstore/detail/bgfbijeikimjmdjldemlegooghdjinmj"
          name="Edge"
        />
        <Plugin
          image="https://cdn.serenade.ai/web/img/icons/hyper.svg"
          link="https://github.com/serenadeai/hyper"
          name="Hyper"
        />
      </PluginGroup>
      <PluginGroup
        title="System Integrations"
        description="Using an application without a dedicated plugin? No problem—Serenade integrates with system APIs and has its own basic editor in order to support text entry into any application."
        columns={3}
      >
        <Plugin image="https://cdn.serenade.ai/web/img/icons/macos.svg" name="macOS" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/windows.svg" name="Windows" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/ubuntu.svg" name="Linux" />
      </PluginGroup>
      <PluginGroup
        title="Supported Apps & Websites"
        description="Through system accessibility APIs and browser extensions, Serenade can integrate with a variety of applications and websites even without a dedicated plugin. Below is a sample of applications known to be supported. Using Serenade with an application not on this list? Let us know!"
        columns={5}
      >
        <Plugin image="https://cdn.serenade.ai/web/img/icons/jupyter.svg" name="Jupyter" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/slack.svg" name="Slack" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/discord.svg" name="Discord" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/github.svg" name="GitHub" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/jira.svg" name="JIRA" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/gitlab.svg" name="GitLab" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/colab.png" name="Colab" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/leetcode.png" name="LeetCode" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/replit.svg" name="Repl.it" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/gmail.svg" name="Gmail" />
      </PluginGroup>
      <PluginGroup
        title="Fully-supported Languages"
        description="In fully-supported languages, you can use all of Serenade's commands, including commands that reference syntax constructs, like functions and classes."
        columns={5}
      >
        <Plugin image="https://cdn.serenade.ai/web/img/icons/python.svg" name="Python" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/javascript.svg" name="JavaScript" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/html.svg" name="HTML" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/java.svg" name="Java" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/cpp.svg" name="C / C++" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/typescript.svg" name="TypeScript" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/css.svg" name="CSS" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/markdown.svg" name="Markdown" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/dart.svg" name="Dart" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/bash.svg" name="Bash" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/sass.svg" name="Sass" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/csharp.svg" name="C#" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/go.svg" name="Go" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/ruby.svg" name="Ruby" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/rust.svg" name="Rust" />
      </PluginGroup>
      <PluginGroup
        title="Limited-support Languages"
        description="Even when a programming language is not fully supported, you can still use all of Serenade's text-based commands for writing code, navigating files, and editing text. Here are just a few languages used in our community."
        columns={5}
      >
        <Plugin image="https://cdn.serenade.ai/web/img/icons/php.svg" name="PHP" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/haskell.svg" name="Haskell" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/scala.svg" name="Scala" />
        <Plugin image="https://cdn.serenade.ai/web/img/icons/swift.svg" name="Swift" />
      </PluginGroup>
    </div>
  </Gray>
);

export default Plugins;
