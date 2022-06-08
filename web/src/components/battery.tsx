import React from "react";

export const Battery = () => (
  <div className="color-slate-600 mt-12 flex items-center justify-center">
    <div
      style={{
        borderRadius: "10px",
        border: "6px solid white",
        padding: "5px",
        maxWidth: "400px",
        height: "120px",
        width: "100%",
      }}
    >
      <div
        className="battery-level battery-animated"
        style={{
          background:
            "linear-gradient(0.25turn, #c5d1e7 1.04%, #82c0ff 14.06%, #a46dff 38.54%, #ff8488 62.5%, #eec14c 79.17%)",
          borderRadius: "6px",
          width: 0,
          height: "98px",
          animationName: "battery-fill",
          animationDuration: "4s",
          animationIterationCount: "infinite",
          animationTimingFunction: "cubic-bezier(0.25, 1, 0.5, 1)",
        }}
      ></div>
    </div>
    <div
      style={{
        borderRadius: "6px",
        borderTopLeftRadius: 0,
        borderBottomLeftRadius: 0,
        backgroundColor: "white",
        width: "20px",
        height: "52px",
      }}
    ></div>
  </div>
);
