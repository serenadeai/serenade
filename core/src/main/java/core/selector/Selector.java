package core.selector;

import core.ast.AstFactory;
import core.ast.api.AstNode;
import core.ast.api.AstParent;
import core.ast.api.AstSyntaxError;
import core.closeness.ClosestObjectFinder;
import core.exception.CannotDetermineLanguage;
import core.exception.ObjectNotFound;
import core.exception.ObjectTooFarAway;
import core.gen.rpc.Language;
import core.util.LinePositionConverter;
import core.util.ObjectType;
import core.util.Range;
import core.util.SearchDirection;
import core.util.TextStyler;
import core.util.Whitespace;
import core.util.selection.Selection;
import core.util.selection.SelectionEndpoint;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;

public class Selector {

  private AstFactory astFactory;
  private ClosestObjectFinder closestObjectFinder;
  private SelectorMap selectorMap;
  private TextStyler textStyler;
  private Whitespace whitespace;
  private Language language;

  @AssistedInject
  public Selector(
    ClosestObjectFinder closestObjectFinder,
    TextStyler textStyler,
    Whitespace whitespace,
    AstFactory astFactory,
    core.selector.SelectorMapFactory selectorMapFactory,
    @Assisted Language language
  ) {
    this.closestObjectFinder = closestObjectFinder;
    this.textStyler = textStyler;
    this.whitespace = whitespace;
    this.astFactory = astFactory;
    this.language = language;
    this.selectorMap = selectorMapFactory.create(language);
  }

  @AssistedFactory
  public interface Factory {
    Selector create(Language language);
  }

  private <T, S extends SelectionContext<T>> Stream<T> applySelectorsToContext(
    Map<ObjectType, Function<S, Stream<T>>> selectors,
    S ctx
  ) {
    Function<S, Stream<T>> f = selectors.get(ctx.selection.object);
    if (f == null) {
      return Stream.empty();
    }

    return f.apply(ctx);
  }

  private Range reasonableRangeFromSelection(
    String source,
    int cursor,
    Selection selection,
    boolean expanded
  ) {
    // these object types are often far away, so don't consider these unreasonable
    Set<ObjectType> exclude = Set.of(
      ObjectType.CLASS,
      ObjectType.CONSTRUCTOR,
      ObjectType.FUNCTION,
      ObjectType.METHOD,
      ObjectType.PARAMETER,
      ObjectType.PARENT
    );

    Range result = rangeFromSelection(source, cursor, selection, expanded);
    if (
      !isAstSelection(selection) ||
      selection.direction != SearchDirection.NONE ||
      selection.endpoint != SelectionEndpoint.NONE ||
      exclude.contains(selection.object)
    ) {
      return result;
    }

    if (!closestObjectFinder.isReasonableDistance(source, cursor, result, 100)) {
      throw new ObjectTooFarAway();
    }

    return result;
  }

  private Range rangeFromSelection(
    String source,
    int cursor,
    Selection selection,
    boolean expanded
  ) {
    return expanded
      ? expandedRangeFromSelection(source, cursor, selection)
      : rangeFromSelection(source, cursor, selection);
  }

  public List<Range> rangesFromSelection(String source, int cursor, Selection selection) {
    try {
      if (isAstSelection(selection)) {
        AstParent root = astFactory.createImmutableFileRoot(source, language);
        return nodesFromSelection(source, cursor, selection, root)
          .stream()
          .map(e -> e.range())
          .collect(Collectors.toList());
      }
    } catch (ObjectNotFound e) {
      return Collections.emptyList();
    }

    return applySelectorsToContext(
      selectorMap.rawSelectors,
      new RawSelectionContext(source, cursor, selection)
    )
      .collect(Collectors.toList());
  }

