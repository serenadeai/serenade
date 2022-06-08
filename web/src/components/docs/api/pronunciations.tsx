import React from "react";
import { Snippet } from "../../snippet";

export const Content = () => (
  <>
    <p>
      You can also create your own custom pronunciations. For instance, if Serenade consistently
      hears <code>hat</code> when you say <code>cat</code>, then you can simply remap{" "}
      <code>hat</code> to <code>cat</code>. That way, the word you intended to say is what's used in
      each command Serenade hears.
    </p>
    <p>
      To define new pronunciations, you can use the <code>.pronounce</code> method. For instance, to
      remap the word <code>prize</code> to <code>price</code>, you can add the below to your{" "}
      <code>custom.js</code> file:
    </p>
    <Snippet code={`serenade.global().pronounce("prize", "price")`} />
    <p>
      Just as with all custom commands, you can also use filters like <code>.language</code> and{" "}
      <code>.extensions</code>.
    </p>
  </>
);
