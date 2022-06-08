package core.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.ByteString;
import core.BaseServiceTest;
import core.gen.rpc.CommandType;
import core.gen.rpc.CommandsResponse;
import core.gen.rpc.CustomCommand;
import core.gen.rpc.CustomCommandChainable;
import core.gen.rpc.CustomCommandOption;
import core.gen.rpc.EditorState;
import core.gen.rpc.Language;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class CustomCommandsTest extends BaseServiceTest {

  private Language language = Language.LANGUAGE_PYTHON;

  @Test
  public void testCustomCommandWithoutSlots() {
    assertEquals(
      "1",
      makeRequest(
        EditorState
          .newBuilder()
          .setLanguage(language)
          .addAllCustomCommands(
            Arrays.asList(
              CustomCommand.newBuilder().setId("1").setTemplated("make").build(),
              CustomCommand.newBuilder().setId("2").setTemplated("bake").build()
            )
          )
          .build(),
        Arrays.asList("make")
      )
        .getAlternatives(0)
        .getCommands(0)
        .getCustomCommandId()
    );

    assertEquals(
      "2",
      makeRequest(
        EditorState
          .newBuilder()
          .setLanguage(language)
          .addAllCustomCommands(
            Arrays.asList(
              CustomCommand.newBuilder().setId("1").setTemplated("make").build(),
              CustomCommand.newBuilder().setId("2").setTemplated("bake").build()
            )
          )
          .build(),
        Arrays.asList("bake")
      )
        .getAlternatives(0)
        .getCommands(0)
        .getCustomCommandId()
    );
  }

  @Test
  public void testCustomCommandWithSlot() {
    CommandsResponse response = makeRequest(
      EditorState
        .newBuilder()
        .setLanguage(language)
        .addAllCustomCommands(
          Arrays.asList(
            CustomCommand.newBuilder().setId("1").setTemplated("make <%project%>").build(),
            CustomCommand.newBuilder().setId("2").setTemplated("bake <%project%>").build()
          )
        )
        .build(),
      Arrays.asList("make foo")
    );
    assertEquals("1", response.getAlternatives(0).getCommands(0).getCustomCommandId());
    assertEquals(
      "foo",
      response.getAlternatives(0).getCommands(0).getReplacementsMap().get("project")
    );
  }

  @Test
  public void testCustomCommandWithSlots() {
    CommandsResponse response = makeRequest(
      EditorState
        .newBuilder()
        .setLanguage(language)
        .addAllCustomCommands(
          Arrays.asList(
            CustomCommand.newBuilder().setId("1").setTemplated("make <% project %>").build(),
            CustomCommand
              .newBuilder()
              .setId("2")
              .setTemplated("make <% project %> then <% something %>")
              .build(),
            CustomCommand
              .newBuilder()
              .setId("3")
              .setTemplated("make <% project %> than <% something %>")
              .build()
          )
        )
        .build(),
      Arrays.asList("make foo then clean")
    );

    assertEquals("2", response.getAlternatives(0).getCommands(0).getCustomCommandId());
    assertEquals(
      "foo",
      response.getAlternatives(0).getCommands(0).getReplacementsMap().get("project")
    );
    assertEquals(
      "clean",
      response.getAlternatives(0).getCommands(0).getReplacementsMap().get("something")
    );
  }

  @Test
  public void testCustomCommandChainingAny() {
    CommandsResponse response = makeRequest(
      EditorState
        .newBuilder()
        .setLanguage(language)
        .addAllCustomCommands(
          Arrays.asList(
            CustomCommand
              .newBuilder()
              .setId("1")
              .setTemplated("make <%project%>")
              .setChainable(CustomCommandChainable.CUSTOM_COMMAND_CHAINABLE_ANY)
              .build(),
            CustomCommand
              .newBuilder()
              .setId("2")
              .setTemplated("bake <%project%>")
              .setChainable(CustomCommandChainable.CUSTOM_COMMAND_CHAINABLE_ANY)
              .build()
          )
        )
        .build(),
      Arrays.asList("make foo bake bar")
    );

    assertEquals(2, response.getAlternatives(0).getCommandsList().size());
    assertEquals("1", response.getAlternatives(0).getCommands(0).getCustomCommandId());
    assertEquals("2", response.getAlternatives(0).getCommands(1).getCustomCommandId());
    assertEquals(
      "foo",
      response.getAlternatives(0).getCommands(0).getReplacementsMap().get("project")
    );
    assertEquals(
      "bar",
      response.getAlternatives(0).getCommands(1).getReplacementsMap().get("project")
    );
  }

  @Test
  public void testCustomCommandChainingFirstOnly() {
    EditorState state = EditorState
      .newBuilder()
      .setLanguage(language)
      .addAllCustomCommands(
        Arrays.asList(
          CustomCommand
            .newBuilder()
            .setId("1")
            .setTemplated("make <%project%>")
            .setChainable(CustomCommandChainable.CUSTOM_COMMAND_CHAINABLE_FIRST_ONLY)
            .build(),
          CustomCommand
            .newBuilder()
            .setId("2")
            .setTemplated("bake <%project%>")
            .setChainable(CustomCommandChainable.CUSTOM_COMMAND_CHAINABLE_ANY)
            .build()
        )
      )
      .build();

    CommandsResponse r1 = makeRequest(state, Arrays.asList("make foo bake bar"));
    assertEquals(2, r1.getAlternatives(0).getCommandsList().size());
    assertEquals("1", r1.getAlternatives(0).getCommands(0).getCustomCommandId());
    assertEquals("2", r1.getAlternatives(0).getCommands(1).getCustomCommandId());

    CommandsResponse r2 = makeRequest(state, Arrays.asList("bake foo make bar"));
    assertEquals(1, r2.getAlternatives(0).getCommandsList().size());
    assertEquals("2", r2.getAlternatives(0).getCommands(0).getCustomCommandId());
  }

  @Test
  public void testCustomCommandChainingLastOnly() {
    EditorState state = EditorState
      .newBuilder()
      .setLanguage(language)
      .addAllCustomCommands(
        Arrays.asList(
          CustomCommand
            .newBuilder()
            .setId("1")
            .setTemplated("make <%project%>")
            .setChainable(CustomCommandChainable.CUSTOM_COMMAND_CHAINABLE_LAST_ONLY)
            .build(),
          CustomCommand
            .newBuilder()
            .setId("2")
            .setTemplated("bake <%project%>")
            .setChainable(CustomCommandChainable.CUSTOM_COMMAND_CHAINABLE_ANY)
            .build()
        )
      )
      .build();

    CommandsResponse r1 = makeRequest(state, Arrays.asList("make foo bake bar"));
    assertEquals(1, r1.getAlternatives(0).getCommandsList().size());
    assertEquals(CommandType.COMMAND_TYPE_CUSTOM, r1.getAlternatives(0).getCommands(0).getType());

    CommandsResponse r2 = makeRequest(state, Arrays.asList("bake foo make bar"));
    assertEquals(2, r2.getAlternatives(0).getCommandsList().size());
    assertEquals("2", r2.getAlternatives(0).getCommands(0).getCustomCommandId());
    assertEquals("1", r2.getAlternatives(0).getCommands(1).getCustomCommandId());
  }

  @Test
  public void testCustomSnippetWithoutLanguage() {
    assertEquals(
      "make()",
      makeRequest(
        EditorState
          .newBuilder()
          .addAllCustomCommands(
            Arrays.asList(
              CustomCommand
                .newBuilder()
                .setId("1")
                .setTemplated("make")
                .setGenerated("make()")
                .build()
            )
          )
          .build(),
        Arrays.asList("make")
      )
        .getAlternatives(0)
        .getCommands(0)
        .getSource()
    );
  }

  @Test
  public void testCustomSnippetWithoutSlots() {
    assertEquals(
      "make()\n",
      makeRequest(
        EditorState
          .newBuilder()
          .setLanguage(language)
          .addAllCustomCommands(
            Arrays.asList(
              CustomCommand
                .newBuilder()
                .setId("1")
                .setTemplated("make")
                .setGenerated("make()")
                .build(),
              CustomCommand
                .newBuilder()
                .setId("2")
                .setTemplated("bake")
                .setGenerated("bake()")
                .build()
            )
          )
          .build(),
        Arrays.asList("make")
      )
        .getAlternatives(0)
        .getCommands(0)
        .getSource()
    );
  }

  @Test
  public void testCustomSnippetWithSlots() {
    CommandsResponse response = makeRequest(
      EditorState
        .newBuilder()
        .setSource(ByteString.copyFromUtf8("class Tests:\n    pass\n"))
        .setCursor(0)
        .setLanguage(language)
        .addAllCustomCommands(
          Arrays.asList(
            CustomCommand
              .newBuilder()
              .setId("1")
              .setTemplated("test <%identifier%>")
              .setGenerated("def test_<%identifier%>():\n<%indent%>pass")
              .setSnippetType("method")
              .addAllOptions(
                Arrays.asList(
                  CustomCommandOption
                    .newBuilder()
                    .setSlot("identifier")
                    .addAllOptions(Arrays.asList("identifier", "underscores"))
                    .build()
                )
              )
              .build(),
            CustomCommand
              .newBuilder()
              .setId("2")
              .setTemplated("bake")
              .setGenerated("bake()")
              .setSnippetType("statement")
              .build()
          )
        )
        .build(),
      Arrays.asList("test foo bar")
    );

    assertEquals(
      "class Tests:\n    def test_foo_bar():\n        pass\n",
      response.getAlternatives(0).getCommands(0).getSource()
    );
  }

  @Test
  public void testCustomSnippetWithInline() {
    assertEquals(
      "def test_foo():\n    passserenade.global().snippet(\"test method <%name%>\",\"def test_<%name%><%cursor%>(self):<%newline%><%indent%>pass\");",
      makeRequest(
        EditorState
          .newBuilder()
          .setSource(
            ByteString.copyFromUtf8(
              "serenade.global().snippet(\"test method <%name%>\",\"def test_<%name%><%cursor%>(self):<%newline%><%indent%>pass\");"
            )
          )
          .setCursor(0)
          .setLanguage(language)
          .addAllCustomCommands(
            Arrays.asList(
              CustomCommand
                .newBuilder()
                .setId("1")
                .setTemplated("test method <%name%>")
                .setGenerated("def test_<%name%><%cursor%>():<%newline%><%indent%>pass")
                .setSnippetType("inline")
                .build()
            )
          )
          .build(),
        Arrays.asList("test method foo")
      )
        .getAlternatives(0)
        .getCommands(0)
        .getSource()
    );
  }

  @Test
  public void testCustomWords() {
    CommandsResponse r1 = makeRequest(
      EditorState
        .newBuilder()
        .setLanguage(language)
        .putAllCustomWords(
          Stream
            .of(new String[][] { { "foo", "bar" }, { "baz", "bar" } })
            .collect(Collectors.toMap(data -> data[0], data -> data[1]))
        )
        .build(),
      Arrays.asList("foo baz")
    );
    assertEquals("bar bar", r1.getAlternatives(0).getTranscript());

    CommandsResponse r2 = makeRequest(
      EditorState
        .newBuilder()
        .setLanguage(language)
        .putAllCustomWords(
          Stream
            .of(new String[][] { { "foo", "bar" } })
            .collect(Collectors.toMap(data -> data[0], data -> data[1]))
        )
        .build(),
      Arrays.asList("foobarbaz foo")
    );
    assertEquals("foobarbaz bar", r2.getAlternatives(0).getTranscript());
  }

  @Test
  public void testManyCustomCommands() {
    // make sure we can handle a large number of custom commands
    Random random = new Random();
    List<CustomCommand> generated = new ArrayList<>();
    for (int i = 0; i < 500; i++) {
      generated.add(
        CustomCommand
          .newBuilder()
          .setId(String.valueOf(i))
          .setChainable(CustomCommandChainable.CUSTOM_COMMAND_CHAINABLE_ANY)
          .setTemplated(
            random
              .ints(48, 122 + 1)
              .filter(e -> (e <= 57 || e >= 65) && (e <= 90 || e >= 97))
              .limit(20)
              .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
              .toString() +
            " <%value%>"
          )
          .build()
      );
    }

    long start = System.currentTimeMillis();
    makeRequest(
      EditorState.newBuilder().setLanguage(language).addAllCustomCommands(generated).build(),
      Arrays.asList("make foo")
    );
    assertTrue(System.currentTimeMillis() - start < 1000);
  }
}
