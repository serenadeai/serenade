import React from "react";
import { Page, PostData, P, OL, UL } from "../../components/blog";

export const Post: PostData = {
  date: "May 17, 2020",
  slug: "creating-serenade",
  title: "Creating Serenade",
  content: (
    <>
      <P>
        Last year, I developed a repetitive strain injury, commonly known as an RSI, in my wrists.
        With this condition, typing at a keyboard for even a few minutes caused immense hand
        pain—years of sitting at a computer for 8+ hours a day finally caught up to me. Suddenly, it
        seemed like I wouldn't be able to write code anymore. After all, the vast majority of code
        is written using a keyboard and mouse, tools that I could no longer use.
      </P>
      <P>
        I looked around to see what people with similar conditions were doing. For some, physical
        therapy (through stretching and exercises) caused the pain to subside. For others, switching
        to an ergonomic keyboard and mouse (like the Kinesis keyboard or Evoluent vertical mouse)
        enabled them to use a computer without discomfort. Neither worked for me.
      </P>
      <P>
        So, I turned to dictation software, since that didn't require my hands at all. With a few of
        these dictation apps, I was able to start writing code again (which felt amazing!) and I was
        really impressed with the state of speech technology. But, I was far from fully productive,
        as the learning curve was quite steep. Some apps required you to speak using the NATO
        alphabet, and others required you to define and memorize your own mapping of words to
        keystrokes (e.g., "pineapple" for the "enter" key, since you don't often say "pineapple"
        when programming). Even after that learning curve, needing to dictate every character that
        occurred in source code was much too slow—creating a function in Python my saying "def hello
        left parenthesis right parenthesis colon newline indent..." simply isn't efficient.
      </P>
      <P>
        With nothing allowing me to be sufficiently productive, I needed to leave my job. I knew
        there had to be a better way to write code without a keyboard. So, I started working on a
        prototype of a new voice coding app (with someone else typing for me to start) called
        Serenade, alongside my close friend Tommy, now my co-founder. We wanted to create a product
        that was really easy to use, to the point where you could just speak naturally, as you would
        in a conversation, and code would be written for you. As Serenade got better and better, I
        could slowly feel my productivity increasing.
      </P>
      <P>
        Fast-forward to today, and I'm fully productive again using Serenade. In fact, I'm using
        Serenade full-time to build itself. Serenade is unlike any other voice programming solution
        in a few ways:
      </P>
      <OL>
        <li>
          Serenade comes with its own speech-to-text engine, using a custom model specifically
          designed for code. Most other speech-to-text technologies are trained on typical
          conversaions between people, which isn't ideal for code. After all, how often do you say
          "attr" or "enum" in conversational speech? Instead, Serenade learns common programming
          constructs, variable names, and other words you'd say when programming, making it much
          more accurate for coding.
        </li>
        <li>
          Dictating code word-for-word (or even worse, letter-for-letter) is really slow. Instead of
          relying on just dictation, Serenade uses natural English input, so to create a function
          called hello, you can just say "create function hello", without needing to worry about any
          syntax or memorization. In the same way, you can naturally describe manipulations to
          existing code, like "delete class" or "add parameter url".
        </li>
        <li>
          If Serenade isn't confident in what you said, you'll see a list of alternatives you can
          choose from. With many speech apps that only use the first result, it can be frustrating
          to repeat yourself just to correct a single word. Instead, Serenade allows you to just
          select a different result, which can dramatically streamline your workflow.
        </li>
      </OL>
      <P>
        Coding by voice with Serenade can actually be faster than using a keyboard and mouse. (And,
        it's certainly more fun.)
      </P>
      <UL>
        <li>
          Is your cursor at the bottom of your screen, but you know you want to delete the function
          at the top of the file? Just say "delete first function".
        </li>
        <li>
          Are you in the middle of writing a function, and you realize that you forgot to pass in a
          variable called foo? Just say "add parameter foo".
        </li>
        <li>
          Do you have a dictionary that really should be defined as an enum instead? Just say
          "convert dictionary to enum".
        </li>
      </UL>
      <P>
        Many editors and IDEs have similar refactoring functionalities, but speaking is often more
        efficient than navigating menus upon menus or memorizing hundreds of keyboard shortcuts.
        And, the same Serenade commands work across any programming language, so whether you're
        writing TypeScript or Python, the same natural commands like "add enum colors" will work.
      </P>
      <P>
        You can talk faster than you can type. We're building a world where you'll be able to code
        faster than you ever could before.
      </P>
    </>
  ),
};

const Component = () => <Page post={Post} />;

export default Component;
