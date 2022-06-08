package corpusgen.mapping;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.inject.Inject;

public class Random {

  @Inject
  public Random() {}

  public <T> T element(List<T> elements) {
    return elements.get(ThreadLocalRandom.current().nextInt(elements.size()));
  }

  public boolean bool(double proportion) {
    return ThreadLocalRandom.current().nextDouble() < proportion;
  }
}
