import React from "react";
import { StaticImage } from "gatsby-plugin-image";
import { Link } from "gatsby";
import { Battery } from "../components/battery";
import { FullWidthFeature, TwoColumnFeature } from "../components/features";
import { FeaturePageBottom } from "../components/footer";
import { Hero } from "../components/hero";
import { Main } from "../components/pages";

const HealthPage = () => (
  <Main>
    <Hero
      title="Get your career back"
      text="An injury to your hands, wrists, or back doesn't need to derail your career as a developer. With Serenade, you can be just as productive without using a mouse and keyboard at all."
      image={
        <div className="w-[400px] mx-auto">
          <StaticImage
            src="https://cdn.serenade.ai/web/img/desk-voice.png"
            alt="Laptop with speech icon"
            placeholder="none"
            eager={true}
          />
        </div>
      }
    />
    <TwoColumnFeature
      dark={true}
      title="Powerful out of the box"
      subtitle="Serenade comes with all of the voice commands you need, so you don't have to worry about additional configuration or hardware. Get started today–no additional software, eye tracker, or microphone required."
      content={<Battery />}
    />
    <FullWidthFeature
      title="Keep doing what you love"
      subtitle="Give your wrists a break without slowing down. Whether you have an injury or are looking to prevent one, Serenade lets you continue doing what you love."
      gradient="bottom-right"
      content={
        <div className="mx-auto w-max max-w-screen-lg px-4">
          <video controls poster="https://cdn.serenade.ai/web/video/workflow-demo-poster.png">
            <source src="https://cdn.serenade.ai/web/video/workflow-demo.mp4" type="video/mp4" />
          </video>
        </div>
      }
    />
    <TwoColumnFeature
      title="Designed for code"
      subtitle="Other Speech APIs are made for conversational speech, so they're not accurate on code. Serenade's speech engine is designed for programming from the ground up, giving you the accuracy you need to get real work done."
      content={
        <div className="w-[200px] md:w-[250px] mx-auto">
          <StaticImage
            src="https://cdn.serenade.ai/web/img/chart.png"
            alt="Comparison chart"
            placeholder="none"
          />
        </div>
      }
    />
    <FeaturePageBottom />
  </Main>
);

export default HealthPage;
