import React from "react";
import classNames from "classnames";
import { connect } from "react-redux";
import { ipcRenderer } from "electron";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faCheck, faFileAlt, faSearch } from "@fortawesome/free-solid-svg-icons";
import { core } from "../../gen/core";
import { languages } from "../../shared/languages";

const LanguagesPageComponent: React.FC<{ languageSwitcherLanguage: core.Language }> = ({
  languageSwitcherLanguage,
}) => (
  <div className="pt-[24px] h-full w-full">
    {["0"].concat(Object.keys(languages)).map((e: string) => {
      const language: core.Language = (e as unknown) as core.Language;
      const active = languageSwitcherLanguage == language;

      let name = "Auto-Detect";
      let icon = (
        <div className="pl-1">
          <FontAwesomeIcon icon={faSearch} />
        </div>
      );
      if (languages[language]) {
        name = languages[language]!.name;
        icon = languages[language]!.icon ? (
          <img className="w-6 h-6" src={languages[language]!.icon} alt={name} />
        ) : (
          <div className="pl-1">
            <FontAwesomeIcon icon={faFileAlt} />
          </div>
        );
      }

      return (
        <a
          key={name}
          href="#"
          className={classNames(
            "block w-full p-4 hover:bg-violet-100 dark:hover:bg-neutral-700 transition-colors border-b",
            {
              "bg-violet-400 hover:bg-violet-400 text-white": active,
            }
          )}
          onClick={(e) => {
            e.preventDefault();
            ipcRenderer.send("setLanguage", language);
            setTimeout(() => {
              ipcRenderer.send("closeLanguages");
            }, 100);
          }}
        >
          <div className="flex items-center">
            {active ? <FontAwesomeIcon icon={faCheck} /> : icon}
            <div className="pl-2">{name}</div>
          </div>
        </a>
      );
    })}
  </div>
);

export const LanguagesPage = connect((state: any) => ({
  languageSwitcherLanguage: state.languageSwitcherLanguage,
}))(LanguagesPageComponent);
