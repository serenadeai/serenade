import React, { useEffect } from "react";
import { StaticImage } from "gatsby-plugin-image";
import { Hero } from "../components/hero";
import { Main } from "../components/pages";

const CommunityPage = () => {
  useEffect(() => {
    setTimeout(() => {
      window.location.href = "https://discord.gg/nz3Q2A4umK";
    }, 100);
  }, []);

  return (
    <Main>
      <Hero
        title="Redirecting you to the Serenade Community"
        text="Join the Serenade Discord to get help, share feedback, and meet other voice coders"
        hideButton={true}
        image={
          <div className="w-[550px] mx-auto">
            <StaticImage
              src="https://cdn.serenade.ai/web/img/desk-voice.png"
              alt="Laptop with speech icon"
              placeholder="none"
            />
          </div>
        }
      />
    </Main>
  );
};

export default CommunityPage;
