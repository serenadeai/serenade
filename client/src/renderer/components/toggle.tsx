import React, { useState } from "react";
import classNames from "classnames";
import { Switch } from "@headlessui/react";

export const Toggle: React.FC<{
  onChange: (value: boolean) => void;
  value: boolean;
}> = ({ onChange, value }) => (
  <Switch
    checked={value}
    onChange={onChange}
    className={classNames(
      "relative inline-flex flex-shrink-0 h-[24px] w-[44px] border-2 border-transparent rounded-full cursor-pointer transition-colors ease-in-out duration-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-white focus-visible:ring-opacity-75 shadow",
      {
        "bg-blue-700": value,
        "bg-gray-300": !value,
      }
    )}
  >
    <span
      aria-hidden="true"
      className={classNames(
        "pointer-events-none inline-block h-[20px] w-[20px] rounded-full bg-white shadow-lg transform ring-0 transition ease-in-out duration-200",
        {
          "translate-x-5": value,
          "translate-x-0": !value,
        }
      )}
    />
  </Switch>
);
