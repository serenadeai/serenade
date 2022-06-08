import React from "react";
import { Link } from "../../link";
import { Snippet } from "../../snippet";

export const Content = () => (
  <>
    <p>
      With custom snippets, you can create shortcuts for code you write regularly. Like custom
      automations, custom snippets are defined via JavaScript files in the{" "}
      <code>~/.serenade/scripts</code> directory. To write custom snippets, create a JavaScript file
      in <code>~/.serenade/scripts</code>, like <code>~/.serenade/scripts/snippets.js</code>, and
      then you can use the Serenade API to register new voice commands.
    </p>
    <p>
      Here's an example of a snippet that creates a new Python method whose name is prefixed with{" "}
      <code>test_</code>.
    </p>
    <Snippet
      code={`serenade.language("python").snippet(
  "test method <%name%>",
  "def test_<%name%>(self):<%newline%><%indent%>pass",
  { "name": ["identifier", "underscores"] },
  "method"
);`}
    />
    <p>
      Now, if you say <code>test method foo</code>, the following code will be generated:
    </p>
    <Snippet
      language="python"
      code={`def test_foo(self):
    pass`}
    />
    <p>
      The <code>snippet</code> method takes four parameters:
    </p>
    <ul>
      <li>
        A string that specifies the trigger for the voice command. Surrounding text in{" "}
        <code>&lt;% %&gt;</code> creates a matching slot that matches any text. You can then
        reference the matched text in the generated snippets, much like regular expression capture
        groups.
      </li>
      <li>
        A snippet to generate. If you defined a matching slot called <code>&lt;%name%&gt;</code> in
        the trigger, then <code>&lt;%name%&gt;</code> in the snippet will be replaced by the words
        that were matched in the transcript.
      </li>
      <li>
        A map of slots to styles. Styles describe how text should be formatted, and a slot can have
        multiple styles. For instance, if a slot represents an identifier (e.g., a class name) where
        symbols aren't allowed, and that identifier should be pascal case, then the values{" "}
        <code>["identifier", "pascal"]</code> could be used. See the{" "}
        <Link to="/docs/api#reference">API Reference</Link> for possible values.
      </li>
      <li>
        How to add the snippet to your code. In the above example, we're specifying that this block
        should be added as a method, so if your cursor is outside of a class, it will move to the
        nearest class before inserting anything, just as it would if you said "add method". The
        default value for this argument is <code>statement</code>. See the{" "}
        <Link to="/docs/api#reference">API Reference</Link> for possible values.
      </li>
    </ul>
    <p>As another example, here's a snippet to add a new React class in a JavaScript file:</p>
    <Snippet
      code={`serenade.language("javascript").snippet(
  "add component <%name%>",
  "const <%name%><%cursor%>: React.FC = () => {};",
  { "identifier": ["identifier", "pascal"] }
);`}
    />
    <p>
      Notice that you can use the special slot <code>&lt;%cursor%&gt;</code> to specify where the
      cursor will be placed after the snippet. The full list of special slots is:
    </p>
    <ul>
      <li>
        <code>&lt;%cursor%&gt;</code>: Where the cursor will be placed after the snippet is added.
      </li>
      <li>
        <code>&lt;%indent%&gt;</code>: One additional level of indentation.
      </li>
      <li>
        <code>&lt;%newline%&gt;</code>: A newline.
      </li>
      <li>
        <code>&lt;%terminator%&gt;</code>: The statement terminator for the current language, often
        a semicolon.
      </li>
    </ul>
    <p>
      As one last example, here's a snippet to create a Java class with an extends and implements in
      one command:
    </p>
    <Snippet
      code={`serenade.language("java").snippet(
  "new class <%name%> extends <%extends%> implements <%implements%>",
  "public class <%name%><%cursor%> extends <%extends%> implements <%implements%> {<%newline%>}",
  {
    "name": ["pascal", "identifier"],
    "extends": ["pascal", "identifier"],
    "implements": ["pascal", "identifier"]
  },
  "class"
);`}
    />
  </>
);
