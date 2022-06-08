import React, { useState } from "react";
import { connect } from "react-redux";
import classNames from "classnames";
import { ipcRenderer } from "electron";
import { Link } from "react-router-dom";
import { Row } from "../settings";
import { Toggle } from "../../components/toggle";
import onboardingPrivacy from "../../../../static/img/onboarding-privacy.svg";

const PrivacyPageComponent: React.FC<{ logAudio: boolean; logSource: boolean }> = ({
  logAudio,
  logSource,
}) => {
  return (
    <div className="h-screen w-full bg-slate-600 dark:bg-indigo-800">
      <div
        className="h-full w-full bg-white dark:bg-neutral-800"
        style={{ borderBottomRightRadius: "100% 100%" }}
      >
        <div className="w-full h-full flex items-center p-6">
          <div className="w-6/12 mx-auto">
            <h2 className="text-xl font-light pt-5">Privacy Settings</h2>
            <p className="text-sm">
              Serenade is an open-source product, and you can help improve Serenade by anonymously
              sharing your audio and command data. You can change these later in the settings menu.
            </p>
            <Row
              title="Share audio data"
              action={
                <Toggle
                  value={logAudio}
                  onChange={(e) =>
                    ipcRenderer.send("setSettings", {
                      logAudio: e,
                    })
                  }
                />
              }
            />
            <Row
              title="Share command data"
              action={
                <Toggle
                  value={logSource}
                  onChange={(e) =>
                    ipcRenderer.send("setSettings", {
                      logSource: e,
                    })
                  }
                />
              }
            />
            <div className="pt-6">
              <Link
                to={process.platform == "darwin" ? "/permissions" : "/tutorials"}
                className="secondary-button"
              >
                Continue
              </Link>
            </div>
          </div>
          <div className="w-6/12">
            <img className="w-full px-12" src={onboardingPrivacy} alt="Privacy Settings" />
          </div>
        </div>
      </div>
    </div>
  );
};

export const PrivacyPage = connect((state: any) => ({
  logAudio: state.logAudio,
  logSource: state.logSource,
}))(PrivacyPageComponent);
