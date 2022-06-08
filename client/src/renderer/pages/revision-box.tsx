import React, { useEffect, useRef, useState } from "react";
import classNames from "classnames";
import store from "../state/store";
import { SupportIcon } from "@heroicons/react/outline";
import { connect } from "react-redux";
import * as actions from "../state/actions";
import icon from "../../../static/img/icon.png";

const CodeMirror = require("codemirror-minified");

export let codeMirror: any = null;
export let textEditor: any = null;

const cursorFromRowAndColumn = (source: string, row: number, column: number): number => {
  let result = 0;
  const lines = source.split("\n");
  for (let i = 0; i < row; i++) {
    result += lines[i].length;
  }

  return result + column;
};

const getCodeEditorState = (): { source: string; cursor: number; cursorEnd: number } => {
  const editor = codeMirror!;
  const source = editor.getValue();
  const { line, ch } = editor.getCursor();
  return {
    source,
    cursor: cursorFromRowAndColumn(source, line, ch),
    cursorEnd: 0,
  };
};

const getTextEditorState = (): { source: string; cursor: number; cursorEnd: number } => {
  return {
    source: textEditor.value,
    cursor: textEditor.selectionStart,
    cursorEnd: textEditor.selectionEnd,
  };
};

const rowAndColumnFromCursor = (
  source: string,
  cursor: number
): { row: number; column: number } => {
  let row = 0;
  let column = 0;
  for (let i = 0; i < source.length; i++) {
    if (i == cursor) {
      break;
    }

    column++;
    if (source[i] == "\n") {
      row++;
      column = 0;
    }
  }

  return { row, column };
};

const setCodeEditorState = (state: { source: string; cursor: number; cursorEnd: number }) => {
  const editor = codeMirror!;
  const { row, column } = rowAndColumnFromCursor(state.source, state.cursor);
  editor.setValue(state.source);
  editor.setCursor(row, column);
};

const setTextEditorState = (state: { source: string; cursor: number; cursorEnd: number }) => {
  textEditor.value = state.source || "";
  textEditor.setSelectionRange(state.cursor || 0, state.cursorEnd ? state.cursorEnd : state.cursor);
};

export const focus = () => {
  if (store.getState().revisionBoxMode == "code") {
    codeMirror!.focus();
  } else if (store.getState().revisionBoxMode == "text") {
    textEditor!.focus();
  }
};

export const getEditorState = (): { source: string; cursor: number; cursorEnd: number } => {
  if (store.getState().revisionBoxMode == "code") {
    return getCodeEditorState();
  } else if (store.getState().revisionBoxMode == "text") {
    return getTextEditorState();
  }

  return {
    source: "",
    cursor: 0,
    cursorEnd: 0,
  };
};

export const setEditorState = (
  state: { source: string; cursor: number; cursorEnd: number },
  allEditors: boolean = true
) => {
  if (store.getState().revisionBoxMode == "code" || allEditors) {
    setCodeEditorState(state);
  }
  if (store.getState().revisionBoxMode == "text" || allEditors) {
    setTextEditorState(state);
  }
};

