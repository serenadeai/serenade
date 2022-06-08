import React, { useEffect, useState } from "react";
import yaml from "js-yaml";
import { Link } from "gatsby";
import { StaticImage } from "gatsby-plugin-image";
import { Hero } from "../components/hero";
import { Main } from "../components/pages";
import { FullWidthFeature, TwoColumnFeature } from "../components/features";

const currentVersion = "2.0.1";
const legacyVersion = "";
const betaVersion = "";

const DownloadPage = () => {
  const [platform, setPlatform] = useState("");
  const [macVersion, setMacVersion] = useState(currentVersion);
  const [windowsVersion, setWindowsVersion] = useState(currentVersion);
  const [linuxVersion, setLinuxVersion] = useState(currentVersion);

  useEffect(async () => {
    if (!currentVersion) {
      const [mac, windows, linux] = await Promise.all([
        fetch(
          `https://s3-us-west-2.amazonaws.com/serenadecdn.com/app/latest-mac.yml?${Date.now()}`
        ),
        fetch(`https://s3-us-west-2.amazonaws.com/serenadecdn.com/app/latest.yml?${Date.now()}`),
        fetch(
          `https://s3-us-west-2.amazonaws.com/serenadecdn.com/app/latest-linux.yml?${Date.now()}`
        ),
      ]);

      setMacVersion(yaml.load(await mac.text()).version);
      setWindowsVersion(yaml.load(await windows.text()).version);
      setLinuxVersion(yaml.load(await linux.text()).version);
    }

    const navigator = window.navigator.platform.toLowerCase();
    if (navigator.includes("mac")) {
      setPlatform("mac");
    } else if (navigator.includes("win")) {
      setPlatform("windows");
    } else if (
      navigator.includes("iphone") ||
      navigator.includes("ipad") ||
      navigator.includes("android")
    ) {
      setPlatform("unsupported");
    } else {
      setPlatform("linux");
    }
  }, []);

  const downloadLink = (version, platform) => {
    if (platform == "mac") {
      return `https://serenadecdn.com/app/Serenade-${version}.dmg`;
    } else if (platform == "windows") {
      return `https://serenadecdn.com/app/Serenade%20Setup%20${version}.exe`;
    } else if (platform == "linux") {
      return `https://serenadecdn.com/app/Serenade-${version}.AppImage`;
    }

    return "";
  };

  return (
    <Main>
      <Hero
        title={
          platform == "unsupported"
            ? "Serenade is only available on desktop"
            : "Install Serenade to start speaking code"
        }
        buttonLink={
          platform == "mac"
            ? downloadLink(macVersion, "mac")
            : platform == "windows"
            ? downloadLink(windowsVersion, "windows")
            : platform == "linux"
            ? downloadLink(linuxVersion, "linux")
            : "/docs"
        }
        buttonText={
          platform == "mac"
            ? "Download for Mac"
            : platform == "windows"
            ? "Download for Windows"
            : platform == "linux"
            ? "Download for Linux"
            : "Read the docs"
        }
        image={
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
        dark={true}
        title="Get started with voice coding"
        subtitle={
          <>
            <div className="mb-8">
              First time writing code with voice? We've got you covered. Check out the Serenade
              documentation to hit the ground running.
            </div>
            <Link to="/docs" className="primary-button">
              Read the docs
            </Link>
          </>
        }
        content={
          <div className="w-[450px] mx-auto">
            <StaticImage
              src="https://cdn.serenade.ai/web/img/email-messages.png"
              alt="Serenade logo"
              placeholder="none"
              eager={true}
            />
          </div>
        }
      />
      <FullWidthFeature
        title="All Serenade Versions"
        subtitle="Serenade works across Mac, Windows, and Linux."
        content={
          <div className="mx-auto max-w-screen-lg text-slate-600 w-full mb-12">
            <table className="w-full">
              <thead>
                <tr className="text-lg md:text-2xl border-gray-300 text-center border-b">
                  <th></th>
                  <th className="py-3 px-6">macOS (x64)</th>
                  <th className="py-3 px-6">Windows (x64)</th>
                  <th className="py-3 px-6">Linux (x64)</th>
                </tr>
              </thead>
              <tbody>
                <tr className="text-base md:text-lg border-gray-300 text-center border-b">
                  <td className="py-3 px-2 md:px-8 font-medium">Stable</td>
                  <td className="py-3 px-2 md:px-6">
                    <Link
                      to={downloadLink(macVersion, "mac")}
                      className="text-purple-600 hover:text-purple-900 transition-colors"
                    >
                      v{macVersion}
                    </Link>
                  </td>
                  <td className="py-3 px-2 md:px-6">
                    <Link
                      to={downloadLink(windowsVersion, "windows")}
                      className="text-purple-600 hover:text-purple-900 transition-colors"
                    >
                      v{windowsVersion}
                    </Link>
                  </td>
                  <td className="py-3 px-2 md:px-6">
                    <Link
                      to={downloadLink(linuxVersion, "linux")}
                      className="text-purple-600 hover:text-purple-900 transition-colors"
                    >
                      v{linuxVersion}
                    </Link>
                  </td>
                </tr>
                {betaVersion ? (
                  <tr className="text-base md:text-lg border-gray-300 text-center border-b">
                    <td className="py-3 px-2 md:px-8 font-medium">Beta</td>
                    <td className="py-3 px-2 md:px-6">
                      <Link
                        to={downloadLink(betaVersion, "mac")}
                        className="text-purple-600 hover:text-purple-900 transition-colors"
                      >
                        v{betaVersion}
                      </Link>
                    </td>
                    <td className="py-3 px-2 md:px-6">
                      <Link
                        to={downloadLink(betaVersion, "windows")}
                        className="text-purple-600 hover:text-purple-900 transition-colors"
                      >
                        v{betaVersion}
                      </Link>
                    </td>
                    <td className="py-3 px-2 md:px-6">
                      <Link
                        to={downloadLink(betaVersion, "linux")}
                        className="text-purple-600 hover:text-purple-900 transition-colors"
                      >
                        v{betaVersion}
                      </Link>
                    </td>
                  </tr>
                ) : null}
                {legacyVersion ? (
                  <tr className="text-base md:text-lg border-gray-300 text-center border-b">
                    <td className="py-3 px-2 md:px-8 font-medium">Legacy</td>
                    <td className="py-3 px-2 md:px-6">
                      <Link
                        to={downloadLink(legacyVersion, "mac")}
                        className="text-purple-600 hover:text-purple-900 transition-colors"
                      >
                        v{legacyVersion}
                      </Link>
                    </td>
                    <td className="py-3 px-2 md:px-6">
                      <Link
                        to={downloadLink(legacyVersion, "windows")}
                        className="text-purple-600 hover:text-purple-900 transition-colors"
                      >
                        v{legacyVersion}
                      </Link>
                    </td>
                    <td className="py-3 px-2 md:px-6">
                      <Link
                        to={downloadLink(legacyVersion, "linux")}
                        className="text-purple-600 hover:text-purple-900 transition-colors"
                      >
                        v{legacyVersion}
                      </Link>
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        }
      />
    </Main>
  );
};

export default DownloadPage;
