package corpusgen.mapping;

import core.ast.Ast;
import core.ast.AstFactory;
import core.ast.api.AstNode;
import core.codeengine.Tokenizer;
import core.exception.SnippetGenerationException;
import core.util.Range;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MultiNodeSectionGenerator {

  @Inject
  protected AstFactory astFactory;

  @Inject
  protected Keywords keywords;

  @Inject
  protected Tokenizer tokenizer;

  @Inject
  public MultiNodeSectionGenerator() {}

  public <T extends AstNode> Mapping generate(
    GenerationContext ctx,
    T node,
    List<NodeSection<?>> sections,
    Function<List<List<String>>, List<String>> aggregatePhrases,
    Function<T, Optional<Integer>> defaultCursor,
    boolean defaultCursorIfNoPartialSections,
    int requiredCount
  ) {
    checkOverlappingNodes(node, sections);
    T clonedNode = (T) astFactory.clone(node);

    clearComments(clonedNode);
    // Clear any AstTokens that are non-whitespace but do not have parents.
    clonedNode.tree().removeHiddenTokens();

    Map<AstNode, AstNode> originalToCloned = new HashMap<>();
    addOriginalToCloned(originalToCloned, node, clonedNode);

    List<Range> remaining = new ArrayList<>(originalCommentRanges(ctx.tokens, node));
    List<List<String>> phrases = new ArrayList<>();
    List<AstNode> allNodes = new ArrayList<>();
    List<AstNode> replacementNodes = new ArrayList<>();
    List<List<Tokenizer.Token>> replacements = new ArrayList<>();
    boolean deleteRemaining = false;
    boolean partialSection = false;
    requiredCount =
      sections
        .stream()
        .skip(requiredCount)
        .filter(e -> e.delete.isEmpty() && !e.implicit)
        .reduce((first, second) -> second)
        .map(e -> sections.indexOf(e) + 1)
        .orElse(requiredCount);

    for (int i = 0; i < sections.size(); i++) {
      NodeSection<?> section = sections.get(i);
      if (section.node.isEmpty()) {
        phrases.add(Collections.emptyList());
        continue;
      }

      AstNode clonedSectionNode = originalToCloned.get(section.node.get());

      if (section.implicit || section.node.map(e -> e.tokenRange().isEmpty()).orElse(false)) {
        phrases.add(Collections.emptyList());
        allNodes.add(clonedSectionNode);
        continue;
      }

      deleteRemaining = deleteRemaining || (i >= requiredCount && ctx.sampler.stopSections());
      if (deleteRemaining || !section.canGenerate()) {
        phrases.add(Collections.emptyList());
        remaining.add(ctx.tokens.tokenRange(section.node.get().range()));
        deleteSection(originalToCloned, section);
        continue;
      }
      allNodes.add(clonedSectionNode);

      GenerationContext innerCtx = new GenerationContext(ctx);
      innerCtx.allowPartial = i + 1 >= requiredCount; // last required section can be a partial

      Mapping mapping = section.generate(innerCtx);
      phrases.add(mapping.phrases);
      remaining.addAll(mapping.remaining);
      // start deleting remaining if we go a partial back
      partialSection = partialSection || mapping.partial;
      deleteRemaining = deleteRemaining || mapping.partial;

      // strip cursor from every section but the last one, and add it to the
      // end by default.
      List<Tokenizer.Token> sectionTokens = new ArrayList<>(mapping.outputTokens);
      replacementNodes.add(clonedSectionNode);
      replacements.add(sectionTokens);
    }

    // create new output token list of mutated thing
    List<Tokenizer.Token> outputTokensList = tokenizer.tokenize(clonedNode.code());
    outputTokensList.remove(0); // remove leading newline.
    Tokens outputTokens = new Tokens(outputTokensList);

    boolean forceDefaultCursor = !partialSection && defaultCursorIfNoPartialSections;
    for (int i = 0; i < replacements.size(); i++) {
      if (i != replacements.size() - 1 || forceDefaultCursor) {
        replacements.get(i).removeIf(t -> t instanceof Tokenizer.CursorToken);
      } else {
        Optional<Tokenizer.CursorToken> cursor = replacements
          .get(i)
          .stream()
          .filter(Tokenizer.CursorToken.class::isInstance)
          .map(Tokenizer.CursorToken.class::cast)
          .findFirst();
        if (cursor.isEmpty()) {
          replacements.get(i).add(new Tokenizer.CursorToken());
        }
      }
    }

    if (replacementNodes.size() == 0 || forceDefaultCursor) {
      defaultCursor
        .apply(clonedNode)
        .ifPresent(
          c -> {
            outputTokens.list.add(outputTokens.tokenPosition(c), new Tokenizer.CursorToken());
          }
        );
    }

    checkUnhandledAlphanumerics(clonedNode, allNodes, outputTokens);

    applySubstitutions(
      outputTokens.list,
      replacementNodes
        .stream()
        .map(e -> outputTokens.tokenRange(e.range()))
        .collect(Collectors.toList()),
      replacements
    );

    return new Mapping(
      aggregatePhrases.apply(phrases),
      outputTokens.list,
      remaining,
      partialSection
    );
  }

  private void addOriginalToCloned(
    Map<AstNode, AstNode> originalToCloned,
    AstNode node,
    AstNode clonedNode
  ) {
    originalToCloned.put(node, clonedNode);
    for (int i = 0; i < node.children().size(); i++) {
      addOriginalToCloned(originalToCloned, node.children().get(i), clonedNode.children().get(i));
    }
  }

  private void applySubstitutions(
    List<Tokenizer.Token> tokens,
    List<Range> ranges,
    List<List<Tokenizer.Token>> replacements
  ) {
    List<Range> sortedRanges = new ArrayList<>(ranges);
    Collections.sort(sortedRanges, Comparator.<Range>comparingInt(r -> r.start).reversed());
    for (Range range : sortedRanges) {
      tokens.subList(range.start, range.stop).clear();
      tokens.addAll(range.start, replacements.get(ranges.indexOf(range)));
    }
    return;
  }

  private void checkOverlappingNodes(AstNode node, List<NodeSection<?>> sections) {
    List<AstNode> nodes = sections
      .stream()
      .filter(s -> s instanceof NodeSection<?>)
      .flatMap(s -> ((NodeSection<?>) s).node.stream())
      .collect(Collectors.toList());
    for (int i = 0; i < nodes.size(); i++) {
      for (int j = 0; j < nodes.size(); j++) {
        if (i == j) {
          continue;
        }
        AstNode inner = nodes.get(i);
        AstNode outer = nodes.get(j);
        if (inner.ancestors().filter(n -> n == outer).findFirst().isPresent()) {
          throw new RuntimeException(
            "Node contained in another: " +
            inner.code() +
            " inside " +
            outer.code() +
            ", tree: " +
            node.toDebugString()
          );
        }
      }
    }
  }

  private void checkUnhandledAlphanumerics(
    AstNode node,
    List<AstNode> sectionNodes,
    Tokens outputTokens
  ) {
    List<Range> sectionRanges = sectionNodes
      .stream()
      .map(e -> outputTokens.tokenRange(e.range()))
      .collect(Collectors.toList());
    // may want to get rid of this, but currently handles cases like "tag div", which repeats the expression.
    for (int i = 0; i < outputTokens.list.size(); i++) {
      if (outputTokens.list.get(i) instanceof Tokenizer.AlphaToken) {
        Tokenizer.AlphaToken token = (Tokenizer.AlphaToken) outputTokens.list.get(i);
        int index = i;
        String word = token.originalCode().toLowerCase();
        if (
          sectionRanges.stream().filter(r -> r.contains(index)).findFirst().isEmpty() &&
          !keywords.snippetKeywords.contains(word)
        ) {
          String message = "Error, could not find word '" + token.originalCode() + "'";
          if (node.range().length() > 3000) {
            message += " in snippet that's too large to display (> 3000).";
          } else {
            message +=
              " in generated input (transcript) nodes for snippet:" +
              node.code() +
              "\n  Handled nodes: " +
              sectionNodes
                .stream()
                .map(e -> e + "='" + e.code() + "'")
                .collect(Collectors.joining(" ")) +
              "\n  Tree:  \n" +
              node.toDebugString();
          }
          throw new SnippetGenerationException(message);
        }
      }
    }
  }

  private void clearComments(AstNode node) {
    if (!(node instanceof Ast.Comment)) {
      while (node.tree().comments.size() > 0) {
        node.tree().comments.get(0).remove();
      }
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends AstNode> void deleteSection(
    Map<AstNode, AstNode> originalToCloned,
    NodeSection<T> section
  ) {
    T sectionClonedNode = (T) originalToCloned.get(section.node.get());
    section.delete.get().accept(sectionClonedNode);
  }

  private List<Range> originalCommentRanges(Tokens tokens, AstNode node) {
    Range nodeRange = node.range();
    return node
      .tree()
      .comments.stream()
      .map(c -> c.range())
      .filter(r -> r.inside(nodeRange))
      .map(r -> tokens.tokenRange(r))
      .collect(Collectors.toList());
  }
}
