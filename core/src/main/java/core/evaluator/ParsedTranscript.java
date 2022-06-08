package core.evaluator;

import com.google.common.base.MoreObjects;
import core.parser.ParseTree;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import speechengine.gen.rpc.Alternative;

public class ParsedTranscript {

  public Alternative alternative;
  public boolean isValid;
  public ParseTree root;
  public Optional<Double> parseCost;

  public ParsedTranscript(Alternative alternative, boolean isValid, ParseTree root) {
    this(alternative, isValid, root, Optional.empty());
  }

  public ParsedTranscript(
    Alternative alternative,
    boolean isValid,
    ParseTree root,
    Optional<Double> parseCost
  ) {
    this.alternative = alternative;
    this.isValid = isValid;
    this.root = root;
    this.parseCost = parseCost;
  }

  public ParsedTranscript(ParsedTranscript e) {
    this(e.alternative, e.isValid, e.root, e.parseCost);
  }

  @Override
  public boolean equals(Object other) {
    if (other.getClass() != getClass()) {
      return false;
    }

    // Intentionally leaves out the scores. This is generally for deduping.
    var o = (ParsedTranscript) other;
    return (
      transcriptId().equals(o.transcriptId()) &&
      transcript().equals(o.transcript()) &&
      isValid == o.isValid &&
      root.equals(o.root)
    );
  }

  @Override
  public String toString() {
    return MoreObjects
      .toStringHelper(this)
      .add("transcriptId", transcriptId())
      .add("transcript", transcript())
      .add("isValid", isValid)
      .add("parseCost", parseCost)
      .toString();
  }

  public List<ParseTree> children() {
    return this.getCommandChain().getChildren();
  }

  public ParseTree getCommandChain() {
    ParseTree returnRoot = null;
    if (root.getType().equals("mainWithoutPrepositions")) {
      returnRoot = root.getChild("commandChainWithoutPrepositions").get();
    } else if (root.getType().equals("main")) {
      returnRoot = root.getChild("commandChain").get();
    }
    if (returnRoot == null) {
      return new ParseTree("", "", "", 0, 0, Optional.empty());
    }
    return returnRoot;
  }

  public boolean isMetaCommand() {
    return this.getCommandChain().getChild("metaCommand").isPresent();
  }

  public String transcriptId() {
    return alternative.getTranscriptId();
  }

  public String transcript() {
    return alternative.getTranscript();
  }
}
