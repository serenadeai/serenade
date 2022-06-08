import React from "react";
import { connect } from "react-redux";
import { ipcRenderer } from "electron";
import Metadata from "../../../shared/metadata";
import { Row, setValue } from "../settings";
import { Select } from "../../components/select";
import { Toggle } from "../../components/toggle";

const GeneralComponent: React.FC<{
  darkMode: string;
  microphones: any[];
  miniMode: boolean;
  miniModeFewerAlternativesCount: number;
  miniModeHideTimeout: number;
  pushToTalk: string;
  useMiniModeFewerAlternatives: boolean;
  useMiniModeHideTimeout: boolean;
  volume: number;
}> = ({
  darkMode,
  microphones,
  miniMode,
  miniModeFewerAlternativesCount,
  miniModeHideTimeout,
  pushToTalk,
  useMiniModeFewerAlternatives,
  useMiniModeHideTimeout,
  volume,
}) => {
  const metadata = new Metadata();
  const appearanceOptions = [
    { id: "system", value: "System" },
    { id: "light", value: "Light" },
    { id: "dark", value: "Dark" },
  ];

  return (
    <div className="px-4">
      {microphones ? (
        <Row
          title="Microphone"
          subtitle={
            <>
              <div className="inline-block border rounded w-44 h-2">
                <div
                  className="bg-violet-600 h-full rounded transition-all"
                  style={{ width: volume * 100 + "%" }}
                ></div>
              </div>
            </>
          }
          action={
            <div className="w-52 ml-auto">
              <Select
                items={microphones.map((e: any) => e.name)}
                value={microphones.filter((e: any) => e.selected)[0].name}
                onChange={(value: any) =>
                  ipcRenderer.send("setSettings", {
                    microphone: microphones.filter((e: any) => e.name == value)[0],
                  })
                }
              />
            </div>
          }
        />
      ) : null}
      <Row
        title="Appearance"
        subtitle="Change the UI to light or dark mode"
        action={
          <div className="w-40 ml-auto">
            <Select
              items={appearanceOptions.map((e: any) => e.value)}
              value={appearanceOptions.filter((e: any) => e.id == darkMode)[0].value}
              onChange={(value: any) =>
                setValue("darkMode", appearanceOptions.filter((e: any) => e.value == value)[0].id)
              }
            />
          </div>
        }
      />
      <Row
        title="Listen shortcut"
        subtitle="Keyboard shortcut for toggling Serenade"
        action={
          <input
            type="text"
            className="input w-32 py-1"
            defaultValue={pushToTalk}
            onChange={(e) => setValue("pushToTalk", e)}
          />
        }
      />
      <Row
        title="Compact UI"
        subtitle="Shrink the main Serenade window"
        action={<Toggle value={miniMode} onChange={(e) => setValue("miniMode", e)} />}
      />
      <Row
        title="Hide alternatives automatically"
        subtitle={
          <>
            In compact UI, hide after{" "}
            <input
              type="text"
              className="input w-8 inline-block disabled:bg-gray-300 py-0"
              defaultValue={miniModeHideTimeout}
              disabled={!useMiniModeHideTimeout}
              onChange={(e) => {
                const value = parseFloat(e.target.value);
                if (isNaN(value)) {
                  return;
                }

                ipcRenderer.send("setSettings", { miniModeHideTimeout: value });
              }}
            />{" "}
            seconds
          </>
        }
        action={
          <Toggle
            value={useMiniModeHideTimeout}
            onChange={(e) => setValue("useMiniModeHideTimeout", e)}
          />
        }
      />
      <Row
        title="Limit alternatives"
        subtitle={
          <>
            In compact UI, only show{" "}
            <input
              type="text"
              className="input w-8 inline-block disabled:bg-gray-300 py-0"
              defaultValue={miniModeFewerAlternativesCount}
              disabled={!useMiniModeFewerAlternatives}
              onChange={(e) => {
                const value = parseInt(e.target.value, 10);
                if (isNaN(value)) {
                  return;
                }

                ipcRenderer.send("setSettings", { miniModeFewerAlternativesCount: value });
              }}
            />{" "}
            alternatives
          </>
        }
        action={
          <Toggle
            value={useMiniModeFewerAlternatives}
            onChange={(e) => setValue("useMiniModeFewerAlternatives", e)}
          />
        }
      />
      <div className="flex items-start py-2">
        <h3 className="block text-sm">Serenade v{metadata.version}</h3>
      </div>
    </div>
  );
};

export const General = connect((state: any) => ({
  darkMode: state.darkMode,
  microphones: state.microphones,
  miniMode: state.miniMode,
  miniModeFewerAlternativesCount: state.miniModeFewerAlternativesCount,
  miniModeHideTimeout: state.miniModeHideTimeout,
  pushToTalk: state.pushToTalk,
  useMiniModeFewerAlternatives: state.useMiniModeFewerAlternatives,
  useMiniModeHideTimeout: state.useMiniModeHideTimeout,
  volume: state.volume,
}))(GeneralComponent);
