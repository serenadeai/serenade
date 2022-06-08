package core.codeengine;

import com.google.common.base.MoreObjects;
import core.ast.api.AstParent;
import core.util.Contractions;
import core.util.Range;
import core.util.TextStyler;
import java.lang.StringBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Tokenizer {

  public abstract static class Token {

    public abstract String modelCodeRepresentation();

    public abstract String originalCode();

    @Override
    public boolean equals(Object o) {
      if (o == null || !(o instanceof Token)) {
        return false;
      }
      Token m = (Token) o;

      return m.modelCodeRepresentation().equals(this.modelCodeRepresentation());
    }

    @Override
    public int hashCode() {
      return this.modelCodeRepresentation().hashCode();
    }
  }

  public abstract static class EmptyToken extends Token {

    @Override
    public String originalCode() {
      return "";
    }
  }

  public abstract static class CodeToken extends Token {

    public final String source;
    public final Range range;

    public CodeToken(String source, Range range) {
      this.source = source;
      this.range = range;
    }

    @Override
    public String originalCode() {
      return source.substring(range.start, range.stop);
    }

    @Override
    public String toString() {
      return MoreObjects
        .toStringHelper(this)
        .add("code", originalCode())
        .add("range", range)
        .toString();
    }
  }

  public static class SymbolToken extends CodeToken {

    public SymbolToken(String source, Range range) {
      super(source, range);
    }

    @Override
    public String modelCodeRepresentation() {
      return originalCode();
    }
  }

  public static class NumberToken extends CodeToken {

    public NumberToken(String source, Range range) {
      super(source, range);
    }

    @Override
    public String modelCodeRepresentation() {
      return originalCode()
        .chars()
        .mapToObj(c -> Character.toString((char) c))
        .collect(Collectors.joining(" "));
    }
  }

  public static class AlphaToken extends CodeToken {

    public final AlphaStyle style;

    public AlphaToken(String source, Range range, AlphaStyle style) {
      super(source, range);
      this.style = style;
    }

    public String word() {
      return originalCode().toLowerCase();
    }

    @Override
    public String modelCodeRepresentation() {
      if (style == AlphaStyle.CAPS) {
        return "A " + word();
      } else if (style == AlphaStyle.CAPITAL) {
        return "C " + word();
      }
      return word();
    }
  }

  public static class SpaceToken extends CodeToken {

    public SpaceToken(String source, Range range) {
      super(source, range);
    }

    @Override
    public String modelCodeRepresentation() {
      return "SP";
    }
  }

  public static class AlphaSubsequenceDelimToken extends EmptyToken {

    @Override
    public String modelCodeRepresentation() {
      return "ASD";
    }
  }

  public static class CursorToken extends EmptyToken {

    @Override
    public String modelCodeRepresentation() {
      return "CRSR";
    }
  }

  public static class IndentToken extends EmptyToken {

    @Override
    public String modelCodeRepresentation() {
      return "I";
    }
  }

  public static class DedentToken extends EmptyToken {

    @Override
    public String modelCodeRepresentation() {
      return "D";
    }
  }

  public static class NewlineToken extends EmptyToken {

    @Override
    public String modelCodeRepresentation() {
      return "NL";
    }
  }

  public static class ContextStartToken extends EmptyToken {

    @Override
    public String modelCodeRepresentation() {
      return "CTX";
    }
  }

  public static class EnglishStartToken extends EmptyToken {

    @Override
    public String modelCodeRepresentation() {
      return "ENG";
    }
  }

  public static class SnippetContainerToken extends EmptyToken {

    String container;

    public SnippetContainerToken(String container) {
      this.container = container;
    }

    @Override
    public String modelCodeRepresentation() {
      return "SNIP_" + container;
    }
  }

  Pattern capsPascal = Pattern.compile("^([A-Z]+)[A-Z][a-z]");
  Pattern caps = Pattern.compile("^[A-Z]+");
  Pattern capital;
  Pattern indentationPattern = Pattern.compile("^\\s*");
  Pattern lowerCase;
  Pattern number = Pattern.compile("^[0-9]+");
  Pattern spaces = Pattern.compile("^\\s+");
  Pattern symbol = Pattern.compile("^[^A-Za-z0-9]");
  Contractions contractions;
  Escaper escaper;
  Resolver resolver;
  TextStyler textStyler = new TextStyler();

  @Inject
  public Tokenizer(Contractions contractions, Escaper escaper, Resolver resolver) {
    this.escaper = escaper;
    this.resolver = resolver;

    // sort in reverse order so that we get the longest match.
    List<String> sortedContractions = new ArrayList<String>(contractions.all);
    Collections.sort(sortedContractions, Collections.reverseOrder());

    lowerCase =
      Pattern.compile(
        "^(" +
        sortedContractions
          .stream()
          .map(s -> Character.toLowerCase(s.charAt(0)) + s.substring(1))
          .collect(Collectors.joining("|")) +
        ")\\b|^[a-z]+"
      );
    capital =
      Pattern.compile(
        "^((" +
        sortedContractions
          .stream()
          .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1))
          .collect(Collectors.joining("|")) +
        ")\\b|^[A-Z][a-z]+)"
      );
  }

  public List<Token> tokenize(String source) {
    source.replaceAll("\t", "    ");
    String[] lines = source.split("\n", -1);
    String previousIndentation = "";
    List<Token> tokens = new ArrayList<>();
    int position = 0;
    for (int i = 0; i < lines.length; i++) {
      // don't strip last line so that we can complete it.
      if (i != lines.length - 1 && lines[i].trim().equals("")) {
        position += lines[i].length() + 1;
        tokens.add(new NewlineToken());
        continue;
      }
      Matcher m = indentationPattern.matcher(lines[i]);
      m.find();
      String currentIndentation = m.group(0);
      Token indentationModifier;
      if (currentIndentation.length() > previousIndentation.length()) {
        indentationModifier = new IndentToken();
      } else if (currentIndentation.length() < previousIndentation.length()) {
        indentationModifier = new DedentToken();
      } else {
        indentationModifier = new NewlineToken();
      }
      previousIndentation = currentIndentation;
      tokens.add(indentationModifier);
      tokens.addAll(tokenizeLine(source, position, lines[i], i == lines.length - 1));
      position += lines[i].length() + 1;
    }
    return tokens;
  }

  public List<Token> tokenizeSourcePrefix(String source) {
    return tokenize(source);
  }

  private List<Token> tokenizeLine(String source, int position, String line, boolean lastLine) {
    List<Token> ret = new ArrayList<Token>();
    String remaining = line;
    int lineStart = position;
    while (remaining.length() > 0) {
      Matcher m;
      int consumed = 0;
      if ((m = spaces.matcher(remaining)).find()) {
        consumed = m.group(0).length();
        // Strip leading/trailing whitespace (indentation handled elsewhere).
        // Don't strip trailing if it's the last line, so that during inference time
        // we can predict after a space before the cursor.
        if (position != lineStart && (lastLine || consumed != remaining.length())) {
          for (int i = 0; i < consumed; i++) {
            ret.add(new SpaceToken(source, new Range(position + i, position + i + 1)));
          }
        }
      } else if ((m = symbol.matcher(remaining)).find()) {
        consumed = m.group(0).length();
        ret.add(new SymbolToken(source, new Range(position, position + consumed)));
      } else if ((m = number.matcher(remaining)).find()) {
        consumed = m.group(0).length();
        ret.add(new NumberToken(source, new Range(position, position + consumed)));
      } else {
        AlphaStyle style;
        if ((m = lowerCase.matcher(remaining)).find()) {
          style = AlphaStyle.LOWERCASE;
          consumed = m.group(0).length();
        } else if ((m = capital.matcher(remaining)).find()) {
          style = AlphaStyle.CAPITAL;
          consumed = m.group(0).length();
        } else if ((m = capsPascal.matcher(remaining)).find()) {
          style = AlphaStyle.CAPS;
          consumed = m.group(1).length();
        } else {
          (m = caps.matcher(remaining)).find();
          style = AlphaStyle.CAPS;
          consumed = m.group(0).length();
        }
        ret.add(new AlphaToken(source, new Range(position, position + consumed), style));
      }
      remaining = remaining.substring(consumed);
      position += consumed;
    }
    return ret;
  }

  // We don't bother with indentation since we don't parse lines with it and it needs
  // extra context to be passed in.
  public String decodeModelCodeRepresentation(String line) {
    List<String> tokens = Arrays
      .asList(line.split("\\s+"))
      .stream()
      .map(word -> escaper.unescapeWord(word))
      .collect(Collectors.toList());
    StringBuilder sb = new StringBuilder();
    int i = 0;
    int indentationLevel = 0;
    while (i < tokens.size()) {
      if (tokens.get(i).equals("SP")) {
        sb.append(" ");
      } else if (
        tokens.get(i).equals("NL") || tokens.get(i).equals("D") || tokens.get(i).equals("I")
      ) {
        if (tokens.get(i).equals("D")) {
          indentationLevel--;
        } else if (tokens.get(i).equals("I")) {
          indentationLevel++;
        }
        sb.append("\n");
        for (int j = 0; j < indentationLevel; j++) {
          sb.append(resolver.wrapInSlot(resolver.indentOverride));
        }
      } else if (tokens.get(i).equals("CRSR")) {
        sb.append(resolver.wrapInSlot(resolver.cursorOverride));
      } else if (tokens.get(i).equals("A") && i < tokens.size() - 1) {
        sb.append(textStyler.toAllCaps(tokens.get(i + 1)));
        i++;
      } else if (tokens.get(i).equals("C") && i < tokens.size() - 1) {
        sb.append(textStyler.toCapitalized(tokens.get(i + 1)));
        i++;
      } else {
        sb.append(tokens.get(i));
      }
      i++;
    }
    return sb.toString();
  }
}
