import React from "react";
import { Snippet } from "../../snippet";

export const Intro = () => (
  <>
    <p>
      In addition to scripting automations and creating snippets, you can also customize how
      Serenade interacts with your system.
    </p>
  </>
);

export const AccessibilityApi = () => (
  <>
    <p>
      On macOS and Windows, Serenade integrates with OS-level accessibility APIs in order to enable
      dictation into applications even without official Serenade plugins or the Revision Box. Since
      these APIs are often inconsistently implemented across applications, this behavior is opt-in
      by default. To enable accessibilty API support for an application, add it to{" "}
      <code>~/.serenade/settings.json</code>:
    </p>
    <Snippet
      code={`{
  "use_accessibility_api": [
    "slack",
    "discord"
  ]
}`}
    />
  </>
);

export const RevisionBox = () => (
  <>
    <p>
      When Serenade can't read a text field, because there's no dedicated Serenade plugin or
      accessibility APIs aren't implemented properly, you can configure the Revision Box to appear
      automatically. In your <code>~/.serenade/settings.json</code>, you can configure the behavior
      of the Revision Box on a per-application basis—below is an example. Here, the default behavior
      for applications is to not show the Revision Box at all, for <code>slack</code> to always show
      the Revision Box, and for <code>mail</code> to show the Revision Box only when the
      accessibility API returns no value.
    </p>
    <Snippet
      code={`{
  "show_revision_box": {
    "all_apps": "never",
    "slack": "always",
    "mail": "auto"
  }
}`}
    />
  </>
);
