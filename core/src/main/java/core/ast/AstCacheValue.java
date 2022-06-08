package core.ast;

import core.ast.api.AstParent;
import core.ast.api.AstSyntaxError;
import java.util.Optional;

public class AstCacheValue {

  public final AstParent root;
  public final Optional<AstSyntaxError> syntaxError;

  public AstCacheValue(AstParent root, Optional<AstSyntaxError> syntaxError) {
    this.root = root;
    this.syntaxError = syntaxError;
  }
}
