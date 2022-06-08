import React from "react";
import { Link } from "react-router-dom";
import wordmark from "../../../../static/img/wordmark.svg";
import onboardingWelcome from "../../../../static/img/onboarding-welcome.png";

export const WelcomePage = () => (
  <div className="h-screen w-full bg-slate-600 dark:bg-indigo-800">
    <div
      className="h-full w-full bg-white dark:bg-neutral-800"
      style={{ borderBottomRightRadius: "100% 100%" }}
    >
      <div className="w-full h-full flex items-center">
        <div className="w-7/12 mx-auto pl-6 pr-4">
          <img
            className="w-36 block dark:bg-white dark:p-2 dark:rounded"
            src={wordmark}
            alt="Serenade"
          />
          <h2 className="text-xl font-light pt-4">Welcome to Serenade!</h2>
          <p className="pt-2">
            Let's start writing code with voice! We'll walk through setting Serenade up with your
            favorite tools.
          </p>
          <div className="mx-auto pt-4">
            <Link to="/plugins" className="secondary-button inline-block">
              Get Started
            </Link>
          </div>
        </div>
        <div className="w-5/12">
          <img className="w-full" src={onboardingWelcome} alt="Welcome to Serenade" />
        </div>
      </div>
    </div>
  </div>
);
