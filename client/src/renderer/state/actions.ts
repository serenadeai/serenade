/**
 * Actions here should only affect the renderer process in the same webview.
 * If the main process or renderer process in another webview needs to be aware
 * of a state change, then use ipcRenderer.send instead.
 */

export const setRevisionBoxMode = (revisionBoxMode: string) => ({
  type: "REVISION_BOX_MODE",
  revisionBoxMode,
});
