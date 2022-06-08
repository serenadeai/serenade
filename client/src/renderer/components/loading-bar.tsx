import React from "react";

export const LoadingBar: React.FC<{ progress: number }> = ({ progress }) => (
  <div className="w-full">
    <div className="px-4">
      <div className="border rounded h-2">
        <div
          className="bg-violet-600 h-full rounded transition-all"
          style={{ width: Math.floor(100 * progress) + "%" }}
        ></div>
      </div>
    </div>
  </div>
);
