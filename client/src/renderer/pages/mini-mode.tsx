import React from "react";
import { connect } from "react-redux";
import { ipcRenderer } from "electron";
import { AlternativesList } from "../components/alternatives-list";

let miniModeWindowHeight = 0;
export const updateMiniModeWindowHeight = () => {
  const calculateHeight = (e: HTMLElement | undefined | null) => {
    if (!e) {
      return 0;
    }

    const computed = getComputedStyle(e);
    return e.offsetHeight + parseInt(computed.marginTop, 10) + parseInt(computed.marginBottom, 10);
  };

  let height =
    calculateHeight(document.getElementById("nux")) +
    calculateHeight(document.getElementById("suggestion")) +
    calculateHeight(document.getElementById("update-notification"));

  for (const e of document.getElementsByClassName("alternative-row")) {
    height += calculateHeight(e as HTMLElement);
  }

  for (const e of document.getElementsByClassName("spacer")) {
    height += calculateHeight(e as HTMLElement);
  }

  // add a small amount of extra padding to prevent a scrollbar on windows
  if (height > 0) {
    height += 2;
  }

  if (height != miniModeWindowHeight) {
    miniModeWindowHeight = height;
    ipcRenderer.send("setMiniModeWindowHeight", {
      height,
    });
  }
};

export const MiniModePage: React.FC = () => (
  <div id="mini-mode-page" className="h-screen w-screen overflow-x-hidden">
    <AlternativesList miniModePage={true} />
  </div>
);
