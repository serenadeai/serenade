import React from "react";
import classNames from "classnames";
import { connect } from "react-redux";
import { ipcRenderer } from "electron";

const ListenStatusComponent: React.FC<{
  listening: boolean;
  localLoading: boolean;
  statusText: string;
}> = ({ listening, localLoading, statusText }) => {
  const toggle = (e: React.MouseEvent) => {
    e.preventDefault();
    if (localLoading) {
      return;
    }

    ipcRenderer.send("toggleChunkManager", !listening);
  };

  const active = listening && statusText == "Listening";
  let text = statusText;
  if (localLoading) {
    text = "Starting Server";
  } else if (!listening) {
    text = "Paused";
  }

  return (
    <a
      href="#"
      onClick={toggle}
      className={classNames("block font-medium drop-shadow-sm text-sm", {
        "text-violet-600 dark:text-violet-400": active,
        "ml-2": localLoading,
      })}
    >
      {text}
    </a>
  );
};

export const ListenStatus = connect((state: any) => ({
  listening: state.listening,
  localLoading: state.localLoading,
  statusText: state.statusText,
}))(ListenStatusComponent);
