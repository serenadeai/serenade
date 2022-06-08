import React from "react";
import { connect } from "react-redux";
import { ListenStatus } from "../components/listen-status";
import { ListenToggle } from "../components/listen-toggle";
import { AlternativesList } from "../components/alternatives-list";
import { ActiveAppIndicator } from "../components/indicators/active-app-indicator";
import { ConnectionIndicator } from "../components/indicators/connection-indicator";
import { EndpointIndicator } from "../components/indicators/endpoint-indicator";
import { LanguageIndicator } from "../components/indicators/language-indicator";
import { ModeIndicator } from "../components/indicators/mode-indicator";
import { SettingsButton } from "../components/settings-button";
import { VolumeIndicator } from "../components/indicators/volume-indicator";

const AlternativesPageComponent: React.FC<{ miniMode: boolean }> = ({ miniMode }) => (
  <div className="overflow-hidden flex flex-col h-screen pt-[24px]">
    <div className="flex items-center justify-between select-none">
      <div className="flex items-center pl-1" style={{ minHeight: "30px" }}>
        <ListenToggle />
        <ListenStatus />
      </div>
      <div className="flex items-center pr-1">
        <VolumeIndicator />
        <ConnectionIndicator />
        <SettingsButton />
      </div>
    </div>
    {miniMode ? null : <AlternativesList miniModePage={false} />}
    <div
      className="status-indicators flex border-t border-gray-200 mt-1.5 dark:border-neutral-600"
      style={{
        padding: "2px 3px",
      }}
    >
      <div className="flex">
        <ActiveAppIndicator />
      </div>
      <div className="flex ml-auto">
        <ModeIndicator />
        <LanguageIndicator />
        <EndpointIndicator />
      </div>
    </div>
  </div>
);

export const AlternativesPage = connect((state: any) => ({
  miniMode: state.miniMode,
}))(AlternativesPageComponent);
