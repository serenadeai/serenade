import React from "react";
import { Subsubheading } from "../headings";

export const Content = () => (
  <>
    <p>
      Below is a list of all of the commands that the Serenade app can send your plugin. There's no
      need to respond to every message type—if a particular message doesn't make sense for your
      plugin, then you can simply ignore it.
    </p>
    <p>
      The most important command types are <code>COMMAND_TYPE_GET_EDITOR_STATE</code> and{" "}
      <code>COMMAND_TYPE_DIFF</code>, which are used for reading and writing text. At a minimum,
      your plugin should support these two commands.
    </p>
    <Subsubheading title="COMMAND_TYPE_GET_EDITOR_STATE" />
    <p>Get the current contents of the editor.</p>
    <ul>
      <li>
        <code>limited</code> If <code>true</code>, then only return the filename of the editor. This
        is used by the Serenade app for language detection.
      </li>
    </ul>
    <Subsubheading title="COMMAND_TYPE_DIFF" />
    <p>Set the current contents of the editor.</p>
    <ul>
      <li>
        <code>source</code> The new source code the editor should display.
      </li>
      <li>
        <code>cursor</code> The new cursor position, expressed as an index into the source.
      </li>
    </ul>
    <Subsubheading title="COMMAND_TYPE_CREATE_TAB" />
    <p>Create a new tab.</p>
    <Subsubheading title="COMMAND_TYPE_CLOSE_TAB" />
    <p>Close the current tab.</p>
    <Subsubheading title="COMMAND_TYPE_NEXT_TAB" />
    <p>Switch to the next tab.</p>
    <Subsubheading title="COMMAND_TYPE_PREVIOUS_TAB" />
    <p>Switch to the previous tab.</p>
    <Subsubheading title="COMMAND_TYPE_SWITCH_TAB" />
    <p>Switch to the tab at the given index.</p>
    <ul>
      <li>
        <code>index</code> The index of the tab to switch to.
      </li>
    </ul>
    <Subsubheading title="COMMAND_TYPE_OPEN_FILE_LIST" />
    <p>
      Get a list of files matching the given search string. Any filename with the search string as a
      substring should match.
    </p>
    <ul>
      <li>
        <code>path</code> The path to search
      </li>
    </ul>
    <Subsubheading title="COMMAND_TYPE_OPEN_FILE" />
    <p>
      Open the file at the given index in the file list returned by the previous{" "}
      <code>COMMAND_TYPE_OPEN_FILE_LIST</code> command.
    </p>
    <ul>
      <li>
        <code>index</code> The index of the file to open
      </li>
    </ul>
    <Subsubheading title="COMMAND_TYPE_UNDO" />
    <p>Undo the previous editor action.</p>
    <Subsubheading title="COMMAND_TYPE_REDO" />
    <p>Redo the previous editor action.</p>
    <Subsubheading title="COMMAND_TYPE_SAVE" />
    <p>Save the current file.</p>
    <Subsubheading title="COMMAND_TYPE_SCROLL" />
    <p>Scroll the app.</p>
    <ul>
      <li>
        <code>direction</code> Direction to scroll.
      </li>
    </ul>
    <Subsubheading title="COMMAND_TYPE_STYLE" />
    <p>Invoke the editor's built-in styler on the current file.</p>
    <Subsubheading title="COMMAND_TYPE_GO_TO_DEFINITION" />
    <p>Go to the definition of the symbol under the cursor.</p>
    <Subsubheading title="COMMAND_TYPE_DEBUGGER_TOGGLE_BREAKPOINT" />
    <p>Toggle a breakpoint at the current line.</p>
    <Subsubheading title="COMMAND_TYPE_DEBUGGER_START" />
    <p>Start the debugger.</p>
    <Subsubheading title="COMMAND_TYPE_DEBUGGER_PAUSE" />
    <p>Pause the debugger.</p>
    <Subsubheading title="COMMAND_TYPE_DEBUGGER_STOP" />
    <p>Stop the debugger.</p>
    <Subsubheading title="COMMAND_TYPE_DEBUGGER_SHOW_HOVER" />
    <p>While debugging, show debug information for the current symbol.</p>
    <Subsubheading title="COMMAND_TYPE_DEBUGGER_CONTINUE" />
    <p>While debugging, continue to the next breakpoint.</p>
    <Subsubheading title="COMMAND_TYPE_DEBUGGER_STEP_INTO" />
    <p>While debugging, step into the current line.</p>
    <Subsubheading title="COMMAND_TYPE_DEBUGGER_STEP_OUT" />
    <p>While debugging, step out of the current function.</p>
    <Subsubheading title="COMMAND_TYPE_DEBUGGER_STEP_OVER" />
    <p>While debugging, step over the next line.</p>
    <Subsubheading title="COMMAND_TYPE_DEBUGGER_INLINE_BREAKPOINT" />
    <p>Toggle an inline breakpoint.</p>
    <Subsubheading title="COMMAND_TYPE_SELECT" />
    <p>Select a block of text.</p>
    <ul>
      <li>
        <code>cursor</code> Index of the start of the selection block.
      </li>
      <li>
        <code>cursorEnd</code> Index of the stop of the selection block.
      </li>
    </ul>
    <Subsubheading title="COMMAND_TYPE_CLICK" />
    <p>For browsers, click an element on the current page.</p>
    <ul>
      <li>
        <code>text</code> Text to click on the page.
      </li>
    </ul>
    <Subsubheading title="COMMAND_TYPE_BACK" />
    <p>For browsers, go to the previous page.</p>
    <Subsubheading title="COMMAND_TYPE_FORWARD" />
    <p>For browsers, go to the next page.</p>
    <Subsubheading title="COMMAND_TYPE_RELOAD" />
    <p>For browsers, reload the current page.</p>
    <Subsubheading title="COMMAND_TYPE_SHOW" />
    <p>
      For browsers, show links that can be clicked, inputs that can be focused, or code that can be
      copied.
    </p>
    <ul>
      <li>
        <code>text</code> What type of elements to show, either <code>links</code>,{" "}
        <code>inputs</code>, or <code>code</code>.
      </li>
    </ul>
  </>
);
