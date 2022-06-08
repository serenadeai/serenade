package core.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.protobuf.ByteString;
import core.gen.rpc.CallbackRequest;
import core.gen.rpc.CallbackType;
import core.gen.rpc.CommandType;
import core.gen.rpc.CommandsResponse;
import core.gen.rpc.EditorState;
import core.gen.rpc.EvaluateRequest;
import core.gen.rpc.EvaluateTextRequest;
import core.gen.rpc.InitializeRequest;
import core.gen.rpc.Language;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toolbelt.client.CoreClient;

public class PasteTest {

  private CoreClient client;

  @Test
  private void checkCopyAndPaste(
    String source,
    int cursor,
    String copyTranscript,
    String pasteTranscript,
    String expectedSource,
    int expectedCursor
  ) {
    EditorState state = EditorState
      .newBuilder()
      .setSource(ByteString.copyFromUtf8(source))
      .setCursor(cursor)
      .setLanguage(Language.LANGUAGE_JAVA)
      .setApplication("atom")
      .setClientIdentifier("{\"version\": \"1.10.0\", \"os\": \"darwin\"}")
      .setPluginInstalled(true)
      .setCanGetState(true)
      .setCanSetState(true)
      .build();
    client.send(
      EvaluateRequest
        .newBuilder()
        .setInitializeRequest(InitializeRequest.newBuilder().setEditorState(state).build())
        .build()
    );
    EvaluateTextRequest textRequest = EvaluateTextRequest
      .newBuilder()
      .setText(copyTranscript)
      .setIncludeAlternatives(true)
      .setRerank(true)
      .build();
    client.send(EvaluateRequest.newBuilder().setTextRequest(textRequest).build());

    CommandsResponse response;
    try {
      response = client.await().getCommandsResponse();
    } catch (Exception e) {
      fail();
      return;
    }
    assertEquals(
      response.getAlternativesList().get(0).getCommandsList().get(0).getType(),
      CommandType.COMMAND_TYPE_COPY
    );
    String clipboard = response.getAlternativesList().get(0).getCommandsList().get(0).getText();
    state = EditorState.newBuilder(state).setClipboard(clipboard).build();
    client.send(
      EvaluateRequest
        .newBuilder()
        .setInitializeRequest(InitializeRequest.newBuilder().setEditorState(state).build())
        .build()
    );
    textRequest =
      EvaluateTextRequest
        .newBuilder()
        .setText(pasteTranscript)
        .setIncludeAlternatives(true)
        .setRerank(true)
        .build();
    client.send(EvaluateRequest.newBuilder().setTextRequest(textRequest).build());

    try {
      response = client.await().getCommandsResponse();
    } catch (Exception e) {
      fail();
      return;
    }
    EvaluateRequest evaluateRequest = EvaluateRequest
      .newBuilder()
      .setCallbackRequest(
        CallbackRequest
          .newBuilder()
          .setType(CallbackType.CALLBACK_TYPE_PASTE)
          .setText(response.getExecute().getCommandsList().get(0).getDirection())
      )
      .build();
    client.send(evaluateRequest);

    try {
      response = client.await().getCommandsResponse();
    } catch (Exception e) {
      fail();
      return;
    }
    assertEquals(expectedSource, response.getExecute().getCommandsList().get(0).getSource());
    assertEquals(expectedCursor, response.getExecute().getCommandsList().get(0).getCursor());
  }

  @BeforeEach
  public void baseServiceBefore() {
    client = new CoreClient("localhost:17200");
    client.connect();
  }

  @AfterEach
  public void baseServiceAfter() {
    client.disconnect();
  }

  @Test
  public void testPaste() {
    checkCopyAndPaste("foo\nbar\n", 0, "copy line two", "paste below", "foo\nbar\nbar\n", 7);
    checkCopyAndPaste("foo\nbar\n", 0, "copy line two", "paste", "foo\nbar\nbar\n", 7);
    checkCopyAndPaste("foo\nbar\n", 0, "copy line two", "paste above", "bar\nfoo\nbar\n", 3);
    checkCopyAndPaste("foo\nbar\n", 5, "copy line two", "paste above", "foo\nbar\nbar\n", 7);

    checkCopyAndPaste("foo\nbar\n", 0, "copy character two", "paste below", "foo\no\nbar\n", 5);
    checkCopyAndPaste("foo\nbar\n", 0, "copy character two", "paste", "ofoo\nbar\n", 1);
    checkCopyAndPaste("foo\nbar\n", 0, "copy character two", "paste above", "o\nfoo\nbar\n", 1);
    checkCopyAndPaste("foo\nbar\n", 5, "copy character two", "paste above", "foo\na\nbar\n", 5);
  }
}
