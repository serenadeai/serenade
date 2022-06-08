package core.formattedtext;

import core.gen.rpc.Language;
import core.util.Contractions;
import core.util.NumberConverter;
import core.util.Range;
import core.util.TextStyle;
import core.util.TextStyler;
import core.util.Whitespace;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FormattedTextConverter {

  public abstract class Node {

    public List<String> tokens;

    public Node(List<String> tokens) {
      this.tokens = tokens;
    }
  }

  public class AlphaNode extends Node {

    public AlphaNode(List<String> tokens) {
      super(tokens);
    }
  }

  public class NumberNode extends Node {

    public NumberNode(List<String> tokens) {
      super(tokens);
    }
  }

  public class OneWordNode extends Node {

    public OneWordNode(List<String> tokens) {
      super(tokens);
    }
  }

  public class RecursiveNode extends Node {

    public Optional<TextStyle> style = Optional.empty();

    public RecursiveNode(List<String> tokens) {
      super(tokens);
      if (tokens.contains("quotes") || tokens.contains("quotations")) {
        this.style = Optional.of(TextStyle.LOWERCASE);
      }
    }
  }

  public class StyleNode extends Node {

    public StyleNode(List<String> tokens) {
      super(tokens);
    }
  }

  public class SymbolNode extends Node {

    public SymbolNode(List<String> tokens) {
      super(tokens);
    }
  }

  public class ConversionState {

    private List<Node> input;
    private ConversionMap conversionMap;
    private FormattedTextOptions options;

    private int index = 0;
    private String result = "";

    public ConversionState(
      List<Node> input,
      FormattedTextOptions options,
      ConversionMap conversionMap
    ) {
      this.input = input;
      this.options = options;
      this.conversionMap = conversionMap;
    }

    private void consumeStyled(TextStyle style) {
      boolean oneWord = false;
      boolean stuckNumberLeft = false;
      String unstyled = "";

      while (index < input.size()) {
        if (input.get(index) instanceof OneWordNode) {
          oneWord = true;
        } else if (
          input.get(index) instanceof AlphaNode || input.get(index) instanceof NumberNode
        ) {
          // stick numbers left, otherwise stick them right, with some hardcoded exceptions.
          if (unstyled.equals("")) {} else if (
            input.get(index) instanceof NumberNode && input.get(index - 1) instanceof AlphaNode
          ) {
            stuckNumberLeft = true;
          } else if (
            !(input.get(index) instanceof NumberNode) &&
            (input.get(index - 1) instanceof NumberNode) &&
            !stuckNumberLeft
          ) {
            stuckNumberLeft = false;
          } else if (!oneWord) {
            unstyled += " ";
            stuckNumberLeft = false;
          }

          unstyled += input.get(index).tokens.get(0);
        } else {
          break;
        }

        index++;
      }

      if (style == TextStyle.LOWERCASE) {
        // preserve captialization of contractions.
        result += unstyled;
      } else {
        result += textStyler.style(unstyled, style);
      }
    }

    public String convert() {
      while (index < input.size()) {
        Node current = input.get(index);
        if (current instanceof StyleNode) {
          if (index > 0 && input.get(index - 1) instanceof AlphaNode) {
            result += " ";
          }

          index++;
          consumeStyled(conversionMap.styleMap.get(current.tokens));
        } else if (current instanceof RecursiveNode) {
          RecursiveNode node = (RecursiveNode) current;
          String inner = new ConversionState(
            input.subList(index + 1, input.size()),
            FormattedTextOptions
              .newBuilder(options)
              .setStyle(node.style.orElse(options.style))
              .build(),
            conversionMap
          )
            .convert();

          return result + conversionMap.recursiveMap.get(current.tokens).apply(inner);
        } else if (current instanceof SymbolNode) {
          result += convertSymbol((SymbolNode) current, conversionMap);
          index++;
        } else {
          consumeStyled(options.style);
        }
      }

      return result;
    }
  }

  public class ParserState {

    private List<String> input;
    private ConversionMap conversionMap;
    private FormattedTextOptions options;

    private int index = 0;
    private List<Node> result = new ArrayList<>();

    public ParserState(String input, FormattedTextOptions options, ConversionMap conversionMap) {
      // Apostrophes and other symbols come through delimited with spaces, and everything
      // is lower case. Undo that for contractions.
      Matcher m = contractions.contractionPattern.matcher(input);
      int lastIndex = 0;
      String inputWithContractions = "";
      while (m.find()) {
        inputWithContractions += input.substring(lastIndex, m.start());
        inputWithContractions += contractions.contractionMatchToFormatted.get(m.group());
        lastIndex = m.end();
      }
      inputWithContractions += input.substring(lastIndex);

      this.input = Arrays.asList(inputWithContractions.split(" "));
      this.options = options;
      this.conversionMap = conversionMap;
      this.index = 0;
    }

    public boolean consumeEscaped() {
      Optional<List<String>> prefix = longestPrefix(conversionMap.escapePrefixes);
      if (prefix.isPresent() && input.size() > prefix.get().size()) {
        index += prefix.get().size();
        result.add(new AlphaNode(input.subList(index, index + 1)));
        index++;
        return true;
      }

      return false;
    }

    public boolean consumeNumber() {
      Optional<List<String>> prefix = longestPrefix(conversionMap.numeralPrefixes);

      if (prefix.isPresent() && input.size() > prefix.get().size()) {
        int numberIndex = prefix.get().size();
        if (numberIndex < input.size()) {
          String number = input.get(numberIndex);
          if (number.matches("[0-9]+")) {
            result.add(new NumberNode(Arrays.asList(number)));
            index = numberIndex + 1;
            return true;
          } else if (number.equals("to")) {
            result.add(new NumberNode(Arrays.asList("2")));
            index = numberIndex + 1;
            return true;
          } else if (number.equals("for")) {
            result.add(new NumberNode(Arrays.asList("4")));
            index = numberIndex + 1;
            return true;
          }
        }
      } else if (input.get(index).matches("[0-9]+")) {
        result.add(new NumberNode(input.subList(index, index + 1)));
        index += 1;
        return true;
      }

      return false;
    }

    public boolean consumeText() {
      return (
        consumeAlphaPrefix(conversionMap.oneWordPrefixes, t -> new OneWordNode(t)) ||
        consumeNumber() ||
        consumeAlpha()
      );
    }

    public boolean consumeAlphaPrefix(
      List<List<String>> prefixes,
      Function<List<String>, ? extends Node> createNode
    ) {
      Optional<List<String>> prefix = longestPrefix(prefixes);
      if (prefix.isPresent() && index + prefix.get().size() < input.size()) {
        result.add(createNode.apply(prefix.get()));
        index += prefix.get().size();
        if (!consumeEscaped()) {
          consumeText();
        }

        return true;
      }

      return false;
    }

    public boolean consumePrefixAndPostfix(
      List<List<String>> prefixes,
      Function<List<String>, ? extends Node> createNode,
      Optional<String> postfix
    ) {
      Optional<List<String>> prefix = longestPrefix(prefixes);
      if (prefix.isPresent()) {
        boolean matchesPostfix = false;
        if (postfix.isPresent()) {
          matchesPostfix =
            input.stream().skip(index + prefix.get().size()).findFirst().equals(postfix);
          if (!matchesPostfix) {
            return false;
          }
        }

        result.add(createNode.apply(prefix.get()));
        index += prefix.get().size();
        if (matchesPostfix) {
          index++;
        }

        return true;
      }

      return false;
    }

    public boolean consumeSymbolCharacter() {
      if (isSymbolCharacter(input.get(index))) {
        result.add(new SymbolNode(input.subList(index, index + 1)));
        index += 1;
        return true;
      }

      return false;
    }

    public boolean consumeAlpha() {
      List<String> word = input.subList(index, index + 1);
      result.add(new AlphaNode(word));
      index += 1;
      return true;
    }

    private Optional<List<String>> longestPrefix(List<List<String>> prefixes) {
      List<String> remaining = input.subList(index, input.size());
      Optional<List<String>> result = prefixes
        .stream()
        .filter(p -> remaining.size() >= p.size() && remaining.subList(0, p.size()).equals(p))
        .findFirst();

      return result;
    }

    public List<Node> parse() {
      while (
        index < input.size() &&
        (
          consumeEscaped() ||
          consumePrefixAndPostfix(
            conversionMap.symbolPrefixes,
            t -> new SymbolNode(t),
            Optional.of("sign")
          ) ||
          consumePrefixAndPostfix(
            conversionMap.symbolPrefixes,
            t -> new SymbolNode(t),
            Optional.of("symbol")
          ) ||
          consumePrefixAndPostfix(
            conversionMap.symbolPrefixes,
            t -> new SymbolNode(t),
            Optional.empty()
          ) ||
          consumeSymbolCharacter() ||
          consumeAlphaPrefix(conversionMap.stylePrefixes, t -> new StyleNode(t)) ||
          consumePrefixAndPostfix(
            conversionMap.recursivePrefixes,
            t -> new RecursiveNode(t),
            Optional.empty()
          ) ||
          consumeText()
        )
      ) {}
      return result;
    }
  }

  private String startBoundary = "(^|[a-zA-Z0-9_\\)\\]\\$])";
  private String endBoundary = "([a-zA-Z0-9_\\(\\)\\[\\]\\{\\}'\"\\-\\+])";
  private Pattern typeTokenPattern = Pattern.compile("^[A-Z_][a-z_]+(?:[A-Z][a-z]+)*$");
  private Pattern tokenPattern = Pattern.compile("^[a-zA-Z0-9_\\$\\-]+$");

  @Inject
  Contractions contractions;

  @Inject
  ConversionMapFactory conversionMapFactory;

  @Inject
  NumberConverter numberConverter;

  @Inject
  TextStyler textStyler;

  @Inject
  Whitespace whitespace;

  @Inject
  public FormattedTextConverter() {}

  private String convertSymbol(SymbolNode node, ConversionMap conversionMap) {
    List<String> tokens = node.tokens;
    if (isSymbolCharacter(tokens.get(0))) {
      return node.tokens.get(0);
    } else if (conversionMap.symbolMap.get(tokens) != null) {
      return conversionMap.symbolMap.get(tokens);
    }

    return conversionMap.symbolMap.get(tokens);
  }

  private String removeSpacesAroundSymbol(String source, String symbol) {
    return source.replaceAll("\\s*" + symbol + "\\s*", symbol.replaceAll("\\\\", ""));
  }

  private String wrapSymbolInSpaces(
    String source,
    String symbol,
    String startPattern,
    String endPattern
  ) {
    return source.replaceAll(
      startPattern + symbol + endPattern,
      "$1 " + symbol.replaceAll("\\\\", "") + " $2"
    );
  }

  private String applyExpressionStyling(String source) {
    List<String> symbolsAtStringStart = Arrays.asList("\\-");
    List<String> symbolsOccurringBetweenSymbols = Arrays.asList("=>", "->");
    List<String> symbols = Arrays.asList(
      "==",
      "!=",
      ">=",
      "<=",
      "\\+=",
      "\\-=",
      "\\*=",
      "\\/=",
      "%=",
      "\\/\\/=",
      "\\*\\*=",
      "\\^=",
      "\\&=",
      ">>",
      "<<",
      "\\|\\|",
      "&&",
      "=",
      ">",
      "<",
      "\\+",
      "\\*",
      "\\*\\*",
      "\\/",
      "\\/\\/",
      "\\|",
      "&",
      "%"
    );

    for (String symbol : Stream
      .concat(
        symbolsOccurringBetweenSymbols.stream(),
        Stream.concat(symbols.stream(), symbolsAtStringStart.stream())
      )
      .collect(Collectors.toList())) {
      source = removeSpacesAroundSymbol(source, symbol);
    }

    for (String symbol : symbols) {
      source = wrapSymbolInSpaces(source, symbol, startBoundary, endBoundary);
    }

    for (String symbol : symbolsAtStringStart) {
      source = wrapSymbolInSpaces(source, symbol, "([a-zA-Z0-9_\\)\\]])", endBoundary);
    }

    source =
      source
        .replaceAll("[a-zA-Z0-9_\\$]" + "\\[\\]" + endBoundary, "$1[] $2")
        .replaceAll(startBoundary + "," + endBoundary, "$1, $2")
        .replaceAll("\\s*\\+ \\+", "++")
        .replaceAll("\\s*\\- \\-", "++");

    // undo spacing around < and > for generics, tags, etc.
    String comparatorsRegex = "\\s*<\\s*(.+)\\s*>\\s*";
    Matcher comparatorsMatcher = Pattern.compile(comparatorsRegex).matcher(source);
    if (comparatorsMatcher.find()) {
      source = source.replaceAll(comparatorsRegex, "<" + comparatorsMatcher.group(1).trim() + ">");
    }

    source = source.replaceAll("(\\w+)>(\\w+)", "$1> $2");

    // we want "foo = <div />" and "return <div />"
    source = source.replaceAll("(\\w+)=<(\\w+)>", "$1 = <$2>");
    source = source.replaceAll("\\b(\\w+)\\b<(\\w+)>", "$1 <$2>");
    for (String symbol : symbolsOccurringBetweenSymbols) {
      source = source.replace(symbol, " " + symbol + " ");
    }

    return source;
  }

  public boolean containsNonAlphaNumeric(String text) {
    List<Node> nodes = new ParserState(
      text,
      FormattedTextOptions.newBuilder().build(),
      conversionMapFactory.create(Language.LANGUAGE_DEFAULT)
    )
      .parse();

    return nodes
      .stream()
      .filter(e -> !(e instanceof AlphaNode) && !(e instanceof NumberNode))
      .findFirst()
      .isPresent();
  }

  public boolean containsStyledText(String text, Language language) {
    ConversionMap conversionMap = conversionMapFactory.create(language);
    List<Node> nodes = new ParserState(
      text,
      FormattedTextOptions.newBuilder().build(),
      conversionMap
    )
      .parse();

    for (int i = 0; i < nodes.size(); i++) {
      if (
        nodes.get(i) instanceof StyleNode ||
        ( // underscores are kind of like a style if they're not a prefix.
          nodes.get(i) instanceof SymbolNode &&
          convertSymbol((SymbolNode) nodes.get(i), conversionMap).equals("_") &&
          i > 0
        )
      ) {
        return true;
      }
    }

    return false;
  }

  public String convert(String text, FormattedTextOptions options, Language language) {
    return convert(text, options, conversionMapFactory.create(language));
  }

  public String convert(String text, FormattedTextOptions options, ConversionMap conversionMap) {
    // text entry sometimes has extra spaces.
    text = text.replaceAll("\\s+", " ").trim();
    text = numberConverter.convertNumbers(text);
    List<Node> nodes = new ParserState(text, options, options.conversionMap.orElse(conversionMap))
      .parse();
    String result = new ConversionState(nodes, options, options.conversionMap.orElse(conversionMap))
      .convert();

    if (options.expression) {
      result = applyExpressionStyling(result);
    }

    return result;
  }

  public String convertNumbers(String text) {
    return numberConverter.convertNumbers(text);
  }

  public boolean isEnclosurePair(String s) {
    Set<String> enclosures = new HashSet<>(Arrays.asList("()", "[]", "{}", "<>", "\"\"", "''"));
    return enclosures.contains(s);
  }

  public boolean isSymbolCharacter(String s) {
    return s.length() == 1 && !Character.isLetter(s.charAt(0)) && !Character.isDigit(s.charAt(0));
  }
}
