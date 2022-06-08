package toolbelt.env;

import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Env {

  @Inject
  public Env() {}

  public boolean docker() {
    return Optional.ofNullable(System.getenv("ENV")).orElse("").contains("docker");
  }

  public boolean local() {
    return Optional.ofNullable(System.getenv("ENV")).orElse("").contains("local");
  }

  public String libraryRoot() {
    return (
      System.getenv("SERENADE_LIBRARY_ROOT") != null
        ? System.getenv("SERENADE_LIBRARY_ROOT")
        : System.getProperty("user.home") + "/libserenade"
    );
  }

  public String sourceRoot() {
    return (
      System.getenv("SERENADE_SOURCE_ROOT") != null
        ? System.getenv("SERENADE_SOURCE_ROOT")
        : System.getProperty("user.home") + "/serenade"
    );
  }
}
