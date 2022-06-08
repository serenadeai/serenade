import React from "react";
import { Page, PostData, P, UL, Video } from "../../components/blog";

export const Post: PostData = {
  date: "January 19, 2021",
  slug: "mini-mode",
  title: "Mini-Mode and More Customization",
  content: (
    <>
      <P>We're starting the new year with a few exciting updates to Serenade!</P>
      <P>
        First, we're excited to launch one of our most-requested features: mini-mode. When coding
        with Serenade, the alternatives window helps you see what Serenade heard you say and enables
        you to easily make corrections by saying "two", "three", and so on. But, when Serenade only
        has one or two alternatives show, the tall window can take up valuable real estate on your
        screen. With mini-mode, Serenade appears in a much smaller window that can be overlaid on
        your editor, and the alternatives list only takes up as much space as it needs. You can even
        configure mini-mode to hide alternatives automatically after a command has been executed, so
        Serenade is as minimally-intrusive as possible.
      </P>
      <P>Check out mini-mode in action:</P>
      <Video src="https://cdn.serenade.ai/web/video/minimode.mov" />
      <P>To enable mini-mode, click the settings icon at the bottom of the Serenade app.</P>
      <P>
        We're also seeing more and more enterprise developers using Serenade for their work. To that
        end, we're continually adding enterprise-oriented features. For instance, the latest version
        of Serenade supports connecting via a proxy server, so devices with restricted network
        access can still authenticate with Serenade servers. And, Serenade Pro is faster than ever
        before, particularly on devices with lower-end specs. To get access, head to{" "}
        <a href="https://serenade.ai/pro">serenade.ai/pro</a>.
      </P>
      <P>
        Finally, we've heard that the `style` command is one of developers' favorites—it
        automatically formats your code using open-source code formatters without needing to
        configure anything in your editor. Now, you're able to customize what code styler is used
        for each language, including{" "}
        <a href="https://prettier.io" target="_blank">
          Prettier
        </a>
        ,{" "}
        <a href="https://github.com/psf/black" target="_blank">
          Black
        </a>
        ,{" "}
        <a href="https://github.com/google/google-java-format" target="_blank">
          google-java-format
        </a>
        , and{" "}
        <a href="https://clang.llvm.org/docs/ClangFormat.html" target="_blank">
          clang-format
        </a>
        . You can also configure Serenade to use formatters built into your editor, so if you have a
        formatter extension set up in VS Code, Serenade can use that as well. To change styler
        settings, just click the settings menu in the Serenade app, and then head to "Editor
        Settings".
      </P>
      <P>
        As always, we've included a number of small bugfixes and polish updates in this release as
        well. For a full list of updates in the latest version of Serenade, check out our{" "}
        <a href="https://serenade.ai/changelog">changelog</a>. And, reach out to our{" "}
        <a href="https://serenade.ai/community">community</a> if you have any questions or feedback!
      </P>
    </>
  ),
};

const Component = () => <Page post={Post} />;

export default Component;
