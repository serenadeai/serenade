package core.snippet;

import com.google.common.base.MoreObjects;
import core.ast.api.AstParent;
import core.codeengine.CodeEngineBatchQueue;
import core.metadata.DiffWithMetadata;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class Snippet {

  public SnippetTrigger trigger;
  public Transform transform;
  public boolean internal = true;

  @FunctionalInterface
  public static interface Transform {
    public CompletableFuture<List<DiffWithMetadata>> apply(
      String source,
      int cursor,
      AstParent root,
      String transcript,
      CodeEngineBatchQueue queue,
      boolean internal
    );
  }

  public Snippet(SnippetTrigger trigger, Transform transform) {
    this.trigger = trigger;
    this.transform = transform;
  }

  public CompletableFuture<List<DiffWithMetadata>> apply(
    String source,
    int cursor,
    AstParent root,
    String transcript,
    CodeEngineBatchQueue queue
  ) {
    return transform.apply(source, cursor, root, transcript, queue, internal);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("trigger", trigger.trigger).toString();
  }
}
