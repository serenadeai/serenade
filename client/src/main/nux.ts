import Active from "./active";
import App from "./app";
import MainWindow from "./windows/main";
import MiniModeWindow from "./windows/mini-mode";
import PluginManager from "./ipc/plugin-manager";
import RendererBridge from "./bridge";
import Settings from "./settings";
import { core } from "../gen/core";
import { Step, Tutorial } from "../shared/tutorial";
import { isValidAlternative } from "../shared/alternatives";

import chromeBasicsTutorial from "../tutorials/chrome-basics.json";
import cPlusPlusBasicsTutorial from "../tutorials/cplusplus-basics.json";
import cSharpBasicsTutorial from "../tutorials/csharp-basics.json";
import formattingTutorial from "../tutorials/formatting.json";
import goBasicsTutorial from "../tutorials/go-basics.json";
import javaBasicsTutorial from "../tutorials/java-basics.json";
import javaScriptAdvancedTutorial from "../tutorials/javascript-advanced.json";
import javaScriptBasicsTutorial from "../tutorials/javascript-basics.json";
import navigationTutorial from "../tutorials/navigation.json";
import pythonAdvancedTutorial from "../tutorials/python-advanced.json";
import pythonBasicsTutorial from "../tutorials/python-basics.json";
import rubyBasicsTutorial from "../tutorials/ruby-basics.json";
import rustBasicsTutorial from "../tutorials/rust-basics.json";

const tutorialSteps: { [k: string]: any } = {
  "chrome-basics": chromeBasicsTutorial,
  "cplusplus-basics": cPlusPlusBasicsTutorial,
  "csharp-basics": cSharpBasicsTutorial,
  "go-basics": goBasicsTutorial,
  formatting: formattingTutorial,
  "java-basics": javaBasicsTutorial,
  "javascript-advanced": javaScriptAdvancedTutorial,
  "javascript-basics": javaScriptBasicsTutorial,
  navigation: navigationTutorial,
  "python-advanced": pythonAdvancedTutorial,
  "python-basics": pythonBasicsTutorial,
  "ruby-basics": rubyBasicsTutorial,
  "rust-basics": rustBasicsTutorial,
};

export default class NUX {
  private index: number = 0;
  private nextButtonEnabled: boolean = false;
  private showingError: boolean = false;
  private tutorial?: Tutorial;
  private verifyEditorInterval?: NodeJS.Timeout;

  constructor(
    private active: Active,
    private app: App,
    private bridge: RendererBridge,
    private mainWindow: MainWindow,
    private miniModeWindow: MiniModeWindow,
    private pluginManager: PluginManager,
    private settings: Settings
  ) {}

  private alternativeMatchesCurrentStep(
    alternative: core.ICommandsResponseAlternative,
    state: { source: string; cursor: number }
  ): boolean {
    if (!this.tutorial) {
      return false;
    }

    const step = this.tutorial.steps[this.index];
    const nextStep = this.tutorial.steps[this.index + 1];
    return (
      alternative.transcript == step.transcript ||
      alternative.description == step.transcript ||
      (!!step.matches && step.matches.indexOf(alternative.description!) > -1) ||
      (!step.textOnly &&
        !!nextStep.source &&
        state.source === nextStep.source &&
        state.cursor === nextStep.cursor)
    );
  }

  private indexToString(i: number): string {
    if (i == 1) {
      return "one";
    } else if (i == 2) {
      return "two";
    } else if (i == 3) {
      return "three";
    } else if (i == 4) {
      return "four";
    } else if (i == 5) {
      return "five";
    } else if (i == 6) {
      return "six";
    } else if (i == 7) {
      return "seven";
    } else if (i == 8) {
      return "eight";
    } else if (i == 9) {
      return "nine";
    } else if (i == 10) {
      return "ten";
    } else if (i == 11) {
      return "eleven";
    } else if (i == 12) {
      return "twelve";
    } else if (i == 13) {
      return "thirteen";
    } else if (i == 14) {
      return "fourteen";
    } else if (i == 15) {
      return "fifteen";
    } else if (i == 16) {
      return "sixteen";
    } else if (i == 17) {
      return "seventeen";
    } else if (i == 18) {
      return "eighteen";
    } else if (i == 19) {
      return "nineteen";
    } else if (i == 20) {
      return "twenty";
    }

    return "";
  }

