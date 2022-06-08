import React from "react";

interface Props {
  title: string;
  id?: string;
  className?: string;
}

const format = (text: string) =>
  (text || "")
    .toLowerCase()
    .replace(" ", "-")
    .replace(/[^0-9a-zA-Z_-]/g, "");

export const Heading: React.FC<Props> = (props: Props) => {
  const id = props.id || format(props.title);
  return (
    <a href={`#${id}`}>
      <h2 id={id} className={`font-bold text-3xl mt-8 mb-4 ${props.className || ""}`}>
        {props.title}
        <i className="fas fa-link" aria-hidden="true" />
      </h2>
    </a>
  );
};

export const Link: React.FC<Props> = (props: Props) => {
  return (
    <a
      className={props.className}
      href={`#${props.id || format(props.title)}`}
      className={props.className}
    >
      {props.title}
    </a>
  );
};

export const Subheading: React.FC<Props> = (props: Props) => {
  const id = props.id || format(props.title);
  return (
    <a href={`#${id}`}>
      <h3 id={id} className={`font-light text-2xl my-3 ${props.className || ""}`}>
        {props.title}
        <i className="fas fa-link" aria-hidden="true" />
      </h3>
    </a>
  );
};

export const Subsubheading: React.FC<Props> = (props: Props) => {
  const id = props.id || format(props.title);
  return (
    <a href={`#${id}`}>
      <h4 id={id} className={`font-medium text-xl my-4 ${props.className || ""}`}>
        {props.title}
        <i className="fas fa-link" aria-hidden="true" />
      </h4>
    </a>
  );
};
