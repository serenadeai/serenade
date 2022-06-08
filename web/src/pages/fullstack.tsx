import React from "react";
import { StaticImage } from "gatsby-plugin-image";
import { Link } from "gatsby";
import { CustomCommands } from "../components/custom-commands";
import { FullWidthFeature, TwoColumnFeature } from "../components/features";
import { FeaturePageBottom } from "../components/footer";
import { Hero } from "../components/hero";
import { Main } from "../components/pages";

const FullstackPage = () => (
  <Main>
    <Hero
      title="Pixel perfect, without the keyboard"
      text="Whether you're writing a frontend with HTML, CSS, and JS or a backend in Python or Ruby, give your hands a break with voice."
      image={
        <div className="w-[500px] mx-auto">
          <StaticImage
            src="https://cdn.serenade.ai/web/img/getting-started.png"
            alt="Laptop with speech icon"
            placeholder="none"
            eager={true}
          />
        </div>
      }
    />
    <TwoColumnFeature
      dark={true}
      title="Build your frontend painlessly"
      subtitle="Write components using React, Vue, Svelte, and more, all without worrying about memorizing hotkeys or hand pain."
      content={
        <div className="mx-auto w-max max-w-screen-lg">
          <video controls>
            <source src="https://cdn.serenade.ai/web/video/refactor.mp4" type="video/mp4" />
          </video>
        </div>
      }
    />
    <FullWidthFeature
      title="Works across your stack"
      subtitle="As a full stack developer, your work spans a variety of languages and platforms—so does Serenade. With over 15 fully-supported languages across the stack, you can use voice across your backend, frontend, and everything in between."
      content={
        <div className="mx-auto w-max max-w-screen-lg px-4">
          <video controls poster="https://cdn.serenade.ai/web/video/fullstack-demo-poster.png">
            <source src="https://cdn.serenade.ai/web/video/fullstack-demo.mp4" type="video/mp4" />
          </video>
        </div>
      }
    />
    <TwoColumnFeature
      title="Powerful beyond code"
      subtitle="Serenade provides you with the right speech engine to match what you’re doing. Send messages, update documentation, or navigate the web without needing to use your keyboard."
      gradient="top-right"
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
    <FullWidthFeature
      title="Less typing, more building"
      subtitle="Tired of typing that same styling for a container? How about that button you use everywhere? Create your own powerful custom commands to automate parts of your workflow."
      gradient="bottom-left"
      content={
        <div className="mx-auto pt-4">
          <CustomCommands
            snippets={[
              {
                id: 0,
                title: "Run tests",
                content: `command("run tests", async api => {
  await api.focus("terminal");
  await api.typeText("npm test");
  await api.pressKey("return");
});`,
              },
              {
                id: 1,
                title: "HTML snippets",
                content: `language("html").snippet(
  "new page <%title%>",
  \`<!doctype html>
  <html>
    <head><title><%title%></title</head>
    <body><div id="root"></div></body>
  </html>\`
});`,
              },
              {
                id: 2,
                title: "React snippets",
                content: `language("javascript").snippet(
  "add state <%name%>",
  "const [<%name%>, set<%name%>] = useState(null);"
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

export default FullstackPage;
