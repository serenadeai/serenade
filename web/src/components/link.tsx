import React from "react";
import { Link as GatsbyLink } from "gatsby";

export const Link: React.FC<{ to: string }> = ({ children, to }) => (
  <GatsbyLink
    to={to}
    className="text-purple-500 hover:text-purple-600 transition-colors cursor-pointer"
  >
    {children}
  </GatsbyLink>
);
