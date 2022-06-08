import React from "react";
import { Page, PostData, H3, P, UL } from "../../components/blog";

export const Post: PostData = {
  date: "June 3, 2021",
  slug: "expanding-the-serenade-ecosystem",
  title: "Expanding the Serenade Ecosystem",
  content: (
    <>
      <P>
        Software developers write more than just code–we write documentation and reviews, emails and
        Slack messages, tweets and blog posts. And, when we do write code, it isn’t always in an
        IDE–we could be honing our skills on LeetCode or iterating on a model in Jupyter. Serenade
        is a powerful way to write code with voice in an editor and terminal, and with the new
        features we’ve added in the latest release, it’s a great way to write everything else,
        everywhere else too.
      </P>
      <H3>Dictate mode</H3>
      <P>
        One of our top requested features has been a persistent dictation mode for writing long
        stretches of text without having to say “insert” or “dictate” before each phrase. We're
        excited to bring dictation mode to the latest release—to start using it, just say “start
        dictating”. Serenade will then automatically convert everything you say afterward into an
        “insert” command until you say “stop dictating”.
      </P>
      <P>
        Common commands like “go to” and “undo” will still work as usual in dictation mode, and the
        option to insert those words will appear as alternatives as well.
      </P>
      <H3>Improved styling</H3>
      <P>
        The latest update also introduces an improved text styling model to capture the differences
        in the ways we format text in prose vs. in code. These include things like capitalizing the
        word “I” and words at the beginning of sentences, as well as adding spaces after punctuation
        marks like periods and commas.
      </P>
      <P>
        Because we trained this new natural English model on thousands of public READMEs, comments,
        and more, we know how to style things like variable names in code blocks (for instance, to
        add `$HOME`, just say “dollar home”).
      </P>
      <P>
        Whether you’re editing a text or Markdown file in a supported editor or using an application
        without a Serenade plugin (like Slack or Discord), all of your insert commands will use this
        new model and be automatically formatted the way you expect.
      </P>
      <H3>Language switching</H3>
      <P>
        We’ve also added the ability to switch which model Serenade uses to edit your files. To
        switch language modes, just say “python mode” or use the new switcher by clicking the
        language icon in the Serenade app. You can return to automatically detecting the current
        language with the “auto mode” command.
      </P>
      <H3>Deeper accessibility integration</H3>
      <P>
        Finally, we’ve improved our integration with macOS and Windows accessibility APIs to better
        edit text in applications without an official Serenade plugin. We've also expanded our
        Chrome extension to better interact with in-browser code editors that you find on websites
        like LeetCode or repl.it, as well as in environments like Jupyter and Google Colaboratory.
        These updates can help you use Serenade everywhere you work.
      </P>
      <P>
        We’ll continue to iterate on all these new features in the future—refining our new models
        and making Serenade available on more native apps. Until then, be sure to join our active
        community and let us know what you think!
      </P>
    </>
  ),
};

const Component = () => <Page post={Post} />;

export default Component;
