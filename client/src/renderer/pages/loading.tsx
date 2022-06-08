import React from "react";
import classNames from "classnames";
import { Redirect } from "react-router-dom";
import { connect } from "react-redux";
import { LoadingBar } from "../components/loading-bar";
import { Spinner } from "../components/spinner";

const LoadingPageComponent: React.FC<{
  loadingPageMessage: string;
  loadingPageProgress: number;
  loggedIn: boolean | undefined;
  miniMode: boolean;
}> = ({ loadingPageMessage, loadingPageProgress, loggedIn, miniMode }) => {
  if (loggedIn === true) {
    return <Redirect to="/alternatives" />;
  } else if (loggedIn === false) {
    return <Redirect to="/welcome" />;
  }

  return (
    <div className="flex flex-col w-screen h-screen overflow-hidden items-center justify-center text-center text-sm">
      {loadingPageMessage ? (
        <div dangerouslySetInnerHTML={{ __html: loadingPageMessage }} className="mb-0.5" />
      ) : null}
      {loadingPageProgress ? (
        <LoadingBar progress={loadingPageProgress} />
      ) : (
        <div>
          <Spinner hidden={false} />
          {loadingPageMessage ? null : <div>Loading...</div>}
        </div>
      )}
    </div>
  );
};

export const LoadingPage = connect((state: any) => ({
  loadingPageMessage: state.loadingPageMessage,
  loadingPageProgress: state.loadingPageProgress,
  loggedIn: state.loggedIn,
  miniMode: state.miniMode,
}))(LoadingPageComponent);
