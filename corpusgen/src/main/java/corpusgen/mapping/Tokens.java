package corpusgen.mapping;

import core.codeengine.Tokenizer;
import core.util.Range;
import java.util.List;

public class Tokens {

  public final List<Tokenizer.Token> list;

  public Tokens(List<Tokenizer.Token> list) {
    this.list = list;
  }

  public int tokenPosition(int position) {
    return tokenRange(new Range(position, position)).start;
  }

  // Converts an AstToken character range to Corpusgen tokenizer range
  public Range tokenRange(Range sourceRange) {
    Range tokenRange = new Range(-1, -1);
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i) instanceof Tokenizer.CodeToken) {
        Tokenizer.CodeToken codeToken = (Tokenizer.CodeToken) list.get(i);
        if (sourceRange.start == codeToken.range.start) {
          tokenRange.start = i;
        }
        if (sourceRange.stop == codeToken.range.stop) {
          tokenRange.stop = i + 1;
        }
      }
    }
    // Found a shared stop, but not a shared start. Must be an empty range.
    if (tokenRange.start == -1) {
      tokenRange.start = tokenRange.stop;
    }
    // Found a shared start, but not a shared stop. Must be an empty range.
    if (tokenRange.stop == -1) {
      tokenRange.stop = tokenRange.start;
    }
    return tokenRange;
  }
}
