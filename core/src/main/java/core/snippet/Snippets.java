package core.snippet;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import core.ast.Ast;
import core.ast.AstFactory;
import core.ast.CSharpAst;
import core.ast.GoAst;
import core.ast.api.AstList;
import core.ast.api.AstListOptional;
import core.ast.api.AstListWithLinePadding;
import core.ast.api.AstNewline;
import core.ast.api.AstNode;
import core.ast.api.AstOptional;
import core.ast.api.AstParent;
import core.ast.api.AstSyntaxError;
import core.ast.api.DefaultAstParent;
import core.ast.api.IndentationUtils;
import core.closeness.ClosestObjectFinder;
import core.codeengine.Resolver;
import core.exception.CannotFindInsertionPoint;
import core.exception.ObjectNotFound;
import core.exception.SafeToDisplayException;
import core.formattedtext.FormattedTextOptions;
import core.gen.rpc.Language;
import core.metadata.DiffWithMetadata;
import core.util.Diff;
import core.util.LinePositionConverter;
import core.util.Range;
import core.util.Whitespace;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;

public class Snippets {

  @FunctionalInterface
  public static interface GenerateTemplatedCode {
    String generate(AstParent parent, Map<String, String> matches);
  }

  @FunctionalInterface
  public static interface GetNode {
    AstParent get(AstParent root);
  }

  @FunctionalInterface
  public static interface FindContainer<ContainerType extends AstParent> {
    ContainerType find(String source, int cursor, AstParent root);
  }

  @FunctionalInterface
  public static interface InsertNode<ContainerType extends AstParent> {
    void insert(String source, int cursor, AstParent root, ContainerType container, AstParent node);
  }

  private Cache<SnippetCacheKey, DiffWithMetadata> cache;
  private ClosestObjectFinder closestObjectFinder;
  private Language language;
  private AstFactory factory;
  private Whitespace whitespace;
  private IndentationUtils indentationUtils;
  private Resolver resolver;

  @AssistedInject
  public Snippets(
    Cache<SnippetCacheKey, DiffWithMetadata> cache,
    AstFactory factory,
    ClosestObjectFinder closestObjectFinder,
    Whitespace whitespace,
    IndentationUtils indentationUtils,
    Resolver resolver,
    @Assisted Language language
  ) {
    this.cache = cache;
    this.closestObjectFinder = closestObjectFinder;
    this.factory = factory;
    this.language = language;
    this.whitespace = whitespace;
    this.indentationUtils = indentationUtils;
    this.resolver = resolver;
  }

  @AssistedFactory
  public interface Factory {
    Snippets create(Language language);
  }

  private void addAtLineAndClearBlankLineAbove(AstList<?> list, int line, AstParent element) {
    // snippet-specific behavior for adding at a line.
    if (line > 0 && list.tree().whitespace.isWhitespace(line - 1)) {
      Range lineTokenRange = list.tree().whitespace.lineTokenRange(line - 1);
      list
        .tree()
        .tokens()
        .subList(
          lineTokenRange.start,
          Math.min(list.tree().tokens().size(), lineTokenRange.stop + 1)
        )
        .clear();
      list.addAtLine(line - 1, element);
    } else {
      list.addAtLine(line, element);
    }
  }

  private AstList findInsertionListBasedOnParent(String source, int cursor, AstParent root) {
    List<Class> eligibleListTypes = Arrays.asList(
      Ast.ClassMemberList.class,
      Ast.EnumMemberList.class,
      Ast.InterfaceMemberList.class,
      Ast.KeyValuePairList.class,
      Ast.List_.class,
      Ast.MarkupContentList.class,
      Ast.StatementList.class,
      Ast.StructMemberList.class,
      Ast.InitializerExpressionList.class,
      Ast.ImportSpecifierList.class,
      Ast.UseClauseList.class,
      Ast.ObjectPatternList.class,
      CSharpAst.InitializerExpressionList.class,
      GoAst.ConstSpecList.class
    );
    AstList list;
    try {
      list =
        closestObjectFinder.closestList(
          root,
          source,
          root
            .find(AstList.class)
            .filter(
              e -> eligibleListTypes.stream().filter(lt -> lt.isInstance(e)).findFirst().isPresent()
            ),
          cursor
        );
    } catch (ObjectNotFound e) {
      throw new CannotFindInsertionPoint();
    }

    return list;
  }

