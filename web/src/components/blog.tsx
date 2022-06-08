import React from "react";
import { Link } from "gatsby";
import { Gray } from "./pages";

export type PostData = {
  content: string;
  date: string;
  slug: string;
  title: string;
};

export const H3: React.FC = ({ children }) => (
  <h3 className="font-bold text-lg py-2">{children}</h3>
);

export const Image: React.FC<{ alt: string; src: string }> = ({ alt, src }) => (
  <img alt={alt} src={src} className="max-w-screen-md mx-auto py-4" />
);

export const P: React.FC = ({ children }) => <p className="py-2">{children}</p>;

export const OL: React.FC = ({ children }) => (
  <ol className="py-2 list-decimal ml-8">{children}</ol>
);

export const UL: React.FC = ({ children }) => <ul className="py-2 list-disc ml-8">{children}</ul>;

export const Video: React.FC<{ src: string }> = ({ src }) => (
  <div className="max-w-screen-md mx-auto py-4">
    <video muted controls>
      <source src={src} type="video/mp4" />
    </video>
  </div>
);

export const Page: React.FC<{ post: PostData }> = ({ post }) => (
  <Gray>
    <Post post={post} />
  </Gray>
);

export const Post: React.FC<{ post: PostData }> = ({ post }) => (
  <div className="py-8 blog-post">
    <div className="mx-auto max-w-screen-lg rounded-lg border shadow bg-white text-slate-600 px-16 pb-12">
      <h2 className="mx-auto font-bold text-3xl pt-12 text-purple-500">
        <Link to={`/blog/${post.slug}`}>{post.title}</Link>
      </h2>
      <h4 className="mx-auto font-light text-xl pt-1">{post.date}</h4>
      <div className="pt-3">{post.content}</div>
    </div>
  </div>
);
