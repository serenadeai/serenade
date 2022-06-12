import React from "react";
import { connect } from "react-redux";
import { plugins } from "../../../shared/plugins";
import icon from "../../../../static/img/icon.png";
import firefox from "../../../../static/img/firefox.png";
import safari from "../../../../static/img/safari.png";
import slack from "../../../../static/img/slack.png";
import terminal from "../../../../static/img/terminal.png";
import dialog from "../../../../static/img/dialog.png";
import windowIcon from "../../../../static/img/window.png";

const apps: { [key: string]: { name: string; icon: string } } = {
  ...plugins,
  "revision-box": {
    name: "Revision Box",
    icon,
  },
  firefox: {
    name: "Firefox",
    icon: firefox,
  },
  safari: {
    name: "Safari",
    icon: safari,
  },
  serenade: {
    name: "Serenade",
    icon,
  },
  slack: {
    name: "Slack",
    icon: slack,
  },
  terminal: {
    name: "Terminal",
    icon: terminal,
  },
  "system dialog": {
    name: "System Dialog",
    icon: dialog,
  },
};

const ActiveAppIndicatorComponent: React.FC<{
  app: string;
  pluginInstalled: boolean;
  icon: string;
}> = ({ app, pluginInstalled, icon }) => {
  let name = app;
  if (Object.keys(apps).includes(app)) {
    name = apps[app].name;
  } else if (!pluginInstalled) {
    name = "Other";
  }

  const iconSrc = icon || (Object.keys(apps).includes(app) ? apps[app].icon : windowIcon);
  return (
    <div className="block text-xs drop-shadow-sm px-1.5 py-0.5 ">
      <img
        className="w-4 h-4 inline-block mr-1"
        src={iconSrc}
        alt={name}
        style={{ marginTop: "-2px" }}
      />{" "}
      {name}
    </div>
  );
};

export const ActiveAppIndicator = connect((state: any) => ({
  app: state.app,
  pluginInstalled: state.pluginInstalled,
  icon: state.icon,
}))(ActiveAppIndicatorComponent);
