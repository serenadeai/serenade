package core.util;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.gson.Gson;
import core.evaluator.ParsedTranscript;
import core.evaluator.TranscriptEvaluator;
import core.gen.rpc.Command;
import core.gen.rpc.CommandType;
import core.gen.rpc.CommandsResponse;
import core.gen.rpc.CommandsResponseAlternative;
import core.gen.rpc.CustomCommand;
import core.metadata.EditorStateWithMetadata;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.xml.bind.DatatypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import toolbelt.env.Env;
import toolbelt.logging.Logs;

@Singleton
public class CommandLogger {

  private Optional<String> bucket = Optional.empty();
  private Optional<AmazonS3> client = Optional.empty();
  private Gson gson = new Gson();
  private ExecutorService service = Executors.newCachedThreadPool();
  private Logger logger = LoggerFactory.getLogger(CommandLogger.class);

  @Inject
  Env env;

  @Inject
  public CommandLogger() {
    if (System.getenv("REGION") != null && System.getenv("S3_BUCKET") != null) {
      bucket = Optional.of(System.getenv("S3_BUCKET"));
      client =
        Optional.of(
          AmazonS3ClientBuilder
            .standard()
            .withRegion(Regions.fromName(System.getenv("REGION")))
            .build()
        );
    }
  }

  private Map<String, Object> getLogEntry(Command command) {
    Map<String, Object> result = new HashMap<>();
    result.put("type", command.getType());
    if (command.getType() == CommandType.COMMAND_TYPE_USE) {
      result.put("index", command.getIndex());
    }

    return result;
  }

  private Map<String, Object> getLogEntry(CommandsResponseAlternative alternative) {
    return Map.of(
      "alternative_id",
      alternative.getAlternativeId(),
      "transcript",
      alternative.getTranscript(),
      "description",
      alternative.getDescription(),
      "commands",
      alternative.getCommandsList().stream().map(e -> getLogEntry(e)).collect(Collectors.toList())
    );
  }

  public void logAudio(String chunkId, String token, byte[] pcmData, boolean local) {
    if (client.isEmpty()) {
      return;
    }

    service.execute(
      () -> {
        try {
          AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
          AudioInputStream audioInputStream = new AudioInputStream(
            new ByteArrayInputStream(pcmData),
            format,
            pcmData.length / format.getFrameSize()
          );

          ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
          AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, byteArrayOutputStream);

          File file = File.createTempFile(chunkId, "wav");
          FileOutputStream fileOutputStream = new FileOutputStream(file);
          fileOutputStream.write(byteArrayOutputStream.toByteArray());
          fileOutputStream.close();
          client.get().putObject(bucket.get(), "audio/" + chunkId + ".wav", file);
          file.delete();
        } catch (Exception e) {
          Logs.logError(logger, "S3 audio upload failed", e);
        }
      }
    );
  }

  public void logResponse(CommandsResponse response, EditorStateWithMetadata editorState) {
    Map<String, Object> data = new HashMap<>();
    data.put("endpoint_id", response.getEndpointId());
    data.put("token", editorState.getToken());
    data.put("chunk_ids", response.getChunkIdsList());
    data.put("nux", editorState.getNux());
    data.put("language", editorState.getLanguage());
    data.put("text", response.getTextResponse());

    if (editorState.getClientIdentifier().length() > 0) {
      data.put("client_identifier", Logs.jsonToObject(editorState.getClientIdentifier()));
    }

    if (editorState.getLogAudio() || editorState.getLogSource()) {
      data.put("execute", getLogEntry(response.getExecute()));
      data.put(
        "alternatives",
        response
          .getAlternativesList()
          .stream()
          .map(e -> getLogEntry(e))
          .collect(Collectors.toList())
      );
    }

    if (client.isPresent() && editorState.getLogSource()) {
      try {
        Map<String, Object> state = new HashMap<>();
        state.put("filename", editorState.getFilename());
        state.put("client_identifier", editorState.getClientIdentifier());
        state.put("application", editorState.getApplication());
        state.put("source", editorState.getSource());
        state.put("cursor", editorState.getCursor());

        byte[] bytes = gson.toJson(state).getBytes();
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update(bytes);
        String hash = DatatypeConverter.printHexBinary(digest.digest()).toLowerCase();
        data.put("state", hash);

        service.execute(
          () -> {
            try {
              File file = File.createTempFile(hash, "txt");
              FileOutputStream fileOutputStream = new FileOutputStream(file);
              fileOutputStream.write(bytes);
              fileOutputStream.close();
              client.get().putObject(bucket.get(), "state/" + hash + ".json", file);
              file.delete();
            } catch (Exception e) {
              Logs.logError(logger, "S3 source upload failed", e);
            }
          }
        );
      } catch (Exception e) {
        Logs.logError(logger, "Source hash failed", e);
      }
    }

    Logs.logData(logger, Level.INFO, "core.responses", data);
  }
}
