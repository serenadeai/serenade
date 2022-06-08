import * as React from "react";
import { Docs } from "../components/pages";
import { EditorTableOfContents } from "../components/docs/editor/table-of-contents";

const DocsPage = () => <Docs tableOfContents={EditorTableOfContents} title="Editor Docs" />;

export default DocsPage;
