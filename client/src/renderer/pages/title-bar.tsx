import React, { useState } from "react";
import classNames from "classnames";
import { connect } from "react-redux";
import { ipcRenderer } from "electron";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faTimes, faMinus } from "@fortawesome/free-solid-svg-icons";
import { faSquare } from "@fortawesome/free-regular-svg-icons";

const TitleBarComponent: React.FC<{ miniMode: boolean }> = ({ miniMode }) => {
  const [maximized, setMaximized] = useState(false);

  const minimize = (e: React.MouseEvent) => {
    e.preventDefault();
    ipcRenderer.send("setWindowState", { state: "minimize", url: window.location.href });
  };

  const maximize = (e: React.MouseEvent) => {
    e.preventDefault();
    if (miniMode) {
      return;
    }

    if (maximized) {
      ipcRenderer.send("setWindowState", { state: "unmaximize", url: window.location.href });
    } else {
      ipcRenderer.send("setWindowState", { state: "maximize", url: window.location.href });
    }

    setMaximized((e) => !e);
  };

  const close = (e: React.MouseEvent) => {
    e.preventDefault();
    ipcRenderer.send("setWindowState", { state: "close", url: window.location.href });
  };

  if (window.location.href.endsWith("input") || window.location.href.endsWith("minimode")) {
    return null;
  }

  return process.platform != "darwin" ? (
    <div className="w-full h-[24px] absolute z-10 top-0 left-0">
      <div className="w-full h-full flex">
        <div className="flex-1 draggable" />
        <div>
          <a
            href="#"
            className="h-[24px] w-[32px] inline-block text-center hover:bg-gray-300 dark:hover:bg-gray-700 transition-colors cursor-pointer outline-none"
            onClick={minimize}
          >
            <FontAwesomeIcon icon={faMinus} />
          </a>
          <a
            href="#"
            className="h-[24px] w-[32px] inline-block text-center hover:bg-gray-300 dark:hover:bg-gray-700 transition-colors cursor-pointer outline-none"
            onClick={maximize}
          >
            <FontAwesomeIcon icon={faSquare} className={classNames({ disabled: miniMode })} />
          </a>
          <a
            href="#"
            className="h-[24px] w-[32px] inline-block text-center hover:bg-gray-300 dark:hover:bg-gray-700 transition-colors cursor-pointer outline-none"
            onClick={close}
          >
            <FontAwesomeIcon icon={faTimes} />
          </a>
        </div>
      </div>
    </div>
  ) : (
    <div className="draggable w-full h-[24px] absolute top-0 left-0" />
  );
};

export const TitleBar = connect((state: any) => ({ miniMode: state.miniMode }))(TitleBarComponent);
