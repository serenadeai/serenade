import React from "react";
import { ipcRenderer } from "electron";
import { connect } from "react-redux";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faFileAlt, faGlobe } from "@fortawesome/free-solid-svg-icons";
import { languages } from "../../../shared/languages";
import { core } from "../../../gen/core";

const LanguageIndicatorComponent: React.FC<{
  language: core.Language;
  sourceAvailable: boolean;
}> = ({ language, sourceAvailable }) => {
  let icon = <FontAwesomeIcon icon={faGlobe} />;
  let name = "Text";
  if (languages[language]) {
    name = languages[language]!.name;
    icon = languages[language]!.icon ? (
      <img
        className={`h-4 w-4 ${language} inline-block`}
        src={languages[language]!.icon}
        alt={languages[language]!.name}
      />
    ) : (
      <FontAwesomeIcon icon={faFileAlt} />
    );
  } else if (sourceAvailable) {
    icon = <FontAwesomeIcon icon={faFileAlt} />;
  }

  return (
    <a
      className="inline-block text-slate-600 bg-gray-200 rounded text-xs px-1.5 py-0.5 mr-1 drop-shadow-sm transition-colors hover:bg-gray-300 dark:bg-gray-600 dark:text-neutral-100 dark:hover:bg-gray-700"
      href="#"
      onClick={(e: React.MouseEvent) => {
        e.preventDefault();
        ipcRenderer.send("showLanguageSwitcher");
      }}
    >
      {icon} {name}
    </a>
  );
};

export const LanguageIndicator = connect((state: any) => ({
  language: state.language,
  sourceAvailable: state.sourceAvailable,
}))(LanguageIndicatorComponent);
