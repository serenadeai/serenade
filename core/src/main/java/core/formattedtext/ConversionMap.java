package core.formattedtext;

import core.codeengine.Resolver;
import core.gen.rpc.Language;
import core.language.LanguageSpecific;
import core.util.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;

public abstract class ConversionMap implements LanguageSpecific {

  public List<List<String>> escapePrefixes = new ArrayList<>();
  public List<List<String>> numeralPrefixes = new ArrayList<>();
  public List<List<String>> oneWordPrefixes = new ArrayList<>();
  public List<List<String>> recursivePrefixes = new ArrayList<>();
  public List<List<String>> stylePrefixes = new ArrayList<>();
  public List<List<String>> symbolPrefixes = new ArrayList<>();

  public Map<List<String>, Function<String, String>> recursiveMap = new HashMap<>();
  public Map<List<String>, TextStyle> styleMap = new HashMap<>();
  public Map<String, String> enclosureNameToStart = new HashMap<>();
  public Map<String, String> enclosureNameToStop = new HashMap<>();
  public Map<List<String>, String> symbolMap = new HashMap<>();

  public abstract Language language();

  protected void initialize() {
    register();
    sortPrefixes();
  }

  protected void register() {
    registerStyle("all caps", TextStyle.ALL_CAPS);
    registerStyle("caps", TextStyle.ALL_CAPS);
    registerStyle("camel case", TextStyle.CAMEL_CASE);
    registerStyle("camelcase", TextStyle.CAMEL_CASE);
    registerStyle("camel", TextStyle.CAMEL_CASE);
    registerStyle("capital", TextStyle.CAPITALIZED);
    registerStyle("dashes", TextStyle.DASHES);
    registerStyle("lower case", TextStyle.LOWERCASE);
    registerStyle("lowercase", TextStyle.LOWERCASE);
    registerStyle("pascal case", TextStyle.PASCAL_CASE);
    registerStyle("pascal", TextStyle.PASCAL_CASE);
    registerStyle("pascalcase", TextStyle.PASCAL_CASE);
    registerStyle("snake", TextStyle.UNDERSCORES);
    registerStyle("snake case", TextStyle.UNDERSCORES);
    registerStyle("snake", TextStyle.UNDERSCORES);
    registerStyle("underscores", TextStyle.UNDERSCORES);
    registerStyle("title", TextStyle.TITLE_CASE);
    registerStyle("title case", TextStyle.TITLE_CASE);
    registerStyle("upper", TextStyle.ALL_CAPS);
    registerStyle("uppercase", TextStyle.ALL_CAPS);

    registerSymbol("comma", ",");
    registerSymbol("colon", ":");
    registerSymbol("period", ".");
    registerSymbol("semicolon", ";");
    registerSymbol("exclamation point", "!");
    registerSymbol("exclamation mark", "!");
    registerSymbol("tilde", "~");
    registerSymbol("dash", "-");
    registerSymbol("backslash", "\\");
    registerSymbol("slash", "/");
    registerSymbol("ampersand", "&");
    registerSymbol("question mark", "?");
    registerSymbol("asterisk", "*");
    registerSymbol("apostrophe", "'");
    registerSymbol("space", " ");
    registerSymbol("plus", "+");
    registerSymbol("minus", "-");
    registerSymbol("binary or", "|");
    registerSymbol("binary xor", "^");
    registerSymbol("binary complement", "~");
    registerSymbol("colon colon", "::");
    registerSymbol("double colon", "::");
    registerSymbol("divided by integer", "//");
    registerSymbol("divided by", "/");
    registerSymbol("divide", "/");
    registerSymbol("equal equal", "==");
    registerSymbol("equals equals", "==");
    registerSymbol("double equals", "==");
    registerSymbol("double equal", "==");
    registerSymbol("triple equals", "===");
    registerSymbol("triple equal", "===");
    registerSymbol("dot", ".");
    registerSymbol(Arrays.asList("triple dot", "ellipsis"), "...");
    registerSymbol("equals", "=");
    registerSymbol("equal", "=");
    registerSymbol("hashtag", "#");
    registerSymbol("caret", "^");
    registerSymbol("percent", "%");
    registerSymbol("double underscore", "__");
    registerSymbol("underscore", "_");
    registerSymbol(Arrays.asList("is less than or equal to", "less than or equal to"), "<=");
    registerSymbol(Arrays.asList("is less than", "less than"), "<");
    registerSymbol(Arrays.asList("is greater than or equal to", "greater than or equal to"), ">=");
    registerSymbol(Arrays.asList("is greater than", "greater than"), ">");
    registerSymbol(Arrays.asList("is not equal to", "not equal to"), "!=");
    registerSymbol("not equals", "!=");
    registerSymbol("not equal", "!=");
    registerSymbol(Arrays.asList("is equal to", "equal to"), "==");
    registerSymbol("left shift", "<<");
    registerSymbol("right shift", ">>");
    registerSymbol("and", "&&");
    registerSymbol("or", "||");
    registerSymbol("point", ".");
    registerSymbol("dunder", "__");
    registerSymbol("bang", "!");
    registerSymbol("exclam", "!");
    registerSymbol("negative", "-");
    registerSymbol("not", "!");
    registerSymbol("mod", "%");
    registerSymbol("modulo", "%");
    registerSymbol("times", "*");
    registerSymbol("at", "@");
    registerSymbol("dollar", "$");
    registerSymbol("right arrow", "->");
    registerSymbol("arrow", "->");
    registerSymbol("hash", "#");
    registerSymbol("star", "*");
    registerSymbol("star star", "**");
    registerSymbol("double star", "**");
    registerSymbol("to the power of", "**");
    registerSymbol("empty string", "\"\"");
    registerSymbol("empty list", "[]");
    registerSymbol("diamond", "<>");
    registerSymbol("tick", "`");
    registerSymbol("forward slash", "/");
    registerSymbol("semi", ";");
    registerSymbol("plus equals", "+=");
    registerSymbol("minus equals", "-=");
    registerSymbol("divide equals", "/=");
    registerSymbol("times equals", "*=");
    registerSymbol("mod equals", "%=");

    registerEnclosureAndSymbols("parenthesee", "(", ")");
    registerEnclosureAndSymbols("parenthesis", "(", ")", "parentheses");
    registerEnclosureAndSymbols("square bracket", "[", "]");
    registerEnclosureAndSymbols("angle bracket", "<", ">");
    registerEnclosureAndSymbols("curly brace", "{", "}");
    registerEnclosureAndSymbols("curly bracket", "{", "}");
    registerEnclosureAndSymbols("quote", "\"", "\"");
    registerEnclosureAndSymbols("quotation", "\"", "\"");
    registerEnclosureAndSymbols("single quote", "'");
    registerEnclosureAndSymbols("double quote", "\"");
    registerEnclosureAndSymbols("backtick", "`");
    registerEnclosureAndSymbols("paren", "(", ")");
    registerEnclosureAndSymbols("bracket", "[", "]");
    registerEnclosureAndSymbols("sub", "[", "]");
    registerEnclosureAndSymbols("brace", "{", "}");
    registerEnclosureAndSymbols("comparator", "<", ">");
    registerEnclosureAndSymbols("double quotation", "\"");
    registerEnclosureAndSymbols("triple quote", "\"\"\"");
    registerEnclosureAndSymbols("triple quotation", "\"\"\"");
    registerEnclosureAndSymbols("double underscore", "__");
    registerEnclosureAndSymbols("pipe", "|");

    registerEnclosure("of", "(", ")");
    registerEnclosure("call", "(", ")");
    // "underscores" should not map to "__", but apply the text style,
    // so special case this.
    registerEnclosure("in underscores", "__", "__");

    registerRecursive("open tag", s -> "<" + s + ">");
    registerRecursive("close tag", s -> "</" + s + ">");
    registerRecursive("empty tag", s -> "<" + s + " />");
    registerRecursive("tag", s -> "<" + s + "><%" + Resolver.cursorOverride + "2%></" + s + ">");

    registerEscape("escape");
    registerOneWord("1 word");
    registerSymbol("newline", "\n");
    registerNumeral("numeral");
  }

