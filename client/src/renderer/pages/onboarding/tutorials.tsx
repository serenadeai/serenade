import React from "react";
import { ipcRenderer } from "electron";
import { Link } from "react-router-dom";
import { tutorials } from "../../../shared/tutorial";
import onboardingTutorials from "../../../../static/img/onboarding-tutorials.svg";

export const TutorialsPage = () => {
  const click = (e: React.MouseEvent, name: string) => {
    ipcRenderer.send("generateToken");
    ipcRenderer.send("loadTutorial", { name, resize: true });
  };

  return (
    <div className="h-screen w-full bg-slate-600 dark:bg-indigo-800">
      <div
        className="h-full w-full bg-white dark:bg-neutral-800"
        style={{ borderBottomRightRadius: "100% 100%" }}
      >
        <div className="w-full h-full flex items-center">
          <div className="w-6/12 mx-auto pl-6 pr-4">
            <h2 className="text-xl font-light pt-5">Complete a Tutorial</h2>
            <p>
              To start learning how to code with voice, choose one of the basic tutorials below.
            </p>
            <div className="grid grid-cols-2 gap-2 pt-4">
              {tutorials.map((tutorial) =>
                tutorial.basic ? (
                  <Link
                    to="/alternatives"
                    className="primary-button block"
                    onClick={(e) => click(e, tutorial.tutorial)}
                    key={tutorial.tutorial}
                  >
                    {tutorial.title.replace("Basics", "")}
                  </Link>
                ) : null
              )}
            </div>
          </div>
          <div className="w-6/12 pr-2">
            <img className="w-full" src={onboardingTutorials} alt="Complete a Tutorial" />
          </div>
        </div>
      </div>
    </div>
  );
};
