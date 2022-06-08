import React, { useRef } from "react";
import classNames from "classnames";
import { connect } from "react-redux";
import { ipcRenderer } from "electron";
import { Spinner } from "./spinner";

const ListenToggleComponent: React.FC<{
  darkTheme: boolean;
  listening: boolean;
  localLoading: boolean;
  volume: number;
}> = ({ darkTheme, listening, localLoading, volume }) => {
  const toggle = (e: React.MouseEvent) => {
    e.preventDefault();
    ipcRenderer.send("toggleChunkManager", !listening);
  };

  const color = 210 - 70 * (listening ? volume : 0);
  const borderPadding = 6;
  const width = 52;
  const height = 24;
  const offset = -4;
  return (
    <>
      <div
        className={classNames("ml-2", {
          hidden: !localLoading,
        })}
      >
        <Spinner hidden={!localLoading} />
      </div>
      <div
        onClick={toggle}
        className={classNames("cursor-pointer mr-[5px] relative", {
          listening,
          hidden: localLoading,
        })}
        style={{
          width: width + "px",
          height: height + "px",
        }}
      >
        <div
          className="rainbow"
          style={{
            borderRadius: height / 2 + "px",
            width: width + "px",
            height: height + "px",
          }}
        >
          <div
            className="absolute"
            style={{
              background: listening ? `rgb(255, ${color}, ${color})` : "white",
              zIndex: 4,
              top: -offset / 2 + "px",
              left: listening ? width - height - offset / 2 + "px" : -offset / 2 + "px",
              width: height + offset + "px",
              height: height + offset + "px",
              borderRadius: height + "px",
              boxShadow: "0 0 4px #333",
              transition:
                "background 0.3s ease-in-out, box-shadow 0.3s ease-in-out, left 0.3s ease-in-out",
            }}
          />
          <div
            className="absolute"
            style={{
              background: listening
                ? darkTheme
                  ? "#6b7280"
                  : "white"
                : darkTheme
                ? "#4b5563"
                : "#ddd",
              position: "absolute",
              zIndex: 3,
              width: width - borderPadding + "px",
              height: height - borderPadding + "px",
              top: borderPadding / 2 + "px",
              left: borderPadding / 2 + "px",
              borderRadius: height / 2,
              transition: "background-color 0.3s ease-in-out",
            }}
          />
        </div>
      </div>
    </>
  );
};

export const ListenToggle = connect((state: any) => ({
  darkTheme: state.darkTheme,
  listening: state.listening,
  localLoading: state.localLoading,
  volume: state.volume,
}))(ListenToggleComponent);
