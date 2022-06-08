import { ipcRenderer } from "electron";
import * as revisionBox from "./pages/revision-box";
import { updateMiniModeWindowHeight } from "./pages/mini-mode";
import store from "./state/store";

export const register = () => {
  ipcRenderer.on("focusRevisionBox", (_event: any, data: any) => {
    revisionBox.focus();
  });

  ipcRenderer.on("focusTextInput", (_event: any, _data: any) => {
    const input: any = document.getElementById("text-input");
    if (!input) {
      return;
    }

    input.focus();
  });

  ipcRenderer.on("getRevisionBoxState", (_event: any, data: { id: string }) => {
    ipcRenderer.send("revisionBoxState", {
      id: data.id,
      ...revisionBox.getEditorState(),
    });
  });

  ipcRenderer.on(
    "setRevisionBoxState",
    (
      _event: any,
      data: { allEditors?: boolean; source: string; cursor: number; cursorEnd: number }
    ) => {
      revisionBox.setEditorState(
        { source: data.source, cursor: data.cursor, cursorEnd: data.cursorEnd },
        data.allEditors
      );
    }
  );

  ipcRenderer.on("setState", (_event: any, data: any) => {
    for (const k of Object.keys(data)) {
      store.dispatch({ type: k, [k]: data[k] });
    }
  });

  ipcRenderer.on("setURL", (_event: any, data: { url: string }) => {
    history.pushState(data.url, "Serenade");
  });

  ipcRenderer.on("updateMiniModeWindowHeight", (_event: any, _data: any) => {
    updateMiniModeWindowHeight();
  });
};