  private <T extends AstParent> GetNode instantiate(Class<T> nodeType) {
    return p -> {
      try {
        return nodeType.getDeclaredConstructor().newInstance();
      } catch (
        NoSuchMethodException
        | InstantiationException
        | IllegalAccessException
        | InvocationTargetException e
      ) {
        throw new RuntimeException("Error instantiating " + nodeType, e);
      }
    };
  }

  private InsertNode<AstListOptional> listOptionalInsert(
    Supplier<? extends AstList<?>> listFactory
  ) {
    return (source, cursor, root, container, node) -> {
      AstList<?> list = container
        .optional()
        .orElseGet(
          () -> {
            AstList<?> newList = factory.create(listFactory.get());
            container.set(newList);
            return newList;
          }
        );

      addToListInstance(source, cursor, root, list, node);
    };
  }

  private FindContainer<AstListOptional> listOptionalWithType(
    Class<? extends AstParent> nodeType,
    Supplier<? extends AstList<?>> listFactory
  ) {
    return (source, cursor, root) -> {
      AstListOptional listOptional;
      try {
        listOptional =
          closestObjectFinder.closestNode(
            root,
            source,
            root
              .find(AstListOptional.class)
              .filter(
                e -> e.canAdd(listFactory.get().getClass()) && e.isInnerElementType(nodeType)
              ),
            cursor
          );
      } catch (ObjectNotFound e) {
        throw new CannotFindInsertionPoint();
      }

      return listOptional;
    };
  }

  private FindContainer<AstList<?>> listWithElementType(Class<? extends AstParent> elementType) {
    return (source, cursor, root) -> {
      AstList<?> list;
      try {
        list =
          closestObjectFinder.closestList(
            root,
            source,
            root.find(AstList.class).filter(e -> ((AstList<?>) e).canAdd(elementType)),
            cursor
          );
      } catch (ObjectNotFound e) {
        throw new CannotFindInsertionPoint();
      }

      return list;
    };
  }

  private FindContainer<AstOptional<?>> optionalWithElementType(
    Class<? extends AstNode> elementType
  ) {
    return (source, cursor, root) -> {
      AstOptional<?> optionalNode;
      try {
        optionalNode =
          closestObjectFinder.closestNode(
            root,
            source,
            root
              .find(AstOptional.class)
              .map(e -> (AstOptional<?>) e)
              .filter(e -> e.canAdd(elementType) && e.optional().isEmpty()),
            cursor
          );
      } catch (ObjectNotFound e) {
        throw new CannotFindInsertionPoint();
      }

      return optionalNode;
    };
  }

  private FindContainer<DefaultAstParent> containerWithElementType(
    Class<? extends AstNode> elementType
  ) {
    return (source, cursor, root) -> {
      DefaultAstParent container;
      try {
        Stream<DefaultAstParent> optionalsStream = root
          .find(AstOptional.class)
          .map(e -> (AstOptional<?>) e)
          .filter(e -> e.canAdd(elementType) && e.optional().isEmpty())
          .map(e -> (DefaultAstParent) e);
        Stream<DefaultAstParent> listsStream = root
          .find(AstList.class)
          .filter(e -> ((AstList<?>) e).canAdd(elementType))
          .map(e -> (DefaultAstParent) e);
        container =
          closestObjectFinder.closestNode(
            root,
            source,
            Stream.concat(optionalsStream, listsStream),
            cursor
          );

        if (container instanceof AstList) {
          container =
            closestObjectFinder.closestList(
              root,
              source,
              root.find(AstList.class).filter(e -> ((AstList<?>) e).canAdd(elementType)),
              cursor
            );
        }
      } catch (ObjectNotFound e) {
        throw new CannotFindInsertionPoint();
      }

      return container;
    };
  }

  private void setOptional(
    String source,
    int cursor,
    AstParent root,
    AstOptional<?> container,
    AstParent node
  ) {
    container.set(node);
  }

  private void addToContainer(
    String source,
    int cursor,
    AstParent root,
    DefaultAstParent container,
    AstParent node
  ) {
    if (container instanceof AstOptional) {
      setOptional(source, cursor, root, (AstOptional<?>) container, node);
    } else if (container instanceof AstList) {
      addToListInstance(source, cursor, root, (AstList<?>) container, node);
    }
  }

