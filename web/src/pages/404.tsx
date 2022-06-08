import * as React from "react";
import { StaticImage } from "gatsby-plugin-image";
import { Hero } from "../components/hero";
import { MainNavigation } from "../components/navigation";
import { Footer } from "../components/footer";

const NotFoundPage = () => {
  return (
    <main>
      <MainNavigation />
      <Hero
        title="Page not found"
        text="Sorry! We couldn't find what you're looking for."
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
      <Footer />
    </main>
  );
};

export default NotFoundPage;
