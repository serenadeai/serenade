import React from "react";
import { Link } from "../../link";
import { Snippet } from "../../snippet";

export const Intro = () => (
  <>
    <p>
      Now that your plugin can connect to Serenade, let's implement support for responding to
      messages sent by Serenade. Keep in mind that not every plugin can respond to every possible
      command. For instance, if you're writing a plugin for an app that doesn't support multiple
      tabs, then supporting the <code>NEXT_TAB</code> command doesn't really make sense. If that's
      the case, then your plugin can simply ignore the message.
    </p>
    <p>
      Remember, complete sample code for a few different example applications using the Serenade
      Protocol can be found at{" "}
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

export const ReceivingMessages: React.FC = () => (
  <>
    <p>
      So far, your plugin has only sent messages to Serenade. Now, let's implement receiving
      messages from the Serenade app so your plugin can respond to voice commands. To do so, we'll
      just need to listen for data sent over the WebSocket and then call APIs from the plugin
      accordingly.
    </p>
    <p>
      The below snippet registers an event handler that fires when the plugin receives data from
      Serenade, decodes the JSON message, determines what type of command is being run, and then
      runs some plugin-specific code.
    </p>
    <Snippet
      code={`websocket.on("message", (message) => {
  const data = JSON.parse(message).data;
  let result = {
    message: "completed"
  };

  for (const command of data.response.execute.commandsList) {
    switch (command.type) {
      case "COMMAND_TYPE_SWITCH_TAB":
        // here's where your plugin will actually do the work!
        editor.setActiveTab(command.index);
        break;
    }
  }

  websocket.send(JSON.stringify({
    message: "callback",
    data: {
      callback: data.callback,
      data: result
    }
  }));
});`}
    />
    <p>
      As you can see, <code>data.response.execute.commandsList</code> contains a list of{" "}
      <code>command</code> objects that should be executed by your plugin. Keep in mind that
      Serenade enables you to chain voice commands together (like "next tab line 3"), and{" "}
      <code>commandsList</code> might have more than one <code>command</code> to execute.{" "}
      <code>command.type</code> specifies what type of command should be executed—a full list can be
      found in the
      <a href="#command-reference">Commands Reference</a>. Some commands also pass additional data.
      In this case, <code>command.index</code> specifies which tab index the plugin should bring to
      the foreground.
    </p>
    <p>
      Inside of the case for the <code>SWITCH_TAB</code> message, the code{" "}
      <code>editor.setActiveTab(command.index)</code> is just a placeholder—that's where you'll want
      to make the relevant API call that's specific to your plugin.
    </p>
    <p>
      Serenade expects a reply for each message it sends to your plugin. Each request will contain a{" "}
      <code>callback</code> field that uniquely identifies that request. After you've handled a
      request, your plugin should send a <code>callback</code> message with the value of the
      request's <code>callback</code> field in the <code>data</code> object. Here, we specify{" "}
      <code>"message": "completed"</code> to indicate that the command ran successfully.
    </p>
  </>
);

export const EditorState: React.FC = () => (
  <>
    <p>
      Next, let's implement a more complex message, which also happens to be one of the most
      important messages that the Serenade app will send your plugin: the{" "}
      <code>GET_EDITOR_STATE</code> message. As its name suggests, your plugin will receive this
      message when Serenade needs an updated version of the text being edited. For instance, in a
      code editor, this message is used to read the current source code contents, the name of the
      file being edited, the cursor position, and so on.
    </p>
    <p>Let's update the above snippet to handle this new command type:</p>
    <Snippet
      code={`websocket.on("message", (message) => {
  const data = JSON.parse(message).data;
  let result = {
    message: "completed"
  };

  for (const command of data.response.execute.commandsList) {
    switch (command.type) {
      case "COMMAND_TYPE_SWITCH_TAB":
        // here's where your plugin will actually do the work!
        editor.setActiveTab(command.index);
        break;
      case "COMMAND_TYPE_GET_EDITOR_STATE":
        result = {
          message: "editorState",
          // here's where your plugin will actually do the work!
          data: editor.getEditorState()
        };
        break;
    }
  }

  websocket.send(JSON.stringify({
    message: "callback",
    data: {
      callback: data.callback,
      data: result
    }
  }));
});`}
    />
    <p>
      While Serenade doesn't expect a response for a <code>SWITCH_TAB</code> command, it does expect
      a response for a <code>GET_EDITOR_STATE</code> command, and it will wait for the plugin to
      send one. To respond to Serenade, specify a message type of <code>callback</code> and pass
      back the callback ID that was given in the original message from Serenade (in this example,
      contained in <code>data.callback</code>). Here, the inner <code>data</code> field also
      contains the relevant data from the active text field.
    </p>
    <p>
      Most commands don't expect any response, but all of the expected responses are documented in
      the
      <Link to="/docs/protocol#command-reference">Commands Reference</Link>
    </p>
    <p>
      You'll notice that when the Serenade app is active, it will regularly send{" "}
      <code>GET_EDITOR_STATE</code> messages to make sure that the app has the most up-to-date
      version of the language being used in the editor. As long as your plugin responds to these
      messages, Serenade will automatically use the correct language.
    </p>
  </>
);

export const CompleteExample: React.FC = () => (
  <>
    <p>
      Let's put everything together into a single example. We've also added some extra error
      checking and factored out some common code to make things easier to read. This example can
      also be found at{" "}
      <a
        href="https://github.com/serenadeai/protocol"
        target="_blank"
        aria-label="Serenade Protocol GitHub - link opens in a new tab"
      >
        https://github.com/serenadeai/protocol
      </a>
    </p>
    <Snippet
      code={`const WebSocket = require("ws");

const id = Math.random().toString();
let websocket = null;

const connect = () => {
  if (websocket) {
    return;
  }

  websocket = new WebSocket("ws://localhost:17373");

  websocket.on("open", () => {
    send("active", {
      id,
      app: "demo",
      match: "term",
    });
  });

  websocket.on("close", () => {
    websocket = null;
  });

  websocket.on("message", (message) => {
    handle(message);
  });
};

const handle = (message) => {
  const data = JSON.parse(message).data;
  if (!data.response || !data.response.execute) {
    return;
  }

  let result = {
    message: "completed"
  };

  for (const command of data.response.execute.commandsList) {
    switch (command.type) {
      case "COMMAND_TYPE_GET_EDITOR_STATE":
        result = {
          message: "editorState",
          // here's where your plugin will actually do the work!
          data: editor.getEditorState()
        };
        break;
      case "COMMAND_TYPE_SWITCH_TAB":
        // here's where your plugin will actually do the work!
        break;
    }
  }

  send("callback", {
    callback: data.callback,
    data: result
  });
};

const send = (message, data) => {
  if (!websocket || websocket.readyState != 1) {
    return;
  }

  try {
    websocket.send(JSON.stringify({ message, data }));
  } catch (e) {
    websocket = null;
  }
};

const start = () => {
  connect();
  setInterval(() => {
    connect();
  }, 1000);

  setInterval(() => {
    send("heartbeat", {
      id,
    });
  }, 60 * 1000);
};

start();
`}
    />
  </>
);