  public Snippet addComment(
    SnippetTrigger trigger,
    Class<? extends AstParent> nodeType,
    GenerateTemplatedCode generateTemplatedCode
  ) {
    return snippet(
      trigger,
      instantiate(nodeType),
      (source, cursor, root) -> {
        AstListWithLinePadding<?> list;
        try {
          list =
            closestObjectFinder.closestList(
              root,
              source,
              root.find(AstListWithLinePadding.class),
              cursor
            );
        } catch (ObjectNotFound e) {
          throw new CannotFindInsertionPoint();
        }

        return list;
      },
      generateTemplatedCode,
      (source, cursor, root, container, node) -> {
        int linePosition = source.contains("\n")
          ? new LinePositionConverter(source).position(cursor) + 1
          : 0;

        List<Integer> availableLines = container.availableLines();
        if (availableLines.contains(linePosition)) {
          addAtLineAndClearBlankLineAbove(container, linePosition, node);
        } else {
          String indentation = whitespace.indentationAtCursor(
            source,
            whitespace.previousNewline(source, cursor)
          );

          node.tokens().add(0, factory.createToken(indentation));
          root.tree().addTokensAtLine(linePosition, node);
        }
      },
      Map.of()
    );
  }

  public Snippet addToListWithMlSnippet(
    SnippetTrigger trigger,
    Class<? extends AstParent> nodeType
  ) {
    return mlSnippet(
      trigger,
      instantiate(nodeType),
      listWithElementType(nodeType),
      this::addToListInstance
    );
  }

  public Snippet addToList(
    SnippetTrigger trigger,
    Class<? extends AstParent> nodeType,
    GenerateTemplatedCode generateTemplatedCode
  ) {
    return snippet(
      trigger,
      instantiate(nodeType),
      listWithElementType(nodeType),
      generateTemplatedCode,
      this::addToListInstance,
      Map.of()
    );
  }

  public Snippet addToList(
    SnippetTrigger trigger,
    Class<? extends AstParent> nodeType,
    GenerateTemplatedCode generateTemplatedCode,
    Map<String, List<FormattedTextOptions>> options
  ) {
    return snippet(
      trigger,
      instantiate(nodeType),
      listWithElementType(nodeType),
      generateTemplatedCode,
      this::addToListInstance,
      options
    );
  }

  public void addToListInstance(
    String source,
    int cursor,
    AstParent root,
    AstList<?> list,
    AstParent node
  ) {
    if (list.isMultiline()) {
      int linePosition = new LinePositionConverter(source).position(cursor) + 1;
      List<Integer> availableLines = list.availableLines();
      if (root.tokens().stream().noneMatch(AstNewline.class::isInstance)) {
        list.addAtLine(0, node);
      } else if (
        linePosition >= availableLines.get(0) &&
        linePosition <= availableLines.get(availableLines.size() - 1)
      ) {
        int adjustedLinePosition = availableLines
          .stream()
          .filter(e -> e >= linePosition)
          .findFirst()
          .get();
        addAtLineAndClearBlankLineAbove(list, adjustedLinePosition, node);
      } else if (linePosition < availableLines.get(0)) {
        addAtLineAndClearBlankLineAbove(list, availableLines.get(0), node);
      } else {
        addAtLineAndClearBlankLineAbove(list, availableLines.get(availableLines.size() - 1), node);
      }
    } else {
      if (cursor >= list.range().start && cursor <= list.range().stop) {
        // touching the start of the element inserts before, otherwise insert after
        int elementIndex = 0;
        while (
          elementIndex < list.elements().size() &&
          cursor > list.elements().get(elementIndex).range().start
        ) {
          elementIndex++;
        }
        list.add(elementIndex, node);
      } else {
        list.add(list.elements().size(), node);
      }
    }
  }

  public Snippet addToListOptional(
    SnippetTrigger trigger,
    Class<? extends AstParent> nodeType,
    Supplier<? extends AstList<?>> listFactory,
    GenerateTemplatedCode generateTemplatedCode
  ) {
    return addToListOptional(trigger, nodeType, listFactory, generateTemplatedCode, Map.of());
  }

  public Snippet addToListOptional(
    SnippetTrigger trigger,
    Class<? extends AstParent> nodeType,
    Supplier<? extends AstList<?>> listFactory,
    GenerateTemplatedCode generateTemplatedCode,
    Map<String, List<FormattedTextOptions>> options
  ) {
    return snippet(
      trigger,
      instantiate(nodeType),
      listOptionalWithType(nodeType, listFactory),
      generateTemplatedCode,
      listOptionalInsert(listFactory),
      options
    );
  }

