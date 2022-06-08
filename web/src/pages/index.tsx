import * as React from "react";
import { Link } from "gatsby";
import { StaticImage } from "gatsby-plugin-image";
import { FullWidthFeature, TwoColumnFeature } from "../components/features";
import { FeaturePageBottom } from "../components/footer";
import { CustomCommands } from "../components/custom-commands";
import { HomepageHero } from "../components/hero";
import { Main } from "../components/pages";
import { PluginGrid } from "../components/plugin-grid";

const IndexPage = () => (
  <Main>
    <HomepageHero />
    <TwoColumnFeature
      title="The open-source voice assistant for developers"
      subtitle="With Serenade, you can write code using natural speech. Serenade's speech-to-code engine is designed for developers from the ground up and fully open-source."
      dark={true}
      content={
        <div className="-mx-8">
          <video controls poster="https://cdn.serenade.ai/web/video/backend-demo-poster.png">
            <source src="https://cdn.serenade.ai/web/video/backend-demo.mp4" type="video/mp4" />
          </video>
        </div>
      }
    />
    <TwoColumnFeature
      title="Take a break from typing"
      subtitle="Give your hands a break without missing a beat. Whether you have an injury or you're looking to prevent one, Serenade can help you be just as productive without typing at all."
      gradient="bottom-right"
      content={
        <div className="w-[550px] mx-auto">
          <StaticImage
            src="https://cdn.serenade.ai/web/img/laptop-hero.png"
            alt="Laptop with speech icon"
            placeholder="none"
          />
        </div>
      }
    />
    <TwoColumnFeature
      title="Secure, fast speech-to-code"
      subtitle="Serenade can run in the cloud, to minimize impact on your system's resources, or completely locally, so all of your voice commands and source code stay on-device. It's up to you, and everything is open-source."
      dark={true}
      content={
        <div className="md:w-[400px] mx-auto">
          <StaticImage
            src="https://cdn.serenade.ai/web/img/enterprise-shield.png"
            alt="Serenade Pro logo"
            placeholder="none"
          />
        </div>
      }
    />
    <FullWidthFeature
      title="Add voice to any application"
      subtitle="Serenade integrates with your existing tools—from writing code with VS Code to messaging with Slack—so you don't have to learn an entirely new workflow. And, Serenade provides you with the right speech engine to match what you're editing, whether that's code or prose."
      content={
        <div className="pt-4">
          <PluginGrid />
        </div>
      }
    />
    <TwoColumnFeature
      title="Code more flexibly"
      subtitle="Don't get stuck at your keyboard all day. Break up your workflow by using natural voice commands without worrying about syntax, formatting, and symbols."
      gradient="top-right"
      content={
        <div className="-mx-8">
          <video controls poster="https://cdn.serenade.ai/web/video/python-demo-poster.png">
            <source src="https://cdn.serenade.ai/web/video/python-demo.mp4" type="video/mp4" />
          </video>
        </div>
      }
    />
    <FullWidthFeature
      title="Customize your workflow"
      subtitle="Create powerful custom voice commands and plugins using Serenade's open protocol, and add them to your workflow. Or, try customizations shared by the Serenade community."
      gradient="bottom-left"
      content={
        <div className="mx-auto">
          <CustomCommands
            snippets={[
              {
                id: 0,
                title: "Run a build",
                content: `command("build", async api => {
  await api.focus("terminal");
  await api.pressKey("k", ["command"]);
  await api.typeText("yarn build");
  await api.pressKey("return");
});`,
              },
              {
                id: 1,
                title: "Clone a repo",
                content: `command("clone <%repo%>", async (api, matches) => {
  await api.focus("terminal");
  await api.typeText(
    "git clone https://github.com/" +
    matches.repo
  );
  await api.pressKey("return");
});`,
              },
              {
                id: 2,
                title: "Add a test",
                content: `language("python").snippet(
  "test method <%name%>",
  "def test_<%name%>(self):<%newline%><%indent%>pass",
  { "name": ["identifier", "underscores"] },
  "method"
);`,
              },
            ]}
          />
        </div>
      }
    />
    <FeaturePageBottom />
  </Main>
);

export default IndexPage;