  private showError(title: string, body: string) {
    this.showingError = true;
    this.setNextButtonEnabled(false);
    this.bridge.setState(
      {
        nuxStep: {
          title,
          body,
          error: true,
        },
      },
      [this.mainWindow, this.miniModeWindow]
    );
  }

  private verifyEditorFocusAndFilename() {
    if (!this.tutorial) {
      return;
    }

    const step = this.tutorial.steps[this.index];
    if (!step || step.skipEditorFocus) {
      return;
    }

    if (step.nextWhenEditorFocused) {
      this.setNextButtonEnabled(this.active.pluginConnected());
      return;
    }

    // don't show this message during setup steps, because they have descriptions telling
    // you in more detail what this means
    const setupStep = step.nextWhenEditorFocused || step.nextWhenEditorFilename;
    if (!this.active.pluginConnected() && !setupStep) {
      this.showError(
        step.title,
        "<p>To continue the tutorial, make sure the Serenade plugin is installed and your editor has focus!</p>"
      );
      return;
    }

    if (this.tutorial.filename) {
      const active = this.active.filename.split(".");
      const required = this.tutorial.filename.split(".");
      const matches = active[active.length - 1] == required[required.length - 1];
      if (step.nextWhenEditorFilename) {
        this.setNextButtonEnabled(matches);
        return;
      }

      if (!matches && !setupStep) {
        this.showError(
          step.title,
          `<p>To continue the tutorial, make sure you have a .${
            required[required.length - 1]
          } file open and your editor is focused!</p>`
        );
        return;
      }
    }

    if (this.showingError) {
      this.showingError = false;
      this.showCurrentStep();
    }
  }

  back(voice: boolean = false) {
    if (this.index == 0 || this.settings.getNuxCompleted() || !this.tutorial) {
      return;
    }

    if (
      voice &&
      this.active.isFirstPartyBrowser() &&
      this.tutorial.steps[this.index].transcript == "back"
    ) {
      return;
    }

    this.index--;
    this.showCurrentStep();
  }

  complete() {
    this.settings.setNuxTutorialName("");
    this.settings.setNuxStep(0);
    this.settings.setNuxCompleted(true);
    this.app.clearAlternativesAndShowExamples();
    this.bridge.setState(
      {
        nuxCompleted: true,
        nuxTutorial: "",
      },
      [this.mainWindow, this.miniModeWindow]
    );

    if (this.verifyEditorInterval) {
      clearInterval(this.verifyEditorInterval);
      this.verifyEditorInterval = undefined;
    }
  }

  load(name: string) {
    this.tutorial = tutorialSteps[name]!;
    if (this.verifyEditorInterval) {
      clearInterval(this.verifyEditorInterval);
      this.verifyEditorInterval = undefined;
    }

    this.settings.setNuxTutorialName(name);
    this.settings.setNuxCompleted(false);
    this.index = this.settings.getNuxStep();
    this.showingError = false;
    this.showCurrentStep();
    this.verifyEditorInterval = global.setInterval(() => {
      this.verifyEditorFocusAndFilename();
    }, 500);

    this.bridge.setState(
      {
        nuxCompleted: false,
        nuxTutorial: name,
      },
      [this.mainWindow, this.miniModeWindow]
    );
  }

  next() {
    this.index++;
    this.settings.setNuxStep(Math.max(this.index, this.settings.getNuxStep()));
    this.showCurrentStep();
  }

  async setEditorStateToStep(step: Step) {
    if (this.settings.getNuxCompleted() || !this.tutorial || step.source === undefined) {
      return;
    }

    let state = await this.active.getEditorState();
    state.source = state.source.toString();
    if (state.source != step.source || state.cursor != step.cursor) {
      this.pluginManager.sendCommandToApp(this.active.app, {
        type: core.CommandType.COMMAND_TYPE_DIFF,
        source: step.source,
        cursor: step.cursor,
      });
    }
  }

