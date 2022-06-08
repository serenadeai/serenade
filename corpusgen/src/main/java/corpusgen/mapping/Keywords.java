package corpusgen.mapping;

import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Keywords {

  // Alpha numeric we would expect to see in a snippet. This is mainly used for error checking
  // to see if we forgot to handle a word.
  public final List<String> snippetKeywords = Arrays.asList(
    "as",
    "begin",
    "catch",
    "class",
    "cls",
    "def",
    "do",
    "elif",
    "elsif",
    "else",
    "end",
    "ensure",
    "enum",
    "except",
    "extends",
    "finally",
    "for",
    "foreach",
    "fn",
    "fun",
    "func",
    "function",
    "if",
    "implements",
    "impl",
    "in",
    "include",
    "init",
    "interface",
    "lambda",
    "loop",
    "method",
    "mixin",
    "module",
    "namespace",
    "of",
    "on",
    "pass",
    "range",
    "return",
    "rescue",
    "self",
    "sizeof",
    "struct",
    "then",
    "trait",
    "true",
    "try",
    "type",
    "var",
    "until",
    "while",
    "with"
  );

  @Inject
  public Keywords() {}
}
