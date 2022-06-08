package corpusgen.mapping;

import core.ast.Ast;
import core.ast.AstFactory;
import core.ast.api.AstNode;
import core.ast.api.AstParent;
import core.ast.api.AstSyntaxError;
import core.ast.api.AstToken;
import core.ast.api.DefaultAstParent;
import core.gen.rpc.Language;
import core.parser.ParseTree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CorpusGenAstFactory {

  @Inject
  AstFactory astFactory;

  @Inject
  public CorpusGenAstFactory() {}

  private void replaceEmbeddedLanguages(AstParent parent) {
    // For now, we avoid learning JS/CSS concepts when generating HTML, so we don't parse
    // scripts and styles using those language, and just wrap them inside a MarkupContentList.
    parent
      .find(Ast.MarkupElement.class)
      .map(e -> (Ast.MarkupElement) e)
      .filter(e -> e.nameString().equals("script") || e.nameString().equals("style"))
      .forEach(
        e -> {
          e.setChildren(
            e
              .tokensInVisibleRange()
              .stream()
              .map(x -> (AstNode) x)
              .filter(child -> !parent.tree().whitespace.isWhitespace(child))
              .collect(Collectors.toList())
          );
        }
      );
  }

  public AstParent createFileRoot(String source, Language language) throws AstSyntaxError {
    AstParent root = astFactory.createFileRoot(source, language, true, false);

    List<Ast.Text> texts = Stream
      .concat(root.find(Ast.Text.class), root.tree().comments.stream().map(c -> c.text()))
      .collect(Collectors.toList());
    for (Ast.Text text : texts) {
      String textSource = text.code();
      // Skipping parsing when this cannot be a tag.
      if (
        textSource.equals("") ||
        textSource.indexOf("<") == -1 ||
        !(textSource.indexOf("<") < textSource.indexOf(">"))
      ) {
        continue;
      }

      // See SER-1247
      if (textSource.contains("<!--") || textSource.contains("-->")) {
        continue;
      }

      Optional<AstParent> innerCandidate = Optional.empty();
      try {
        // Don't cache parses of the inner HTML.
        innerCandidate =
          Optional.of(astFactory.parseSource(textSource, Language.LANGUAGE_HTML, false).root);
      } catch (Exception e) {
        // Nested text might not be valid HTML, so we skip instead of blowing up.
        continue;
      }

      if (innerCandidate.isEmpty() || innerCandidate.get().tree().syntaxError.isPresent()) {
        // Skip if there's no valid parse.
        continue;
      }

      AstParent inner = new DefaultAstParent();
      inner.setChildren(text.children());
      inner.setTree(text.tree());
      text.setChildren(Arrays.asList(inner));
      inner.setParent(Optional.of(text));
      inner.replace(innerCandidate.get());
    }
    replaceEmbeddedLanguages(root);

    return root;
  }
}
