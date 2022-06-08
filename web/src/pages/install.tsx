import React from "react";
import { P, OL, LI, Group } from "../components/block";
import { Gray } from "../components/pages";

const InstallingPage = () => (
  <Gray>
    <div className="max-w-screen-md mx-auto pt-6">
      <h1 className="font-bold text-4xl">Installing Serenade</h1>
    </div>
    <div className="max-w-screen-md mx-auto">
      <Group
        title="Serenade"
        description="Installing the main Serenade client application"
        id="app"
      >
        <h4 className="text-lg font-medium -mt-6">macOS</h4>
        <OL>
          <LI>
            Drag Serenade.app into your Applications folder.
            <img
              src="https://cdn.serenade.ai/web/img/macos-install.png"
              width={400}
              alt="macOS Finder window with Serenade"
            />
          </LI>
          <LI>Open your Applications folder, and click Serenade to run it.</LI>
          <LI>Follow the prompts in the app to continue setup! </LI>
        </OL>
        <h4 className="text-lg font-medium pt-2">Windows</h4>
        <OL>
          <LI>
            Run the installer application, which will install and run the Serenade app.
            <img
              src="https://cdn.serenade.ai/web/img/windows-install.png"
              width={600}
              alt="Windows Explorer window with Serenade"
            />
          </LI>
          <LI>Follow the prompts in the app to continue setup!</LI>
        </OL>
        <h4 className="text-lg font-medium pt-2">Linux</h4>
        <OL>
          <LI>
            Set the permissions of Serenade-*.AppImage to be executable with:
            <div className="py-2 px-4 my-4 bg-gray-700 text-white rounded">
              <code>chmod +x Serenade-*.AppImage</code>
            </div>
            Or, use a file manager UI to make Serenade executable.
            <img
              src="https://cdn.serenade.ai/web/img/linux-install.png"
              width={400}
              alt="Ubuntu Linux Serenade file properties"
              style={{ marginTop: "0.5rem" }}
            />
          </LI>
          <LI>Run the Serenade-*.AppImage file, either via the terminal or a file manager UI.</LI>
          <LI>Follow the prompts in the app to continue setup!</LI>
        </OL>
      </Group>
      <Group title="Serenade Pro" description="Getting set up with Serenade Pro" id="pro">
        <h4 className="text-lg font-medium -mt-6">Windows Only: Install WSL</h4>
        <P>
          Serenade Pro requires WSL to run on Windows. On macOS and Linux, no other dependencies are
          required.
        </P>
        <P>
          If you already have WSL installed, then you're all set! If not, you can follow these steps
          to install.
        </P>
        <OL>
          <LI>
            Open up PowerShell as an administrator and run:
            <div className="py-2 px-4 my-2 mt-4 bg-gray-700 text-white rounded">
              <code>
                dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all
                /norestart
              </code>
            </div>
          </LI>
          <LI>Restart your computer.</LI>
          <LI>
            Install the Ubuntu app{" "}
            <a
              href="https://www.microsoft.com/en-us/p/ubuntu/9nblggh4msv6?activetab=pivot:overviewtab"
              target="_blank"
            >
              here
            </a>
            . You can also search Ubuntu in the Microsoft Store.
          </LI>
          <LI>Open the newly installed Ubuntu app to complete the installation process.</LI>
        </OL>
        <h4 className="text-lg font-medium pt-2">Install Serenade Pro</h4>
        <P>
          Once Pro has been enabled on your account, Serenade Pro will download (and auto-update)
          when you start Serenade.
        </P>
        <P>
          To switch to Pro, open Serenade, and then open the Settings window via the gear icon at
          top-right. Then, head to Server and select Serenade Pro.
        </P>
        <P>
          <img
            src="https://cdn.serenade.ai/web/img/server-settings-window.png"
            alt="Serenade Server Settings"
            style={{ width: "400px" }}
          />
        </P>
      </Group>
      <Group
        title="JetBrains Plugin"
        description="Integrating Serenade with JetBrains IDEs"
        id="jetbrains"
      >
        <ol className="list-decimal ml-8 -mt-8">
          <LI>Open the JetBrains preferences window.</LI>
          <LI>Navigate to Plugins on the left sidebar.</LI>
          <LI>
            Search for Serenade in the search bar, then click Install.
            <img
              src="https://cdn.serenade.ai/web/img/jetbrains-plugins-window.png"
              alt="JetBrains Plugins Window"
              style={{ maxWidth: "650px" }}
            />
          </LI>
        </ol>
      </Group>
    </div>
  </Gray>
);

export default InstallingPage;
