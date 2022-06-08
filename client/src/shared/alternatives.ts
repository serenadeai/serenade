import { core } from "../gen/core";

export const commandTypeToString = (commandType: any): string => {
  if (typeof commandType === "number") {
    return core.CommandType[commandType];
  }

  return commandType;
};

export const isMetaResponse = (response: core.ICommandsResponse) => {
  return (
    !!response &&
    response.alternatives &&
    response.alternatives.length > 0 &&
    response.alternatives[0].commands &&
    response.alternatives[0].commands.length > 0 &&
    [core.CommandType.COMMAND_TYPE_USE, core.CommandType.COMMAND_TYPE_CANCEL].includes(
      response.alternatives[0].commands[0].type!
    )
  );
};

export const isValidAlternative = (alternative: any) => {
  return (
    alternative.commands &&
    alternative.commands.every(
      (command: core.ICommand) => command.type != core.CommandType.COMMAND_TYPE_INVALID
    )
  );
};
