import React from "react";
import { Snippet } from "../../snippet";

export const Intro = () => (
  <>
    <p>
      The Serenade Protocol is a powerful way to write your own plugins and custom integrations
      using Serenade. In this guide, you'll see how you can create your own application to handle
      commands that are spoken into the Serenade app.
    </p>
    <p>
      Complete sample code for a few different example applications using the Serenade Protocol can
      be found at{" "}
      <a
        href="https://github.com/serenadeai/protocol"
        target="_blank"
        aria-label="Serenade Protocol GitHub - link opens in a new tab"
      >
        https://github.com/serenadeai/protocol
      </a>
    </p>
  </>
);

export const Concepts: React.FC = () => (
  <>
    <p>
      The Serenade Protocol defines how any application can receive data from the Serenade app (like
      a list of voice commands to execute) and send data to the Serenade app (like the source code
      to modify). All communication happens via JSON over WebSockets, so any language with support
      for those (common!) technologies can be used to create a Serenade plugin. You can think of the
      protocol as a simple message-passing framework, whereby Serenade can pass messages to your
      plugin, and your plugin can pass messages back to Serenade.
    </p>
    <p>
      Serenade works by detecting which application on your desktop has focus, and then sending data
      over a WebSocket to the plugin listening for that application. The Serenade app runs a
      WebSocket server on <code>localhost:17373</code>, and "registering" your plugin with the
      Serenade app is as simple as opening a new WebSocket connection to{" "}
      <code>localhost:17373</code>. Each Serenade plugin also defines a regular expression that
      specifies which proesss it corresponds to. For instance, our Atom plugin tells the Serenade
      app that it should receive messages when an application whose process name matches{" "}
      <code>atom</code> is focused.
    </p>
    <p>
      All messages sent over the protocol will be JSON-encoded data with two top-level fields:
      <ul>
        <li>
          <code>message</code>: The type of message is being sent.
        </li>
        <li>
          <code>data</code>: The payload of the message.
        </li>
      </ul>
    </p>
    <p>
      As you'll see, every WebSockets request sent to your plugin and every response to the Serenade
      app will follow this format.
    </p>
    <p>From a plugin's perspective, the lifecycle of a typical voice command looks like this:</p>
    <ol>
      <li>The user speaks a voice command.</li>
      <li>
        The Serenade app asks the plugin registered for the currently-focused app for information
        about the source code being edited, like its text, cursor position, and filename.
      </li>
      <li>
        The plugin sends back all of the information it has about the text or source code being
        edited.
      </li>
      <li>
        Using the data from the editor, the Serenade app performs some source code manipulation,
        like adding a function or deleting a line.
      </li>
      <li>The Serenade app sends back the result to the plugin for the currently-focused app.</li>
      <li>The plugin displays the resulting source code, cursor position, etc. in the editor.</li>
    </ol>
  </>
);

export const Connecting: React.FC = () => (
  <>
    <p>
      Once your plugin starts up, we need to create a new WebSocket connection to the Serenade app.
      Then, you need to send an <code>active</code> message to the Serenade app, so it knows we
      exist. The <code>active</code> message should specify the following data:
    </p>
    <ul>
      <li>
        <code>id</code>: A unique ID for this instance of the plugin. In some cases, users can open
        multiple windows of the same application and create multiple plugin instances—this all
        depends on each applications plugin API. This ID will be used by Serenade to keep track of
        this instance.
      </li>
      <li>
        <code>app</code>: The name of your plugin. This will be displayed in the bottom-left of the
        Serenade app, so users know they're using your plugin.
      </li>
      <li>
        <code>match</code>: A case-insensitive regular expression that matches your application
        name. For instance, a plugin for Atom would supply a <code>match</code> of <code>atom</code>
        , so when a process containing <code>atom</code> is in the foreground.
      </li>
      <li>
        <code>icon</code>: The application icon to show in the main Serenade window when the
        application is active. Must be encoded as a{" "}
        <a
          href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/Data_URLs"
          className="text-purple-500 hover:text-purple-600 transition-colors cursor-pointer"
          target="_blank"
          aria-label="Data URLs - HTTP | MDN"
          title="Data URLs - HTTP | MDN"
        >data URL</a>
        {" "}string and cannot be more than 20,000 characters long; ideally use a small (less than
        48x48 pixels) version of the application icon. This field is only needed in the first{" "}
        <code>active</code> message or whenever the icon changes (e.g. to show custom status
        icons), and may be left out entirely if an icon isn't requred.
      </li>
    </ul>
    <p>
      Here's a snippet that opens a WebSocket connection and sends the <code>active</code> message.
      Because we'll probably run this application from a terminal, we'll specify a{" "}
      <code>match</code> of <code>term</code>, which will match processes like <code>terminal</code>{" "}
      and <code>iterm2</code>.
    </p>
    <Snippet
      code={`const WebSocket = require("ws");

let id = Math.random().toString();
let websocket = new WebSocket("ws://localhost:17373");
websocket.on("open", () => {
  websocket.send(JSON.stringify({
    message: "active",
    data: {
      id,
      app: "demo",
      match: "term",
    }
  }));
});`}
    />
    <p>
      If the Serenade app isn't running yet, then trying to connect to <code>localhost:17373</code>{" "}
      will naturally fail. So, you might want your plugin to automatically try to reconnect, so it
      connects as soon as the Serenade app is started. Or, you might want to display an error to the
      user, with a button to manually reconnect. Which approach you use is up to you, and may be
      limited by the API of the application you're developing a plugin for.
    </p>
    <p>
      One last note—if the application you're writing a plugin for defines events for windows coming
      into focus, you should also send an <code>active</code> event when the foreground window
      changes. This will tell Serenade which instance of the plugin it should message, so it doesn't
      try to make changes to a window that's in the background.
    </p>
  </>
);

export const Heartbeats: React.FC = () => (
  <>
    <p>
      In order to keep the connection between your plugin and the Serenade app alive, you should
      send regular heartbeat messages. If Serenade hasn't receive a heartbeat from your plugin in
      over 5 minutes, it will consider the plugin idle, and remove it.
    </p>
    <p>
      To send heartbeats on a regular interval, simply send a <code>heartbeat</code> message with
      the <code>id</code> of your plugin instance:
    </p>
    <Snippet
      code={`setInterval(() => {
  websocket.send(JSON.stringify({
    message: "heartbeat",
    data: {
      id,
    }
  }));
});`}
    />
  </>
);