  setNextButtonEnabled(enabled: boolean) {
    this.nextButtonEnabled = enabled;
    this.bridge.setState(
      {
        nuxNextButtonEnabled: enabled,
      },
      [this.mainWindow, this.miniModeWindow]
    );
  }

  showIfNeeded() {
    if (!this.settings.getNuxCompleted() && this.settings.getNuxTutorialName()) {
      this.load(this.settings.getNuxTutorialName());
    }
  }

  async showCurrentStep() {
    if (!this.tutorial || this.showingError || this.settings.getNuxCompleted()) {
      return;
    }

    if (this.index == this.tutorial.steps.length) {
      this.complete();
      return;
    }

    let step = this.tutorial.steps[this.index];
    if (!step) {
      this.complete();
      return;
    }

    step.index = this.index;
    this.setEditorStateToStep(step);
    this.setNextButtonEnabled(
      !step.nextWhenEditorFocused &&
        !step.nextWhenEditorFilename &&
        (!step.transcript || step.textOnly || this.index < this.settings.getNuxStep())
    );

    if (this.settings.getMiniMode()) {
      this.mainWindow.resizeCallbackEnabled = false;
    }

    this.bridge.setState(
      {
        nuxHintShown: false,
        nuxStep: step,
      },
      [this.mainWindow, this.miniModeWindow]
    );

    // Keep applying this idempotent operation until calculation of the
    // size is right. It's really unclear why electron gives us an incorrect
    // size the first couple times.
    if (this.settings.getMiniMode()) {
      setTimeout(() => {
        this.miniModeWindow.setHeight(500);
        setTimeout(() => {
          this.miniModeWindow.show();
          this.bridge.send("updateMiniModeWindowHeight", {}, [this.miniModeWindow]);
          this.mainWindow.resizeCallbackEnabled = true;
        }, 50);
      }, 50);
    }
  }

  async updateForResponse(response: core.ICommandsResponse) {
    if (!response || !this.tutorial || this.showingError || this.settings.getNuxCompleted()) {
      return;
    }

    const step = this.tutorial.steps[this.index];
    if (!step) {
      return;
    }

    let state = await this.active.getEditorState();
    state.source = state.source.toString();
    if (
      response.execute &&
      (this.alternativeMatchesCurrentStep(response.execute, state) ||
        (this.nextButtonEnabled && !this.showingError && response.execute.transcript == "next"))
    ) {
      this.next();
      return;
    }

    let correct: number | null = null;
    const valid = (response.alternatives || []).filter((e: core.ICommandsResponseAlternative) =>
      isValidAlternative(e)
    );

    for (let i = 0; i < valid.length; i++) {
      if (this.alternativeMatchesCurrentStep(valid[i], state)) {
        correct = i;
        break;
      }
    }

    // in quiz mode, allow the user to explore more, possibly using multiple commands to achieve
    // the desired editor state
    if (!step.hideAnswer) {
      if (correct != null && correct > 0) {
        this.bridge.setState(
          {
            nuxStep: {
              title: step.title,
              body: "<p>Now, select a different alternative by saying:</p>",
              transcript: this.indexToString(correct + 1),
            },
          },
          [this.mainWindow, this.miniModeWindow]
        );

        return;
      }

      if (response.execute && response.execute.commands && !step.skipEditorFocus) {
        const commands = response.execute.commands.filter(
          (e: core.ICommand) => e.type == core.CommandType.COMMAND_TYPE_DIFF
        );

        if (commands.length > 0) {
          const source = commands[commands.length - 1].source;
          if (step && step.transcript && step.source != source) {
            this.bridge.setState(
              {
                nuxStep: {
                  title: "Undo",
                  body: "<p>To get your editor back to where it was before, say:</p>",
                  transcript: "undo",
                  error: true,
                },
              },
              [this.mainWindow, this.miniModeWindow]
            );

            return;
          }
        }
      }
    }

    // enable the next button if the user said something that didn't match,
    // so they can skip this step if they can't get it to work
    if (!response.execute || (response.execute && response.execute.transcript != "next")) {
      this.setNextButtonEnabled(true);
    }
  }
}
