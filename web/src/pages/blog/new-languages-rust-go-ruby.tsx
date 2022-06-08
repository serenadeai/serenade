import React from "react";
import { Link } from "gatsby";
import { Page, PostData, P, UL } from "../../components/blog";

export const Post: PostData = {
  date: "February 25, 2022",
  slug: "new-languages-rust-go-ruby",
  title: "The Next Generation of Language Support",
  content: (
    <>
      <P>
        Today, we're excited to continue expanding the Serenade ecosystem with first-class support
        for even more languages: C#, Rust, Go, and Ruby. That brings the total to{" "}
        <Link to="/plugins">15 languages</Link> across your favorite developer tools.
      </P>
      <P>
        To use any of these new languages, just open up a file in your editor of choice, and start
        using the Serenade commands you already know. For instance, you can create a C# class with
        "add class", delete a Go function with "delete function", or change a Ruby parameter with
        "change parameter". We've updated our <Link to="/docs">documentation</Link> to include
        examples from all our new languages so you can get up and running quickly.
      </P>
      <P>
        We've also brought some exciting new changes to the Serenade UI. We heard consistently from
        our community that more options to use Serenade in a minimized state were a must, so we've
        streamlined the UI to make sure you can foucs on what's most important—your code. And, we've
        made our voice activity detection more accurate and customizable, so you can tune Serenade
        to exactly your speaking pace and microphone sensitivity.
      </P>
      <P>
        Finally, we've enhanced our speech-to-code engine to understand more natural vocalizations
        of Serenade commands. Rather than needing to say precise commands like "add function hello",
        you can now describe operations in a wider variety of ways, like "create a function called
        hello" or "delete the next two parameters". Of course, you can continue using the commands
        you've already learned and the custom commands you've already created without any changes.
        For more, check out our <Link to="/docs">documentation</Link>.
      </P>
      <P>
        Writing C#, Rust, Go, and Ruby code with voice is now easier than ever. We're excited to see
        what you create! Check out the <Link to="/community">Serenade community</Link> to share what
        you've built and connect with other voice coders.
      </P>
    </>
  ),
};

const Component = () => <Page post={Post} />;

export default Component;
