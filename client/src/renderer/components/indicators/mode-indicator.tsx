import React from "react";
import classNames from "classnames";
import { ipcRenderer } from "electron";
import { connect } from "react-redux";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faICursor } from "@fortawesome/free-solid-svg-icons";

const ModeIndicatorComponent: React.FC<{ dictateMode: boolean }> = ({ dictateMode }) => (
  <a
    className={classNames(
      "inline-block text-slate-600 bg-gray-200 rounded text-xs px-1.5 py-0.5 mr-1 drop-shadow-sm transition-colors hover:bg-gray-300 dark:bg-gray-600 dark:text-neutral-100 dark:hover:bg-gray-700",
      { hidden: !dictateMode }
    )}
    href="#"
    onClick={(e: React.MouseEvent) => {
      e.preventDefault();
      ipcRenderer.send("toggleDictateMode");
    }}
  >
    <FontAwesomeIcon icon={faICursor} /> Dictate
  </a>
);

export const ModeIndicator = connect((state: any) => ({
  dictateMode: state.dictateMode,
}))(ModeIndicatorComponent);
