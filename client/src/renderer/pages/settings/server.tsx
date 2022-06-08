import React from "react";
import { connect } from "react-redux";
import { ipcRenderer } from "electron";
import { Row } from "../settings";
import { LoadingBar } from "../../components/loading-bar";
import { Select } from "../../components/select";
import { Spinner } from "../../components/spinner";
import { Toggle } from "../../components/toggle";
import { Endpoint as EndpointType } from "../../../shared/endpoint";

const ServerComponent: React.FC<{
  endpoint: EndpointType;
  endpoints: EndpointType[];
  latency: number;
  loadingPageMessage: string;
  loadingPageProgress: number;
  localLoading: boolean;
  logAudio: boolean;
  logSource: boolean;
  requiresNewerMac: boolean;
  requiresWsl: boolean;
}> = ({
  endpoint,
  endpoints,
  latency,
  loadingPageMessage,
  loadingPageProgress,
  localLoading,
  logAudio,
  logSource,
  requiresNewerMac,
  requiresWsl,
}) => {
  const setEndpoint = (endpoint: string) => {
    ipcRenderer.send("setSettings", { endpoint });
    ipcRenderer.send(endpoint == "local" ? "startLocal" : "stopLocal");
  };

  const endpointOptions = endpoints.map((e: EndpointType) => ({
    id: e.id,
    value: e.name,
  }));

  return (
    <div className="px-4">
      <h2 className="text-lg font-light">Server</h2>
      {!requiresWsl ? null : (
        <div className="bg-yellow-100 border-l-2 border-yellow-500 text-yellow-700 p-2 my-1 text-small">
          <p>
            To use Serenade Local, you'll need to install{" "}
            <a className="underline" href="https://serenade.ai/install#pro" target="_blank">
              WSL
            </a>
            .
          </p>
        </div>
      )}
      {!requiresNewerMac ? null : (
        <div className="bg-yellow-100 border-l-2 border-yellow-500 text-yellow-700 p-2 my-1 text-small">
          <p>To use Serenade Local, you'll need to upgrade to macOS 11.0+.</p>
        </div>
      )}
      {endpoints && endpoints.length > 0 ? (
        <Row
          title="Server endpoint"
          subtitle={
            <>
              <div>Which server to connect to</div>
              <div>
                {endpoint && endpoint.id != "local" && latency
                  ? `Latency: ${Math.round(latency / 5)}ms`
                  : null}
                {localLoading ? (
                  <span className="font-bold ml-2">
                    <Spinner hidden={false} />
                    <span className="ml-1">Starting Local</span>
                  </span>
                ) : null}
              </div>
            </>
          }
          action={
            <div className="w-48 ml-auto">
              <Select
                items={endpointOptions.map((e: any) => e.value)}
                value={endpointOptions.filter((e: any) => e.id == endpoint.id)[0].value}
                onChange={(value: any) => {
                  setEndpoint(endpointOptions.filter((e: any) => e.value == value)[0].id);
                }}
              />
            </div>
          }
        />
      ) : null}
      <Row
        title="Share audio data"
        subtitle="You can help improve Serenade by sharing your audio data, which will be used to train Serenade's custom speech models."
        action={
          <Toggle
            value={logAudio}
            onChange={(e) =>
              ipcRenderer.send("setSettings", {
                logAudio: e,
              })
            }
          />
        }
      />
      <Row
        title="Share code data"
        subtitle="You can help improve Serenade by sharing your source code and command data, which will be used to train Serenade's custom code models."
        action={
          <Toggle
            value={logSource}
            onChange={(e) =>
              ipcRenderer.send("setSettings", {
                logSource: e,
              })
            }
          />
        }
      />
    </div>
  );
};

export const Server = connect((state: any) => ({
  endpoint: state.endpoint,
  endpoints: state.endpoints,
  latency: state.latency,
  localLoading: state.localLoading,
  loadingPageMessage: state.loadingPageMessage,
  loadingPageProgress: state.loadingPageProgress,
  localVersion: state.localVersion,
  logAudio: state.logAudio,
  logSource: state.logSource,
  requiresNewerMac: state.requiresNewerMac,
  requiresWsl: state.requiresWsl,
}))(ServerComponent);
