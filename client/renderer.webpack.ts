import HtmlWebpackPlugin from "html-webpack-plugin";
import * as path from "path";
import { Configuration } from "webpack";
import * as webpackDevServer from "webpack-dev-server";

const config: Configuration = {
  resolve: {
    extensions: [".tsx", ".ts", ".js"],
    mainFields: ["main", "module", "browser"],
  },
  entry: path.resolve(__dirname, "src/renderer/index.tsx"),
  target: "electron-renderer",
  devtool: "source-map",
  module: {
    rules: [
      {
        test: /\.(ts|tsx)$/,
        include: [
          path.resolve(__dirname, "src/gen"),
          path.resolve(__dirname, "src/renderer"),
          path.resolve(__dirname, "src/shared"),
        ],
        use: ["ts-loader"],
      },
      {
        test: /\.(png|svg|jpg)$/i,
        type: "asset/resource",
      },
      {
        test: /\.css$/,
        include: [path.resolve(__dirname, "src/renderer/css")],
        use: [
          {
            loader: "style-loader",
          },
          {
            loader: "css-loader",
            options: {
              importLoaders: 1,
            },
          },
          {
            loader: "postcss-loader",
          },
        ],
      },
    ],
  },
  devServer: {
    static: {
      directory: path.resolve(__dirname, "out/renderer"),
      publicPath: "/",
    },
    port: 4000,
    historyApiFallback: true,
    compress: true,
  },
  output: {
    path: path.resolve(__dirname, "out/renderer"),
    filename: "js/[name].js",
  },
  plugins: [
    new HtmlWebpackPlugin({ template: path.resolve(__dirname, "src/renderer/index.html") }),
  ],
};

export default config;
