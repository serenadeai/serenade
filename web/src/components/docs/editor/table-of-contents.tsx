import React from "react";
import * as AddingCode from "./adding-code";
import * as EditingCode from "./editing-code";
import * as NavigatingCode from "./navigating-code";
import * as GettingStarted from "../shared/getting-started";
import * as Reference from "./reference";
import * as SymbolsFormatting from "./symbols-formatting";
import * as System from "./system";
import { TableOfContents } from "../table-of-contents";

export const EditorTableOfContents: TableOfContents = {
  title: "Editor Documentation",
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
      title: "Adding Code",
      content: <AddingCode.Intro />,
      subsections: [
        {
          title: "Inserting Code",
          content: <AddingCode.Insert />,
        },
        {
          title: "Adding Code Blocks",
          content: <AddingCode.Add />,
        },
        {
          title: "Raw Text",
          content: <AddingCode.RawText />,
        },
      ],
    },
    {
      title: "Navigating Code",
      content: <NavigatingCode.Intro />,
      subsections: [],
    },
    {
      title: "Editing Code",
      content: <EditingCode.Intro />,
      subsections: [
        {
          title: "Changing Code",
          content: <EditingCode.Changing />,
        },
        {
          title: "Deleting Code",
          content: <EditingCode.Deleting />,
        },
        {
          title: "Refactoring Code",
          content: <EditingCode.Refactoring />,
        },
      ],
    },
    {
      title: "System Commands",
      content: <System.Intro />,
      subsections: [
        {
          title: "Keyboard & Mouse",
          content: <System.KeyboardMouse />,
        },
        {
          title: "Application Control",
          content: <System.ApplicationControl />,
        },
        {
          title: "Editor Integrations",
          content: <System.EditorIntegrations />,
        },
        {
          title: "Revision Box",
          content: <System.RevisionBox />,
        },
        {
          title: "Command Modes",
          content: <System.Modes />,
        },
      ],
    },
    {
      title: "Symbols & Formatting",
      content: <SymbolsFormatting.Intro />,
      subsections: [
        {
          title: "Symbols List",
          content: <SymbolsFormatting.Symbols />,
        },
        {
          title: "Text Formatting",
          content: <SymbolsFormatting.TextFormatting />,
        },
      ],
    },
    {
      title: "Reference",
      content: <Reference.Intro />,
      subsections: [
        {
          title: "Code Selectors",
          content: <Reference.CodeSelectors />,
        },
        {
          title: "Text Selectors",
          content: <Reference.TextSelectors />,
        },
        {
          title: "Selector Modifiers",
          content: <Reference.SelectorModifiers />,
        },
      ],
    },
  ],
};
