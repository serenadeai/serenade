package corpusgen.util;

import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Symbols {

  @Inject
  public Symbols() {}

  public String stripNonAscii(String input) {
    StringBuilder builder = new StringBuilder(input);
    for (int i = input.length() - 1; i >= 0; i--) {
      Character c = input.charAt(i);
      // Delete non-ascii characters and special ascii characters Delete \r but not \n, since
      // the former is stripped by NewlineNormalizer in core.
      if (c != 10 && (c <= 31 || c >= 127)) {
        builder.deleteCharAt(i);
      }
    }
    return builder.toString();
  }
}
