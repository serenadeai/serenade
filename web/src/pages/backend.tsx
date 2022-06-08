import React from "react";
import { StaticImage } from "gatsby-plugin-image";
import { Link } from "gatsby";
import { FullWidthFeature, TwoColumnFeature } from "../components/features";
import { FeaturePageBottom } from "../components/footer";
import { Hero } from "../components/hero";
import { Main } from "../components/pages";

const BackendPage = () => (
  <Main>
    <Hero
      title="Power your infra with voice"
      text="Build your services and scripts with voice—from Python, Ruby, and JavaScript to Go, Rust, and C++."
      image={
        <div className="w-[500px] mx-auto">
          <StaticImage
            src="https://cdn.serenade.ai/web/img/backend.png"
            alt="Laptop with speech icon"
            placeholder="none"
            eager={true}
          />
        </div>
      }
    />
    <TwoColumnFeature
      dark={true}
      title="Reduce wrist strain while reducing server strain"
      subtitle="Serenade enables you to switch up your workflow and take a break from typing. With Serenade's natural speech-to-code engine, you can seamlessly reduce your keyboard usage without decreasing productivity."
      content={
        <video controls poster="https://cdn.serenade.ai/web/video/python-demo-poster.png">
          <source src="https://cdn.serenade.ai/web/video/python-demo.mp4" type="video/mp4" />
        </video>
      }
    />
    <FullWidthFeature
      title="Use natural speech across your backend"
      subtitle="Serenade supports over 15 different languages and a variety of editors, so you can use voice across even the most polyglot codebases."
      content={
        <div className="mx-auto w-max max-w-screen-lg px-4">
          <video controls poster="https://cdn.serenade.ai/web/video/backend-demo-poster.png">
            <source src="https://cdn.serenade.ai/web/video/backend-demo.mp4" type="video/mp4" />
          </video>
        </div>
      }
    />
    <TwoColumnFeature
      title="Powerful beyond code"
      subtitle="Send messages, update documentation, or navigate the web without a keyboard. Serenade provides you with a speech engine that matches what you're doing."
      gradient="bottom-right"
      content={
        <div className="md:w-[450px] mx-auto">
          <StaticImage
            src="https://cdn.serenade.ai/web/img/plugins.png"
            alt="Serenade logo"
            placeholder="none"
          />
        </div>
      }
    />
    <FeaturePageBottom />
  </Main>
);

export default BackendPage;
