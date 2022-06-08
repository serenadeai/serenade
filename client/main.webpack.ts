import * as path from "path";
import { Configuration } from "webpack";

const nodeExternals = require("webpack-node-externals");
const WebpackShellPlugin = require("webpack-shell-plugin-next");

const config: Configuration = {
  resolve: {
    extensions: [".tsx", ".ts", ".js"],
  },
  devtool: "source-map",
  entry: path.resolve(__dirname, "src/main/index.ts"),
  target: "electron-main",
  module: {
    rules: [
      {
        test: /\.(ts|tsx)$/,
        include: [
          path.resolve(__dirname, "src/gen"),
          path.resolve(__dirname, "src/main"),
          path.resolve(__dirname, "src/shared"),
        ],
        use: ["ts-loader"],
      },
      {
        test: /\.(png|svg|jpg)$/i,
        type: "asset/resource",
      },
    ],
  },
  output: {
    path: path.resolve(__dirname, "out"),
    filename: "[name].js",
  },
  externals: [nodeExternals()],
  externalsPresets: { node: true },
  plugins: [
    new WebpackShellPlugin({
      onBuildEnd: {
        scripts: [
          () => {
            const fs = require("fs-extra");
            fs.mkdirpSync("out/static");
            fs.copySync(
              "static/custom-commands-server/node_modules",
              "out/static/custom-commands-server-modules"
            );
          },
        ],
      },
    }),
  ],
};

export default config;
