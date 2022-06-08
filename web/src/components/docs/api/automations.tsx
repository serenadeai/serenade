import React from "react";
import { Link } from "../../link";
import { Snippet } from "../../snippet";

export const Intro = () => (
  <>
    <p>
      With custom automations, you can create your own voice commands to automate keypresses,
      clicks, and more. For instance, you could write a voice command to search Stack Overflow or
      switch to a terminal and build your project.
    </p>
  </>
);

export const Defining: React.FC = () => (
  <>
    <p>
      Custom automations are defined in <code>~/.serenade/scripts/</code>. Serenade has already
      created a file called <code>~/.serenade/scripts/custom.js</code> for you, which you can use as
      a starting point. You can create other files and install npm packages in that directory as
      well.
    </p>
    <p>
      Every script in <code>~/.serenade/scripts</code> has a global variable called{" "}
      <code>serenade</code> in scope, which can be used to create voice commands. A custom
      automation can either be <code>global</code>, meaning it can be activated from any
      application, or it can be scoped to one or more <code>apps</code>. For instance, if you only
      wanted to be able to trigger a certain automation from Chrome, you could scope the command to{" "}
      <code>chrome</code>, a case-insensitive substring of the process name.
    </p>
    <p>
      All output from scripts in <code>~/.serenade/scripts</code> is piped to{" "}
      <code>~/.serenade/serenade.log</code>, so if you use functions like <code>console.log</code>{" "}
      from your custom automations, output will appear in <code>~/.serenade/serenade.log</code>.
    </p>
    <p>
      Let's look at an example. The below custom automation will bring your terminal to the
      foreground (launching it if it's not already running), type in a bash command to make a
      project, and execute it.
    </p>
    <Snippet
      code={`serenade.global().command("make", async (api) => {
  await api.focusOrLaunchApplication("terminal");
  await api.typeText("make clean && make");
  await api.pressKey("return");
});`}
    />
    <p>
      The <code>global</code> method on the <code>serenade</code> object specifies that we'd like
      this command to be triggerable from any application. The <code>command</code> method takes two
      arguments:
    </p>
    <ul>
      <li>The voice command you want to create, specified as a string</li>
      <li>
        The automation that will be executed when you speak that command, specified as a callback.
        The <code>api</code> object that's passed to the callback as a the first argument has a
        variety of automation methods, all of which are outlined in the{" "}
        <Link to="/docs/api#reference">API Reference</Link>.
      </li>
    </ul>
    <p>
      In this example, we used <code>focusOrLaunchApplication</code> to bring the{" "}
      <code>terminal</code> app to the foreground if it's running and to launch it if not, then{" "}
      <code>typeText</code> to type a string of text, and finally, <code>pressKey</code> to press a
      key on the keyboard.
    </p>
    <p>
      Let's look at another example. This custom automation will search the current web page in
      Chrome for a string you specify with voice. For instance, you could trigger this automation by
      saying <code>find hello world</code>
    </p>
    <Snippet
      code={`serenade.app("chrome").command("find <%text%>", async (api, matches) => {
  await api.pressKey("f", ["command"]);
  await api.typeText(matches.text);
});`}
    />
    <p>
      This time, rather than specifying <code>global()</code>, we used <code>app("chrome")</code> to
      make this command valid only when Google Chrome is in the foreground. In the first argument,
      surrounding text in <code>&lt;% %&gt;</code> creates a matching slot that will match anything.
      The words matched by a slot are passed to the callback via the <code>matches</code> parameter.
      So, for example, if you said <code>find hello world</code>, this command would be triggered,
      and <code>matches.text</code> would have a value of <code>hello world</code>. This automation
      will press the <code>f</code> key while holding down the <code>command</code> key, which will
      open Chrome's search box, then will type in whatever you said into the box.
    </p>
    <p>
      You can specify multiple slots in a voice command, and <code>matches</code> will be populated
      with all of them.
    </p>
  </>
);

export const Dynamic: React.FC = () => (
  <>
    <p>
      After defining an automation, you can dynamically <code>enable</code> or <code>disable</code>{" "}
      it using the Serenade API. For instance, suppose you wanted to create voice commands to enter
      and exit a "mode" where only some commands are valid. You could implement something like this:
    </p>
    <Snippet
      code={`const spellingModeCommands = [
  serenade.global().key("alpha", "a"),
  serenade.global().key("bravo", "b")
  // and more!
];

// disabled by default, until you say "start spelling"
serenade.global().disable(spellingModeCommands);

serenade.global().command("start spelling", async (api) => {
  serenade.global().enable(spellingModeCommands);
});

serenade.global().command("stop spelling", async (api) => {
  serenade.global().disable(spellingModeCommands);
});`}
    />
  </>
);
