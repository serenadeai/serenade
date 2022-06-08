import React from "react";
import classNames from "classnames";
import { connect } from "react-redux";

const UpdateNotificationComponent: React.FC<{ miniMode: boolean; updateNotification: string }> = ({
  miniMode,
  updateNotification,
}) =>
  updateNotification == "downloaded" || updateNotification == "available" ? (
    <div
      id="update-notification"
      className={classNames("rounded-md mb-4 p-3 text-sm bg-white dark:bg-neutral-800", {
        "border shadow mt-2 mx-2": !miniMode,
      })}
    >
      {updateNotification == "available" ? (
        <>A new update for Serenade is downloading.</>
      ) : updateNotification == "downloaded" ? (
        <>A new update for Serenade has been downloaded. Restart to update to the latest version!</>
      ) : null}
    </div>
  ) : null;

export const UpdateNotification = connect((state: any) => ({
  miniMode: state.miniMode,
  updateNotification: state.updateNotification,
}))(UpdateNotificationComponent);