  private Range searchForRange(String source, int cursor, Selection selection, boolean expanded) {
    // if we have no transcript for the selection, then we can't search for a phrase range
    if (selection.transcript.isEmpty()) {
      return rangeFromSelection(source, cursor, selection, expanded);
    }

    // first, try to find a "reasonable" range, defined as the object isn't too far from the cursor
    // if we find a reasonable range for the object, then the user probably meant that
    try {
      return reasonableRangeFromSelection(source, cursor, selection, expanded);
    } catch (ObjectTooFarAway | ObjectNotFound e1) {
      // the selector object was too far away, so see if there's a matching phrase closer than the selector
      // object, and if there is, then assume the user meant that instead
      Range phraseRange = null;
      Range objectRange = null;
      try {
        phraseRange =
          rangeFromSelection(
            source,
            cursor,
            new Selection.Builder(ObjectType.PHRASE).setName(selection.transcript.get()).build(),
            expanded
          );

        objectRange = rangeFromSelection(source, cursor, selection, expanded);

        // if we found both a phrase and an object, then take the closer one (with ties going to the object)
        return (
            Math.min(Math.abs(phraseRange.start - cursor), Math.abs(phraseRange.stop - cursor)) <
            Math.min(Math.abs(objectRange.start - cursor), Math.abs(objectRange.stop - cursor))
          )
          ? phraseRange
          : objectRange;
      } catch (ObjectNotFound e2) {
        // if neither a phrase nor an object was found, then propagate that error to the client
        if (phraseRange == null) {
          throw e2;
        }

        // we can only get here if a phrase was found and an object was not
        return phraseRange;
      }
    }
  }

  public Range expandedRangeFromSelection(String source, int cursor, Selection selection) {
    if (selection.endpoint != SelectionEndpoint.NONE) {
      Selection.Builder builder = new Selection.Builder(selection);
      builder.setFromCursorToObject(true);
      selection = builder.build();
    }

    return rangeFromSelection(source, cursor, selection);
  }

  public boolean isAstSelection(Selection selection) {
    return selectorMap.astSelectors.containsKey(selection.object);
  }

  public List<AstNode> nodesFromSelection(
    String source,
    int cursor,
    Selection selection,
    AstParent root
  ) {
    if (astFactory == null) {
      throw new CannotDetermineLanguage();
    }

    List<AstNode> nodes = applySelectorsToContext(
      selectorMap.astSelectors,
      new AstSelectionContext(source, cursor, selection, root)
    )
      .collect(Collectors.toList());

    return nodes;
  }

  public Integer positionFromSelection(String source, int cursor, Selection selection) {
    return rangeFromSelection(source, cursor, selection).start;
  }

  public Range rangeFromSelection(String source, int cursor, Selection selection) {
    List<Range> ranges = rangesFromSelectionNonEmpty(source, cursor, selection)
      .stream()
      .collect(Collectors.toList());

    Integer start = ranges.stream().map(r -> r.start).reduce(Integer::min).get();
    Integer stop = ranges.stream().map(r -> r.stop).reduce(Integer::max).get();
    Range range = new Range(start, stop);
    if (selection.endpoint == SelectionEndpoint.START) {
      range.stop = range.start;
    } else if (selection.endpoint == SelectionEndpoint.END) {
      range.start = range.stop;
    }

    if (selection.fromCursorToObject) {
      int startDistance = Math.abs(range.start - cursor);
      int stopDistance = Math.abs(range.stop - cursor);
      int index = startDistance < stopDistance ? range.start : range.stop;
      if (index < cursor) {
        range.start = index;
        range.stop = cursor;
      } else {
        range.start = cursor;
        range.stop = index;
      }
    }

    return range;
  }

  public List<Range> rangesFromSelectionNonEmpty(String source, int cursor, Selection selection) {
    List<Range> result = rangesFromSelection(source, cursor, selection);
    if (result.size() == 0) {
      throw new ObjectNotFound();
    }

    return result;
  }

  public Range searchForExpandedRange(String source, int cursor, Selection selection) {
    return searchForRange(source, cursor, selection, true);
  }

  public Range searchForRange(String source, int cursor, Selection selection) {
    return searchForRange(source, cursor, selection, false);
  }

  public Optional<Integer> syntaxErrorPosition(String source) {
    // Currently a separate method because it's the only ast-based object that returns a range.
    AstParent root = astFactory.createImmutableFileRoot(source, language);
    if (root.tree().getSyntaxError().isPresent()) {
      AstSyntaxError error = root.tree().getSyntaxError().get();
      return Optional.of(error.position);
    }
    return Optional.empty();
  }
}
