package core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Whitespace {

  Pattern pattern = Pattern.compile("\\s*");

  @Inject
  public Whitespace() {}

  public Range strip(String source, Range range) {
    return leftStrip(source, rightStrip(source, range));
  }

  public boolean followedByNewline(String source, Range range) {
    Range expanded = expandRight(source, range);
    return source.substring(range.stop, expanded.stop).indexOf('\n') >= 0;
  }

  public boolean precededByNewline(String source, Range range) {
    Range expanded = expandLeftToIncludeNewline(source, range);
    return source.charAt(expanded.start) == '\n';
  }

  public String indentationAtCursor(String source, int cursor) {
    int start = lineStart(source, cursor);
    int lineNonWhitespaceStart = lineNonWhitespaceStart(source, start);

    String indentation = "";
    for (int i = start; i < lineNonWhitespaceStart; i++) {
      indentation += source.charAt(i);
    }

    return indentation;
  }

  public int indentationLevelAtCursor(String source, int cursor, int defaultLevel) {
    String token = indentationToken(source, defaultLevel);
    if (token.length() == 0) {
      return 0;
    }

    cursor = Math.min(Math.max(cursor, 0), source.length() - 1);
    int i = cursor;
    for (; i >= 0; i--) {
      if (source.charAt(i) == '\n' || source.charAt(i) == '\r') {
        break;
      }
    }

    i++;
    if (token.equals("\t")) {
      return cursor - i;
    }

    return (cursor - i) / token.length();
  }

  public String indentationToken(String source, int defaultLevel) {
    // strip comments and multi-line strings (using some common delimiters)
    source = Comments.strip(source);

    // look at the number of spaces preceding each line, and create a map of counts
    int level = Integer.MAX_VALUE;
    String[] lines = source.split("\n");
    Map<Integer, Integer> indentationCounts = new HashMap<>();
    for (String line : lines) {
      // skip blank lines
      if (line.length() == 0) {
        continue;
      }

      // if line starts with a tab, we must be using tabs
      if (line.charAt(0) == '\t') {
        return "\t";
      }

      int indentation = 0;
      for (; indentation < line.length(); indentation++) {
        if (line.charAt(indentation) != ' ') {
          break;
        }
      }

      if (indentation > 0) {
        indentationCounts.put(indentation, indentationCounts.getOrDefault(indentation, 0) + 1);
      }
    }

    // as a heuristic, use the smallest amount of whitespace preceding a line, but only if at least 10% of the lines
    // in the file start with that token
    if (!indentationCounts.isEmpty()) {
      int total = indentationCounts.values().stream().reduce(0, Integer::sum);
      int minimum = Collections.min(indentationCounts.keySet());
      if ((float) indentationCounts.get(minimum) / (float) total > 0.1) {
        level = minimum;
      }
    }

    // if we're not confident in the indentation, then use the given default
    if (level == Integer.MAX_VALUE) {
      level = defaultLevel;
    }

    // create indentation from spaces
    String result = "";
    for (int i = 0; i < level; i++) {
      result += " ";
    }

    return result;
  }

  public String indentationForLevel(String source, int level, int defaultLevel) {
    String result = "";
    String token = indentationToken(source, defaultLevel);
    for (int i = 0; i < level; i++) {
      result += token;
    }

    return result;
  }

  public boolean isWhitespace(char c) {
    return c == ' ' || c == '\n' || c == '\t' || c == '\r';
  }

  public boolean isWhitespace(String source) {
    return pattern.matcher(source).matches();
  }

  public boolean isWhitespace(String source, Range range) {
    return isWhitespace(source.substring(range.start, range.stop));
  }

  public Range expandLeft(String source, Range range) {
    Range ret = new Range(range.start, range.stop);
    while (ret.start - 1 >= 0) {
      char c = source.charAt(ret.start - 1);
      if (!isWhitespace(c)) {
        break;
      }

      ret.start--;
    }

    return ret;
  }

  public Range expandRight(String source, Range range) {
    Range ret = new Range(range.start, range.stop);
    while (ret.stop < source.length()) {
      char c = source.charAt(ret.stop);
      if (!isWhitespace(c)) {
        break;
      }

      ret.stop++;
    }

    return ret;
  }

  public Range leftStrip(String source, Range range) {
    Range ret = new Range(range.start, range.stop);

    // look forward until we find a non-whitespace character
    while (ret.start < ret.stop) {
      char c = source.charAt(ret.start);
      if (!isWhitespace(c)) {
        break;
      }

      ret.start++;
    }

    return ret;
  }

  public Range rightStrip(String source, Range range) {
    Range ret = new Range(range.start, range.stop);
    while (ret.stop > ret.start) {
      char c = source.charAt(ret.stop - 1);
      if (!isWhitespace(c)) {
        break;
      }

      ret.stop--;
    }

    return ret;
  }

  public Range expandLeftToIncludeNewline(String source, Range range) {
    Range result = new Range(range.start, range.stop);
    while (result.start > 0) {
      char c = source.charAt(result.start - 1);
      if (!isWhitespace(c)) {
        break;
      }

      result.start--;
      if (c == '\n') {
        break;
      }
    }

    return result;
  }

  public Range includeAdjacentIndentation(String source, Range range) {
    // include indentation if there is any
    Range expandedRange = expandLeftToIncludeNewline(source, range);
    if (expandedRange.start == 0) {
      // Special case for first line in file.
      range = new Range(0, expandedRange.stop);
    } else if (source.charAt(expandedRange.start) == '\n') {
      range = new Range(expandedRange.start + 1, expandedRange.stop);
    }

    return range;
  }

  public String stripIndentation(String source, Range range) {
    int lineNonWhitespaceStart = lineNonWhitespaceStart(source, range.start);
    int lineStart = lineStart(source, range.start);
    String s = source.substring(range.start, range.stop);

    String[] lines = s.split("\n", -1);
    if (lines.length == 0) {
      return "";
    }

    // remove the indentation of the first line from subsequent lines.
    String token = source.substring(lineStart, lineNonWhitespaceStart);
    String result = "";
    for (String line : lines) {
      line = line.replaceFirst("^" + token, "");
      result += line + "\n";
    }

    // remove the new line that we just added.
    return result.substring(0, result.length() - 1);
  }

  // TODO:unit test the new functions in this file
  public int lineStart(String source, int position) {
    // note that because of the -1 return value, this works on the first line of a file.
    return previousNewline(source, position) + 1;
  }

  public int lineEnd(String source, int position) {
    int ret = nextNewline(source, position);
    if (ret < 0) {
      return source.length();
    }

    return ret;
  }

  public int previousNewline(String source, int position) {
    int index = position - 1;
    while (index >= 0 && source.charAt(index) != '\n') {
      index--;
    }

    return index;
  }

  public int nextNewline(String source, int position) {
    while (position < source.length() && source.charAt(position) != '\n') {
      position++;
    }

    return position;
  }

  public int nextNonWhitespace(String s, int position) {
    int i;
    for (i = position; i < s.length(); i++) {
      if (!isWhitespace(s.charAt(i))) {
        break;
      }
    }

    return i;
  }

  public int lineNonWhitespaceStart(String source, int cursor) {
    int position = lineStart(source, cursor);
    return Math.min(lineEnd(source, cursor), nextNonWhitespace(source, position));
  }

  public List<Range> nonWhitespaceRanges(String source) {
    List<Range> ranges = new ArrayList<>();
    boolean previousWasWhitespace = true;
    int start = 0;
    for (int i = 0; i < source.length(); i++) {
      boolean isWhitespace = isWhitespace(source.charAt(i));
      if (previousWasWhitespace && !isWhitespace) {
        start = i;
      } else if (!previousWasWhitespace && isWhitespace) {
        ranges.add(new Range(start, i));
      }

      previousWasWhitespace = isWhitespace;
    }

    if (!previousWasWhitespace) {
      ranges.add(new Range(start, source.length()));
    }

    return ranges;
  }

  public Range lineFromIndex(String source, int index) {
    // using 0-indexing
    int start = 0;
    int currentIndex = 0;
    for (; start < source.length() && currentIndex != index; start++) {
      if (start > 0 && source.charAt(start - 1) == '\n') {
        currentIndex++;
      }
    }

    return new Range(start, lineEnd(source, start));
  }

  public List<Range> lineRanges(String source, Range range) {
    List<Range> ranges = new ArrayList<>();
    int start = range.start;
    for (int i = range.start; i <= range.stop; i++) {
      if (i == range.stop || source.charAt(i) == '\n') {
        ranges.add(new Range(start, i));
        start = i + 1;
      }
    }

    return ranges;
  }
}
