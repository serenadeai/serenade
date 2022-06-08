import React from "react";
import { Post } from "../components/blog";
import { Gray } from "../components/pages";
import * as OpenSource from "./blog/open-sourcing-serenade";
import * as Languages from "./blog/new-languages-rust-go-ruby";
import * as Expanding from "./blog/expanding-the-serenade-ecosystem";
import * as Terminal from "./blog/bringing-serenade-to-the-terminal";
import * as Protocol from "./blog/the-serenade-protocol";
import * as MiniMode from "./blog/mini-mode";
import * as Jetbrains from "./blog/jetbrains-kotlin-dart";
import * as Chrome from "./blog/serenade-for-chrome";
import * as NewSpeechEngine from "./blog/a-new-speech-engine-june-2020";
import * as Creating from "./blog/creating-serenade";

const BlogPage = () => (
  <Gray>
    <div className="bg-gray-100">
      <div className="max-w-screen-lg mx-auto pt-6">
        <h1 className="font-bold text-4xl">Serenade Blog</h1>
      </div>
      <Post post={OpenSource.Post} />
      <Post post={Languages.Post} />
      <Post post={Expanding.Post} />
      <Post post={Terminal.Post} />
      <Post post={Protocol.Post} />
      <Post post={MiniMode.Post} />
      <Post post={Jetbrains.Post} />
      <Post post={Chrome.Post} />
      <Post post={NewSpeechEngine.Post} />
      <Post post={Creating.Post} />
    </div>
  </Gray>
);

export default BlogPage;
