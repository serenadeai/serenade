import * as React from "react";
import { Docs } from "../../components/pages";
import { ProtocolTableOfContents } from "../../components/docs/protocol/table-of-contents";

const DocsProtocolPage = () => (
  <Docs tableOfContents={ProtocolTableOfContents} title="Protocol Docs" />
);

export default DocsProtocolPage;
