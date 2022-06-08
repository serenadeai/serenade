import React from "react";
import classNames from "classnames";
import { connect } from "react-redux";
import { ipcRenderer } from "electron";
import { Step } from "../../shared/tutorial";

const NuxComponent: React.FC<{
  miniMode: boolean;
  nuxHintShown: boolean;
  nuxNextButtonEnabled: boolean;
  nuxStep: Step;
}> = ({ miniMode, nuxHintShown, nuxNextButtonEnabled, nuxStep }) => {
  const back = (e: React.MouseEvent) => {
    e.preventDefault();
    ipcRenderer.send("nuxBack");
  };

  const close = (e: React.MouseEvent) => {
    e.preventDefault();
    ipcRenderer.send("setNuxCompleted", true);
  };

  const next = (e: React.MouseEvent) => {
    e.preventDefault();
    ipcRenderer.send("nuxNext");
  };

  const reset = (e: React.MouseEvent) => {
    e.preventDefault();
    ipcRenderer.send("setNuxCompleted", true);
    ipcRenderer.send("setNuxCompleted", false);
  };

  const showHint = (e: React.MouseEvent) => {
    e.preventDefault();
    ipcRenderer.send("showNuxHint");
  };

  if (!nuxStep) {
    return null;
  }

  return (
    <div
      id="nux"
      className={classNames(
        "border rounded shadow p-3 relative bg-white dark:bg-neutral-800 dark:border-neutral-500",
        {
          "border shadow mt-2 mb-4 mx-2": !miniMode,
          "mb-2": miniMode,
        }
      )}
    >
      <h2 className="font-bold pb-1">{nuxStep.title}</h2>
      <a className="absolute top-[-4px] right-[4px]" href="#" onClick={close}>
        &times;
      </a>
      <div>
        <div className="text-sm" dangerouslySetInnerHTML={{ __html: nuxStep.body }} />
        {nuxStep.hideAnswer && !nuxHintShown ? (
          <div className="pb-1 pl-1">
            <a
              href="#"
              onClick={showHint}
              className="font-bold text-blue-500 hover:text-blue-700 dark:text-blue-300 dark:hover:text-blue-500 text-sm transition-colors"
            >
              Show hint
            </a>
          </div>
        ) : null}
        {nuxStep.transcript ? (
          <div
            className={classNames("p-[2px] rainbow rounded-md", {
              hidden: nuxStep.hideAnswer && !nuxHintShown,
            })}
          >
            <div className="rounded-md bg-white dark:bg-gray-500 px-3 py-1 shadow">
              {nuxStep.transcript}
            </div>
          </div>
        ) : null}
      </div>
      {!nuxStep.last ? (
        <div className="mt-2 flex w-full">
          {!nuxStep.error && nuxStep.index !== undefined && nuxStep.index > 0 ? (
            <button className="primary-button text-xs" onClick={back}>
              &lsaquo; Back
            </button>
          ) : null}
          {!nuxStep.error ? (
            <button
              onClick={next}
              disabled={!nuxNextButtonEnabled}
              className={classNames("primary-button text-xs ml-auto", {
                "bg-gray-500 hover:bg-gray-500": !nuxNextButtonEnabled,
              })}
            >
              Next &rsaquo;
            </button>
          ) : null}
        </div>
      ) : (
        <div className="my-1">
          <div className="mb-2">
            <a href="#" className="primary-button block text-center text-sm" onClick={reset}>
              More tutorials
            </a>
          </div>
          <div>
            <a href="#" className="primary-button block text-center text-sm" onClick={next}>
              Done
            </a>
          </div>
        </div>
      )}
    </div>
  );
};

export const NUX = connect((state: any) => ({
  miniMode: state.miniMode,
  nuxHintShown: state.nuxHintShown,
  nuxNextButtonEnabled: state.nuxNextButtonEnabled,
  nuxStep: state.nuxStep,
}))(NuxComponent);
