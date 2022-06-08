package corpusgen.mapping;

import core.ast.Ast;
import core.ast.api.AstNode;
import java.util.Optional;

public class NameSection extends Section {

  public final Optional<Ast.Identifier> node;
  public final String phrase;

  public NameSection(Optional<Ast.Identifier> node, String phrase) {
    this.node = node;
    this.phrase = phrase;
  }
}
