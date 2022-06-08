import React from "react";
import { Page, PostData, P, UL } from "../../components/blog";

export const Post: PostData = {
  date: "June 2, 2020",
  slug: "a-new-speech-engine-june-2020",
  title: "A New Speech Engine",
  content: (
    <>
      <P>
        Today, we're excited to launch a new version of the Serenade speech engine after several
        months of development. Our speech engine is now faster and more powerful than ever before,
        which should boost the productivity of every developer using Serenade.
      </P>
      <P>
        The new speech engine uses a state-of-the-art acoustic model and language model with
        significantly better performance than our previous models. These new models are trained on a
        much larger set of data, which means the new engine should be more accurate across a wider
        variety of pronunciations. We've also introduced a dedicated model for noise detection,
        which enables Serenade to handle background noise more effectively than before.
      </P>
      <P>
        Perhaps the biggest change in our new speech engine is that it's able to use context from
        the file you're editing. For instance, let's say you're editing a function, and a variable
        called "docusaurus" is in scope. Without any context, Serenade would probably rank the word
        "docusaurus" fairly low, since it's not a common word. But, with the context from the code
        you're working with, Serenade can learn that "docusaurus" is actually much more likely and
        rank it at the top of the alternatives list. So, as you speak the names of variables,
        functions, classes, etc., and Serenade will know what you mean much more often.
      </P>
      <P>
        Let's talk results. In order to measure accuracy, we look at recall metrics, which measure
        how frequently the correct transcript was found in the top *n* results. For instance,
        recall@5 measures how frequently the correct transcript appeared in the first 5 results. Of
        particular importance is recall@1, which essentially measures how frequently the first
        result was correct, meaning no clarification commands were needed, and the developer could
        continue with their workflow.
      </P>
      <P>With our new engine, we're seeing significantly higher recall metrics across the board.</P>
      <UL>
        <li>recall@1: 35% reduction in error rate</li>
        <li>recall@5: 57% reduction in error rate</li>
        <li>recall@10: 60% reduction in error rate</li>
      </UL>
      <P>
        In addition to having significantly higher accuracy, our new speech engine is much faster.
        Previously, live transcript results would only appear every ~700ms due to limitations in our
        speech processing and streaming backend. Now, our speech engine is able to respond much more
        quickly, using smaller chunks of audio, so live results will appear much more frequently.
        You should also see a much shorter delay between when you finish speaking a voice command
        and when the result appears in your editor, which helps keep you in your development flow.
      </P>
      <UL>
        <li>speech decoding speed: ~50% faster</li>
        <li>end-to-end speed: ~60% faster</li>
      </UL>
      <P>
        We hope these changes will help you be more productive than ever when coding with voice. If
        you have any questions or feedback, don't hesitate to reach out to us in the{" "}
        <a href="https://serenade.ai/community">Serenade community</a>.
      </P>
    </>
  ),
};

const Component = () => <Page post={Post} />;

export default Component;
