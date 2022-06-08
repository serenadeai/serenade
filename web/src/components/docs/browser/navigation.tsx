import React from "react";
import { ExampleTable } from "../example-table";
import { Transcript } from "../../transcript";

export const Content: React.FC = () => (
  <>
    <p>With Serenade, you can navigate pages and manage tabs with voice commands.</p>
    <ExampleTable
      rows={[
        [["open google dot com", "go to stack overflow"], "Navigate to a web page"],
        [["back", "forward"], "Navigate back or forward in history"],
        [["reload"], "Reload the web page"],
        [["scroll up", "scroll down"], "Scroll the browser"],
        [["scroll to top", "scroll to bottom"], "Scroll to the top or bottom of the page"],
        [["scroll to search", "scroll to login"], "Scroll to given text on the page"],
        [["new tab", "create tab"], "Create a new tab"],
        [["close tab"], "Close the active tab"],
        [["first tab", "second tab", "tab one", "tab two"], "Switch tabs"],
        [["undo"], "Trigger an undo"],
        [["redo"], "Trigger an redo"],
      ]}
    />
  </>
);
