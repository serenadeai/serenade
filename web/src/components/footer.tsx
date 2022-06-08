import React from "react";
import { Link } from "gatsby";
import { FullWidthFeature } from "./features";

const LargeLink: React.FC<{ href: string; text: string }> = ({ href, text }) => (
  <Link
    to={href}
    className="block font-bold text-white text-2xl pb-3 hover:text-purple-300 transition-colors"
  >
    {text}
  </Link>
);

const SmallLink: React.FC<{ href: string; text: string }> = ({ href, text }) => (
  <Link
    to={href}
    className="block font-light text-white hover:text-purple-300 transition-colors pb-2"
  >
    {text}
  </Link>
);

export const FeaturePageBottom: React.FC = () => (
  <FullWidthFeature
    gray={true}
    title="Start coding with voice today"
    subtitle="Ready to supercharge your workflow with voice? Download Serenade for free and start using speech alongside typing, or leave your keyboard behind."
    content={
      <div className="mx-auto text-center text-xl mt-4">
        <span className="pr-6">
          <Link to="/download" className="primary-button">
            Download
          </Link>
        </span>
        <Link to="/docs" className="secondary-button">
          Docs
        </Link>
      </div>
    }
  />
);

export const Footer: React.FC = () => (
  <div className="w-screen bg-slate-600 max-w-full">
    <div className="mx-auto max-w-md py-10 flex w-max">
      <div className="px-8">
        <LargeLink href="/download" text="Download" />
        <LargeLink href="/docs" text="Docs" />
        <LargeLink href="/community" text="Community" />
        <LargeLink href="/blog" text="Blog" />
      </div>
      <div className="px-8">
        <SmallLink href="/terms" text="Terms of Service" />
        <SmallLink href="/privacy" text="Privacy Policy" />
        <SmallLink href="/code-privacy" text="Code Privacy" />
        <a
          href="mailto:contact@serenade.ai"
          className="block text-white font-light hover:text-purple-300 transition-colors"
        >
          Contact
        </a>
      </div>
    </div>
  </div>
);
