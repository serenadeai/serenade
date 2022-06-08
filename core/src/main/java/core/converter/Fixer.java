package core.converter;

import core.ast.Ast;
import core.ast.api.AstList;
import core.ast.api.AstListOptional;
import core.ast.api.AstNode;
import core.ast.api.AstOptional;
import core.ast.api.AstParent;
import core.ast.api.AstToken;
import core.ast.api.DefaultAstParent;
import core.gen.rpc.Language;
import core.language.LanguageSpecific;
import core.parser.ParseTree;
import core.util.Range;
import core.util.Whitespace;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;

public class Fixer {

  public AstParent parent;

  public Fixer(AstParent parent) {
    this.parent = parent;
  }

  public Fixer changeChildType(
    Supplier<AstParent> childFactory,
    Supplier<AstParent> replacementFactory
  ) {
    return changeChildType(
      child -> childFactory.get().getClass().equals(child.getClass()),
      replacementFactory
    );
  }

  public Fixer changeChildType(
    Predicate<AstNode> replacementCondition,
    Supplier<AstParent> replacementFactory
  ) {
    List<AstNode> children = new ArrayList<>();
    for (int i = 0; i < parent.children().size(); i++) {
      AstNode child = parent.children().get(i);
      if (replacementCondition.test(child)) {
        AstParent childAsParent = (AstParent) child;
        AstParent newParent = replacementFactory.get();
        newParent.setChildren(childAsParent.children());
        children.add(newParent);
      } else {
        children.add(child);
      }
    }
    parent.setChildren(children);
    return this;
  }

  public Fixer fixChildren(Function<Fixer, Fixer> childFixer) {
    for (AstNode child : parent.children()) {
      if (child instanceof AstParent) {
        childFixer.apply(new Fixer((AstParent) child));
      }
    }
    return this;
  }

  public boolean hasChild(Predicate<AstNode> exists) {
    return parent.children().stream().anyMatch(child -> exists.test(child));
  }

  public Fixer inline(Predicate<AstNode> match) {
    List<AstNode> children = new ArrayList<>();
    for (int i = 0; i < parent.children().size(); i++) {
      AstNode child = parent.children().get(i);
      if (!match.test(child)) {
        children.add(child);
      } else {
        children.addAll(child.children());
      }
    }

    parent.setChildren(children);
    return this;
  }

  public Fixer setType(Supplier<AstParent> replacementFactory) {
    return setType(e -> true, replacementFactory);
  }

  public Fixer setType(Predicate<AstParent> condition, Supplier<AstParent> replacementFactory) {
    if (condition.test(this.parent)) {
      AstParent newParent = replacementFactory.get();
      newParent.setChildren(parent.children());
      this.parent = newParent;
    }
    return this;
  }

  public Fixer splitLeadingEqualsFromChild(Predicate<AstNode> match) {
    List<AstNode> matchingChildren = parent
      .children()
      .stream()
      .filter(child -> match.test(child))
      .collect(Collectors.toList());

    for (AstNode child : matchingChildren) {
      Optional<AstToken> token = child.leftMost(AstToken.class).map(AstToken.class::cast);
      if (token.isPresent() && token.get().code().equals("=")) {
        List<AstNode> children = token.get().parent().get().children();
        children.remove(token.get());

        List<AstNode> newChildren = new ArrayList<>(parent.children());
        newChildren.add(parent.children().indexOf(child), token.get());
        parent.setChildren(newChildren);
      }
    }
    return this;
  }

  public Fixer wrapIndividually(Predicate<AstNode> match, Supplier<DefaultAstParent> factory) {
    List<AstNode> children = new ArrayList<>();
    for (int i = 0; i < parent.children().size(); i++) {
      AstNode child = parent.children().get(i);
      if (match.test(child)) {
        DefaultAstParent inner = factory.get();
        inner.setChildren(Arrays.asList(child));
        children.add(inner);
      } else {
        children.add(child);
      }
    }

    parent.setChildren(children);
    return this;
  }

  public Fixer wrap(Supplier<DefaultAstParent> factory) {
    return wrap(e -> true, factory);
  }

  public Fixer wrap(Predicate<AstNode> condition, Supplier<DefaultAstParent> factory) {
    if (condition.test(this.parent)) {
      DefaultAstParent wrapper = factory.get();
      wrapper.setChildren(parent.children());
      parent.setChildren(Arrays.asList(wrapper));
    }
    return this;
  }

  public Fixer wrapSublists(Supplier<AstList<?>> factory) {
    AstList<?> container = factory.get();
    wrapSublists(
      e -> container.canAdd(e) || e.isToken(container.delimiter()),
      () -> ((AstParent) factory.get())
    );
    return this;
  }

  public Fixer wrapSublists(Predicate<AstNode> match, Supplier<AstParent> factory) {
    List<AstNode> children = new ArrayList<>();
    List<AstNode> inner = new ArrayList<>();
    for (int i = 0; i < parent.children().size(); i++) {
      AstNode child = parent.children().get(i);
      if (match.test(child)) {
        inner.add(child);
      } else {
        if (inner.size() > 0) {
          AstParent nestedParent = factory.get();
          nestedParent.setChildren(inner);
          children.add(nestedParent);
          inner = new ArrayList<>();
        }

        children.add(child);
      }
    }

    if (inner.size() > 0) {
      AstParent nestedParent = factory.get();
      nestedParent.setChildren(inner);
      children.add(nestedParent);
    }

    parent.setChildren(children);
    return this;
  }
}
