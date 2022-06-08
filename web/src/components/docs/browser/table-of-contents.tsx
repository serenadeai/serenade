import React from "react";
import * as Editing from "./editing";
import * as LinksInputs from "./links-inputs";
import * as GettingStarted from "../shared/getting-started";
import * as Navigation from "./navigation";
import { TableOfContents } from "../table-of-contents";

export const BrowserTableOfContents: TableOfContents = {
  title: "Browser Documentation",
  sections: [
    {
      title: "Getting Started",
      content: <GettingStarted.Intro />,
      subsections: [
        {
          title: "Installation",
          content: <GettingStarted.Installation />,
        },
        {
          title: "Setup",
          content: <GettingStarted.Setup />,
        },
        {
          title: "Basics",
          content: <GettingStarted.Basics />,
        },
        {
          title: "Common Commands",
          content: <GettingStarted.CommonCommands />,
        },
      ],
    },
    {
      title: "Navigation",
      content: <Navigation.Content />,
      subsections: [],
    },
    {
      title: "Links & Inputs",
      content: <LinksInputs.Content />,
      subsections: [],
    },
    {
      title: "Editing Text & Code",
      content: <Editing.Content />,
      subsections: [],
    },
  ],
};
