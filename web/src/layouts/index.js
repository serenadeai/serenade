import React from "react";
import { StoreProvider } from "../lib/store";

export default function Layout({ children }) {
  return <StoreProvider>{children}</StoreProvider>;
}
