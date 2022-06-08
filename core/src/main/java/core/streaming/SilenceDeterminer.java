package core.streaming;

import core.evaluator.ParsedTranscript;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SilenceDeterminer {

  private List<String> continuationKeywords = Arrays.asList(
    "argument",
    "attribute",
    "class",
    "comment",
    "decorator",
    "element",
    "else if",
    "entry",
    "extends",
    "for",
    "function",
    "if",
    "implements",
    "import",
    "interface",
    "method",
    "parameter",
    "print",
    "property",
    "return",
    "tag",
    "while"
  );

  private List<String> textKeywords = Arrays.asList(
    "add",
    "change",
    "dictate",
    "go to",
    "phrase",
    "insert",
    "rename",
    "replace",
    "system",
    "type"
  );

  @Inject
  public SilenceDeterminer() {}

  public int threshold(List<ParsedTranscript> transcripts, String identifier) {
    // command that is likely unfinished, because it starts with a certain prefix and doesn't
    // have a certain postfix. for instance, "add function" is likely unfinished, because
    // the function has no name
    if (
      transcripts
        .stream()
        .limit(1)
        .map(e -> e.transcript())
        .anyMatch(
          e ->
            (e.startsWith("add") && continuationKeywords.stream().anyMatch(k -> e.endsWith(k))) ||
            (e.startsWith("change") && e.endsWith("to")) ||
            (e.startsWith("change") && !e.contains("to") && !e.contains("two")) ||
            (e.startsWith("replace") && e.endsWith("with")) ||
            (e.startsWith("replace") && !e.contains("with")) ||
            textKeywords.stream().anyMatch(k -> e.equals(k))
        )
    ) {
      return identifier.contains("1.8.") ? 2000 : 66;
    }
    // command that is specifying free-form text, which is longer than a non-text command
    // but shorter than the above
    else if (
      transcripts
        .stream()
        .limit(3)
        .map(e -> e.transcript())
        .anyMatch(e -> textKeywords.stream().anyMatch(k -> e.startsWith(k)))
    ) {
      return identifier.contains("1.8.") ? 1000 : 33;
    }

    // default threshold for non-text commands
    return identifier.contains("1.8.") ? 500 : 16;
  }
}
