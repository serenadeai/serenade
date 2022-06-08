package core.ast;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import core.ast.Ast;
import core.ast.api.AstNewline;
import core.ast.api.AstNode;
import core.ast.api.AstParent;
import core.ast.api.AstSyntaxError;
import core.ast.api.AstToken;
import core.ast.api.AstTree;
import core.ast.api.DefaultAstParent;
import core.ast.api.IndentationUtils;
import core.converter.ParseTreeToAstConverter;
import core.converter.ParseTreeToAstConverterFactory;
import core.exception.SafeToDisplayException;
import core.gen.rpc.Language;
import core.parser.ParseTree;
import core.parser.Parser;
import core.util.LinePositionConverter;
import core.util.Range;
import core.util.Whitespace;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AstFactory {

  private Cache<AstCacheKey, AstCacheValue> astCache = CacheBuilder
    .newBuilder()
    .maximumSize(1000)
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .build();

  @Inject
  IndentationUtils indentationUtils;

  @Inject
  ParseTreeToAstConverterFactory parseTreeToAstConverterFactory;

  @Inject
  Parser parser;

  @Inject
  Whitespace whitespace;

  @Inject
  public AstFactory() {}

  private List<AstToken> hiddenTokens(String source, int start, int stop) {
    List<AstToken> nodes = new ArrayList<>();
    while (start < stop && start != source.length()) {
      if (source.charAt(start) == '\n') {
        nodes.add(createWithoutTree(new AstNewline(), "\n"));
        start++;
      } else {
        // Stop at next non-whitespace or newline.
        int nextNewLine = whitespace.nextNewline(source, start);
        int nonWhitespace = whitespace.lineNonWhitespaceStart(source, start);
        int nextStart = Math.min(nextNewLine, stop);

        // starts of lines that are not blank.
        if ((start == 0 || source.charAt(start - 1) == '\n') && nonWhitespace <= nextStart) {
          nodes.add(createWithoutTree(new AstToken(), source.substring(start, nonWhitespace)));
          if (nonWhitespace != nextStart) {
            // note that comments fall into this category.
            nodes.add(
              createWithoutTree(new AstToken(), source.substring(nonWhitespace, nextStart))
            );
          }
        } else {
          nodes.add(createWithoutTree(new AstToken(), source.substring(start, nextStart)));
        }
        start = nextStart;
      }
    }

    return nodes;
  }

  private List<AstToken> fillHiddenTokens(
    String source,
    AstParent root,
    List<Ast.Comment> comments
  ) {
    List<AstToken> tokens = new ArrayList<>();
    List<AstToken> visibleTokens = new ArrayList<>(
      root.find(AstToken.class).collect(Collectors.toList())
    );
    for (Ast.Comment comment : comments) {
      visibleTokens.addAll(comment.find(AstToken.class).collect(Collectors.toList()));
    }
    Collections.sort(visibleTokens, Comparator.comparingInt(t -> t.range.start));
    if (visibleTokens.size() > 0) {
      int previousHiddenStart = 0;
      for (AstToken visibleToken : visibleTokens) {
        // add leading hidden tokens.
        tokens.addAll(hiddenTokens(source, previousHiddenStart, visibleToken.range.start));
        tokens.add(visibleToken);
        previousHiddenStart = visibleToken.range.stop;
      }

      // add final trailing hidden tokens.
      tokens.addAll(hiddenTokens(source, previousHiddenStart, source.length()));
    } else {
      tokens.addAll(hiddenTokens(source, 0, source.length()));
    }

    return tokens;
  }

  private <T extends AstToken> T createWithoutTree(T token, String code) {
    token.setCode(code);
    return token;
  }

  private List<ParseTree> errors(ParseTree root) {
    List<ParseTree> errors = new ArrayList<>();
    errors(root, errors);
    return errors;
  }

  private void errors(ParseTree node, List<ParseTree> errors) {
    if (node.getType().equals("ERROR")) {
      errors.add(node);
    } else {
      node.getChildren().stream().forEach(c -> errors(c, errors));
    }
  }

  public AstCacheValue parseSource(String source, Language language, boolean parseEmbedded) {
    try {
      ParseTree parseTree = parser.parse(source, language);
      Optional<AstSyntaxError> syntaxError = Optional.empty();
      if (errors(parseTree).size() > 0) {
        ParseTree token = errors(parseTree).get(0);
        int position = token.getStart();
        int line = new LinePositionConverter(source).position(position);
        int column = position - whitespace.lineStart(source, position);
        syntaxError = Optional.of(new AstSyntaxError(line, column, position));
      }
      AstParent result = convertParseTreeToAst(source, parseTree, language);
      result.tree().setSyntaxError(syntaxError);

      if (parseEmbedded && language == Language.LANGUAGE_HTML) {
        List<Ast.MarkupElement> scripts = result
          .find(Ast.MarkupElement.class)
          .filter(e -> e.nameString().equals("script"))
          .collect(Collectors.toList());

        for (Ast.MarkupElement e : scripts) {
          if (e.contentList().isEmpty()) {
            continue;
          }

          AstParent innerParent = createFileRoot(
            e.contentList().get().code(),
            Language.LANGUAGE_JAVASCRIPT
          );
          e.contentList().get().replace(innerParent);
        }

        List<Ast.MarkupElement> styles = result
          .find(Ast.MarkupElement.class)
          .filter(e -> e.nameString().equals("style"))
          .collect(Collectors.toList());

        for (Ast.MarkupElement e : styles) {
          if (e.contentList().isEmpty()) {
            continue;
          }

          AstParent innerParent = createFileRoot(
            e.contentList().get().code(),
            Language.LANGUAGE_SCSS
          );
          e.contentList().get().replace(innerParent);
        }
      }

      return new AstCacheValue(result, syntaxError);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  private AstParent convertParseTreeToAst(String source, ParseTree parseTree, Language language) {
    ParseTreeToAstConverter converter = parseTreeToAstConverterFactory.create(language);
    AstParent ret = (AstParent) converter.convert(source, parseTree);
    List<Ast.Comment> comments = converter.convertComments(source, parseTree);
    List<AstToken> tokens = fillHiddenTokens(source, ret, comments);
    AstTree.attachTree(this, ret, comments, tokens);
    ret.find(Ast.Comment.class).forEach(node -> node.detachParent());
    return ret;
  }

  @SuppressWarnings("unchecked")
  public <T extends AstNode> T clone(T root) {
    Map<AstToken, AstToken> oldToNew = new HashMap<>();
    Range tokenRange = root == root.tree().root
      ? root.tokenRangeWithCommentsAndWhitespace()
      : root.tokenRange().orElse(new Range(0, 0));
    List<AstToken> newTokens = new ArrayList<>();
    List<AstToken> oldTokens = root.tokens().subList(tokenRange.start, tokenRange.stop);
    for (AstToken oldToken : oldTokens) {
      try {
        AstToken newToken = oldToken.clone();
        oldToNew.put(oldToken, newToken);
        newTokens.add(newToken);
      } catch (CloneNotSupportedException e) {
        throw new RuntimeException(e);
      }
    }
    List<Ast.Comment> comments = root
      .tree()
      .comments.stream()
      .filter(c -> tokenRange.contains(c.tokenRange().get()))
      .map(c -> (Ast.Comment) c.cloneTree(Optional.empty(), oldToNew))
      .collect(Collectors.toList());
    T ret = (T) root.cloneTree(Optional.empty(), oldToNew);
    AstTree.attachTree(this, ret, comments, newTokens);
    // If we're cloning a sub-tree, ensure there's a newline at the end because that's an invariant
    // we usually ensure for the larger tree. Technically we might not need the first clause,
    // but reducing the scope to be safe since this behavior mainly needed for CorpusGen.
    if (
      root != root.tree().root &&
      ret.tokens().size() > 0 &&
      !(ret.tokens().get(ret.tokens().size() - 1) instanceof AstNewline)
    ) {
      ret.tokens().add(createNewline());
    }
    if (root != root.tree().root) {
      indentationUtils.decreaseIndentation(ret, indentationUtils.indent(root));
    }
    if (root.tree().getSyntaxError().isPresent()) {
      ret.tree().setSyntaxError(root.tree().getSyntaxError());
    }
    return ret;
  }

  public <T extends AstParent> T create(T node) {
    List<AstToken> tokens = Collections.emptyList();
    AstTree.attachTree(this, node, Collections.emptyList(), tokens);
    return node;
  }

  public <T extends AstParent> T create(T parent, String source) {
    List<AstToken> tokens = fillHiddenTokens(source, parent, Collections.emptyList());
    List<AstNode> children = tokens
      .stream()
      .filter(t -> !whitespace.isWhitespace(t.code))
      .collect(Collectors.toList());
    parent.setChildren(children);
    AstTree.attachTree(this, parent, Collections.emptyList(), tokens);
    return parent;
  }

  public AstParent createFileRoot(String source, Language language) {
    return createFileRoot(source, language, true, true);
  }

  public AstParent createImmutableFileRoot(String source, Language language) {
    return createFileRoot(source, language, false, true);
  }

  public AstParent createFileRoot(
    String source,
    Language language,
    boolean mutable,
    boolean parseEmbedded
  ) {
    AstCacheValue value;
    try {
      value =
        astCache.get(
          new AstCacheKey(source, language),
          () -> parseSource(source, language, parseEmbedded)
        );
    } catch (UncheckedExecutionException e) {
      if (e.getCause() instanceof SafeToDisplayException) {
        throw ((SafeToDisplayException) e.getCause());
      }
      throw new RuntimeException("AST parse error", e);
    } catch (Exception e) {
      throw new RuntimeException("AST parse error", e);
    }

    if (!mutable) {
      return value.root;
    }

    return clone(value.root);
  }

  public AstNewline createNewline() {
    return createToken(() -> new AstNewline(), "\n");
  }

  public <T extends AstToken> T createToken(Supplier<T> nodeFactory, String source) {
    T node = nodeFactory.get();
    List<AstToken> tokens = Arrays.asList(node);
    node.setCode(source);
    AstTree.attachTree(this, node, Collections.emptyList(), tokens);
    return node;
  }

  public AstToken createToken(String source) {
    return createToken(() -> new AstToken(), source);
  }
}
