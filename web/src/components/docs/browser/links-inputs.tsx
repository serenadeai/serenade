import React from "react";
import { ExampleTable } from "../example-table";
import { Transcript } from "../../transcript";

export const Content: React.FC = () => (
  <>
    <p>Using Serenade, you can also interact with links and inputs on a web page.</p>
    <ExampleTable
      rows={[
        [["show links"], "Show numbered overlays for each link on the page"],
        [["show inputs"], "Show numbered overlays for each input field on the page"],
        [["click one", "click two"], "Click a numbered overlay"],
        [["clear"], "Hide overlays"],
        [["click login", "click search"], "Click the link matching the given text"],
      ]}
    />
  </>
);
