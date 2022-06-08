import * as React from "react";
import { Docs } from "../../components/pages";
import { ApiTableOfContents } from "../../components/docs/api/table-of-contents";

const DocsApiPage = () => <Docs tableOfContents={ApiTableOfContents} title="API Docs" />;

export default DocsApiPage;
