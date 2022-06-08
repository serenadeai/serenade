package core.util;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FillerWords {

  public final List<String> all = Arrays.asList("um", "uh");

  @Inject
  public FillerWords() {}

  public String sample() {
    return all.get(ThreadLocalRandom.current().nextInt(all.size()));
  }

  public String strip(String input) {
    return Arrays
      .asList(input.split("\\s"))
      .stream()
      .filter(w -> !all.contains(w))
      .collect(Collectors.joining(" "));
  }
}
