package grammarflattener;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import grammar.gen.rpc.GrammarRoot;
import grammar.gen.rpc.GrammarTree;
import grammar.gen.rpc.NodeType;
import grammar.gen.rpc.Production;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class GrammarFlattener {

  private Gson gson = new Gson();

  // Make sure more permissive conditions are placed later in this list
  // or they will override specific constructs.
  private List<Function<JsonRule, Optional<GrammarTree>>> converters = Arrays.asList(
    this::convertString,
    this::convertPattern,
    this::convertBlank,
    this::convertSymbol,
    this::convertField,
    this::convertDelimiterSep,
    this::convertSeq,
    this::convertOptionalPlaceholder,
    this::convertChoice,
    this::convertPrec,
    this::convertRepeat,
    this::convertToken,
    this::convertSimpleAlias
  );

  @Inject
  public GrammarFlattener() {}

  private Set<String> inlinedRules(JsonRoot jsonRoot) {
    Set<String> inlined = new HashSet<>();
    inlined.addAll(jsonRoot.inline);
    inlined.addAll(jsonRoot.supertypes);
    // Any rule prefixed with _ is automatically inlined.
    inlined.addAll(
      jsonRoot.rules
        .keySet()
        .stream()
        .filter(name -> name.startsWith("_"))
        .collect(Collectors.toSet())
    );
    return inlined;
  }

  private GrammarTree convert(JsonRule jsonRule) {
    for (Function<JsonRule, Optional<GrammarTree>> converter : converters) {
      Optional<GrammarTree> tree = converter.apply(jsonRule);
      if (tree.isPresent()) {
        return tree.get();
      }
    }

    NodeType nodeType = NodeType
      .newBuilder()
      .setType("UNMATCHABLE")
      .setName("UNMATCHABLE_UNIMPLEMENTED")
      .build();
    return GrammarTree
      .newBuilder()
      .setNodeType(nodeType)
      .addFlattenedProduction(Production.newBuilder().addNodeType(nodeType))
      .build();
  }

  private Map<String, GrammarTree> convert(Map<String, JsonRule> rules) {
    Map<String, GrammarTree> result = new HashMap<>();
    for (Map.Entry<String, JsonRule> rule : rules.entrySet()) {
      GrammarTree child = convert(rule.getValue());
      child =
        GrammarTree
          .newBuilder(child)
          .setNodeType(NodeType.newBuilder().setType("SYMBOL").setName(rule.getKey()))
          .build();
      result.put(rule.getKey(), child);
    }
    return result;
  }

  private Optional<GrammarTree> convertString(JsonRule jsonRule) {
    if (jsonRule.type.equals("STRING")) {
      // These should be direct simple string matches.
      NodeType nodeType = NodeType.newBuilder().setType("STRING").setName(jsonRule.value).build();
      return Optional.of(
        GrammarTree
          .newBuilder()
          .setNodeType(nodeType)
          .addFlattenedProduction(Production.newBuilder().addNodeType(nodeType))
          .build()
      );
    }
    return Optional.empty();
  }

  private Optional<GrammarTree> convertPattern(JsonRule jsonRule) {
    if (jsonRule.type.equals("PATTERN")) {
      // These are regex matches which we don't handle.
      NodeType nodeType = NodeType
        .newBuilder()
        .setType("UNMATCHABLE")
        .setName("UNMATCHABLE_PATTERN")
        .build();
      return Optional.of(
        GrammarTree
          .newBuilder()
          .setNodeType(nodeType)
          .addFlattenedProduction(Production.newBuilder().addNodeType(nodeType))
          .build()
      );
    }
    return Optional.empty();
  }

  private Optional<GrammarTree> convertBlank(JsonRule jsonRule) {
    if (jsonRule.type.equals("BLANK")) {
      // Special case, adding an empty list as a valid token sequence
      // (taking nothing when it's an optional)
      // This node is just temporary since it'll be merged later.
      return Optional.of(
        GrammarTree.newBuilder().addFlattenedProduction(Production.newBuilder()).build()
      );
    }
    return Optional.empty();
  }

  private Optional<GrammarTree> convertSymbol(JsonRule jsonRule) {
    if (jsonRule.type.equals("SYMBOL")) {
      // References to top-level rules
      NodeType nodeType = NodeType.newBuilder().setType("SYMBOL").setName(jsonRule.name).build();
      return Optional.of(
        GrammarTree
          .newBuilder()
          .setNodeType(nodeType)
          .addFlattenedProduction(Production.newBuilder().addNodeType(nodeType))
          .build()
      );
    }
    return Optional.empty();
  }

  private Optional<GrammarTree> convertField(JsonRule jsonRule) {
    if (
      jsonRule.type.equals("FIELD") &&
      jsonRule.content.type.equals("CHOICE") &&
      jsonRule.content.members.size() == 2 &&
      jsonRule.content.members.get(1).type.equals("BLANK")
    ) {
      // Handle field('name', optional($.rule)) the same as optional(field('name', $.rule))
      NodeType nodeType = NodeType.newBuilder().setType("FIELD").setName(jsonRule.name).build();
      GrammarTree childTree = convert(jsonRule.content.members.get(0));

      // We change the child node type to finalize the childTree as the field's contents.
      childTree = GrammarTree.newBuilder(childTree).setNodeType(nodeType).build();

      return Optional.of(
        GrammarTree
          .newBuilder()
          .setNodeType(nodeType)
          .addFlattenedProduction(Production.newBuilder().addNodeType(nodeType))
          .addFlattenedProduction(Production.newBuilder())
          .addLocalChild(childTree)
          .build()
      );
    } else if (jsonRule.type.equals("FIELD")) {
      // field() construct in tree-sitter
      NodeType nodeType = NodeType.newBuilder().setType("FIELD").setName(jsonRule.name).build();
      GrammarTree childTree = convert(jsonRule.content);
      // We change the child node type to finalize the childTree as the field's contents.
      childTree = GrammarTree.newBuilder(childTree).setNodeType(nodeType).build();
      return Optional.of(
        GrammarTree
          .newBuilder()
          .setNodeType(nodeType)
          .addFlattenedProduction(Production.newBuilder().addNodeType(nodeType))
          .addLocalChild(childTree)
          .build()
      );
    }
    return Optional.empty();
  }

  private Optional<GrammarTree> convertDelimiterSep(JsonRule jsonRule) {
    // Detect and handle commaSep usage when there is a nested field name
    // The standard use case here is handling statements of the form:
    // commaSep1(field('field_name', rule))
    // However, using other string-matchable separators should work too.
    if (
      jsonRule.type.equals("SEQ") &&
      jsonRule.members.size() == 2 &&
      jsonRule.members.get(0).type.equals("FIELD") &&
      jsonRule.members.get(1).type.equals("REPEAT") &&
      jsonRule.members.get(1).content.type.equals("SEQ") &&
      jsonRule.members.get(1).content.members.get(0).type.equals("STRING") &&
      jsonRule.members.get(1).content.members.get(1).type.equals("FIELD") &&
      jsonRule.members.get(1).content.members.get(1).name.equals(jsonRule.members.get(0).name) &&
      jsonRule.members.get(1).content.members.get(1).content.equals(jsonRule.members.get(0).content)
    ) {
      // We treat this as a repeat -- not post-procesable but we can recurse into the nested field.
      return Optional.of(
        repeatGrammarTree(convert(jsonRule.members.get(1).content.members.get(1)))
      );
    }
    return Optional.empty();
  }

  private Optional<GrammarTree> convertSeq(JsonRule jsonRule) {
    if (jsonRule.type.equals("SEQ")) {
      // seq() construct in tree-sitter
      return Optional.of(
        mergeSeqGrammarTree(
          jsonRule.members.stream().map(child -> convert(child)).collect(Collectors.toList())
        )
      );
    }
    return Optional.empty();
  }

  private Optional<GrammarTree> convertOptionalPlaceholder(JsonRule jsonRule) {
    if (
      jsonRule.type.equals("CHOICE") &&
      jsonRule.members.size() == 2 &&
      jsonRule.members.get(0).type.equals("FIELD") &&
      jsonRule.members.get(1).type.equals("FIELD") &&
      jsonRule.members.get(1).content.type.equals("BLANK")
    ) {
      // Detect our special optional placeholder where we want to insert the field, if it's named.
      // Javascript code for this construct is:
      // function optional_with_placeholder(field_name, rule) {
      //   return choice(field(field_name, rule), field(field_name, blank()));
      // }
      NodeType nodeType = NodeType
        .newBuilder()
        .setType("PLACEHOLDER")
        .setName(jsonRule.members.get(1).name)
        .build();
      GrammarTree childTree = convert(jsonRule.members.get(0).content);
      // We change the child node type to finalize the childTree as the placeholder's contents.
      childTree = GrammarTree.newBuilder(childTree).setNodeType(nodeType).build();
      return Optional.of(
        GrammarTree
          .newBuilder()
          .setNodeType(nodeType)
          .addFlattenedProduction(Production.newBuilder().addNodeType(nodeType))
          .addLocalChild(childTree)
          .build()
      );
    }
    return Optional.empty();
  }

  private Optional<GrammarTree> convertChoice(JsonRule jsonRule) {
    if (jsonRule.type.equals("CHOICE")) {
      // choice() construct in tree-sitter
      return Optional.of(
        mergeChoiceGrammarTree(
          jsonRule.members.stream().map(child -> convert(child)).collect(Collectors.toList())
        )
      );
    }
    return Optional.empty();
  }

  private Optional<GrammarTree> convertPrec(JsonRule jsonRule) {
    if (
      jsonRule.type.equals("PREC") ||
      jsonRule.type.equals("PREC_LEFT") ||
      jsonRule.type.equals("PREC_DYNAMIC") ||
      jsonRule.type.equals("PREC_RIGHT")
    ) {
      // Prec nodes don't show up in the parse tree itself.
      return Optional.of(convert(jsonRule.content));
    }
    return Optional.empty();
  }

  private Optional<GrammarTree> convertRepeat(JsonRule jsonRule) {
    if (jsonRule.type.equals("REPEAT") || jsonRule.type.equals("REPEAT1")) {
      // Implementing matching for repeat is tricky, so we don't do post-processing on a node when
      // they are immediate children of a node.
      GrammarTree childTree = convert(jsonRule.content);
      return Optional.of(repeatGrammarTree(childTree));
    }
    return Optional.empty();
  }

  private Optional<GrammarTree> convertToken(JsonRule jsonRule) {
    if (jsonRule.type.equals("TOKEN") || jsonRule.type.equals("IMMEDIATE_TOKEN")) {
      // Generally regex patterns -- postprocessing should be unnecessary.
      NodeType nodeType = NodeType
        .newBuilder()
        .setType("UNMATCHABLE")
        .setName("UNMATCHABLE_TOKEN")
        .build();
      return Optional.of(
        GrammarTree
          .newBuilder()
          .setNodeType(nodeType)
          .addFlattenedProduction(Production.newBuilder().addNodeType(nodeType))
          .build()
      );
    }
    return Optional.empty();
  }

  private Optional<GrammarTree> convertSimpleAlias(JsonRule jsonRule) {
    if (jsonRule.type.equals("ALIAS") && jsonRule.named && jsonRule.content.type.equals("SYMBOL")) {
      // We only allow simple named aliasing of a single rule.
      // More complex aliasing should be avoided.

      NodeType nodeType = NodeType.newBuilder().setType("ALIAS").setName(jsonRule.value).build();
      return Optional.of(
        GrammarTree
          .newBuilder()
          .setNodeType(nodeType)
          .addFlattenedProduction(Production.newBuilder().addNodeType(nodeType))
          .putAliasToRule(jsonRule.value, jsonRule.content.name)
          .build()
      );
    } else if (
      jsonRule.type.equals("ALIAS") &&
      (
        jsonRule.content.type.equals("PATTERN") ||
        jsonRule.content.type.equals("TOKEN") ||
        jsonRule.content.type.equals("STRING") ||
        (jsonRule.content.type.equals("SYMBOL") && !jsonRule.named)
      )
    ) {
      // Allow aliasing patterns as nodes we don't recurse into.
      NodeType nodeType = NodeType.newBuilder().setType("ALIAS").setName(jsonRule.value).build();
      return Optional.of(
        GrammarTree
          .newBuilder()
          .setNodeType(nodeType)
          .addFlattenedProduction(Production.newBuilder().addNodeType(nodeType))
          .build()
      );
    } else if (jsonRule.type.equals("ALIAS")) {
      // Not throwing error for certain rules that are necessary but don't affect flattening.
      if (jsonRule.value.equals("\"")) {
        // Leaf nodes, so we don't need to recurse into them.
        NodeType nodeType = NodeType.newBuilder().setType("ALIAS").setName(jsonRule.value).build();
        return Optional.of(
          GrammarTree
            .newBuilder()
            .setNodeType(nodeType)
            .addFlattenedProduction(Production.newBuilder().addNodeType(nodeType))
            .build()
        );
      }

      // Other alias types are unsupported.
      NodeType nodeType = NodeType
        .newBuilder()
        .setType("UNIMPLEMENTED_ALIAS")
        .build();
      return Optional.of(
        GrammarTree
          .newBuilder()
          .setNodeType(nodeType)
          .addFlattenedProduction(Production.newBuilder().addNodeType(nodeType))
          .build()
      );
    }
    return Optional.empty();
  }

  private GrammarRoot create(String language, JsonRoot jsonRoot) {
    GrammarRoot.Builder result = GrammarRoot.newBuilder();

    result = result.addWildCardRule("comment");
    result = result.addWildCardRule("line_comment");
    result = result.addWildCardRule("block_comment");
    if (language.equals("csharp")) {
      result = result.addWildCardRule("preprocessor_call");
    }

    Map<String, GrammarTree> rules = convert(jsonRoot.rules);
    rules = expandInlinedRulesAndPrune(rules, inlinedRules(jsonRoot));
    result = result.putAllRule(rules);
    return result.build();
  }

  private GrammarTree expandInlinedRules(
    GrammarTree tree,
    Set<String> inlinedRules,
    Map<String, GrammarTree> rules,
    int maxRecursiveSteps
  ) {
    if (
      tree.getFlattenedProductionList().size() == 0 ||
      tree.getFlattenedProductionList().size() > 500
    ) {
      return tree;
    }

    // Recursive steps is set to 1 here, so that the recursion happens in concert with the parent recursion.
    List<GrammarTree> expandedChildren = tree
      .getLocalChildList()
      .stream()
      .map(child -> expandInlinedRules(child, inlinedRules, rules, 1))
      .collect(Collectors.toList());
    tree =
      GrammarTree.newBuilder(tree).clearLocalChild().addAllLocalChild(expandedChildren).build();

    Set<GrammarTree> localChildren = new HashSet<>(tree.getLocalChildList());
    Map<String, String> aliasToRule = new HashMap<>(tree.getAliasToRuleMap());
    List<Production> newProductions = new ArrayList<>();
    // We use cartesian products to construct new sequences similar to expanding SEQs.
    for (Production seq : tree.getFlattenedProductionList()) {
      List<List<Production>> expansionByPosition = new ArrayList<>();
      for (NodeType nodeType : seq.getNodeTypeList()) {
        if (nodeType.getType().equals("SYMBOL") && inlinedRules.contains(nodeType.getName())) {
          GrammarTree rule = rules.get(nodeType.getName());
          expansionByPosition.add(rule.getFlattenedProductionList());
          localChildren.addAll(rule.getLocalChildList());
          aliasToRule.putAll(rule.getAliasToRuleMap());
        } else {
          expansionByPosition.add(
            Arrays.asList(Production.newBuilder().addNodeType(nodeType).build())
          );
        }
      }

      // After taking cartsian product of possibilities at each position, we need to flatten the
      // interior lists
      List<List<Production>> cartesianProductCommands = Lists.cartesianProduct(expansionByPosition);

      for (List<Production> listToConcat : cartesianProductCommands) {
        newProductions.add(
          Production
            .newBuilder()
            .addAllNodeType(
              listToConcat
                .stream()
                .flatMap(p -> p.getNodeTypeList().stream())
                .collect(Collectors.toList())
            )
            .build()
        );
      }
    }
    tree =
      GrammarTree
        .newBuilder(tree)
        .clearFlattenedProduction()
        .clearLocalChild()
        .addAllFlattenedProduction(newProductions)
        .addAllLocalChild(localChildren)
        .putAllAliasToRule(aliasToRule)
        .build();

    // To prevent infinite recursion due to cycles in hidden rules, we pass in an upper bound on the number
    // of times we expand the inlined rules.
    if (maxRecursiveSteps > 1) {
      return expandInlinedRules(tree, inlinedRules, rules, maxRecursiveSteps - 1);
    }
    return tree;
  }

  private Map<String, GrammarTree> expandInlinedRulesAndPrune(
    Map<String, GrammarTree> rules,
    Set<String> inlinedRules
  ) {
    Map<String, GrammarTree> result = new HashMap<>();
    for (Map.Entry<String, GrammarTree> rule : rules.entrySet()) {
      // Expand hidden nodes (do two levels of recursion)
      GrammarTree tree = expandInlinedRules(rule.getValue(), inlinedRules, rules, 3);

      // We fill in placeholder optionals by existing placeholders from both the production and also
      // the ParseTree's children, and then look for exact matches.
      Set<Production> deduplicatedProductions = new HashSet<Production>(
        tree.getFlattenedProductionList()
      );
      deduplicatedProductions.removeAll(tree
      .getFlattenedProductionList()
      .stream()
      .filter(e -> !productionPostprocessable(e))
      .collect(Collectors.toSet()));
      result.put(
        rule.getKey(),
        GrammarTree
          .newBuilder(tree)
          .clearFlattenedProduction()
          .addAllFlattenedProduction(deduplicatedProductions)
          .build()
      );
    }
    return result;
  }

  private GrammarTree mergeSeqGrammarTree(List<GrammarTree> nodes) {
    Set<GrammarTree> allFieldChildren = new HashSet<>();
    nodes.stream().forEach(node -> allFieldChildren.addAll(node.getLocalChildList()));

    Map<String, String> allAliasToRule = new HashMap<>();
    nodes.stream().forEach(node -> allAliasToRule.putAll(node.getAliasToRuleMap()));

    List<Production> productions = new ArrayList<>();
    // The input nodes sequentially (i.e. seq(this, other1, other2, ...))
    List<List<Production>> typesByPosition = nodes
      .stream()
      .map(node -> node.getFlattenedProductionList())
      .collect(Collectors.toList());

    // After taking cartsian product of possibilities at each position, we need to flatten the
    // interior lists
    List<List<Production>> cartesianProductCommands = Lists.cartesianProduct(typesByPosition);
    for (List<Production> listToConcat : cartesianProductCommands) {
      productions.add(
        Production
          .newBuilder()
          .addAllNodeType(
            listToConcat
              .stream()
              .flatMap(p -> p.getNodeTypeList().stream())
              .collect(Collectors.toList())
          )
          .build()
      );
    }
    return GrammarTree
      .newBuilder()
      .addAllFlattenedProduction(productions)
      .addAllLocalChild(allFieldChildren)
      .putAllAliasToRule(allAliasToRule)
      .build();
  }

  private GrammarTree mergeChoiceGrammarTree(List<GrammarTree> nodes) {
    Set<GrammarTree> allFieldChildren = new HashSet<>();
    nodes.stream().forEach(node -> allFieldChildren.addAll(node.getLocalChildList()));

    Map<String, String> allAliasToRule = new HashMap<>();
    nodes.stream().forEach(node -> allAliasToRule.putAll(node.getAliasToRuleMap()));

    List<Production> productions = new ArrayList<>();
    nodes.stream().forEach(node -> productions.addAll(node.getFlattenedProductionList()));

    return GrammarTree
      .newBuilder()
      .addAllFlattenedProduction(productions)
      .addAllLocalChild(allFieldChildren)
      .putAllAliasToRule(allAliasToRule)
      .build();
  }

  private GrammarTree repeatGrammarTree(GrammarTree repeatedChild) {
    // REPEATs are not matchable, but we propagate inner fields so we can do named-field
    // merging and recursion.
    NodeType nodeType = NodeType
      .newBuilder()
      .setType("UNMATCHABLE_PATTERN")
      .setName("UNMATCHABLE_REPEAT")
      .build();
    List<Production> productions = repeatedChild
      .getFlattenedProductionList()
      .stream()
      .map(
        production ->
          Production
            .newBuilder()
            .addNodeType(nodeType)
            .addAllNodeType(production.getNodeTypeList())
            .build()
      )
      .collect(Collectors.toList());

    return GrammarTree
      .newBuilder()
      .setNodeType(nodeType)
      .addAllFlattenedProduction(productions)
      .putAllAliasToRule(repeatedChild.getAliasToRuleMap())
      .addAllLocalChild(repeatedChild.getLocalChildList())
      .build();
  }

  private boolean productionPostprocessable(Production production) {
    if (
      !production
        .getNodeTypeList()
        .stream()
        .anyMatch(nodeType -> Arrays.asList("FIELD", "PLACEHOLDER").contains(nodeType.getType()))
    ) {
      // If no FIELDs or PLACEHOLDERs, no postprocessing necessary.
      return true;
    }
    if (
      production
        .getNodeTypeList()
        .stream()
        .anyMatch(nodeType -> nodeType.getType().equals("UNMATCHABLE"))
    ) {
      return false;
    }

    // If all the intermediate token names are unique, it's matchable.
    List<NodeType> intermediateNodes = production
      .getNodeTypeList()
      .stream()
      .filter(
        nodeType ->
          Arrays.asList("FIELD", "PLACEHOLDER", "ALIAS", "SYMBOL").contains(nodeType.getType())
      )
      .collect(Collectors.toList());

    return (
      intermediateNodes.size() ==
      intermediateNodes
        .stream()
        .map(nodeType -> nodeType.getName())
        .collect(Collectors.toSet())
        .size()
    );
  }

  public void create(String input, String language, String output) {
    JsonRoot jsonRoot;
    try {
      jsonRoot =
        gson.fromJson(
          Files.readString(Path.of(input, "src", "grammar.json"), Charsets.UTF_8),
          JsonRoot.class
        );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    GrammarRoot root = create(language, jsonRoot);
    if (!output.endsWith("/")) {
      output += "/";
    }

    try {
      Files.createDirectories(Paths.get(output));
    } catch (Exception e) {}

    try (FileOutputStream stream = new FileOutputStream(output + language + "-grammar.pb")) {
      stream.write(root.toByteArray());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) {
    ArgumentParser parser = ArgumentParsers
      .newFor("GrammarFlattener")
      .build()
      .defaultHelp(true)
      .description("Flattens tree-sitter grammars to determine where placeholder nodes go");
    parser.addArgument("input").type(String.class).help("Input directory").required(true);
    parser.addArgument("language").type(String.class).help("Language name").required(true);
    parser.addArgument("output").type(String.class).help("Output directory").required(true);

    Namespace namespace = null;
    try {
      namespace = parser.parseArgs(args);
    } catch (ArgumentParserException e) {
      parser.handleError(e);
      System.exit(1);
    }

    GrammarFlattenerComponent component = DaggerGrammarFlattenerComponent.builder().build();
    GrammarFlattener grammarFlattener = component.grammarFlattener();
    grammarFlattener.create(
      namespace.getString("input"),
      namespace.getString("language"),
      namespace.getString("output")
    );
  }
}
