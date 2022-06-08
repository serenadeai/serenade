package core.commands;

import core.ast.Ast;
import core.ast.api.AstParent;
import core.codeengine.CodeEngineBatchQueue;
import core.exception.LanguageFeatureNotSupported;
import core.formattedtext.ConversionMap;
import core.formattedtext.ConversionMapFactory;
import core.gen.rpc.Language;
import core.metadata.DiffWithMetadata;
import core.selector.Selector;
import core.snippet.Snippet;
import core.snippet.SnippetCollection;
import core.snippet.SnippetCollectionFactory;
import core.util.Diff;
import core.util.Range;
import core.util.selection.Selection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class HtmlCommands extends Commands {

  @Inject
  public HtmlCommands() {}

  @Override
  public Language language() {
    return Language.LANGUAGE_HTML;
  }

  @Override
  public CompletableFuture<List<DiffWithMetadata>> add(
    String source,
    int cursor,
    String transcript,
    CodeEngineBatchQueue queue
  ) {
    SnippetCollection collection = snippetCollectionFactory.create(Language.LANGUAGE_HTML);
    AstParent root = astFactory.createFileRoot(source, language());

    Optional<Ast.MarkupElement> scriptRoot = root
      .find(Ast.MarkupElement.class)
      .filter(e -> e.nameString().equals("script") && e.range().contains(cursor))
      .findFirst();

    Optional<Ast.MarkupElement> styleRoot = root
      .find(Ast.MarkupElement.class)
      .filter(e -> e.nameString().equals("style") && e.range().contains(cursor))
      .findFirst();

    if (scriptRoot.isPresent()) {
      collection = snippetCollectionFactory.create(Language.LANGUAGE_JAVASCRIPT);
      queue.language = Language.LANGUAGE_JAVASCRIPT;
    } else if (styleRoot.isPresent()) {
      collection = snippetCollectionFactory.create(Language.LANGUAGE_SCSS);
      queue.language = Language.LANGUAGE_SCSS;
    }

    Snippet snippet = collection
      .snippets()
      .stream()
      .filter(e -> e.trigger.matches(transcript))
      .findFirst()
      .orElseThrow(() -> new LanguageFeatureNotSupported());

    return snippet
      .apply(source, cursor, root, transcript, queue)
      .thenApply(
        diffs -> {
          for (DiffWithMetadata diff : diffs) {
            diff.nameForDescription = snippet.trigger.description;
          }

          return diffs;
        }
      );
  }

  @Override
  public Diff comment(String source, int cursor, Selection selection) {
    ConversionMap conversionMap = conversionMapFactory.create(language());
    Range range = selectorFactory.create(language()).rangeFromSelection(source, cursor, selection);

    // if given a selection that doesn't start with a line, start the
    // range at the nearest start of line.
    int start = whitespace.lineStart(source, range.start);
    int stop = range.stop;

    boolean isMultiline = false;
    boolean foundFirst = false;
    String indentation = "";
    for (int i = start; i < stop; i++) {
      if (!foundFirst && whitespace.isWhitespace(source.charAt(i))) {
        indentation += source.charAt(i);
        continue;
      }
      foundFirst = true;
      if (source.charAt(i) == '\n') {
        isMultiline = true;
        break;
      }
    }

    // adjust style if the comment is for a multiline block
    String prefix = indentation + conversionMap.commentPrefix();
    String postfix = conversionMap.commentPostfix();
    prefix = isMultiline ? prefix + "\n" : prefix + " ";
    postfix = isMultiline ? "\n" + indentation + postfix : " " + postfix;

    String blockFromSource = isMultiline
      ? source.substring(start, stop)
      : source.substring(start + indentation.length(), stop);
    String result = prefix + blockFromSource + postfix;

    return Diff
      .fromInitialState(source, cursor)
      .replaceRange(new Range(start, stop), result)
      .moveCursor(start + result.length());
  }

  @Override
  public Diff uncomment(String source, int cursor, Selection selection) {
    ConversionMap conversionMap = conversionMapFactory.create(language());
    Range range = selectorFactory.create(language()).rangeFromSelection(source, cursor, selection);

    // if given a selection that doesn't start with a line, start the
    // range at the nearest start of line.
    int start = whitespace.lineStart(source, range.start);
    int stop = range.stop;

    String prefix = conversionMap.commentPrefix();
    String postfix = conversionMap.commentPostfix();

    String result = "";
    int i = start;
    boolean uncommented = false;
    while (i < stop) {
      // if a line starts with whitespace, add it first before uncommenting
      while (!uncommented && whitespace.isWhitespace(source.charAt(i))) {
        result += source.charAt(i);
        if (source.charAt(i) == '\n') result = "";
        i++;
      }
      // remove all consecutive comment prefixes as start of line (not after)
      if (
        i <= stop - prefix.length() &&
        !uncommented &&
        source.substring(i, i + prefix.length()).equals(prefix)
      ) {
        i += prefix.length();
        if (source.charAt(i) == ' ') i++;
        if (source.charAt(i) == '\n') {
          result = "";
          i++;
        }
        uncommented = true;
        continue;
      }

      if (
        i <= stop - postfix.length() && source.substring(i, i + postfix.length()).equals(postfix)
      ) {
        int j = result.length() - 1;
        while (j > 0 && whitespace.isWhitespace(result.charAt(j))) {
          if (result.charAt(j) == '\n') {
            result = result.substring(0, j);
            break;
          }
          j--;
        }
        i += postfix.length();
        continue;
      }

      result += source.charAt(i);
      i++;
    }

    return Diff
      .fromInitialState(source, cursor)
      .replaceRange(new Range(start, stop), result)
      .moveCursor(start + result.length());
  }
}