  public Snippet addToListOptionalWithMlSnippet(
    SnippetTrigger trigger,
    Class<? extends AstParent> nodeType,
    Supplier<? extends AstList<?>> listFactory
  ) {
    return mlSnippet(
      trigger,
      instantiate(nodeType),
      listOptionalWithType(nodeType, listFactory),
      listOptionalInsert(listFactory)
    );
  }

  public Snippet addToOptional(
    SnippetTrigger trigger,
    Class<? extends AstParent> nodeType,
    GenerateTemplatedCode generateTemplatedCode
  ) {
    return addToOptional(trigger, nodeType, generateTemplatedCode, Map.of());
  }

  public Snippet addToOptional(
    SnippetTrigger trigger,
    Class<? extends AstParent> nodeType,
    GenerateTemplatedCode generateTemplatedCode,
    Map<String, List<FormattedTextOptions>> options
  ) {
    return snippet(
      trigger,
      instantiate(nodeType),
      optionalWithElementType(nodeType),
      generateTemplatedCode,
      this::setOptional,
      options
    );
  }

  public Snippet addToOptionalWithMlSnippet(
    SnippetTrigger trigger,
    Class<? extends AstParent> nodeType
  ) {
    return mlSnippet(
      trigger,
      instantiate(nodeType),
      optionalWithElementType(nodeType),
      this::setOptional
    );
  }

  public Snippet addToContainerWithMlSnippet(
    SnippetTrigger trigger,
    Class<? extends AstParent> nodeType
  ) {
    return mlSnippet(
      trigger,
      instantiate(nodeType),
      containerWithElementType(nodeType),
      this::addToContainer
    );
  }

  public Snippet addToTopLevelStatementList(
    SnippetTrigger trigger,
    Class<? extends AstParent> nodeType,
    GenerateTemplatedCode generateTemplatedCode,
    Map<String, List<FormattedTextOptions>> options
  ) {
    return snippet(
      trigger,
      instantiate(nodeType),
      (source, cursor, root) ->
        root
          .find(Ast.StatementList.class)
          .filter(e -> e.ancestor(Ast.StatementList.class).isEmpty())
          .findFirst()
          .orElseThrow(() -> new CannotFindInsertionPoint()),
      generateTemplatedCode,
      this::addToListInstance,
      options
    );
  }

  public Snippet addToTopLevelStatementList(
    SnippetTrigger trigger,
    Class<? extends AstParent> nodeType,
    GenerateTemplatedCode generateTemplatedCode
  ) {
    return addToTopLevelStatementList(trigger, nodeType, generateTemplatedCode, Map.of());
  }

  public Snippet addToTopLevelStatementListWithMlSnippet(
    SnippetTrigger trigger,
    Class<? extends AstParent> nodeType
  ) {
    return mlSnippet(
      trigger,
      instantiate(nodeType),
      (source, cursor, root) -> {
        return closestObjectFinder.closestList(
          root,
          source,
          root
            .find(Ast.StatementList.class)
            .filter(e -> e.ancestor(Ast.StatementList.class).isEmpty()),
          cursor
        );
      },
      this::addToListInstance
    );
  }

  public Snippet addBasedOnParentWithMlSnippet(SnippetTrigger trigger) {
    return mlSnippet(
      trigger,
      e -> new Ast.Statement(), // Appropriate wrapping done in AstList
      this::findInsertionListBasedOnParent,
      this::addToListInstance
    );
  }

  public Snippet addBasedOnParent(
    SnippetTrigger trigger,
    Map<String, List<FormattedTextOptions>> options
  ) {
    return snippet(
      trigger,
      e -> new Ast.Statement(), // Appropriate wrapping done in AstList
      this::findInsertionListBasedOnParent,
      (parent, metadata) -> {
        if (
          parent instanceof Ast.EnumMemberList ||
          parent instanceof Ast.List_ ||
          parent instanceof Ast.KeyValuePairList
        ) {
          return "<%expression%>";
        }

        return "<%expression%><%cursor%><%terminator%>";
      },
      this::addToListInstance,
      options
    );
  }

