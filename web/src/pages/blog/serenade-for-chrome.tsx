import React from "react";
import { Page, PostData, Image, P, UL, Video } from "../../components/blog";

export const Post: PostData = {
  date: "June 30, 2020",
  slug: "serenade-for-chrome",
  title: "Serenade for Chrome",
  content: (
    <>
      <P>
        Software development involves much more than just writing code, and Serenade aims to bring
        voice to the entire development process. As a developer, chances are you spend a lot of time
        in a web browser reading documentation, browsing code on GitHub, and looking up answers on
        Stack Overflow. Today, we're excited to bring voice to all of these workflows with the
        launch of Serenade for Chrome.
      </P>
      <P>
        Serenade for Chrome brings Serenade's powerful voice commands to the web browser. Navigation
        is as simple as saying `open stack overflow` or `back`, you can manage tabs with commands
        like `new tab` or `next tab`, and text can be input with commands like `type hello`—the same
        commands you're already accustomed to using in Atom and VS Code.
      </P>
      <P>Here's a demo of Serenade for Chrome in action:</P>
      <Video src="https://cdn.serenade.ai/web/video/chrome-controls.mp4" />
      <P>
        Serenade for Chrome also introduces the new `show` command, which can be used to show
        selectable links, inputs, and code. For instance, by saying `show code` followed by a
        number, you can copy a block of code from a Stack Overflow answer or GitHub gist, then paste
        it into your editor by just saying `paste`. Or, you can use `show links` followed by a
        number to navigate link-heavy pages.
      </P>
      <Image src="https://cdn.serenade.ai/web/img/chrome-links.png" alt="Chrome Links Overlay" />
      <P>
        All of Serenade's text editing commands—like `type hello`, `delete next two words`, and
        `copy previous line`—are available in Chrome as well, whether you're typing a GitHub search
        or Gmail reply.
      </P>
      <Video src="https://cdn.serenade.ai/web/video/chrome-edit.mp4" />
      <P>
        Serenade for Chrome is now freely available from the Chrome web store{" "}
        <a href="https://chrome.google.com/webstore/detail/serenade-for-chrome/bgfbijeikimjmdjldemlegooghdjinmj">
          here
        </a>
        . To learn all of the voice commands supported in Serenade for Chrome, check out our{" "}
        <a href="https://serenade.ai/docs/browser">Chrome documentation</a>.
      </P>
      <P>
        We're excited to hear your feedback! If you run into any issues or have ideas for features,
        don't hesitate to reach out in the{" "}
        <a href="https://serenade.ai/community">community channel</a>.
      </P>
    </>
  ),
};

const Component = () => <Page post={Post} />;

export default Component;