  protected void deregisterEnclosure(String name) {
    enclosureNameToStart.remove(name);
    enclosureNameToStop.remove(name);
    deregisterRecursive(name);
  }

  protected void deregisterEnclosureAndSymbols(String name) {
    String plural = name + "s";
    deregisterEnclosure("in " + plural);
    deregisterSymbol(plural);
    deregisterSymbol(name);
  }

  protected void deregisterRecursive(String name) {
    List<String> prefix = tokenize(name);
    recursivePrefixes.remove(prefix);
    recursiveMap.remove(prefix);
  }

  protected void deregisterSymbol(String name) {
    List<String> prefix = tokenize(name);
    symbolPrefixes.remove(prefix);
    symbolMap.remove(prefix);
  }

  protected void registerEnclosure(String name, String start, String stop) {
    enclosureNameToStart.put(name, start);
    enclosureNameToStop.put(name, stop);
    registerRecursive(name, s -> start + s + stop);
  }

  protected void registerEnclosureAndSymbols(
    String name,
    String start,
    String stop,
    String plural
  ) {
    registerEnclosure("in " + plural, start, stop);
    registerSymbol(plural, start + stop);
    registerSymbol(name, start);
    if (!start.equals(stop)) {
      registerSymbol("left " + name, start);
      registerSymbol("open " + name, start);
      registerSymbol("right " + name, stop);
      registerSymbol("close " + name, stop);
      registerSymbol("end " + name, stop);
    }
  }

