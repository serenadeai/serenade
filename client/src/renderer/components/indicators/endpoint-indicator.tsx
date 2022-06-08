import React from "react";
import { ipcRenderer } from "electron";
import { connect } from "react-redux";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faCloud, faLock } from "@fortawesome/free-solid-svg-icons";
import { Endpoint } from "../../../shared/endpoint";

const EndpointIndicatorComponent: React.FC<{ endpoint: Endpoint }> = ({ endpoint }) => (
  <a
    href="#"
    className="inline-block text-slate-600 bg-gray-200 rounded text-xs px-1.5 py-0.5 mr-0.5 drop-shadow-sm transition-colors hover:bg-gray-300 dark:bg-gray-600 dark:text-neutral-100 dark:hover:bg-gray-700"
    onClick={(e: React.MouseEvent) => {
      e.preventDefault();
      ipcRenderer.send("setSettingsPage", "server");
      ipcRenderer.send("showSettingsWindow");
    }}
  >
    <div className="indicator-inner">
      <FontAwesomeIcon icon={endpoint && endpoint.id == "local" ? faLock : faCloud} />{" "}
      {endpoint && endpoint.id == "local" ? "Local" : "Cloud"}
    </div>
  </a>
);

export const EndpointIndicator = connect((state: any) => ({
  endpoint: state.endpoint,
}))(EndpointIndicatorComponent);
