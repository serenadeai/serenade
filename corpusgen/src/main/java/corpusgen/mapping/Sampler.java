package corpusgen.mapping;

import core.codeengine.Tokenizer;
import core.gen.rpc.Language;
import core.util.Range;
import core.util.TextStyle;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class Sampler {

  private Config config;
  private Random random;

  private boolean probablyIncludeSpaces;
  private boolean implicitSymbolsEnabled;
  private boolean explicitSymbolBias;
  private boolean titleCaseAsCapitals;
  private boolean inEnclosuresAsPostfixes;

  @AssistedInject
  public Sampler(Random random, @Assisted Config config) {
    this.random = random;
    this.config = config;

    probablyIncludeSpaces = random.bool(config.language == Language.LANGUAGE_DEFAULT ? 0.01 : 0.1);
    implicitSymbolsEnabled = random.bool(0.02);
    explicitSymbolBias = random.bool(0.5);
    titleCaseAsCapitals = random.bool(0.5);
    inEnclosuresAsPostfixes = random.bool(0.1);
  }

  @AssistedFactory
  public interface Factory {
    Sampler create(Config config);
  }

  public boolean explicitSpaces(boolean multiple) {
    if (multiple) {
      // These are more likely to be said explicitly because they're so rare.
      // Also, because they're rare, we want to make sure they're sampled often.
      return random.bool(0.4);
    }
    return probablyIncludeSpaces && random.bool(0.67);
  }

  public boolean explicitSymbol(String symbol) {
    return (
      !implicitSymbolsEnabled || symbol.length() > 1 || (explicitSymbolBias == random.bool(0.85))
    );
  }

  public boolean explicitEnclosures(String enclosureStart) {
    return (
      !implicitSymbolsEnabled ||
      enclosureStart.length() > 1 ||
      (explicitSymbolBias == random.bool(0.85))
    );
  }

  public boolean explicitStyle(boolean insideText, TextStyle style) {
    double proportion = 0.4;
    if (config.language == Language.LANGUAGE_DEFAULT) {
      // Let's be less opinionated with our default model since we tested this with
      // 0 by accident, so this is less of a change. It also might make sense
      // since it's already skewed heavily towards english formatting. We may also want to
      // revisit these proportions for other config.languages. It's possible that we thought
      // we needed more skew because of some bugs in the system.
      proportion = 0.1;
    }
    if (style == TextStyle.LOWERCASE) {
      proportion = 0.005;
    } else if (insideText) {
      // don't make any other style implicit.
    } else if (config.language == Language.LANGUAGE_PYTHON && style == TextStyle.UNDERSCORES) {
      proportion = 0.1;
    } else if (
      (
        config.language == Language.LANGUAGE_JAVASCRIPT || config.language == Language.LANGUAGE_JAVA
      ) &&
      style == TextStyle.CAMEL_CASE
    ) {
      proportion = 0.1;
    }
    return random.bool(proportion);
  }

  public boolean inEnclosureAsPostfix() {
    return inEnclosuresAsPostfixes;
  }

  public boolean titleCaseAsCapitals() {
    return titleCaseAsCapitals;
  }

  public boolean saySnippet() {
    return random.bool(0.8);
  }

  public boolean splitPlural() {
    return random.bool(0.1);
  }

  public boolean stop() {
    return random.bool(0.07);
  }

  public boolean stopSections() {
    return random.bool(0.2);
  }

  public boolean stripInFromEnclosure(boolean empty) {
    if (empty) {
      return random.bool(0.02);
    }
    return random.bool(0.2);
  }

  public int stopIndex(Range range) {
    int index = range.start;
    while (!stop() && index < range.stop) {
      index++;
    }
    return index;
  }
}
