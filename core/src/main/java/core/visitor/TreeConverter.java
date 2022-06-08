package core.visitor;

import core.exception.InvalidRange;
import core.parser.ParseTree;
import core.util.ArrowKeyDirection;
import core.util.NumberConverter;
import core.util.ObjectType;
import core.util.ObjectTypeConverter;
import core.util.Preposition;
import core.util.Range;
import core.util.SearchDirection;
import core.util.TextStyle;
import core.util.selection.Selection;
import core.util.selection.SelectionEndpoint;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TreeConverter {

  @Inject
  NumberConverter numberConverter;

  @Inject
  ObjectTypeConverter objectTypeConverter;

  @Inject
  public TreeConverter() {}

  private SearchDirection convertSearchDirection(Optional<ParseTree> node) {
    if (node.isEmpty()) {
      return SearchDirection.NONE;
    }
    if (
      node.get().getTerminal("next").isPresent() ||
      node.get().getTerminal("forward").isPresent() ||
      node.get().getTerminal("right").isPresent() ||
      node.get().getTerminal("down").isPresent()
    ) {
      return SearchDirection.NEXT;
    } else if (
      node.get().getTerminal("previous").isPresent() ||
      node.get().getTerminal("back").isPresent() ||
      node.get().getTerminal("left").isPresent() ||
      node.get().getTerminal("up").isPresent()
    ) {
      return SearchDirection.PREVIOUS;
    }
    return SearchDirection.NONE;
  }

  private void updateIndexing(Selection.Builder builder, ParseTree node) {
    Range range;
    List<ParseTree> numbers = node.getChildren("number");
    if (numbers.size() == 1) {
      range = new Range(convertNumber(numbers.get(0)) - 1);
    } else if (numbers.size() == 2) {
      range = new Range(convertNumber(numbers.get(0)) - 1, convertNumber(numbers.get(1)));
    } else if (node.getChild("positional").isPresent()) {
      range = new Range(convertPositional(node.getChild("positional").get()));
    } else if (node.getChild("count").isPresent()) {
      if (node.getTerminal("last").isPresent()) {
        range = new Range(-convertCount(node.getChild("count").get()), 0);
      } else {
        builder.setCount(convertCount(node.getChild("count").get()));
        return;
      }
    } else {
      return;
    }
    if (range.stop <= range.start) {
      throw new InvalidRange();
    }
    builder.setAbsoluteRange(range);
  }

  public Integer convertCount(ParseTree node) {
    return convertNumber(node.getChild("number").get());
  }

  public Optional<ArrowKeyDirection> extractArrowKeyDirection(ParseTree node) {
    String value = convertToEnglish(node.getChild("arrowKeyDirection").get());
    if (value.equals("up")) {
      return Optional.of(ArrowKeyDirection.UP);
    } else if (value.equals("down")) {
      return Optional.of(ArrowKeyDirection.DOWN);
    } else if (value.equals("left")) {
      return Optional.of(ArrowKeyDirection.LEFT);
    } else if (value.equals("right")) {
      return Optional.of(ArrowKeyDirection.RIGHT);
    }

    return Optional.empty();
  }

  public Optional<String> convertToDirectionString(ParseTree node) {
    return extractArrowKeyDirection(node).map(dir -> dir.name().toLowerCase());
  }

  public SelectionEndpoint convertEndpoint(Optional<ParseTree> node) {
    if (!node.isPresent()) {
      return SelectionEndpoint.NONE;
    }
    if (
      node.get().getTerminal("start").isPresent() ||
      node.get().getTerminal("top").isPresent() ||
      node.get().getTerminal("beginning").isPresent()
    ) {
      return SelectionEndpoint.START;
    } else if (
      node.get().getTerminal("end").isPresent() || node.get().getTerminal("bottom").isPresent()
    ) {
      return SelectionEndpoint.END;
    }

    return SelectionEndpoint.NONE;
  }

  public Integer convertNumber(ParseTree node) {
    return numberConverter.fromString(convertToEnglish(node));
  }

  public Selection convertPhraseSelection(ParseTree node) {
    // Note that if this node has neither child, it goes down the
    // PhraseSelectionPrefixSingularContext
    // path, which should be the default case.
    Selection.Builder builder = new Selection.Builder(ObjectType.PHRASE);
    node
      .getChildren()
      .stream()
      .filter(c -> c.getType().contains("SelectionPrefix"))
      .findFirst()
      .ifPresent(
        prefix -> {
          builder.setDirection(convertSearchDirection(prefix.getChild("searchDirection")));
          builder.setFromCursorToObject(prefix.getTerminal("to").isPresent());
          builder.setEndpoint(convertEndpoint(prefix.getChild("endpoint")));
          updateIndexing(builder, prefix);
        }
      );
    builder.setName(convertToEnglish(node.getChild("formattedText")));
    return builder.build();
  }

  public Selection convertNamedSelection(ParseTree node) {
    ParseTree objectNode = node.getChild("namedSelectionObject").get().getChildren().get(0);
    ObjectType object = objectTypeConverter.objectNameToObjectType(objectNode.getType());

    Selection.Builder builder = new Selection.Builder(object);
    updateIndexing(builder, node);
    builder.setEndpoint(convertEndpoint(node.getChild("endpoint")));
    builder.setFromCursorToObject(node.getTerminal("to").isPresent());
    builder.setDirection(convertSearchDirection(node.getChild("searchDirection")));
    builder.setName(convertToEnglish(node.getChild("formattedText")));
    builder.setTranscript(convertToEnglish(node));
    return builder.build();
  }

  public Selection convertNavigationPositionSelection(ParseTree node) {
    ObjectType object = null;
    ParseTree objectNode = null;
    if (node.getChild("selectionObjectSingular").isPresent()) {
      objectNode = node.getChild("selectionObjectSingular").get().getChildren().get(0);
      object = objectTypeConverter.objectNameToObjectType(objectNode.getType());
    } else if (node.getChild("selectionObjectPlural").isPresent()) {
      objectNode = node.getChild("selectionObjectPlural").get().getChildren().get(0);
      object = objectTypeConverter.objectNameToObjectType(objectNode.getType());
    }

    if (object == null) {
      throw new RuntimeException("Invalid ObjectType: " + objectNode.getType());
    }

    Selection.Builder builder = new Selection.Builder(object);
    builder.setDirection(convertSearchDirection(node.getChild("movementDirection")));
    builder.setOffset(
      node.getTerminal("one").isPresent() ? 0 : convertCount(node.getChild("count").get()) - 1
    );
    builder.setTranscript(convertToEnglish(node));
    return builder.build();
  }

  public int convertPositional(ParseTree node) {
    int ret = 0;
    if (node.getTerminal("first").isPresent()) {
      ret = 0;
    } else if (node.getTerminal("second").isPresent()) {
      ret = 1;
    } else if (node.getTerminal("third").isPresent()) {
      ret = 2;
    } else if (node.getTerminal("fourth").isPresent()) {
      ret = 3;
    } else if (node.getTerminal("fifth").isPresent()) {
      ret = 4;
    } else if (node.getTerminal("sixth").isPresent()) {
      ret = 5;
    } else if (node.getTerminal("seventh").isPresent()) {
      ret = 6;
    } else if (node.getTerminal("eighth").isPresent()) {
      ret = 7;
    } else if (node.getTerminal("ninth").isPresent()) {
      ret = 8;
    } else if (node.getTerminal("tenth").isPresent()) {
      ret = 9;
    } else if (node.getTerminal("last").isPresent()) {
      ret = -1;
    }
    return node.getTerminal("to").isPresent() ? -ret - 1 : ret;
  }

  public Selection convertPositionSelection(ParseTree node) {
    if (node.getChild("namedPositionSelection").isPresent()) {
      return convertNamedSelection(node.getChild("namedPositionSelection").get());
    } else if (node.getChild("unnamedPositionSelection").isPresent()) {
      return convertUnnamedSelection(node.getChild("unnamedPositionSelection").get());
    } else {
      return convertNavigationPositionSelection(node.getChild("navigationPositionSelection").get());
    }
  }

  public Selection convertPositionSelectionWithImplicitPhrase(ParseTree node) {
    if (node.getChild("positionPhraseSelection").isPresent()) {
      return convertPhraseSelection(node.getChild("positionPhraseSelection").get());
    } else {
      return convertPositionSelection(node.getChild("positionSelection").get());
    }
  }

  public Preposition convertPreposition(Optional<ParseTree> node) {
    if (!node.isPresent()) {
      return Preposition.HERE;
    }

    Preposition result = Preposition.HERE;
    if (node.get().getTerminal("after").isPresent()) {
      result = Preposition.AFTER;
    } else if (node.get().getTerminal("before").isPresent()) {
      result = Preposition.BEFORE;
    }
    return result;
  }

  public TextStyle convertTextStyle(ParseTree node) {
    TextStyle result = TextStyle.UNKNOWN;
    if (node.getChild("allCaps").isPresent()) {
      result = TextStyle.ALL_CAPS;
    } else if (node.getChild("underscores").isPresent()) {
      result = TextStyle.UNDERSCORES;
    } else if (node.getChild("pascalCase").isPresent()) {
      result = TextStyle.PASCAL_CASE;
    } else if (node.getChild("camelCase").isPresent()) {
      result = TextStyle.CAMEL_CASE;
    } else if (node.getChild("capitalize").isPresent()) {
      result = TextStyle.CAPITALIZED;
    } else if (node.getChild("dashes").isPresent()) {
      result = TextStyle.DASHES;
    } else if (node.getChild("lowercase").isPresent()) {
      result = TextStyle.LOWERCASE;
    } else if (node.getChild("titleCase").isPresent()) {
      result = TextStyle.TITLE_CASE;
    }
    return result;
  }

  public Selection convertUnnamedSelection(ParseTree node) {
    ObjectType object = null;
    ParseTree objectNode = null;
    if (node.getChild("selectionObjectSingular").isPresent()) {
      objectNode = node.getChild("selectionObjectSingular").get().getChildren().get(0);
      object = objectTypeConverter.objectNameToObjectType(objectNode.getType());
    } else if (node.getChild("selectionObjectPlural").isPresent()) {
      objectNode = node.getChild("selectionObjectPlural").get().getChildren().get(0);
      object = objectTypeConverter.objectNameToObjectType(objectNode.getType());
    }

    if (object == null) {
      throw new RuntimeException("Invalid ObjectType");
    }

    Selection.Builder builder = new Selection.Builder(object);
    updateIndexing(builder, node);
    builder.setEndpoint(convertEndpoint(node.getChild("endpoint")));
    // has "to", but isn't a range.
    builder.setFromCursorToObject(
      node.getChildren("number").size() != 2 && node.getTerminal("to").isPresent()
    );
    builder.setDirection(convertSearchDirection(node.getChild("searchDirection")));
    builder.setTranscript(convertToEnglish(node));
    return builder.build();
  }

  public Integer convertQuantifier(ParseTree node) {
    if (node.getTerminal("once").isPresent()) {
      return 1;
    } else if (node.getTerminal("twice").isPresent()) {
      return 2;
    } else if (node.getTerminal("thrice").isPresent()) {
      return 3;
    }
    return convertNumber(node.getChild("numberRange1To99").get());
  }

  public Selection convertSelection(ParseTree node) {
    if (node.getChild("namedSelection").isPresent()) {
      return convertNamedSelection(node.getChild("namedSelection").get());
    }
    return convertUnnamedSelection(node.getChild("unnamedSelection").get());
  }

  public Selection convertSelectionWithImplicitPhrase(ParseTree node) {
    if (node.getChild("phraseSelection").isPresent()) {
      return convertPhraseSelection(node.getChild("phraseSelection").get());
    } else {
      return convertSelection(node.getChild("selection").get());
    }
  }

  public String convertToEnglish(Optional<ParseTree> node) {
    if (node.isPresent()) {
      return convertToEnglish(node.get());
    }
    return "";
  }

  public String convertToEnglish(ParseTree node) {
    return node.getCode();
  }
}
