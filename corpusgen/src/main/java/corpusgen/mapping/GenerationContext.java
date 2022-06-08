package corpusgen.mapping;

import core.ast.api.AstNode;
import core.util.Range;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class GenerationContext {

  public Optional<AstObjects> astObjects;
  public Tokens tokens;
  public Sampler sampler;
  public Range eligible;
  public boolean allowPartial;
  public boolean allowTrailingImplicit;

  public GenerationContext(
    Optional<AstObjects> astObjects,
    Tokens tokens,
    Sampler sampler,
    Range eligible,
    boolean allowPartial,
    boolean allowTrailingImplicit
  ) {
    this.astObjects = astObjects;
    this.tokens = tokens;
    this.sampler = sampler;
    this.eligible = eligible;
    this.allowPartial = allowPartial;
    this.allowTrailingImplicit = allowTrailingImplicit;
  }

  public GenerationContext(GenerationContext ctx) {
    this.astObjects = ctx.astObjects;
    this.tokens = ctx.tokens;
    this.sampler = ctx.sampler;
    this.eligible = ctx.eligible;
    this.allowPartial = ctx.allowPartial;
    this.allowTrailingImplicit = ctx.allowTrailingImplicit;
  }

  public List<AstNode> eligibleObjects() {
    return astObjects
      .map(
        a ->
          a
            .eligibleObjects(tokens.list, eligible.start)
            .stream()
            .filter(node -> tokens.tokenRange(node.range()).stop <= eligible.stop)
            .collect(Collectors.toList())
      )
      .orElse(Collections.emptyList());
  }
}
