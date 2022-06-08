import React from "react";
import classNames from "classnames";
import { Route, Switch, withRouter } from "react-router-dom";
import { connect } from "react-redux";
import { AlternativesPage } from "./pages/alternatives";
import { LoadingPage } from "./pages/loading";
import { LanguagesPage } from "./pages/languages";
import { MiniModePage } from "./pages/mini-mode";
import { PermissionsPage } from "./pages/onboarding/permissions";
import { PluginsPage } from "./pages/onboarding/plugins";
import { PrivacyPage } from "./pages/onboarding/privacy";
import { RevisionBoxPage } from "./pages/revision-box";
import { SettingsPage } from "./pages/settings";
import { TitleBar } from "./pages/title-bar";
import { TutorialsPage } from "./pages/onboarding/tutorials";
import { TextInputPage } from "./pages/text-input";
import { WelcomePage } from "./pages/onboarding/welcome";
import "./css/main.css";

const AppComponent: React.FC<{
  darkTheme: boolean;
  location: {
    pathname: string;
  };
  miniMode: boolean;
  nuxCompleted: boolean;
}> = ({ darkTheme, location, miniMode, nuxCompleted }) => {
  // set this manually to always render a page for development
  let page = null;
  const miniModeWindow = location.pathname.endsWith("minimode");
  return (
    <div
      className={classNames("app", process.platform, {
        dark: darkTheme,
        "mini-mode-window": miniModeWindow,
        transparent: miniModeWindow,
      })}
    >
      <TitleBar />
      {page ? (
        page
      ) : (
        <Switch>
          <Route exact path="/" render={() => <LoadingPage />} />
          <Route exact path="/alternatives" render={() => <AlternativesPage />} />
          <Route exact path="/input" render={() => <TextInputPage />} />
          <Route exact path="/languages" render={() => <LanguagesPage />} />
          <Route exact path="/minimode" render={() => <MiniModePage />} />
          <Route exact path="/permissions" render={() => <PermissionsPage />} />
          <Route exact path="/plugins" render={() => <PluginsPage />} />
          <Route exact path="/privacy" render={() => <PrivacyPage />} />
          <Route exact path="/revision" render={() => <RevisionBoxPage />} />
          <Route exact path="/settings" render={() => <SettingsPage />} />
          <Route exact path="/tutorials" render={() => <TutorialsPage />} />
          <Route exact path="/welcome" render={() => <WelcomePage />} />
        </Switch>
      )}
    </div>
  );
};

const mapState = (state: any) => ({
  darkTheme: state.darkTheme,
  miniMode: state.miniMode,
  nuxCompleted: state.nuxCompleted,
});

// @ts-ignore
export const App = withRouter(connect(mapState)(AppComponent));
