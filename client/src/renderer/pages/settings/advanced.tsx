import React from "react";
import { connect } from "react-redux";
import { ipcRenderer } from "electron";
import { Row, setValue } from "../settings";
import { Select } from "../../components/select";
import { Toggle } from "../../components/toggle";

const AdvancedComponent: React.FC<{
  animations: boolean;
  clipboardInsert: boolean;
  editorAutocomplete: boolean;
  chunkSilenceThreshold: number;
  chunkSpeechThreshold: number;
  continueRunningInTray: boolean;
  disableSuggestions: boolean;
  executeSilenceThreshold: number;
  minimizedPosition: string;
  miniModeReversed: boolean;
  showRevisionBox: any;
  textInputKeybinding: string;
  useVerboseLogging: boolean;
}> = ({
  animations,
  clipboardInsert,
  editorAutocomplete,
  chunkSilenceThreshold,
  chunkSpeechThreshold,
  continueRunningInTray,
  disableSuggestions,
  executeSilenceThreshold,
  minimizedPosition,
  miniModeReversed,
  showRevisionBox,
  textInputKeybinding,
  useVerboseLogging,
}) => {
  const minimizedPositionOptions = [
    { id: "window", value: "Follow window" },
    { id: "top-left", value: "Top-left" },
    { id: "top-right", value: "Top-right" },
    { id: "bottom-right", value: "Bottom-right" },
    { id: "bottom-left", value: "Bottom-left" },
  ];

  return (
    <div className="px-4">
      <Row
        title="Continue running in tray"
        subtitle="When closed, continue running in the tray rather than quitting"
        action={
          <Toggle
            value={continueRunningInTray}
            onChange={(e) => setValue("continueRunningInTray", e)}
          />
        }
      />
      <Row
        title="Automatically show revision box"
        subtitle="For apps without plugin, automatically show the revision box when dictating"
        action={
          <Toggle
            value={showRevisionBox && showRevisionBox["default"] != "never"}
            onChange={(e) =>
              ipcRenderer.send("setSettings", {
                showRevisionBox: { default: e ? "auto" : "never" },
              })
            }
          />
        }
      />
      <Row
        title="Show suggestions"
        subtitle="Display tips and tricks as you're using Serenade"
        action={
          <Toggle
            value={!disableSuggestions}
            onChange={(e) => setValue("disableSuggestions", !e)}
          />
        }
      />
      <Row
        title="Reverse alternatives when above"
        subtitle="When alternatives are above the window, show the first at the bottom"
        action={
          <Toggle value={miniModeReversed} onChange={(e) => setValue("miniModeReversed", e)} />
        }
      />
      <Row
        title="Use clipboard for text"
        subtitle="When adding system-wide text, use the clipboard rather than individual keypresses"
        action={<Toggle value={clipboardInsert} onChange={(e) => setValue("clipboardInsert", e)} />}
      />
      <Row
        title="Show editor animations"
        subtitle="Show animations when editing code"
        action={<Toggle value={animations} onChange={(e) => setValue("animations", e)} />}
      />
      <Row
        title="Automatically trigger autocomplete"
        subtitle="Show the autocomplete window after adding code"
        action={
          <Toggle value={editorAutocomplete} onChange={(e) => setValue("editorAutocomplete", e)} />
        }
      />
      <Row
        title="Minimized position"
        subtitle="Where alternatives appear when Serenade is minimized"
        action={
          <div className="w-40 ml-auto">
            <Select
              items={minimizedPositionOptions.map((e: any) => e.value)}
              value={
                minimizedPositionOptions.filter((e: any) => e.id == minimizedPosition)[0].value
              }
              onChange={(value: any) =>
                setValue(
                  "minimizedPosition",
                  minimizedPositionOptions.filter((e: any) => e.value == value)[0].id
                )
              }
            />
          </div>
        }
      />
      <Row
        title="Command wait time"
        subtitle={
          <>
            How long to wait before executing a command. Higher means slower speaking pace.{" "}
            <a
              href="#"
              className="underline"
              onClick={(e) => {
                e.preventDefault();
                ipcRenderer.send("setSettings", {
                  executeSilenceThreshold: 1,
                });
              }}
            >
              Reset
            </a>
          </>
        }
        action={
          <input
            type="number"
            className="text-sm input ml-1"
            min="0.5"
            max="2.0"
            step="0.1"
            value={executeSilenceThreshold}
            onChange={(e) =>
              ipcRenderer.send("setSettings", {
                executeSilenceThreshold: parseFloat(e.target.value),
              })
            }
          />
        }
      />
      <Row
        title="Speech strictness"
        subtitle={
          <>
            How strict the speech detector should be. Higher means fewer things called speech.{" "}
            <a
              href="#"
              className="underline"
              onClick={(e) => {
                e.preventDefault();
                ipcRenderer.send("setSettings", {
                  chunkSpeechThreshold: 0.3,
                });
              }}
            >
              Reset
            </a>
          </>
        }
        action={
          <input
            type="number"
            className="text-sm input ml-1"
            min="0.0"
            max="1.0"
            step="0.1"
            value={chunkSpeechThreshold}
            onChange={(e) =>
              ipcRenderer.send("setSettings", {
                chunkSpeechThreshold: parseFloat(e.target.value),
              })
            }
          />
        }
      />
      <Row
        title="Silence strictness"
        subtitle={
          <>
            How strict the silence detector should be. Higher means more things called silence.{" "}
            <a
              href="#"
              className="underline"
              onClick={(e) => {
                e.preventDefault();
                ipcRenderer.send("setSettings", {
                  chunkSilenceThreshold: 0.1,
                });
              }}
            >
              Reset
            </a>
          </>
        }
        action={
          <input
            type="number"
            className="text-sm input ml-1"
            min="0.0"
            max="1.0"
            step="0.1"
            value={chunkSilenceThreshold}
            onChange={(e) =>
              ipcRenderer.send("setSettings", {
                chunkSilenceThreshold: parseFloat(e.target.value),
              })
            }
          />
        }
      />
      <Row
        title="Toggle text input"
        subtitle="Keyboard shortcut for toggling type to Serenade"
        action={
          <input
            type="text"
            className="input w-36 py-1"
            defaultValue={textInputKeybinding}
            onChange={(e) => setValue("textInputKeybinding", e)}
          />
        }
      />
      <Row
        title="Use verbose logging"
        subtitle={
          <>
            Write more information to logs; useful for debugging.
            <br />
            <a
              href="#"
              className="underline"
              onClick={(e) => {
                e.preventDefault();
                ipcRenderer.send("openLogDirectory");
              }}
            >
              View logs
            </a>
          </>
        }
        action={
          <Toggle value={useVerboseLogging} onChange={(e) => setValue("useVerboseLogging", e)} />
        }
      />
    </div>
  );
};

export const Advanced = connect((state: any) => ({
  animations: state.animations,
  clipboardInsert: state.clipboardInsert,
  editorAutocomplete: state.editorAutocomplete,
  chunkSilenceThreshold: state.chunkSilenceThreshold,
  chunkSpeechThreshold: state.chunkSpeechThreshold,
  continueRunningInTray: state.continueRunningInTray,
  disableSuggestions: state.disableSuggestions,
  executeSilenceThreshold: state.executeSilenceThreshold,
  minimizedPosition: state.minimizedPosition,
  miniModeReversed: state.miniModeReversed,
  showRevisionBox: state.showRevisionBox,
  textInputKeybinding: state.textInputKeybinding,
  useVerboseLogging: state.useVerboseLogging,
}))(AdvancedComponent);