  public Snippet insert(
    SnippetTrigger trigger,
    GenerateTemplatedCode generateTemplatedCode,
    Map<String, List<FormattedTextOptions>> options,
    boolean below
  ) {
    return new Snippet(
      trigger,
      (source, cursor, root, transcript, queue, internal) -> {
        Map<String, String> slotsToEnglish = trigger.getSlotValuesFromTranscript(transcript);
        String generated = generateTemplatedCode.generate(new DefaultAstParent(), slotsToEnglish);
        Diff diff;
        if (below) {
          diff =
            Diff
              .fromInitialState(source, cursor)
              .insertStringAndMoveCursorToStop(
                whitespace.lineEnd(source, cursor),
                "\n" + whitespace.indentationAtCursor(source, cursor)
              );
        } else {
          diff = Diff.fromInitialState(source, cursor);
        }

        return resolver
          .resolve(
            diff.withoutChanges(),
            new Range(diff.getCursor(), diff.getCursor()),
            language,
            generated,
            slotsToEnglish,
            options,
            Optional.empty(),
            queue,
            false
          )
          .thenApply(
            l ->
              l
                .stream()
                .map(
                  d -> {
                    d.diff = diff.then(d.diff);
                    return d;
                  }
                )
                .collect(Collectors.toList())
          );
      }
    );
  }

  public <ContainerType extends AstParent> Snippet mlSnippet(
    SnippetTrigger trigger,
    GetNode getNode,
    FindContainer<ContainerType> findContainer,
    InsertNode<ContainerType> insertNode
  ) {
    return snippet(
      trigger,
      getNode,
      findContainer,
      (c, m) -> "<%snippet%>",
      insertNode,
      Map.of(),
      true
    );
  }

  public <ContainerType extends AstParent> Snippet snippet(
    SnippetTrigger trigger,
    GetNode getNode,
    FindContainer<ContainerType> findContainer,
    GenerateTemplatedCode generateTemplatedCode,
    InsertNode<ContainerType> insertNode,
    Map<String, List<FormattedTextOptions>> options
  ) {
    return snippet(
      trigger,
      getNode,
      findContainer,
      generateTemplatedCode,
      insertNode,
      options,
      false
    );
  }

  public <ContainerType extends AstParent> Snippet snippet(
    SnippetTrigger trigger,
    GetNode getNode,
    FindContainer<ContainerType> findContainer,
    GenerateTemplatedCode generateTemplatedCode,
    InsertNode<ContainerType> insertNode,
    Map<String, List<FormattedTextOptions>> options,
    boolean mlSnippet
  ) {
    return new Snippet(
      trigger,
      (source, cursor, root, transcript, queue, internal) -> {
        String placeholder = resolver.wrapInSlot(UUID.randomUUID().toString());

        ContainerType container = findContainer.find(source, cursor, root);
        AstParent node = factory.create(getNode.get(container), placeholder);
        insertNode.insert(source, cursor, root, container, node);

        // use the node's actual parent to minimize risk of misalignment with training data
        Optional<AstParent> snippetContainer = Optional.empty();
        if (mlSnippet) {
          snippetContainer = node.parent();

          Optional<AstParent> parent = node.parent();
          Optional<AstParent> grandparent = parent.flatMap(p -> p.parent());
          if (parent.isPresent() && grandparent.isPresent()) {
            Optional<AstList> list = grandparent
              .filter(l -> l instanceof AstList)
              .map(l -> (AstList) l);
            if (list.isPresent() && list.get().elementType().isInstance(parent.get())) {
              snippetContainer = grandparent;
            }
          }
        }

        String sourceWithPlaceholder = root.codeWithCommentsAndWhitespace();
        int insertionPoint = sourceWithPlaceholder.indexOf(placeholder);
        String sourceWithInsertionPoint =
          sourceWithPlaceholder.substring(0, insertionPoint) +
          sourceWithPlaceholder.substring(insertionPoint + placeholder.length());

        Map<String, String> slotsToEnglish = trigger.getSlotValuesFromTranscript(transcript);
        String templated = generateTemplatedCode.generate(container, slotsToEnglish);
        if (mlSnippet) {
          slotsToEnglish.clear();
          slotsToEnglish.put("snippet", transcript);
        }

        // diff only aggregates changes to the original source, so we need to recalculate the
        // diff to be able to combine the source replacement and the insertion.
        return resolver
          .resolve(
            Diff.fromInitialState(sourceWithInsertionPoint, cursor),
            new Range(insertionPoint, insertionPoint),
            language,
            templated,
            slotsToEnglish,
            options,
            snippetContainer,
            queue,
            internal
          )
          .thenApply(
            diffs -> {
              for (DiffWithMetadata diff : diffs) {
                diff.diff =
                  Diff
                    .fromInitialState(source, cursor)
                    .replaceSource(diff.diff.getSource())
                    .moveCursor(diff.diff.getCursor());
              }

              return diffs;
            }
          );
      }
    );
  }
}
