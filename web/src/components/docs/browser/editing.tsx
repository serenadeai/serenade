import React from "react";
import { Link } from "../../link";
import ExampleTable from "../example-table";
import { Transcript } from "../../transcript";

export const Content: React.FC = () => (
  <>
    <p>
      When focused on an input, all of Serenade's functionality for writing code and editing text
      will be enabled. For instance, you can say <Transcript text="insert hello world" /> to add
      text, <Transcript text="change login to logout" /> to edit text, or "
      <Transcript text="delete next two words" /> to remove text.
    </p>
    <p>
      Serenade is designed to work with with a variety of inputs. So, whether you're training on
      model on Jupyter, analyzing data on Google Colab, building a project on repl.it, or practicing
      algorithms on LeetCode, Serenade can help you write code in a web browser.
    </p>
    <p>
      To learn more about using Serenade to write code, check out the{" "}
      <Link to="/docs">Editors & IDEs</Link> documentation.
    </p>
  </>
);
