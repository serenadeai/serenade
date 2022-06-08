import React, { useState } from "react";
import { ipcRenderer } from "electron";

export const TextInputPage = () => {
  const [text, setText] = useState("");

  return (
    <div className="w-screen h-screen">
      <input
        type="text"
        className="draggable w-full h-full outline-none text-lg px-4 text-slate-600"
        autoFocus
        value={text}
        onChange={(e) => {
          setText(e.target.value);
        }}
        onKeyDown={(e) => {
          // escape
          if (e.keyCode == 27) {
            e.preventDefault();
            ipcRenderer.send("hideTextInput");
            setText("");
          }
          // enter
          else if (e.keyCode == 13) {
            e.preventDefault();

            if (!e.shiftKey && !e.ctrlKey && !e.metaKey) {
              ipcRenderer.send("hideTextInput");
            }

            ipcRenderer.send("sendTextRequest", {
              text,
              includeAlternatives: true,
            });

            setText("");
          }
        }}
      />
    </div>
  );
};