export const RevisionBoxPageComponent: React.FC<{
  revisionBoxMode: string;
  setRevisionBoxMode: (mode: string) => void;
}> = ({ revisionBoxMode, setRevisionBoxMode }) => {
  const codeInput = useRef<HTMLTextAreaElement>(null);
  const textInput = useRef<HTMLTextAreaElement>(null);
  const [helpShown, setHelpShown] = useState(false);

  useEffect(() => {
    codeMirror = CodeMirror.fromTextArea(codeInput.current!, {
      lineNumbers: true,
      tabSize: 2,
    });

    textEditor = textInput.current;
  }, []);

  const toggleMode = (e: React.MouseEvent, mode: string) => {
    e.preventDefault();
    if (mode == "code") {
      const state = getCodeEditorState();
      setTextEditorState(state);
    } else if (mode == "text") {
      const state = getTextEditorState();
      setCodeEditorState(state);
    }

    setRevisionBoxMode(mode);
    setTimeout(() => {
      if (mode == "code") {
        codeMirror!.refresh();
        codeMirror!.focus();
      } else if (mode == "text") {
        textInput.current!.focus();
      }
    }, 100);
  };

  return (
    <div>
      <div className="absolute top-0 left-0 right-0 h-[32px] border-b">
        <div className="flex items-center justify-center h-[32px]">
          <img className="w-4 h-4 inline-block mr-1" src={icon} />
          <h3 className="inline-block font-medium">Revision Box</h3>
        </div>
        <div
          className={classNames("absolute top-[2px]", {
            "right-2": process.platform == "darwin",
            "left-2": process.platform != "darwin",
          })}
        >
          <div className="flex items-center">
            <a
              href="#"
              className="inline-block mr-1"
              onClick={(e) => {
                e.preventDefault();
              }}
              onMouseOver={(e) => {
                setHelpShown(true);
              }}
              onMouseOut={(e) => {
                setHelpShown(false);
              }}
            >
              <SupportIcon className="w-[20px]" />
            </a>
            <div
              className={classNames(
                "absolute top-[32px] right-1 z-20 w-96 px-2 py-1.5 bg-white text-sm border rounded shadow dark:bg-neutral-800 dark:border-neutral-500",
                {
                  hidden: !helpShown,
                  "right-1": process.platform == "darwin",
                  "left-1": process.platform != "darwin",
                }
              )}
            >
              <p>
                You can use the revision box to edit text without a Serenade plugin. All Serenade
                editing commands work here. When you're done, you can say:
              </p>
              <ul className="list-disc ml-6 mt-1">
                <li>"close" to copy the contents of the revision box to the active application</li>
                <li>
                  "send" to copy the contents of the revision box to the active application, then
                  press enter
                </li>
                <li>"copy" to copy the contents of the revision box to the clipboard</li>
                <li>"cancel" to close this window and discard the text</li>
              </ul>
            </div>
            <div className="border rounded dark:bg-gray-600 dark:text-neutral-100 dark:border-neutral-500">
              <a
                href="#"
                onClick={(e) => toggleMode(e, "text")}
                className={classNames(
                  "px-2 py-[2px] rounded-[0.15rem] cursor-pointer transition-colors border-r rounded-r-none dark:border-neutral-500",
                  {
                    "bg-violet-600 text-white": revisionBoxMode == "text",
                  }
                )}
              >
                Text
              </a>
              <a
                href="#"
                onClick={(e) => toggleMode(e, "code")}
                className={classNames(
                  "px-2 py-[2px] rounded-[0.15rem] cursor-pointer transition-colors rounded-l-none",
                  {
                    "bg-violet-600 text-white": revisionBoxMode == "code",
                  }
                )}
              >
                Code
              </a>
            </div>
          </div>
        </div>
      </div>
      <div className="pt-[32px] revision-box-editors h-screen">
        <div
          className={classNames("w-full h-full", {
            hidden: revisionBoxMode != "text",
          })}
        >
          <textarea
            className="w-full h-full outline-none p-2 dark:bg-gray-700 dark:text-gray-200"
            ref={textInput}
            autoFocus
          ></textarea>
        </div>
        <div
          className={classNames("w-full h-full", {
            hidden: revisionBoxMode != "code",
          })}
        >
          <textarea className="w-full h-full" ref={codeInput}></textarea>
        </div>
      </div>
    </div>
  );
};

export const RevisionBoxPage = connect(
  (state: any) => ({
    revisionBoxMode: state.revisionBoxMode,
  }),
  (dispatch) => ({
    setRevisionBoxMode: (mode: string) => {
      dispatch(actions.setRevisionBoxMode(mode));
    },
  })
)(RevisionBoxPageComponent);
