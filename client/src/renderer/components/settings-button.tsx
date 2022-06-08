import React from "react";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faCog } from "@fortawesome/free-solid-svg-icons";
import { ipcRenderer } from "electron";

export const SettingsButton = () => (
  <a
    href="#"
    className="block text-slate-600 bg-gray-200 h-[26px] w-[26px] leading-[26px] rounded-md text-sm text-center drop-shadow-sm transition-colors hover:bg-gray-300 dark:bg-gray-600 dark:text-neutral-100 dark:hover:bg-gray-700"
    title="Settings"
    onClick={(e: React.MouseEvent) => {
      e.preventDefault();
      ipcRenderer.send("showSettingsWindow");
    }}
  >
    <FontAwesomeIcon icon={faCog} className="settings-icon" />
  </a>
);
