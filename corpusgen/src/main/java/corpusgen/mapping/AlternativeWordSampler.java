package corpusgen.mapping;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AlternativeWordSampler {

  @Inject
  public AlternativeWordSampler() {}

  private static HashSet<String> acronyms;

  static {
    try {
      acronyms =
        new HashSet<String>(
          Arrays.asList(
            Resources.toString(Resources.getResource("acronyms.txt"), Charsets.UTF_8).split("\n")
          )
        );
    } catch (IOException e) {
      throw new RuntimeException("Error loading acronyms.txt", e);
    }
  }

  public String sample(String word) {
    if (word.length() < 10 && ThreadLocalRandom.current().nextInt(1, 41) == 1) {
      StringBuilder sb = new StringBuilder();
      for (char c : word.toCharArray()) {
        if (!Character.isLetter(c)) {
          return word;
        }
      }
      if (ThreadLocalRandom.current().nextInt(1, 3) == 1) {
        for (char c : word.toCharArray()) {
          sb.append(c);
          sb.append("(spell) ");
        }
      } else {
        // sample an acronym with random letters and length.
        int length = ThreadLocalRandom.current().nextInt(1, 11);
        for (int j = 0; j < length; j++) {
          sb.append((char) (ThreadLocalRandom.current().nextInt(0, 26) + 97));
          sb.append("(spell) ");
        }
      }
      return sb.toString().trim();
    } else if (ThreadLocalRandom.current().nextInt(2000) == 0) {
      return "serenade";
    } else if (acronyms.contains(word)) {
      // Finally check if the word is an acronym. If so, with some probability, use acronym version
      // or spell it out.
      int randInt = ThreadLocalRandom.current().nextInt(1, 11);
      if (randInt >= 8) {
        // Spell it out
        StringBuilder sb = new StringBuilder();
        for (char c : word.toCharArray()) {
          sb.append(c);
          sb.append("(spell) ");
        }
      } else if (randInt == 7) {
        return word;
      } else {
        // Append (spell) to acronym as the representation
        return word + "(spell)";
      }
    }
    return word;
  }
}
