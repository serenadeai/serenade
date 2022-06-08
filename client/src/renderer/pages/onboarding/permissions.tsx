import React, { useState } from "react";
import classNames from "classnames";
import { ipcRenderer } from "electron";
import { Link } from "react-router-dom";
import onboardingPermissions from "../../../../static/img/onboarding-permissions.svg";

export const PermissionsPage = () => {
  const [accessibilityClicked, setAccessibilityClicked] = useState(false);
  const [microphoneClicked, setMicrophoneClicked] = useState(false);

  return (
    <div className="h-screen w-full bg-slate-600 dark:bg-indigo-800">
      <div
        className="h-full w-full bg-white dark:bg-neutral-800"
        style={{ borderBottomRightRadius: "100% 100%" }}
      >
        <div className="w-full h-full flex items-center p-6">
          <div className="w-6/12">
            <h2 className="text-xl font-light pt-5">Grant Permissions</h2>
            <p>
              Serenade integrates with microphone and accessibility APIs to give you powerful voice
              control over your device.
            </p>
            <div className="pt-2">
              <button
                className={classNames("primary-button block my-2", {
                  disabled: microphoneClicked,
                })}
                onClick={() => {
                  ipcRenderer.send("microphonePermission");
                  setMicrophoneClicked(true);
                }}
              >
                {microphoneClicked ? "Microphone requested" : "Request microphone"}
              </button>
              <button
                className={classNames("primary-button block my-2", {
                  disabled: accessibilityClicked,
                })}
                onClick={() => {
                  ipcRenderer.send("accessibilityPermission");
                  setAccessibilityClicked(true);
                }}
              >
                {accessibilityClicked ? "Accessibility requested" : "Request accessibility"}
              </button>
            </div>
            <div
              className={classNames("pt-2", {
                invisible: !microphoneClicked || !accessibilityClicked,
              })}
            >
              <Link to="/tutorials" className="secondary-button">
                Continue
              </Link>
            </div>
          </div>
          <div className="w-6/12">
            <img className="w-full" src={onboardingPermissions} alt="Grant Permissions" />
          </div>
        </div>
      </div>
    </div>
  );
};
