import React from "react";
import { Subsubheading } from "../headings";

export const Content = () => (
  <>
    <p>Below is a list of all of the messages that can be sent over the Serenade Protocol.</p>
    <Subsubheading title="active" />
    <p>
      Sent by a plugin when a plugin first starts, a new window is opened, or multiple windows are
      open and the foreground window changes.
    </p>
    <ul>
      <li>
        <code>id</code> A unique identifier for this plugin & window. Often, plugins just generate a
        random string.
      </li>
      <li>
        <code>app</code> The human-readable name of the app this plugin is for. This name will be
        displayed at the bottom-left of the Serenade app.
      </li>
      <li>
        <code>match</code> A regular expression describing what processes this plugin is active for.
        This field is used by the Serenade app to know which plugin it should forward messages to,
        based on the process name of the foreground application. For instance, our Atom plugin uses
        a <code>match</code> of <code>atom</code>, so it will match any process with{" "}
        <code>atom</code> in its name.
      </li>
    </ul>
    <Subsubheading title="heartbeat" />
    <p>Sent by a plugin to keep the connection alive with the Serenade app.</p>
    <ul>
      <li>
        <code>id</code> The ID of the current plugin.
      </li>
    </ul>
    <Subsubheading title="callback" />
    <p>Sent by a plugin to respond to a message from the Serenade app. Possible values are:</p>
    <ul>
      <li>
        <code>{`{ message: "completed" }`}</code>: The default response for messages, used when
        Serenade doesn't expect any additional data from your plugin.
      </li>
      <li>
        <code>{`{ message: "editorState", data: { source, cursor, filename }}`}</code>: In response
        to a <code>COMMAND_TYPE_GET_EDITOR_STATE</code> command, send data about the active editor.
      </li>
    </ul>
    <Subsubheading title="response" />
    <p>Sent by the Serenade app when it needs the plugin to execute a sequence of commands.</p>
    <ul>
      <li>
        <code>response</code> An object containing a list of commands to execute.
        <code>callback</code> A unique ID for the list of commands, which can be sent back to the
        Serenade app via a <code>callback</code> message as a means of responding to the Serenade
        app.
      </li>
    </ul>
  </>
);
