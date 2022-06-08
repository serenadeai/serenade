package core.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MutableParseTreeFactory {

  @Inject
  public MutableParseTreeFactory() {}

  private boolean isOpeningTag(String token) {
    return token.startsWith("<") && !token.startsWith("</");
  }

  private boolean isClosingTag(String token) {
    return token.startsWith("</");
  }

  private boolean isTag(String token) {
    return isOpeningTag(token) || isClosingTag(token);
  }

  private String stripTags(String token) {
    if (isOpeningTag(token)) {
      return token.substring("<".length(), token.length() - ">".length());
    } else if (isClosingTag(token)) {
      return token.substring("</".length(), token.length() - ">".length());
    }
    return token;
  }

  public Optional<List<MutableParseTree>> create(List<String> markup) {
    Deque<MutableParseTree> stack = new LinkedList<MutableParseTree>();
    stack.push(new MutableParseTree(""));
    for (int i = 0; i < markup.size(); i++) {
      if (isOpeningTag(markup.get(i))) {
        stack.push(new MutableParseTree(stripTags(markup.get(i))));
      } else if (isClosingTag(markup.get(i))) {
        if (stack.size() == 0) {
          return Optional.empty();
        }
        MutableParseTree current = stack.removeFirst();
        if (!current.type.equals(stripTags(markup.get(i)))) {
          return Optional.empty();
        }
        List<MutableParseTree> children = new ArrayList<>(stack.peekFirst().children());
        children.add(current);
        stack.peekFirst().setChildren(children);
      } else {
        List<MutableParseTree> children = new ArrayList<>(stack.peekFirst().children());
        children.add(new MutableParseTree("", markup.get(i)));
        stack.peekFirst().setChildren(children);
      }
    }
    if (stack.size() != 1) {
      return Optional.empty();
    }
    return Optional.of(stack.peekFirst().children());
  }
}
