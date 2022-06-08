package toolbelt.client;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Parser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import toolbelt.exception.HostNotFoundException;

@Singleton
public class ServiceHttpClient {

  public static enum Service {
    Auth("AUTH"),
    Speech("SPEECH_ENGINE"),
    Core("CORE"),
    CodeEngine("CODE_ENGINE");

    private final String value;

    Service(String value) {
      this.value = value;
    }

    public String toString() {
      return value;
    }
  }

  private HttpClient httpClient = HttpClient
    .newBuilder()
    .connectTimeout(Duration.ofSeconds(30))
    .build();

  @Inject
  public ServiceHttpClient() {}

  private String getUrl(Service service, String path) {
    String host = System.getenv(service + "_HOST");
    if (host == null) {
      throw new HostNotFoundException(service.toString());
    }

    return (
      (System.getenv(service + "_SCHEME") != null ? System.getenv(service + "_SCHEME") : "http") +
      "://" +
      host +
      (System.getenv(service + "_PORT") != null ? ":" + System.getenv(service + "_PORT") : "") +
      path
    );
  }

  private HttpRequest newRequest(String url, GeneratedMessageV3 data) {
    return HttpRequest
      .newBuilder()
      .POST(HttpRequest.BodyPublishers.ofByteArray(data.toByteArray()))
      .uri(URI.create(url))
      .header("Content-Type", "application/octet-stream")
      .timeout(Duration.ofSeconds(30))
      .version(HttpClient.Version.HTTP_1_1)
      .build();
  }

  public <ResponseType extends GeneratedMessageV3> CompletableFuture<ResponseType> post(
    String url,
    GeneratedMessageV3 data,
    Parser<ResponseType> parser
  ) {
    return httpClient
      .sendAsync(newRequest(url, data), HttpResponse.BodyHandlers.ofByteArray())
      .thenApply(
        response -> {
          try {
            if (response.statusCode() != 200) {
              throw new ServiceHttpException(url, response.statusCode());
            }

            return parser.parseFrom(response.body());
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
      );
  }

  public <ResponseType extends GeneratedMessageV3> CompletableFuture<ResponseType> post(
    Service service,
    String path,
    GeneratedMessageV3 data,
    Parser<ResponseType> parser
  ) {
    return post(getUrl(service, path), data, parser);
  }

  public <ResponseType extends GeneratedMessageV3> ResponseType postBlocking(
    String url,
    GeneratedMessageV3 data,
    Parser<ResponseType> parser
  ) {
    try {
      HttpResponse<byte[]> response = httpClient.send(
        newRequest(url, data),
        HttpResponse.BodyHandlers.ofByteArray()
      );

      if (response.statusCode() != 200) {
        throw new ServiceHttpException(url, response.statusCode());
      }

      return parser.parseFrom(response.body());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public <ResponseType extends GeneratedMessageV3> ResponseType postBlocking(
    Service service,
    String path,
    GeneratedMessageV3 data,
    Parser<ResponseType> parser
  ) {
    return postBlocking(getUrl(service, path), data, parser);
  }
}