  protected void registerEnclosureAndSymbols(String name, String start, String stop) {
    registerEnclosureAndSymbols(name, start, stop, name + "s");
  }

  protected void registerEnclosureAndSymbols(String name, String symbol) {
    String plural = name + "s";
    registerEnclosure("in " + plural, symbol, symbol);
    registerSymbol(plural, symbol + symbol);
    registerSymbol(name, symbol);
  }

  protected void registerEscape(String name) {
    escapePrefixes.add(tokenize(name));
  }

  protected void registerLambda(Function<String, String> wrapRemaining) {
    for (List<String> prefix : lambdaPrefixes()) {
      registerRecursive(prefix.stream().collect(Collectors.joining(" ")), wrapRemaining);
    }
  }

  protected void registerNumeral(String name) {
    numeralPrefixes.add(tokenize(name));
  }

  protected void registerOneWord(String name) {
    oneWordPrefixes.add(tokenize(name));
  }

  protected void registerStyle(String name, TextStyle style) {
    List<String> prefix = tokenize(name);
    stylePrefixes.add(prefix);
    styleMap.put(prefix, style);
  }

  protected void registerSymbol(String name, String symbol) {
    List<String> prefix = tokenize(name);
    symbolPrefixes.add(prefix);
    symbolMap.put(prefix, symbol);
  }

  protected void registerSymbol(List<String> names, String symbol) {
    for (String name : names) {
      registerSymbol(name, symbol);
    }
  }

  protected void registerRecursive(String name, Function<String, String> wrapRemaining) {
    List<String> prefix = tokenize(name);
    recursivePrefixes.add(prefix);
    recursiveMap.put(prefix, wrapRemaining);
  }

  protected void sortPrefixes() {
    sortPrefixes(recursivePrefixes);
    sortPrefixes(stylePrefixes);
    sortPrefixes(symbolPrefixes);
    sortPrefixes(oneWordPrefixes);
    sortPrefixes(escapePrefixes);
  }

  protected void sortPrefixes(List<List<String>> prefixes) {
    Collections.sort(prefixes, Comparator.<List<String>>comparingInt(e -> e.size()).reversed());
  }

  protected List<String> tokenize(String name) {
    return Arrays.asList(name.split(" "));
  }

  public String commentPrefix() {
    return "// ";
  }

  public String commentPostfix() {
    return "";
  }

  public int indentation() {
    return 2;
  }

  public List<List<String>> lambdaPrefixes() {
    return new ArrayList<>(Arrays.asList(Arrays.asList("lambda"), Arrays.asList("lambda", "of")));
  }

  public String statementTerminator() {
    return ";";
  }
}
