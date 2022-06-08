package corpusgen.command;

import core.gen.antlr.antlr4.ANTLRv4Parser;
import core.gen.antlr.antlr4.ANTLRv4ParserBaseVisitor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AlternativeNameVisitor extends ANTLRv4ParserBaseVisitor<List<String>> {

  public List<String> visitRuleref(ANTLRv4Parser.RulerefContext ctx) {
    String command = ctx.RULE_REF().getText();
    return Arrays.asList(command);
  }

  @Override
  protected List<String> aggregateResult(List<String> aggregate, List<String> nextResult) {
    return Stream.concat(aggregate.stream(), nextResult.stream()).collect(Collectors.toList());
  }

  @Override
  public List<String> defaultResult() {
    return Collections.emptyList();
  }
}
