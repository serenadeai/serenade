import React from "react";
import classNames from "classnames";

export const LI: React.FC = ({ children }) => <li className="py-1">{children}</li>;

export const P: React.FC = ({ children }) => <p className="py-1">{children}</p>;

export const OL: React.FC = ({ children }) => (
  <ol className="py-2 ml-8 list-decimal">{children}</ol>
);

export const UL: React.FC = ({ children }) => <ol className="py-2 ml-8 list-disc">{children}</ol>;

export const Block: React.FC<{ id?: string; title?: string }> = ({ children, id, title }) => (
  <div
    id={id}
    className="container mx-auto rounded-lg border max-w-screen-lg shadow bg-white text-slate-600 px-10 py-8 my-8"
  >
    {title ? <h2 className="font-bold text-3xl pt-4 text-purple-500">{title}</h2> : null}
    {children}
  </div>
);

export const FormBlock: React.FC<{ title?: string }> = ({ children, title }) => (
  <div className="m-auto rounded-lg border shadow bg-white text-slate-600 my-20 px-20 py-12 w-5/12">
    {title ? <h2 className="font-bold text-3xl text-purple-500 pb-6">{title}</h2> : null}
    {children}
  </div>
);

export const Group: React.FC<{ description: string; id?: string; title: string }> = ({
  children,
  description,
  id,
  title,
}) => (
  <Block id={id}>
    <h2 className="font-bold text-3xl pb-2">{title}</h2>
    <h3 className="font-light text-xl pb-10">{description}</h3>
    {children}
  </Block>
);
