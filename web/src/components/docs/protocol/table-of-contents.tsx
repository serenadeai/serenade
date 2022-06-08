import React from "react";
import * as CommandsReference from "./commands-reference";
import * as GettingStarted from "./getting-started";
import * as HandlingCommands from "./handling-commands";
import * as MessagesReference from "./messages-reference";
import { TableOfContents } from "../table-of-contents";

export const ProtocolTableOfContents: TableOfContents = {
  title: "Custom Plugins Documentation",
  sections: [
    {
      title: "Getting Started",
      content: <GettingStarted.Intro />,
      subsections: [
        {
          title: "Concepts",
          content: <GettingStarted.Concepts />,
        },
        {
          title: "Connecting to Serenade",
          content: <GettingStarted.Connecting />,
        },
        {
          title: "Heartbeats",
          content: <GettingStarted.Heartbeats />,
        },
      ],
    },
    {
      title: "Handling Commands",
      content: <HandlingCommands.Intro />,
      subsections: [
        {
          title: "Receiving Messages",
          content: <HandlingCommands.ReceivingMessages />,
        },
        {
          title: "Editor State",
          content: <HandlingCommands.EditorState />,
        },
        {
          title: "Complete Example",
          content: <HandlingCommands.CompleteExample />,
        },
      ],
    },
    {
      title: "Commands Reference",
      content: <CommandsReference.Content />,
      subsections: [],
    },
    {
      title: "Messages Reference",
      content: <MessagesReference.Content />,
      subsections: [],
    },
  ],
};
