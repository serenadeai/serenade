import React from "react";
import classNames from "classnames";

export const FullWidthFeature: React.FC<{
  content?: any;
  dark?: boolean;
  gray?: boolean;
  gradient?: string;
  subtitle: string;
  title: string;
}> = ({ content, dark, gradient, gray, subtitle, title }) => (
  <div
    className={classNames("w-screen", {
      "bg-slate-600": dark,
      "bg-gray-100": gray,
      "py-12": gray,
    })}
    style={
      gradient
        ? {
            background: "linear-gradient(0deg, #EDC04C, #F78291, #A56DFE, #89C2FC, #C3D1E8)",
          }
        : {}
    }
  >
    <div
      className={classNames("w-screen", {
        "bg-white": gradient,
      })}
      style={{
        borderTopLeftRadius: gradient == "top-left" ? "100% 70%" : 0,
        borderTopRightRadius: gradient == "top-right" ? "100% 70%" : 0,
        borderBottomLeftRadius: gradient == "bottom-left" ? "100% 70%" : 0,
        borderBottomRightRadius: gradient == "bottom-right" ? "100% 70%" : 0,
      }}
    >
      <div
        className={classNames("py-16 md:py-24", {
          "mb-12": dark,
          "py-32": gray,
          "pb-36": dark,
        })}
      >
        <div className="mx-auto max-w-screen-md px-4 md:text-center">
          <h2
            className={classNames("text-5xl font-bold pb-2", {
              "text-white": dark,
            })}
          >
            {title}
          </h2>
          <p
            className={classNames("text-2xl font-light pt-2 pb-6", {
              "text-white": dark,
            })}
          >
            {subtitle}
          </p>
        </div>
        {content}
      </div>
    </div>
  </div>
);

export const TwoColumnFeature: React.FC<{
  content: any;
  dark?: boolean;
  gradient?: string;
  gray?: boolean;
  subtitle: string;
  title: string;
}> = ({ content, dark, gray, gradient, subtitle, title }) => (
  <div
    className={classNames("w-screen", {
      "bg-slate-600": dark,
      "bg-gray-100": gray,
    })}
    style={
      gradient
        ? {
            background: "linear-gradient(180deg, #EDC04C, #F78291, #A56DFE, #89C2FC, #C3D1E8)",
          }
        : {}
    }
  >
    <div
      className={classNames("w-screen", {
        "bg-white": gradient,
      })}
      style={{
        borderTopLeftRadius: gradient == "top-left" ? "80% 100%" : 0,
        borderTopRightRadius: gradient == "top-right" ? "80% 100%" : 0,
        borderBottomLeftRadius: gradient == "bottom-left" ? "80% 100%" : 0,
        borderBottomRightRadius: gradient == "bottom-right" ? "80% 100%" : 0,
      }}
    >
      <div
        className={classNames(
          "mx-auto max-w-screen-xl flex items-center py-16 md:py-24 two-column",
          {
            "mb-12": dark,
            "pb-24 md:pb-36": dark,
          }
        )}
      >
        <div className="two-column-left px-8 flex-1" style={{ flexGrow: 2 }}>
          <h2
            className={classNames("text-5xl font-bold pb-4", {
              "text-white": dark,
            })}
          >
            {title}
          </h2>
          <p
            className={classNames("text-2xl font-light", {
              "text-white": dark,
            })}
          >
            {subtitle}
          </p>
        </div>
        <div className="two-column-right px-8 flex-1 text-center" style={{ flexGrow: 3 }}>
          {content}
        </div>
      </div>
    </div>
  </div>
);
