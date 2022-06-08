import atom from "../../static/img/atom.png";
import vscode from "../../static/img/vscode.png";
import jetbrains from "../../static/img/jetbrains.png";
import hyper from "../../static/img/hyper.png";
import iterm from "../../static/img/iterm.png";
import chrome from "../../static/img/chrome.png";
import edge from "../../static/img/edge.png";

export interface PluginConfiguration {
  name: string;
  icon: string;
  url: string;
}

export const plugins: { [key: string]: PluginConfiguration } = {
  atom: {
    name: "Atom",
    icon: atom,
    url: "https://atom.io/packages/serenade",
  },
  vscode: {
    name: "VS Code",
    icon: vscode,
    url: "https://marketplace.visualstudio.com/items?itemName=serenade.serenade",
  },
  jetbrains: {
    name: "JetBrains",
    icon: jetbrains,
    url: "https://serenade.ai/install#jetbrains",
  },
  hyper: {
    name: "Hyper",
    icon: hyper,
    url: "https://github.com/serenadeai/hyper",
  },
  iterm: {
    name: "iTerm",
    icon: iterm,
    url: "https://github.com/serenadeai/iterm2",
  },
  chrome: {
    name: "Chrome",
    icon: chrome,
    url: "https://chrome.google.com/webstore/detail/bgfbijeikimjmdjldemlegooghdjinmj",
  },
  edge: {
    name: "Edge",
    icon: edge,
    url: "https://chrome.google.com/webstore/detail/bgfbijeikimjmdjldemlegooghdjinmj",
  },
};
