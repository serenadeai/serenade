import React, { useState } from "react";
import classNames from "classnames";
import { Link } from "react-router-dom";
import { plugins } from "../../../shared/plugins";
import onboardingPlugins from "../../../../static/img/onboarding-plugins.svg";

export const PluginsPage = () => {
  const [disabled, setDisabled] = useState(true);

  const onClick = (e: React.MouseEvent) => {
    setDisabled(false);
  };

  return (
    <div className="h-screen w-full bg-slate-600 dark:bg-indigo-800">
      <div
        className="h-full w-full bg-white dark:bg-neutral-800"
        style={{ borderBottomRightRadius: "100% 100%" }}
      >
        <div className="w-full h-full flex items-center p-6">
          <div className="w-6/12">
            <h2 className="text-xl font-light pt-5">Install Plugins</h2>
            <p>
              Serenade integrates with your editor via plugins. You'll need one to code with voice!
            </p>
            <div className="pt-2">
              <a
                href={plugins.vscode.url}
                onClick={onClick}
                target="_blank"
                className="primary-button block my-2 w-48"
              >
                VS Code
              </a>
              <a
                href={plugins.atom.url}
                onClick={onClick}
                target="_blank"
                className="primary-button block my-2 w-48"
              >
                Atom
              </a>
              <a
                href={plugins.jetbrains.url}
                onClick={onClick}
                target="_blank"
                className="primary-button block my-2 w-48"
              >
                JetBrains
              </a>
            </div>
            <div
              className={classNames("pt-4", {
                invisible: disabled,
              })}
            >
              <Link to="/privacy" className="secondary-button">
                Continue
              </Link>
            </div>
          </div>
          <div className="w-6/12">
            <img className="w-full px-4" src={onboardingPlugins} alt="Install Plugins" />
          </div>
        </div>
      </div>
    </div>
  );
};
