import React from "react";
import classNames from "classnames";
import { Link } from "gatsby";

export const GradientButton: React.FC<{
  text: string;
  href?: string;
  innerStyleOverrides?: { [k: string]: any };
  light?: boolean;
  onClick?: (e: React.MouseEvent) => void;
  outerStyleOverrides?: { [k: string]: any };
  submit?: boolean;
}> = ({ href, innerStyleOverrides, light, onClick, outerStyleOverrides, submit, text }) => {
  const outerClassName = "inline-block rounded-full gradient-button";
  const outerStyle = {
    background: "linear-gradient(#a46dff 3.88%, #ff8388 40.34%, #eec14d 70.02%, #82c0ff 100%)",
    padding: onClick || href ? "3px" : "2px",
    ...outerStyleOverrides,
  };

  const interactive = onClick || href || submit;
  const innerClassNames = classNames(
    "gradient-button-inner inline-block rounded-full transition-colors",
    {
      "px-6": interactive,
      "py-2": interactive,
      "hover:bg-slate-700": interactive,
      "hover:text-white": interactive,
      "px-3": !interactive,
      "font-light": !interactive,
      "bg-slate-600": !light,
      "text-white": !light,
      "bg-white": light,
      "text-slate-600": light,
    }
  );

  if (onClick) {
    return (
      <a href="#" onClick={onClick} className={outerClassName} style={outerStyle}>
        <div className={innerClassNames} style={innerStyleOverrides}>
          {text}
        </div>
      </a>
    );
  } else if (href) {
    return (
      <Link to={href} className={outerClassName} style={outerStyle}>
        <div className={innerClassNames} style={innerStyleOverrides}>
          {text}
        </div>
      </Link>
    );
  } else if (submit) {
    return (
      <button type="submit" className={outerClassName} style={outerStyle}>
        <span className={innerClassNames} style={innerStyleOverrides}>
          {text}
        </span>
      </button>
    );
  }

  return (
    <span to={href} className={outerClassName} style={outerStyle}>
      <span className={innerClassNames} style={innerStyleOverrides}>
        {text}
      </span>
    </span>
  );
};
