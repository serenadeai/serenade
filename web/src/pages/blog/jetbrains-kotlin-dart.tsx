import React from "react";
import { Page, PostData, P, UL, Video } from "../../components/blog";

export const Post: PostData = {
  date: "October 6, 2020",
  slug: "jetbrains-kotlin-dart",
  title: "JetBrains, Kotlin, and Dart",
  content: (
    <>
      <P>
        Today, we're excited to bring our next round of platforms and languages out of beta:
        Serenade now natively supports all JetBrains IDEs—including IntelliJ, PyCharm, and
        WebStorm—along with the Kotlin and Dart programming languages.
      </P>
      <P>
        The JetBrains IDE family is loved by developers for its powerful refactoring, navigation,
        and debugging tools. With the Serenade for JetBrains plugin, you can control your IDE with
        just your voice, from managing tabs and files to editing and running code. Whether you're
        looking to replace keyboard shortcuts with succinct voice commands or enable a totally
        hands-free IDE workflow, Serenade's best-in-class speech to code engine can provide a major
        boost for your productivity. Serenade for JetBrains is available for download{" "}
        <a href="https://plugins.jetbrains.com/plugin/14939-serenade">here</a>.
      </P>
      <Video src="https://cdn.serenade.ai/web/video/kotlin-demo.mp4" />
      <P>
        Along with our JetBrains plugin, we're excited to add support for the Kotlin and Dart
        languages to Serenade. We're seeing more of our community using Serenade to build mobile
        applications, so Kotlin and Dart were natural languages to support next. Kotlin brings
        modern language features like null-safety and coroutines to the JVM, making it a popular
        choice for new Android and server applications alike. Similar in spirit is Dart, which
        focuses on speed and cross-platform compilation; with frameworks like Flutter, Dart can be
        used to create native apps across mobile, web, and desktop.
      </P>
      <P>
        Kotlin and Dart use the same natural language commands as Serenade's other languages, so if
        you've used Serenade to write Python or TypeScript, you already know how to write Kotlin and
        Dart with voice:
      </P>
      <Video src="https://cdn.serenade.ai/web/video/dart-demo.mp4" />
      <P>
        Finally, based on feedback from our community, we're bringing a host of new editing commands
        to Serenade. Here are just a few new additions:
      </P>
      <UL>
        <li>
          The `duplicate` command lets you quickly duplicate any selector. Just say `duplicate
          method` or `duplicate next five lines` to efficiently copy your existing code.
        </li>
        <li>
          The `surround` command can easily surround any selector with text. For instance, you could
          say `surround line with quotes` or `surround block with div tag`.
        </li>
        <li>
          The `rename` command changes the name of any selector, via commands like `rename function
          get to post` or `rename class to manager`.
        </li>
      </UL>
      <P>
        You can read more about these commands at the [Serenade Changelog]
        <a href="https://serenade.ai/changelog">Serenade Changelog</a> and browse all of Serenade's
        functionality at the <a href="https://serenade.ai/docs">Serenade Docs</a>.
      </P>
      <P>
        We're continually adding more languages and platforms to make Serenade a powerful tool for
        every developer. Stay tuned for more language and feature announcements coming soon!
      </P>
    </>
  ),
};

const Component = () => <Page post={Post} />;

export default Component;
