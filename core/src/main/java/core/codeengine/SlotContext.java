package core.codeengine;

import com.google.common.base.MoreObjects;
import core.ast.api.AstParent;
import java.util.Optional;

public class SlotContext {

  public final String source;
  public final String english;
  public final int slotStart;
  public final Optional<AstParent> snippetContainer;

  public SlotContext(
    String source,
    String english,
    int slotStart,
    Optional<AstParent> snippetContainer
  ) {
    this.source = source;
    this.english = english;
    this.slotStart = slotStart;
    this.snippetContainer = snippetContainer;
  }

  @Override
  public String toString() {
    return MoreObjects
      .toStringHelper(this)
      .add("source", source)
      .add("english", english)
      .add("slotStart", slotStart)
      .add("snippetContainer", snippetContainer)
      .toString();
  }
}
