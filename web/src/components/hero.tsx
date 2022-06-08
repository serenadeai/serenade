import React, { useEffect, useState } from "react";
import TextLoop from "react-text-loop";
import { Link } from "gatsby";
import { StaticImage } from "gatsby-plugin-image";

export const Hero: React.FC<{
  buttonLink?: string;
  buttonText?: string;
  hideButton: boolean;
  image: any;
  text: string;
  title: string;
}> = ({ buttonLink, buttonText, children, hideButton, image, text, title }) => (
  <div className="w-screen bg-white md:bg-slate-600 pb-24">
    <div className="w-screen bg-white" style={{ borderBottomRightRadius: "100% 100%" }}>
      <div className="flex max-w-full xl:max-w-screen-xl mx-auto pt-8 text-left">
        <div className="my-auto pl-8 pr-12 hero-text">
          <h1 className="text-slate-600 text-6xl font-extrabold">{title}</h1>
          <p className="text-slate-600 text-2xl font-light mt-4">{text}</p>
          {!hideButton ? (
            <div className="mt-10 text-2xl font-medium">
              <div className="hidden md:block">
                <Link to={buttonLink || "/download"} className="primary-button">
                  {buttonText || "Get Started"}
                </Link>
              </div>
              <div className="md:hidden">
                <Link to={buttonLink || "/docs"} className="primary-button">
                  {buttonText || "Get Started"}
                </Link>
              </div>
            </div>
          ) : null}
          {children}
        </div>
        <div className="my-auto ml-auto hidden md:block hero-image" style={{ flexBasis: "40%" }}>
          {image}
        </div>
      </div>
    </div>
  </div>
);

export const HomepageHero = () => (
  <div className="w-screen bg-slate-600 pb-16">
    <div className="w-screen bg-white" style={{ borderBottomRightRadius: "100% 100%" }}>
      <div className="flex items-center max-w-full xl:max-w-screen-xl mx-auto">
        <div className="pl-8 md:min-w-[475px]">
          <h1 className="text-slate-600 text-6xl font-extrabold">
            <div className="hidden md:block font-light">
              <TextLoop
                interval={1500}
                children={[
                  "Write code with",
                  "Create PRs with",
                  "Run tests with",
                  "Fix bugs with",
                  "Write docs with",
                  "Refactor with",
                ]}
              />
            </div>
            <div className="md:hidden font-light pt-8">Code with</div>
            {/* there's a safari rendering bug when you have a div inside this div */}
            <div
              className="hidden md:block text-7xl"
              style={{
                backgroundColor: "#f3ec78",
                backgroundImage:
                  "linear-gradient(45deg, #EDC04C, #F78291, #A56DFE, #89C2FC, #C3D1E8)",
                backgroundSize: "100%",
                WebkitBackgroundClip: "text",
                MozBackgroundClip: "text",
                WebkitTextFillColor: "transparent",
                MozTextFillColor: "transparent",
              }}
            >
              natural speech
            </div>
            <div
              className="md:hidden text-6xl pt-1"
              style={{
                backgroundColor: "#f3ec78",
                backgroundImage:
                  "linear-gradient(45deg, #EDC04C, #F78291, #A56DFE, #89C2FC, #C3D1E8)",
                backgroundSize: "100%",
                WebkitBackgroundClip: "text",
                MozBackgroundClip: "text",
                WebkitTextFillColor: "transparent",
                MozTextFillColor: "transparent",
              }}
            >
              natural
              <br />
              speech
            </div>
          </h1>
          <div className="mt-10 text-2xl font-medium">
            <div className="hidden md:block">
              <Link to="/download" className="primary-button">
                Get Started
              </Link>
            </div>
            <div className="md:hidden mt-10">
              <div className="w-screen -mx-8">
                <video controls poster="https://cdn.serenade.ai/web/video/workflow-demo-poster.png">
                  <source
                    src="https://cdn.serenade.ai/web/video/workflow-demo.mp4"
                    type="video/mp4"
                  />
                </video>
              </div>
            </div>
          </div>
        </div>
        <div className="my-auto ml-auto max-w-2xl py-32 hidden md:block">
          <div className="mx-auto">
            <video controls poster="https://cdn.serenade.ai/web/video/workflow-demo-poster.png">
              <source src="https://cdn.serenade.ai/web/video/workflow-demo.mp4" type="video/mp4" />
            </video>
          </div>
        </div>
      </div>
    </div>
  </div>
);
