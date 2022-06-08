package core.parser;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import core.gen.rpc.Language;
import core.parser.ParseTree;
import grammar.gen.rpc.GrammarRoot;
import grammar.gen.rpc.GrammarTree;
import grammar.gen.rpc.NodeType;
import grammar.gen.rpc.Production;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import toolbelt.languages.LanguageDeterminer;

@Singleton
public class PostProcessor {
  // For a given rule, stores pre-computed map keyed on a list of ParseTree types
  // without placeholders, so that we can quickly find the matching production with placeholders
  private IdentityHashMap<GrammarTree, Map<List<String>, Production>> productionsWithPlaceholders = new IdentityHashMap<>();

  public Map<Language, GrammarRoot> flattenedGrammars = new HashMap<>();

  @Inject
  public PostProcessor(LanguageDeterminer languageDeterminer) {
    for (Language language : languageDeterminer.languages()) {
      if (language == Language.LANGUAGE_DEFAULT) {
        continue;
      }

      String name = languageDeterminer.enumString(language);
      try {
        flattenedGrammars.put(
          language,
          GrammarRoot.parseFrom(
            Resources.toByteArray(Resources.getResource("grammars/" + name + "-grammar.pb"))
          )
        );
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      flattenedGrammars
        .get(language)
        .getRuleMap()
        .values()
        .stream()
        .forEach(tree -> initializeProductionsWithPlaceholders(tree));
    }
  }

  private void initializeProductionsWithPlaceholders(GrammarTree tree) {
    Map<List<String>, Production> localProductions = new HashMap<>();
    tree
      .getFlattenedProductionList()
      .stream()
      .forEach(
        production ->
          localProductions.put(
            production
              .getNodeTypeList()
              .stream()
              .filter(nodeType -> !nodeType.getType().equals("PLACEHOLDER"))
              .map(nodeType -> nodeType.getName())
              .collect(Collectors.toList()),
            production
          )
      );

    productionsWithPlaceholders.put(tree, localProductions);
    tree
      .getLocalChildList()
      .stream()
      .forEach(childTree -> initializeProductionsWithPlaceholders(childTree));
  }

  private Optional<Production> findMatchingSequence(
    GrammarRoot root,
    GrammarTree tree,
    ParseTree node
  ) {
    List<String> sequenceWithoutPlaceholders = node
      .getChildren()
      .stream()
      .map(child -> child.getType())
      .filter(
        name ->
          !(
            tree
              .getLocalChildList()
              .stream()
              .anyMatch(
                childTree ->
                  childTree.getNodeType().getType().equals("PLACEHOLDER") &&
                  childTree.getNodeType().getName().equals(name)
              )
          ) &&
          !root.getWildCardRuleList().contains(name)
      )
      .collect(Collectors.toList());

    return Optional.ofNullable(
      productionsWithPlaceholders.get(tree).get(sequenceWithoutPlaceholders)
    );
  }

  private void addPlaceholdersToTree(GrammarRoot root, GrammarTree tree, ParseTree node) {
    Optional<Production> matchingSequence = findMatchingSequence(
      root,
      tree,
      node
    );
    if (matchingSequence.isEmpty()) {
      return;
    }

    // If we find a canonical matching sequence for our input sequence, we match elements one at a time
    // until we hit placeholder nodes, and insert those as needed.
    List<ParseTree> originalChildren = new ArrayList<>(node.getChildren());
    List<ParseTree> newChildren = new ArrayList<>();
    for (NodeType seqNode : matchingSequence.get().getNodeTypeList()) {
      while (
        originalChildren.size() > 0 &&
        root.getWildCardRuleList().contains(originalChildren.get(0).getType())
      ) {
        // These nodes can appear anywhere, so we skip them.
        newChildren.add(originalChildren.remove(0));
      }
      if (
        originalChildren.size() > 0 && originalChildren.get(0).getType().equals(seqNode.getName())
      ) {
        newChildren.add(originalChildren.remove(0));
      } else {
        int start = newChildren.size() > 0
          ? newChildren.get(newChildren.size() - 1).getStop()
          : node.getStart();
        newChildren.add(
          new ParseTree(
            seqNode.getName(),
            seqNode.getName(), // Don't need to set name once we merge rules + fields
            node.getSource(),
            start,
            start,
            Optional.of(node)
          )
        );
      }
    }
    node.setChildren(newChildren);
  }

  private Optional<GrammarTree> findChildTree(
    GrammarRoot root,
    GrammarTree tree,
    NodeType nodeType
  ) {
    if (nodeType.getType().equals("ALIAS")) {
      // For aliases, we want to return the corresponding top-level rule.
      return Optional
        .ofNullable(tree.getAliasToRuleMap().get(nodeType.getName()))
        .map(ruleName -> root.getRuleOrThrow(ruleName));
    }
    Optional<GrammarTree> localChild = tree
      .getLocalChildList()
      .stream()
      .filter(child -> child.getNodeType().equals(nodeType))
      .findFirst();
    if (localChild.isPresent()) {
      return localChild;
    }
    return Optional.ofNullable(root.getRuleMap().get(nodeType.getName()));
  }

  private Optional<GrammarTree> findChildTree(GrammarRoot root, GrammarTree tree, String name) {
    // Grab the matcher tree when we don't know the type (field vs rule vs alias).
    // If there are duplicated field names, the results here may not be useful.
    Optional<GrammarTree> localChild = tree
      .getLocalChildList()
      .stream()
      .filter(child -> child.getNodeType().getName().equals(name))
      .findFirst();

    if (localChild.isPresent()) {
      return findChildTree(root, tree, localChild.get().getNodeType());
    } else if (tree.getAliasToRuleMap().containsKey(name)) {
      return Optional.ofNullable(root.getRuleMap().get(tree.getAliasToRuleMap().get(name)));
    }
    return Optional.ofNullable(root.getRuleMap().get(name));
  }

  private void postProcessNode(
    GrammarRoot root,
    GrammarTree tree,
    ParseTree node
  ) {
    if (node.getType().equals("ERROR")) {
      // Don't apply postprocessing to ERROR nodes.
      return;
    }

    // Recurse on children first, if any.
    List<String> unrecursableChildren = new ArrayList<>();
    if (node.getChildren().size() > 0) {
      List<ParseTree> newChildren = new ArrayList<>();

      Optional<Production> matchingSequence = findMatchingSequence(root, tree, node);
      for (ParseTree child : node.getChildren()) {
        Optional<GrammarTree> childTree;
        if (matchingSequence.isPresent() && !root.getWildCardRuleList().contains(child.getType())) {
          // This needs to exist since it's a valid sequence match.
          childTree =
            findChildTree(
              root,
              tree,
              matchingSequence
                .get()
                .getNodeTypeList()
                .stream()
                .filter(t -> t.getName().equals(child.getType()))
                .findFirst()
                .get()
            );
        } else {
          childTree = findChildTree(root, tree, child.getType());
        }

        if (childTree.isPresent()) {
          postProcessNode(root, childTree.get(), child);
        } else {
          unrecursableChildren.add(child.getType());
        }
        newChildren.add(child);
      }
      node.getChildren().stream().forEach(child -> child.setParent(Optional.of(node)));
    }

    mergeConsecutiveFields(root, tree, node);
    addPlaceholdersToTree(root, tree, node);
  }

  private ParseTree appendChildren(ParseTree node, List<ParseTree> newChildren) {
    List<ParseTree> children = new ArrayList<>(node.getChildren());

    children.addAll(newChildren);
    ParseTree result = new ParseTree(
      node.getType(),
      node.getName(),
      node.getSource(),
      node.getStart(),
      children.get(children.size() - 1).getStop(),
      Optional.empty()
    );
    result.setChildren(children);
    children.stream().forEach(child -> child.setParent(Optional.of(result)));
    return result;
  }

  private void mergeConsecutiveFields(GrammarRoot root, GrammarTree tree, ParseTree node) {
    // Fields (in tree-sitter), when they correspond to a sequence of rules, will be applied
    // to each rule in the sequence. So the input ParseTree might look like
    // <{ />
    // <StatementList>
    //   <Statement />
    // </StatementList>
    // <Comment />
    // <StatementList>
    //   <Statement />
    // </StatementList>
    // <} />
    // In this case, if StatementList are a field, we want to merge into one node (Comment nodes are "invisible").

    Set<String> fieldsToMerge = tree
      .getLocalChildList()
      .stream()
      .filter(
        child -> Arrays.asList("FIELD", "PLACEHOLDER").contains(child.getNodeType().getType())
      )
      .map(child -> child.getNodeType().getName())
      .collect(Collectors.toSet());

    if (fieldsToMerge.size() == 0 || node.getChildren().size() == 0) {
      return;
    } else {
      List<ParseTree> newChildren = new ArrayList<>();
      for (ParseTree child : node.getChildren()) {
        String previousChildType = newChildren.size() == 0
          ? ""
          : newChildren.get(newChildren.size() - 1).getType();

        if (fieldsToMerge.contains(previousChildType)) {
          if (root.getWildCardRuleList().contains(child.getType())) {
            // Comments/preprocessor will break consecutive field merging, so we merge them directly into the previous child.
            newChildren.add(
              appendChildren(newChildren.remove(newChildren.size() - 1), Arrays.asList(child))
            );
          } else if (previousChildType.equals(child.getType())) {
            newChildren.add(
              appendChildren(newChildren.remove(newChildren.size() - 1), child.getChildren())
            );
          } else {
            newChildren.add(child);
          }
        } else {
          newChildren.add(child);
        }
      }
      newChildren.stream().forEach(child -> child.setParent(Optional.of(node)));
      node.setChildren(newChildren);
    }
  }

  public void postProcessParseTree(Language language, ParseTree tree) {
    Optional
      .ofNullable(flattenedGrammars.get(language))
      .ifPresent(
        root -> {
          postProcessNode(root, root.getRuleMap().get(tree.getType()), tree);
        }
      );
  }
}
