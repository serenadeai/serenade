export default class InsertHistory {
  private history: { app: string; dt: number; text: string }[] = [];

  add(text: string, app: string) {
    this.history.unshift({ app, text, dt: Date.now() });
  }

  clear() {
    this.history = [];
  }

  latest(app: string) {
    if (this.history.length == 0) {
      return "";
    }

    const value = this.history[0];
    if (app != value.app || Date.now() - value.dt > 10000) {
      this.clear();
      return "";
    }

    return value.text;
  }

  normalize(text: string, app: string) {
    const value = this.latest(app);
    if (value && text.startsWith(value)) {
      text = text.substring(value.length);
    }

    return text;
  }
}
