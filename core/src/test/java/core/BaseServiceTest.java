package core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.protobuf.ByteString;
import core.gen.rpc.Command;
import core.gen.rpc.CommandType;
import core.gen.rpc.CommandsResponse;
import core.gen.rpc.EditorState;
import core.gen.rpc.EvaluateRequest;
import core.gen.rpc.EvaluateTextRequest;
import core.gen.rpc.InitializeRequest;
import core.gen.rpc.Language;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import toolbelt.client.CoreClient;

public class BaseServiceTest extends BaseTest {

  protected CoreClient client;

  @BeforeEach
  public void baseServiceBefore() {
    client = new CoreClient("localhost:17200");
    client.connect();
  }

  @AfterEach
  public void baseServiceAfter() {
    client.disconnect();
  }

  private void printTrees(String source, Language language) {
    if (System.getenv("PRINT_TREES") != null) {
      System.out.println(trees(source, language));
    }
  }

  private String trees(String source, Language language) {
    try {
      String result = "";
      result += "Parse tree:\n" + component.parser().parse(source, language).toDebugString();
      result +=
        "\nAST:\n" + component.astFactory().createFileRoot(source, language).toDebugString();
      return result;
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  protected Command assertCommandType(
    String source,
    int cursor,
    String transcript,
    Language language,
    CommandType type
  ) {
    List<Command> commands = assertCommandTypes(
      source,
      cursor,
      transcript,
      language,
      Arrays.asList(type)
    );
    return commands.get(commands.size() - 1);
  }

  protected Command assertCommandType(
    String source,
    int cursor,
    String transcript,
    String filename,
    CommandType type
  ) {
    return assertCommandType(
      source,
      cursor,
      transcript,
      component.languageDeterminer().fromFilename(filename),
      type
    );
  }

  protected List<Command> assertCommandTypes(
    String source,
    int cursor,
    String transcript,
    Language language,
    List<CommandType> types
  ) {
    CommandsResponse response = makeRequest(source, cursor, transcript, language);
    if (response.getAlternativesList().size() == 0) {
      fail("No alternatives returned:\n" + response);
    }

    assertIterableEquals(
      types,
      response
        .getAlternatives(0)
        .getCommandsList()
        .stream()
        .map(e -> e.getType())
        .collect(Collectors.toList())
    );

    return response.getAlternatives(0).getCommandsList();
  }

  protected void assertDescription(
    String source,
    int cursor,
    String transcript,
    String filename,
    String description
  ) {
    assertTrue(
      makeRequest(source, cursor, transcript, filename)
        .getAlternativesList()
        .stream()
        .filter(alternative -> alternative.getDescription().equals(description))
        .findFirst()
        .isPresent()
    );
  }

  protected Command assertSelectedRange(
    String source,
    int cursor,
    String transcript,
    String filename,
    int cursorStart,
    int cursorEnd
  ) {
    return assertSelectedRange(
      source,
      cursor,
      transcript,
      component.languageDeterminer().fromFilename(filename),
      cursorStart,
      cursorEnd
    );
  }

  protected Command assertSelectedRange(
    String source,
    int cursor,
    String transcript,
    Language language,
    int cursorStart,
    int cursorEnd
  ) {
    if (System.getenv("ALWAYS_PRINT_TREES") != null) {
      System.out.println(trees(source, language));
    }

    CommandsResponse response = makeRequest(source, cursor, transcript, language);
    Command command = response.getAlternatives(0).getCommands(0);
    if (command.getType() != CommandType.COMMAND_TYPE_SELECT) {
      printTrees(source, language);
      fail(
        "Response is not a select command! If you're not trying to test a select command, do you have an extra cursor in your test?\n\nGot response:\n" +
        response
      );
    }

    if (cursorStart != command.getCursor() || cursorEnd != command.getCursorEnd()) {
      printTrees(source, language);
      fail(
        "Cursor mismatch!\n\nExpected:\n---\n" +
        source.substring(0, cursorStart) +
        "<>" +
        source.substring(cursorStart, cursorEnd) +
        "<>" +
        source.substring(cursorEnd) +
        "\n---\n\nActual:\n---\n" +
        source.substring(0, command.getCursor()) +
        "<>" +
        source.substring(command.getCursor(), command.getCursorEnd()) +
        "<>" +
        source.substring(command.getCursorEnd()) +
        "\n---"
      );
    }

    assertEquals(cursorStart, command.getCursor());
    assertEquals(cursorEnd, command.getCursorEnd());
    return command;
  }

  protected Command assertStringsMatch(
    String beforeSource,
    int beforeCursor,
    String transcript,
    String filename,
    String afterSource,
    int afterCursor,
    String description,
    boolean allowSecondAlternative
  ) {
    return assertStringsMatch(
      beforeSource,
      beforeCursor,
      transcript,
      component.languageDeterminer().fromFilename(filename),
      afterSource,
      afterCursor,
      description,
      allowSecondAlternative
    );
  }

  protected Command assertStringsMatch(
    String beforeSource,
    int beforeCursor,
    String transcript,
    Language language,
    String afterSource,
    int afterCursor,
    String description,
    boolean allowSecondAlternative
  ) {
    if (System.getenv("ALWAYS_PRINT_TREES") != null) {
      System.out.println(trees(beforeSource, language));
    }

    CommandsResponse response = makeRequest(beforeSource, beforeCursor, transcript, language);
    if (response.getAlternativesList().size() == 0) {
      printTrees(beforeSource, language);
      fail("No alternatives returned:\n" + response);
    }

    List<Optional<Command>> commands = new ArrayList<>();
    commands.add(
      response
        .getAlternatives(0)
        .getCommandsList()
        .stream()
        .filter(e -> e.getType() == CommandType.COMMAND_TYPE_DIFF)
        .reduce((a, b) -> b)
    );

    if (allowSecondAlternative && response.getAlternativesList().size() > 1) {
      commands.add(
        response
          .getAlternatives(1)
          .getCommandsList()
          .stream()
          .filter(e -> e.getType() == CommandType.COMMAND_TYPE_DIFF)
          .reduce((a, b) -> b)
      );
    }

    for (int i = 0; i < commands.size(); i++) {
      Optional<Command> command = commands.get(i);
      if (command.isEmpty()) {
        continue;
      }

      if (
        afterSource.equals(command.get().getSource()) &&
        afterCursor == command.get().getCursor() &&
        (description.equals("") || description.equals(response.getAlternatives(i).getDescription()))
      ) {
        return command.get();
      }
    }

    if (
      commands.get(0).isEmpty() || commands.get(0).get().getType() != CommandType.COMMAND_TYPE_DIFF
    ) {
      printTrees(beforeSource, language);
      fail("Response is not a diff command:\n" + response);
    }

    Command command = commands.get(0).get();
    if (!afterSource.equals(command.getSource())) {
      printTrees(beforeSource, language);
      fail(
        "Source mismatch!\n\nExpected (length = " +
        afterSource.length() +
        "):\n---\n" +
        afterSource +
        "\n---\n\nActual (length = " +
        command.getSource().length() +
        "):\n---\n" +
        command.getSource() +
        "\n---"
      );
    }

    if (afterCursor != command.getCursor()) {
      printTrees(beforeSource, language);
      if (command.getCursor() < 0 || command.getCursor() > command.getSource().length()) {
        fail(
          "Cursor outside file bounds!\n\nSource length: " +
          command.getSource().length() +
          "\nCursor: " +
          command.getCursor()
        );
      }

      fail(
        "Cursor mismatch!\n\nExpected:\n---\n" +
        afterSource.substring(0, afterCursor) +
        "<>" +
        afterSource.substring(afterCursor) +
        "\n---\n\nActual:\n---\n" +
        command.getSource().substring(0, command.getCursor()) +
        "<>" +
        command.getSource().substring(command.getCursor()) +
        "\n---"
      );
    }

    assertEquals(afterSource, command.getSource());
    assertEquals(afterCursor, command.getCursor());
    return command;
  }

  protected EditorState editorState(String source, int cursor, Language language) {
    return EditorState
      .newBuilder()
      .setSource(ByteString.copyFromUtf8(source))
      .setCursor(cursor)
      .setLanguage(language)
      .setApplication("atom")
      .setClientIdentifier("{\"version\": \"1.10.0\", \"os\": \"darwin\"}")
      .setPluginInstalled(true)
      .setCanGetState(true)
      .setCanSetState(true)
      .build();
  }

  protected CommandsResponse makeRequest(EditorState state, List<String> transcripts) {
    client.send(
      EvaluateRequest
        .newBuilder()
        .setInitializeRequest(InitializeRequest.newBuilder().setEditorState(state).build())
        .build()
    );

    return makeRequest(
      state,
      EvaluateTextRequest
        .newBuilder()
        .setText(String.join(" ; ", transcripts))
        .setIncludeAlternatives(true)
        .setRerank(true)
        .build()
    );
  }

  protected CommandsResponse makeRequest(EditorState state, EvaluateTextRequest textRequest) {
    client.send(
      EvaluateRequest
        .newBuilder()
        .setInitializeRequest(InitializeRequest.newBuilder().setEditorState(state).build())
        .build()
    );

    client.send(EvaluateRequest.newBuilder().setTextRequest(textRequest).build());
    try {
      return client.await().getCommandsResponse();
    } catch (Exception e) {
      fail();
    }

    return null;
  }

  protected CommandsResponse makeRequest(
    String source,
    int cursor,
    List<String> transcripts,
    String filename
  ) {
    return makeRequest(
      source,
      cursor,
      transcripts,
      component.languageDeterminer().fromFilename(filename)
    );
  }

  protected CommandsResponse makeRequest(
    String source,
    int cursor,
    List<String> transcripts,
    Language language
  ) {
    return makeRequest(editorState(source, cursor, language), transcripts);
  }

  protected CommandsResponse makeRequest(
    String source,
    int cursor,
    String transcript,
    String filename
  ) {
    return makeRequest(
      source,
      cursor,
      transcript,
      component.languageDeterminer().fromFilename(filename)
    );
  }

  protected CommandsResponse makeRequest(
    String source,
    int cursor,
    String transcript,
    Language language
  ) {
    return makeRequest(source, cursor, Arrays.asList(transcript), language);
  }
}
