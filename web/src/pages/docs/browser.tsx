import * as React from "react";
import { Docs } from "../../components/pages";
import { BrowserTableOfContents } from "../../components/docs/browser/table-of-contents";

const DocsBrowserPage = () => (
  <Docs tableOfContents={BrowserTableOfContents} title="Browser Docs" />
);

export default DocsBrowserPage;
