import React from "react";
import { Page, PostData, P, UL } from "../../components/blog";

export const Post: PostData = {
  date: "April 6, 2021",
  slug: "bringing-serenade-to-the-terminal",
  title: "Bringing Serenade to the Terminal",
  content: (
    <>
      <P>
        With plugins for editors like VS Code and browsers like Chrome, you can use Serenade to
        write code, read documentation, and create web pages entirely with voice. Now, we're
        bringing Serenade to one of the most important developer tools—the terminal. Today, we're
        releasing Serenade plugins for two popular terminal applications: iTerm2 and Hyper. Head to
        the <a href="https://serenade.ai/plugins">plugins page</a> to give them a try!
      </P>
      <P>
        <a href="https://iterm2.com">iTerm2</a> is one of the most-used terminal applications on
        macOS. With powerful features like multiple panes and search, along with lightning-fast
        performance, it's a huge upgrade over the terminal app that comes bundled with macOS.
        Meanwhile, <a href="https://hyper.is">Hyper</a> is a modern terminal application built on
        web technologies. Hyper works seamlessly across Windows, Linux, and macOS, while offering
        customizability based on open web standards.
      </P>
      <P>Let’s take a quick look at how Serenade’s terminal plugins work.</P>
      <P>
        The most common voice command you’ll use in the terminal is the "run" command. As you’d
        guess, you can execute commands in your terminal simply by saying "run cd" or "run apt
        install python". We’ve trained machine learning models just for Bash, so when you say "run
        ls dash la", Serenade knows you mean "ls -la", without you needing to specify spacing
        manually. And don’t worry—Serenade will never execute a destructive command in your terminal
        without you confirming first.
      </P>
      <P>
        All of Serenade’s editing commands work in the terminal as well. If your active terminal
        line already contains part of a command, you can simply say commands like "insert dash v" or
        "insert star" to append text. Or, you can quickly make edits by saying commands like "change
        find to grep" or "delete to end of line".
      </P>
      <P>
        Serenade can also control terminal application features. For instance, you can use commands
        like "new tab" and "previous tab" to manage tabs, and "undo" to roll back your last edit to
        a command. And, for full control, you can always specify keystrokes to be sent to the
        terminal with commands like "press command k".
      </P>
      <P>
        Like all of Serenade’s plugins, we’ve open-sourced Serenade for iTerm2 and Serenade for
        Hyper on our <a href="https://github.com/serenadeai">Github page</a>. Both of these plugins
        are also built on top of the open-source Serenade Protocol, which defines how any
        application can integrate with Serenade’s powerful voice commands.
      </P>
      <P>
        We’re excited to see what you build with Serenade's new terminal integrations! Today’s
        release is just the start for terminal support—we’re continuing to iterate on terminal
        integrations and bring more functionality to Serenade across all applications.
      </P>
    </>
  ),
};

const Component = () => <Page post={Post} />;

export default Component;
