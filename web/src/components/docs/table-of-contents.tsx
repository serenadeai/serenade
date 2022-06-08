import React from "react";

export type TableOfContents = {
  sections: Section[];
  title: string;
};

export type Section = {
  title: string;
  content: React.FC;
  subsections?: Subsection[];
};

export type Subsection = {
  title: string;
  content: React.FC;
};
