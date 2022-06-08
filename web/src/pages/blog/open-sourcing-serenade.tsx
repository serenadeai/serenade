import React from "react";
import { Link } from "gatsby";
import { Page, PostData, P } from "../../components/blog";

export const Post: PostData = {
  date: "June 8, 2022",
  slug: "open-sourcing-serenade",
  title: "Open-Sourcing Serenade",
  content: (
    <>
      <P>
        We started Serenade nearly three years ago with the goal of making programming accessible to
        everyone. Since then, we've been blown away by the support of the Serenade community and
        what developers have coded with voice.
      </P>
      <P>
        Our vision has always been to ensure that Serenade is a viable long-term solution for anyone
        who needs it. To that end, one request from the community has been louder, clearer, and more
        consistent than any other: to open up the product and enable everyone to contribute to
        Serenade's mission. Today, we're excited to do just that. Serenade is now fully open-source.
      </P>
      <P>
        We've released a new{" "}
        <a href="https://github.com/serenadeai/serenade" target="_blank">
          GitHub repository
        </a>{" "}
        containing all of the code that's used to build Serenade. This repository includes our
        client application, online services (like our speech engine and code engine), machine
        learning models, and offline model training pipelines. You'll also find instructions for
        building and modifying Serenade yourself, as well as documentation describing how our
        systems work. Serenade is licensed under the permissive Apache 2.0 license, so you to use
        and modify the code freely.
      </P>
      <P>
        We're looking forward to accepting contributions from the community. We've published a brief{" "}
        <a
          href="https://github.com/serenadeai/serenade/blob/master/CONTRIBUTING.md"
          target="_blank"
        >
          Contributing Guide
        </a>{" "}
        that describes our process. In short, before opening any Pull Requests, just be sure to get
        the sign-off of someone from the Serenade core team to make sure that we're all aligned on
        changes to the Serenade experience. And, by centralizing on GitHub Issues, rather than
        solely our Discord community, everyone will have more visibility into changes and the status
        of reports.
      </P>
      <P>
        By open-sourcing the Serenade codebase and models, we're affirming our commitment to making
        Serenade freely available to everyone for the long-term. Involving the community more deeply
        in the ongoing development of Serenade is an important next step in the evolution of the
        platform, and it's one that we're excited to make together.
      </P>
    </>
  ),
};

const Component = () => <Page post={Post} />;

export default Component;
