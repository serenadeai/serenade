import React from "react";
import { Link } from "../../link";
import { Subheading, Subsubheading } from "../headings";
import * as data from "../data";

export const Content: React.FC = () => (
  <>
    <p>Below is a reference for all methods that are available in the Serenade API.</p>
    <Subheading title="class Serenade" />
    <p>
      Methods to create new <code>Builder</code> objects with either a global scope or scoped to a
      single application. You can access an instance of this class via the <code>serenade</code>{" "}
      global in any script.
    </p>
    <Subsubheading title="global()" />
    <p>
      Create a new <code>Builder</code> with a global scope. Any commands registered with the
      builder will be valid regardless of which application is focused or language is used.
    </p>
    <Subsubheading title="app(application)" />
    <p>
      Create a new <code>Builder</code> scoped to the given application. Any commands registered
      with the builder will only be valid when the given application is in the foreground.
    </p>
    <ul>
      <li>
        <code>{`application <string>`}</code> Application to scope commands to.
      </li>
    </ul>
    <Subsubheading title="language(language)" />
    <p>
      Create a new <code>Builder</code> scoped to the given language. Any commands registered with
      the builder will only be valid when editing a file of the given language.
    </p>
    <ul>
      <li>
        <code>{`language <string>`}</code> Language to scope commands to.
      </li>
    </ul>
    <Subsubheading title="extension(extension)" />
    <p>
      Create a new <code>Builder</code> scoped to the given file extension. Any commands registered
      with the builder will only be valid when editing a file with the given extension.
    </p>
    <ul>
      <li>
        <code>{`extension <string>`}</code> File extension to scope commands to.
      </li>
    </ul>
    <Subsubheading title="scope(applications, languages)" />
    <p>
      Create a new <code>Builder</code> scoped to the given applications and languages. Any commands
      registered with the builder will only be valid when one of the given applications is focused
      and one of the given languages is being used. To specify any application or language, pass an
      empty list for that parameter.
    </p>
    <ul>
      <li>
        <code>{`applications <string[]>`}</code> List of applications to scope commands to.
      </li>
      <li>
        <code>{`languages <string[]>`}</code> List of languages to scope commands to.
      </li>
    </ul>
    <Subsubheading title="url(url)" />
    <p>
      Create a new <code>Builder</code> scoped to a specific URL or domain name when using Chrome
      with the Serenade extension. Commands registered with this builder will only be valid when the
      active tab matches one of the given URLs.
    </p>
    <ul>
      <li>
        <code>{`urls <string[]>`}</code> List of URLs to scope commands to.
      </li>
    </ul>
    <Subheading title="class Builder" />
    <p>Methods to register new voice commands.</p>
    <Subsubheading title="command(trigger, callback[, options])" />
    <p>Register a new voice command.</p>
    <ul>
      <li>
        <code>{`trigger <string>`}</code> Voice trigger for this command.
      </li>
      <li>
        <code>{`callback <function>`}</code> Function to be executed when the specified{" "}
        <code>trigger</code> is heard. Arguments to the callback are:
        <ul>
          <li>
            {" "}
            <code>{`api <object>`}</code> An instance of the API class
          </li>
          <li>
            <code>{`matched <object>`}</code> A map from slot names to matched text.
          </li>
          <li>
            Returns <code>{`<string>`}</code> Command ID that can be passed to <code>enable</code>{" "}
            or <code>disable</code>.
          </li>
        </ul>
      </li>
      <li>
        <code>{`options <object>`}</code> Options for how this command is executed. (Available only
        in the latest Serenade beta.)
        <ul>
          <li>
            <code>{`autoExecute <boolean>`}</code> Whether this command executes automatically or
            requires confirmation. For destructive commands (e.g., closing a window), you likely
            want this to be <code>false</code>, and for non-destructive commands (e.g., scrolling
            up), you like want this to be <code>true</code>. Defaults to <code>false</code>.
          </li>
          <li>
            <code>{`chainable <string>`}</code> Whether this command can be chained together with
            other custom commands. Possible values are:
            <ul>
              <li>
                <code>none</code> This command is not chainable with other custom commands.
              </li>
              <li>
                <code>any</code> This command can appear anywhere in a chain.
              </li>
              <li>
                <code>firstOnly</code> This command can only appear as the first element of a chain.
              </li>
              <li>
                <code>lastOnly</code> This command can only appear as the last element of a chain.
              </li>
            </ul>
            Defaults to <code>none</code>.
          </li>
        </ul>
      </li>
    </ul>
    <Subsubheading title="disable(id)" />
    <p>Disable a voice command.</p>
    <ul>
      <li>
        <code>{`id <string[] | string>`}</code> List of command IDs or a single command ID, which is
        the return value when the command was registered.
      </li>
    </ul>
    <Subsubheading title="enable(id)" />
    <p>Enable a voice command.</p>
    <ul>
      <li>
        <code>{`id <string[] | string>`}</code> List of command IDs or a single command ID, which is
        the return value when the command was registered.
      </li>
    </ul>
    <Subsubheading title="hint(word)" />
    <p>
      Give a hint to the speech engine that a word is more likely to be heard than would be assumed
      otherwise.
    </p>
    <ul>
      <li>
        <code>{`word <string>`}</code> Word to hint to the speech engine.
      </li>
      <li>
        Returns <code>{`<string>`}</code> Command ID that can be passed to <code>enable</code> or{" "}
        <code>disable</code>.
      </li>
    </ul>
    <Subsubheading title="key(trigger, key[, modifiers, options])" />
    <p>
      Shortcut for the <code>command</code> method if you just want to map a voice trigger to a
      keypress. This method is equivalent to:
      <code>{`command("trigger", async api => { api.pressKey(key, modifiers); });`}</code>
    </p>
    <ul>
      <li>
        <code>{`trigger <string>`}</code> Voice trigger for this command.
      </li>
      <li>
        <code>{`key <string>`}</code> Key to press. See <Link to="#keys">keys</Link> for a full
        list.
      </li>
      <li>
        <code>{`modifiers <string[]>`}</code> Modifier keys (e.g., "command" or "alt") to hold down
        when pressing <code>key</code>. See <Link to="#keys">keys</Link> for a full list.
      </li>
      <li>
        <code>{`options <object>`}</code> Options for how this command is executed. (Available only
        in the latest Serenade beta.) See <code>command</code> for possible values.
      </li>
      <li>
        Returns <code>{`<string>`}</code> Command ID that can be passed to <code>enable</code> or{" "}
        <code>disable</code>.
      </li>
    </ul>
    <Subsubheading title="pronounce(before, after)" />
    <p>
      Remap the pronounciation of a word from <code>before</code> to <code>after</code>.
    </p>
    <ul>
      <li>
        <code>{`before <string>`}</code> What to remap.
      </li>
      <li>
        <code>{`after <string>`}</code> What to remap to.
      </li>
      <li>
        Returns <code>{`<string>`}</code> Command ID that can be passed to <code>enable</code> or{" "}
        <code>disable</code>.
      </li>
    </ul>
    <Subsubheading title="snippet(templated, generated[, transform])" />
    <p>Register a new snippet.</p>
    <ul>
      <li>
        <code>{`templated <string>`}</code> A string that specifies the trigger for the voice
        command. Surrounding text in <code>{`<% %>`}</code> creates a matching slot that matches any
        text. You can then reference the matched text in the generated snippets, much like regular
        expression capture groups.
      </li>
      <li>
        <code>{`generated <string>`}</code> A snippet to generate. You can use
        <code>{`<% %>`}</code> to reference matching slots. You can also define the default
        formatting for any matching slot by putting a colon after the slot's name; to specify
        multiple styles, separate them with commands. The default text style is{" "}
        <code>lowercase</code>. Possible values for formatting are:
        <ul>
          <li>
            <code>caps</code> All capital letters.{" "}
          </li>
          <li>
            <code>capital</code> The first letter of the first word capitalized.
          </li>
          <li>
            <code>camel</code> Camel case.
          </li>
          <li>
            <code>condition</code> The condition of an if, for, while, etc.—symbols like "equals"
            will automatically become "==". <code>condition</code> implies
            <code>expression</code>.
          </li>
          <li>
            <code>dashes</code> Dashes between words.
          </li>
          <li>
            <code>expression</code> Any expression; symbols will be automatically mapped, so{" "}
            <code>dash</code>
            will become <code>-</code>.
          </li>
          <li>
            <code>identifier</code> The name of a function, class, variable, etc.; symbols will be
            automatically escaped, so <code>dash</code> will become <code>dash</code>.
          </li>
          <li>
            <code>lowercase</code> Spaces between words.
          </li>
          <li>
            <code>pascal</code> Pascal case.
          </li>
          <li>
            <code>underscores</code> Underscores between words.
          </li>
        </ul>
      </li>
      <li>
        <code>{`transform <string>`}</code> How to add the snippet to your code. Defaults to
        <code>statement</code>. Possible values are:
        <ul>
          <li>
            <code>inline</code> (directly at the cursor)
          </li>
          <li>
            <code>argument</code>
          </li>
          <li>
            <code>attribute</code>
          </li>
          <li>
            <code>catch</code>
          </li>
          <li>
            <code>class</code>
          </li>
          <li>
            <code>decorator</code>
          </li>
          <li>
            <code>element</code> (i.e., an element of a list)
          </li>
          <li>
            <code>else</code>
          </li>
          <li>
            <code>else_if</code>
          </li>
          <li>
            <code>entry</code> (i.e., an element of a dictionary)
          </li>
          <li>
            <code>enum</code>
          </li>
          <li>
            <code>extends</code>
          </li>
          <li>
            <code>finally</code>
          </li>
          <li>
            <code>function</code>
          </li>
          <li>
            <code>import</code>
          </li>
          <li>
            <code>method</code>
          </li>
          <li>
            <code>parameter</code>
          </li>
          <li>
            <code>return_value</code>
          </li>
          <li>
            <code>ruleset</code> (i.e., a CSS ruleset)
          </li>
          <li>
            <code>statement</code>
          </li>
          <li>
            <code>tag</code> (i.e., an HTML tag)
          </li>
        </ul>
      </li>
    </ul>
    <Subsubheading title="text(trigger, text[, options])" />
    <p>
      Shortcut for the command method if you just want to map a voice trigger to to typing a string.
      This method is equivalent to:{" "}
      <code>{`command("trigger", async api => { api.typeText(text); });
`}</code>
    </p>
    <ul>
      <li>
        <code>{`trigger <string>`}</code> Voice trigger for this command.
      </li>
      <li>
        <code>{`text <string>`}</code> Text to type.
      </li>
      <li>
        <code>{`options <object>`}</code> Options for how this command is executed. (Available only
        in the latest Serenade beta.) See <code>command</code> for possible values.
      </li>
      <li>
        Returns <code>{`<string>`}</code> Command ID that can be passed to <code>enable</code> or{" "}
        <code>disable</code>.
      </li>
    </ul>
    <Subheading title="class API" />
    <p>
      Methods for workflow automation. An instance of <code>API</code> is passed as the first
      argument to the callback passed to the <code>command</code> method on a <code>Builder</code>.
      All methods on the API are <code>async</code>, so you should <code>await</code> their result,
      or use <code>.then()</code> to attach a callback.
    </p>
    <Subsubheading title="click([button][, count])" />
    <p>Trigger a mouse click.</p>
    <ul>
      <li>
        <code>{`button <string>`}</code> Mouse button to click. Can be <code>left</code>,{" "}
        <code>right</code>, or <code>middle</code>.
      </li>
      <li>
        <code>{`count <number`}</code> How many times to click. For instance, <code>2</code> would
        be a double-click, and <code>3</code> would be a triple-click.
      </li>
      <li>
        Returns <code>{`<Promise>`}</code> Fulfills with undefined upon success.
      </li>
    </ul>
    <Subsubheading title="clickButton(button)" />
    <p>Click a native system button matching the given text. Currently macOS only.</p>
    <ul>
      <li>
        <code>{`button <string>`}</code> Button to click. This value is a substring of the text
        displayed in the button.
      </li>
      <li>
        Returns <code>{`<Promise>`}</code> Fulfills with undefined upon success.
      </li>
    </ul>
    <Subsubheading title="domBlur(selector)" />
    <p>
      Currently available only in Chrome. Remove keyboard focus from the first DOM element matching
      the given{" "}
      <a href="https://developer.mozilla.org/en-US/docs/Web/API/Document_object_model/Locating_DOM_elements_using_selectors">
        CSS selector string.
      </a>
    </p>
    <ul>
      <li>
        <code>{`selector <string>`}</code> CSS selector string corresponding to the element to
        defocus.
      </li>
    </ul>
    <Subsubheading title="domClick(selector)" />
    <p>
      Currently available only in Chrome. Click on the first DOM element matching the given{" "}
      <a href="https://developer.mozilla.org/en-US/docs/Web/API/Document_object_model/Locating_DOM_elements_using_selectors">
        CSS selector string.
      </a>
    </p>
    <ul>
      <li>
        <code>{`selector <string>`}</code> CSS selector string corresponding to the element to
        click.
      </li>
    </ul>
    <Subsubheading title="domCopy(selector)" />
    <p>
      Currently available only in Chrome. Copy all of the text contained within the first DOM
      element matching the given{" "}
      <a href="https://developer.mozilla.org/en-US/docs/Web/API/Document_object_model/Locating_DOM_elements_using_selectors">
        CSS selector string.
      </a>
    </p>
    <ul>
      <li>
        <code>{`selector <string>`}</code> CSS selector string corresponding to the element
        containing the text to be copied.
      </li>
    </ul>
    <Subsubheading title="domFocus(selector)" />
    <p>
      Currently available only in Chrome. Give keyboard focus the first DOM element matching the
      given{" "}
      <a href="https://developer.mozilla.org/en-US/docs/Web/API/Document_object_model/Locating_DOM_elements_using_selectors">
        CSS selector string.
      </a>
    </p>
    <ul>
      <li>
        <code>{`selector <string>`}</code> CSS selector string corresponding to the element to
        focus.
      </li>
    </ul>
    <Subsubheading title="domScroll(selector)" />
    <p>
      Currently available only in Chrome. Scrolls to the first DOM element matching the given{" "}
      <a href="https://developer.mozilla.org/en-US/docs/Web/API/Document_object_model/Locating_DOM_elements_using_selectors">
        CSS selector string.
      </a>
    </p>
    <ul>
      <li>
        <code>{`selector <string>`}</code> CSS selector string corresponding to the element to
        scroll to.
      </li>
    </ul>
    <Subsubheading title="evaluateInPlugin(command)" />
    <p>
      Currently available only on VS Code. Evaluate a command inside of a plugin. On VS Code, the{" "}
      <code>command</code> argument is passed to <code>vscode.commands.executeCommand</code>.
    </p>
    <ul>
      <li>
        <code>{`command <string>`}</code> Command to evaluate within the plugin.
      </li>
    </ul>
    <Subsubheading title="focusApplication(application)" />
    <p>Bring an application to the foreground.</p>
    <ul>
      <li>
        <code>{`application <string>`}</code> Application to focus. This value is a substring of the
        application's path.
      </li>
      <li>
        Returns <code>{`<Promise>`}</code> Fulfills with undefined upon success.
      </li>
    </ul>
    <Subsubheading title="getActiveApplication()" />
    <p>Get the path of the currently-active application.</p>
    <ul>
      <li>
        Returns: <code>{`<Promise<string>>`}</code> Fulfills with the name of the active application
        upon success.
      </li>
    </ul>
    <Subsubheading title="getClickableButtons()" />
    <p>
      Get a list of all of the buttons that can currently be clicked (i.e., are visible in the
      active application). Currently macOS only.
    </p>
    <ul>
      <li>
        Returns: <code>{`<Promise<string[]>>`}</code> Fulfills with a list of button titles upon
        success.
      </li>
    </ul>
    <Subsubheading title="getInstalledApplications()" />
    <p>Get a list of applications installed on the system.</p>
    <ul>
      <li>
        Returns: <code>{`<Promise<string[]>>`}</code> Fulfills with a list of application paths upon
        success.
      </li>
    </ul>
    <Subsubheading title="getMouseLocation()" />
    <p>Get the current mouse coordinates.</p>
    <ul>
      <li>
        Returns: <code>{`<Promise<{ x: number, y: number }>>`}</code> Fulfills with the location of
        the mouse upon success.
      </li>
    </ul>
    <Subsubheading title="getRunningApplications()" />
    <p>Get a list of currently-running applications.</p>
    <ul>
      <li>
        Returns: <code>{`<Promise<string[]>>`}</code> Fulfills with a list of application paths upon
        success.
      </li>
    </ul>
    <Subsubheading title="launchApplication(application)" />
    <p>Launch an application.</p>
    <ul>
      <li>
        <code>{`application <string>`}</code> Substring of the application to launch.
      </li>
      <li>
        Returns <code>{`<Promise>`}</code> Fulfills with undefined upon success.
      </li>
    </ul>
    <Subsubheading title="mouseDown([button])" />
    <p>Press the mouse down.</p>
    <ul>
      <li>
        <code>{`button <string>`}</code> The mouse button to press. Can be <code>left</code>,{" "}
        <code>right</code>, or <code>middle</code>.
      </li>
      <li>
        Returns <code>{`<Promise>`}</code> Fulfills with undefined upon success.
      </li>
    </ul>
    <Subsubheading title="mouseUp([button])" />
    <p>Release a mouse press.</p>
    <ul>
      <li>
        <code>{`button <string>`}</code> The mouse button to release. Can be <code>left</code>,{" "}
        <code>right</code>, or <code>middle</code>.
      </li>
      <li>
        Returns <code>{`<Promise>`}</code> Fulfills with undefined upon success.
      </li>
    </ul>
    <Subsubheading title="pressKey(key[, modifiers][, count])" />
    <p>Press a key on the keyboard, optionally while holding down other keys.</p>
    <ul>
      <li>
        <code>{`key <string>`}</code> Key to press. Can be a letter, number, or the name of the key,
        like <code>enter</code>, <code>backspace</code>, or <code>comma</code>.
      </li>
      <li>
        <code>{`modifiers <string[]>`}</code> List of modifier keys to hold down while pressing the
        key. Can be one or more of <code>control</code>, <code>alt</code>, <code>command</code>,{" "}
        <code>option</code>, <code>shift</code>, or <code>function</code>.
      </li>
      <li>
        <code>{`count <number>`}</code> The number of times to press the key.
      </li>
      <li>
        Returns <code>{`<Promise>`}</code> Fulfills with undefined upon success.
      </li>
    </ul>
    <Subsubheading title="quitApplication(application)" />
    <p>Quit an application.</p>
    <ul>
      <li>
        <code>{`application <string>`}</code> Substring of the application to quit.
      </li>
      <li>
        Returns <code>{`<Promise>`}</code> Fulfills with undefined upon success.
      </li>
    </ul>
    <Subsubheading title="runCommand(command)" />
    <p>Execute a voice command.</p>
    <ul>
      <li>
        <code>{`command <string>`}</code> Transcript of the command to run (e.g., "go to line 1" or
        "next tab").
      </li>
    </ul>
    <Subsubheading title="runShell(command[, args][, options][, callback])" />
    <p>Run a command at the shell.</p>
    <ul>
      <li>
        <code>{`command <string>`}</code> Name of the executable to run.
      </li>
      <li>
        <code>{`args <string[]>`}</code> List of arguments to pass to the executable.
      </li>
      <li>
        <code>{`options <object>`}</code> Object of spawn arguments. Can simply be <code>{}</code>.
        See{" "}
        <a href="https://nodejs.org/api/child_process.html#child_process_child_process_spawn_command_args_options">
          https://nodejs.org/api/child_process.html#child_process_child_process_spawn_command_args_options
        </a>{" "}
        for more.
      </li>
      <li>
        Returns <code>{`<Promise<{ stdout: string, stderr: string }>>`}</code> Fulfills with the
        output of the command upon success.
      </li>
    </ul>
    <Subsubheading title="setMouseLocation(x, y)" />
    <p>Move the mouse to the given coordinates, with the origin at the top-left of the screen.</p>
    <ul>
      <li>
        <code>{`x <number>`}</code> x-coordinate of the mouse.
      </li>
      <li>
        <code>{`y <number>`}</code> y-coordinate of the mouse.
      </li>
      <li>
        Returns <code>{`<Promise>`}</code> Fulfills with undefined upon success.
      </li>
    </ul>
    <Subsubheading title="typeText(text)" />
    <p>Type a string of text.</p>
    <ul>
      <li>
        <code>{`text <string>`}</code> Text to type.
      </li>
      <li>
        Returns <code>{`<Promise>`}</code> Fulfills with undefined upon success.
      </li>
    </ul>
    <Subheading title="Keys" />
    <p>
      You can speak any key name in order to reference it in a Serenade command. In addition to any
      letter or number, you can also say any of the below:
    </p>
    {data.table(data.keys(), 2, "Key")}
  </>
);
