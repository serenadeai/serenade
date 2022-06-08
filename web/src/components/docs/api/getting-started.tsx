import React from "react";
import { Heading } from "../headings";

export const Content = () => (
  <>
    <p>
      The Serenade API is a powerful way to write your own custom voice commands. With the Serenade
      API, you can create custom automations (like keypresses, clicks, and more), custom
      pronunciations, and custom snippets (which insert customizable code snippets into your
      editor).
    </p>
    <p>
      All of your custom commands will be defined in (node.js) JavaScript files in the{" "}
      <code>~/.serenade/scripts</code> directory. Any JavaScript file in that directory will be
      loaded by Serenade, and you can also <code>require</code> other files and third-party
      libraries. Each script in that directory will have access to a global object called{" "}
      <code>serenade</code> that serves as the entry point for the Serenade API. If you prefer, you
      can also symlink <code>~/.serenade/scripts</code> to another directory on your device.
    </p>
    <p>
      A repository of example custom commands can be found at{" "}
      <a
        href="https://github.com/serenadeai/custom-commands"
        target="_blank"
        aria-label="Serenade Custom Commands GitHub - link opens in a new tab"
      >
        https://github.com/serenadeai/custom-commands
      </a>
      . Feel free to use these commands directly, or use them as a reference for writing your own.
      If you do create your own commands, open up a pull request to that repository to share them
      with other Serenade developers!
    </p>
  </>
);
