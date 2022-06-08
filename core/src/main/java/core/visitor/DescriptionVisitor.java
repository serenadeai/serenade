package core.visitor;

import core.formattedtext.FormattedTextConverter;
import core.formattedtext.FormattedTextOptions;
import core.gen.rpc.Language;
import core.parser.ParseTree;
import core.util.Whitespace;
import java.util.HashMap;
import java.util.Map;

public class DescriptionVisitor extends BaseVisitor<Void, String> {

  private FormattedTextConverter formattedTextConverter;
  private Language language;
  private TreeConverter treeConverter;
  private Whitespace whitespace;
  private Map<ParseTree, String> overrides = new HashMap<>();

  public DescriptionVisitor(
    FormattedTextConverter formattedTextConverter,
    TreeConverter treeConverter,
    Whitespace whitespace,
    Language language
  ) {
    this.formattedTextConverter = formattedTextConverter;
    this.treeConverter = treeConverter;
    this.whitespace = whitespace;
    this.language = language;

    register("formattedText", this::visitFormattedText);
    register("number", this::visitNumber);
    register("numberWords1To9", this::visitNumberWords1To9);
    register("numberRange1To99", this::visitNumberRange1To99);
    register("terminal", this::visitTerminal);
  }

  private String codeTag(String text) {
    String wrapped = "<code>" + text + "</code>";
    if (text.equals("\n")) {
      return "<code>\\n</code>";
    }

    return wrapped;
  }

  @Override
  protected String aggregateResult(String aggregate, String nextResult) {
    return (aggregate.trim() + " " + nextResult.trim()).trim();
  }

  @Override
  protected String defaultResult() {
    return "";
  }

  public void setCodeNode(ParseTree node, String code) {
    overrides.put(node, codeTag(code));
  }

  public void setCodeNode(ParseTree node, String prefix, String code) {
    overrides.put(node, prefix + codeTag(code));
  }

  @Override
  public String visit(ParseTree tree, Void context) {
    if (overrides.containsKey(tree)) {
      return overrides.get(tree);
    }

    return super.visit(tree, context).trim();
  }

  public String visitFormattedText(ParseTree node, Void context) {
    return codeTag(
      formattedTextConverter.convert(
        treeConverter.convertToEnglish(node),
        FormattedTextOptions.newBuilder().build(),
        language
      )
    );
  }

  public String visitNumber(ParseTree node, Void context) {
    return treeConverter.convertNumber(node).toString();
  }

  public String visitNumberWords1To9(ParseTree node, Void context) {
    return treeConverter.convertNumber(node).toString();
  }

  public String visitNumberRange1To99(ParseTree node, Void context) {
    return treeConverter.convertNumber(node).toString();
  }

  public String visitTerminal(ParseTree node, Void context) {
    return treeConverter.convertToEnglish(node);
  }
}
