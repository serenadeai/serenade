import React from "react";
import { ipcRenderer } from "electron";
import { faUniversalAccess } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";

export const AccessibilityPermission = () => (
  <div className="frame">
    <div className="main-frame unsupported-page">
      <FontAwesomeIcon icon={faUniversalAccess} className="icon-big" />
      <p>
        Serenade requires the Accessibility permission to automate your workflow and enable you to
        control your system with voice.
      </p>
      <button className="btn" onClick={() => ipcRenderer.send("accessibilityPermission")}>
        Check again
      </button>
    </div>
  </div>
);
