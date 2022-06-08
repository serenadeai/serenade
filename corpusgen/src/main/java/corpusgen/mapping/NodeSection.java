package corpusgen.mapping;

import core.ast.api.AstNode;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class NodeSection<T extends AstNode> extends Section {

  private Optional<BiFunction<GenerationContext, T, Mapping>> generate = Optional.empty();

  public Optional<? extends T> node;
  public Optional<Consumer<T>> delete = Optional.empty();

  public boolean implicit = false;

  public NodeSection(Optional<? extends T> node) {
    this.node = node;
  }

  public NodeSection(T node) {
    this.node = Optional.of(node);
  }

  public Mapping generate(GenerationContext ctx) {
    return this.generate.get().apply(ctx, this.node.get());
  }

  public boolean canGenerate() {
    return node.isPresent() && generate.isPresent();
  }

  public NodeSection<T> setDelete(Consumer<T> delete) {
    this.delete = Optional.of(delete);
    return this;
  }

  public NodeSection<T> setGenerate(BiFunction<GenerationContext, T, Mapping> generate) {
    this.generate = Optional.of(generate);
    return this;
  }

  public NodeSection<T> setImplicit(boolean implicit) {
    this.implicit = implicit;
    return this;
  }
}
