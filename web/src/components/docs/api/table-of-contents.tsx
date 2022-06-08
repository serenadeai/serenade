import React from "react";
import * as ApiReference from "./api-reference";
import * as Automations from "./automations";
import * as GettingStarted from "./getting-started";
import * as Pronunciations from "./pronunciations";
import * as System from "./system";
import * as Snippets from "./snippets";
import { TableOfContents } from "../table-of-contents";

export const ApiTableOfContents: TableOfContents = {
  title: "Custom Commands Documentation",
  sections: [
    {
      title: "Getting Started",
      content: <GettingStarted.Content />,
      subsections: [],
    },
    {
      title: "Automations",
      content: <Automations.Intro />,
      subsections: [
        {
          title: "Defining Automations",
          content: <Automations.Defining />,
        },
        {
          title: "Dynamic Automations",
          content: <Automations.Dynamic />,
        },
      ],
    },
    {
      title: "Snippets",
      content: <Snippets.Content />,
      subsections: [],
    },
    {
      title: "Pronunciations",
      content: <Pronunciations.Content />,
      subsections: [],
    },
    {
      title: "System",
      content: <System.Intro />,
      subsections: [
        { title: "Accessibility API", content: <System.AccessibilityApi /> },
        { title: "Revision Box", content: <System.RevisionBox /> },
      ],
    },
    {
      title: "API Reference",
      content: <ApiReference.Content />,
      subsections: [],
    },
  ],
};
