import React from "react";
import { Page, PostData, H3, P, OL, UL } from "../../components/blog";

export const Post: PostData = {
  date: "March 22, 2021",
  slug: "the-serenade-protocol",
  title: "Creating Custom Plugins with the Serenade Protocol",
  content: (
    <>
      <P>
        Since launching Serenade, we've seen a lot of excitement from our community around building
        customizations on top of Serenade, like customized voice automations and voice snippets.
        Today, we're excited to release two new features that expand what you're able to build on
        top of Serenade.
      </P>
      <P>
        First, we're open-sourcing the Serenade Protocol, a way to create Serenade plugins for any
        application.
      </P>
      <P>
        Second, we're releasing a new version of the Serenade API that makes it possible to write
        more powerful custom voice commands than ever before.
      </P>
      <H3>Open-Sourcing the Serenade Protocol</H3>
      <P>
        Serenade can type text and send keystrokes to any application, and through plugins for apps
        (like VS Code, JetBrains, and Chrome) it can integrate more deeply with the debugger, file
        manager, and more. To date we've been creating these integrations on our own. However with
        all the amazing developer tools available today, we wanted to empower anyone in the
        community to create plugins for your favorite applications. So, we're standardizing and
        publishing the protocol Serenade uses to communicate with other applications. Now anyone can
        write a plugin that connects to Serenade and responds to voice commands!
      </P>
      <P>
        Communication between Serenade and other apps happens via JSON over WebSockets, so you can
        write a Serenade plugin using any language.
      </P>
      <P>Creating a Serenade plugin is simple:</P>
      <OL>
        <li>
          Open a WebSocket connection to the Serenade app (which runs on a specific localhost port).
        </li>
        <li>
          Send a message over the WebSocket with some information about your plugin, like its name
          and what processes it matches.
        </li>
        <li>
          Handle messages sent over the WebSocket, which represent spoken voice commands, by making
          calls to your application's plugin API.
        </li>
      </OL>
      <P>
        For a full walkthrough of the process of writing a new plugin check out our new{" "}
        <a href="https://serenade.ai/docs/protocol">Protocol Documentation</a>.
      </P>
      <P>
        We've also published examples in both Python and JavaScript on a dedicated{" "}
        <a href="https://github.com/serenadeai/protocol">Github repository</a>. All of our existing
        plugins use the Serenade Protocol, and they're open-sourced on our{" "}
        <a href="https://github.com/serenadeai">Github</a> page. So, if you're writing a plugin,
        feel free to reference our page to see how our existing implementations work.
      </P>
      <H3>Updates to the Serenade API</H3>
      <P>
        In addition to publishing the Serenade Protocol, we're also releasing three substantial
        changes to the Serenade API, which you can use to create your own custom voice commands.
      </P>
      <P>
        First, we've revamped how custom commands are implemented under the hood (including app
        detection, sending keystrokes, and more) and{" "}
        <a href="https://github.com/serenadeai/driver">open-sourced</a> the result as a native, C++
        node.js addon. Now, everyone in the community can contribute to Serenade's system
        integrations and help fix issues that might arise on a specific device.
      </P>
      <P>
        Second, we've added several new API methods, including the ability to trigger mouse events
        like click + drag, evaluate VS Code commands, and get a list of running and installed
        applications. This new functionality should make your custom commands more portable and
        shareable than they were before.
      </P>
      <P>
        Finally, we've changed how custom commands are evaluated by Serenade. Previously, custom
        commands would run in a node.js sandbox within the Serenade app, which meant you couldn't
        easily use third-party libraries from custom commands. Now, custom commands run in a
        full-fledged node.js environment, removing those limitations. Want to test your web app with
        voice? Now you can:
      </P>
      <code>
        <pre>
          {`
const axios = require("axios");

serenade.global().command("login <%username%>", async (api, matches) => {
  axios.post("https://localhost:8080/login", {
    username: matches.username
  })
  .then((response) => {
    console.log(response);
  });
});`}
        </pre>
      </code>
      <P>
        All of these changes are backwards-compatible with the existing API, so you won't need to
        make any changes to your existing custom commands. You can see all of this new functionality
        on our new <a href="https://serenade.ai/docs/api">API Documentation</a>.
      </P>
      <P>
        Even with our protocol and API now open-sourced, we're still committed to developing plugins
        for more developer tools and expanding the automations you can create. We'd love to hear
        your feedback on our <a href="https://serenade.ai/community">community</a> channels, and
        we're excited to see what our community will build with these changes!
      </P>
    </>
  ),
};

const Component = () => <Page post={Post} />;

export default Component;
