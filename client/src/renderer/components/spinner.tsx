import React from "react";
import classNames from "classnames";

export const Spinner: React.FC<{ hidden: boolean }> = ({ hidden }) => (
  <div className={classNames("lds-ring", { hidden })}>
    <div />
    <div />
    <div />
    <div />
  </div>
);
