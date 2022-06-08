import React, { Fragment, useState } from "react";
import classNames from "classnames";
import { Listbox, Transition } from "@headlessui/react";
import { CheckIcon, SelectorIcon } from "@heroicons/react/solid";

export const Select: React.FC<{
  items: string[];
  onChange: (item: string) => void;
  value: string;
}> = ({ items, onChange, value }) => (
  <div className="w-full">
    <Listbox value={value} onChange={onChange}>
      <div className="relative">
        <Listbox.Button className="relative w-full py-1 pl-3 pr-10 text-left bg-white rounded-lg shadow-md cursor-default focus:outline-none focus-visible:ring-2 focus-visible:ring-opacity-75 focus-visible:ring-white focus-visible:ring-offset-orange-300 focus-visible:ring-offset-2 focus-visible:border-violet-600 text-sm dark:text-slate-600">
          <span className="block truncate">{value}</span>
          <span className="absolute inset-y-0 right-0 flex items-center pr-2 pointer-events-none">
            <SelectorIcon className="w-5 h-5 text-gray-400" aria-hidden="true" />
          </span>
        </Listbox.Button>
        <Transition
          as={Fragment}
          leave="transition ease-in duration-100"
          leaveFrom="opacity-100"
          leaveTo="opacity-0"
        >
          <Listbox.Options className="absolute w-full py-1 mt-1 overflow-auto bg-white rounded-md shadow-lg max-h-60 ring-1 ring-black ring-opacity-5 focus:outline-none text-sm z-10">
            {items.map((item, i) => (
              <Listbox.Option
                key={i}
                value={item}
                className={({ active }) =>
                  classNames("cursor-pointer select-none relative py-2 pl-6 pr-4", {
                    "text-slate-600": !active,
                    "text-violet-800 bg-violet-100": active,
                  })
                }
              >
                {({ selected, active }) => (
                  <>
                    <span
                      className={classNames("block", {
                        "font-medium": selected,
                        "font-normal": !selected,
                      })}
                    >
                      {item}
                    </span>
                    {selected ? (
                      <span className="absolute inset-y-0 left-0 flex items-center pl-1 text-violet-600">
                        <CheckIcon className="w-4 h-4" aria-hidden="true" />
                      </span>
                    ) : null}
                  </>
                )}
              </Listbox.Option>
            ))}
          </Listbox.Options>
        </Transition>
      </div>
    </Listbox>
  </div>
);
