package core.ast.api;

import core.util.Range;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

class AstTokens implements List<AstToken> {

  private final AstTree tree;
  private List<AstToken> inner;

  class TokensSubList implements List<AstToken> {

    private List<AstToken> inner;

    public TokensSubList(List<AstToken> inner) {
      this.inner = inner;
    }

    @Override
    public boolean add(AstToken e) {
      boolean ret = inner.add(e);
      updateInvariants();
      return ret;
    }

    @Override
    public void add(int index, AstToken e) {
      inner.add(index, e);
      updateInvariants();
    }

    @Override
    public boolean addAll(Collection<? extends AstToken> c) {
      boolean ret = inner.addAll(c);
      updateInvariants();
      return ret;
    }

    @Override
    public boolean addAll(int index, Collection<? extends AstToken> c) {
      boolean ret = inner.addAll(index, c);
      updateInvariants();
      return ret;
    }

    @Override
    public void clear() {
      inner.clear();
      updateInvariants();
    }

    @Override
    public boolean contains(Object o) {
      return inner.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      return inner.containsAll(c);
    }

    @Override
    public boolean equals(Object o) {
      return inner.equals(o);
    }

    @Override
    public AstToken get(int index) {
      return inner.get(index);
    }

    @Override
    public int hashCode() {
      return inner.hashCode();
    }

    @Override
    public int indexOf(Object o) {
      return inner.indexOf(o);
    }

    @Override
    public boolean isEmpty() {
      return inner.isEmpty();
    }

    @Override
    public Iterator<AstToken> iterator() {
      return inner.iterator();
    }

    @Override
    public int lastIndexOf(Object o) {
      return inner.lastIndexOf(o);
    }

    @Override
    public ListIterator<AstToken> listIterator() {
      return inner.listIterator();
    }

    @Override
    public ListIterator<AstToken> listIterator(int index) {
      return inner.listIterator(index);
    }

    @Override
    public boolean remove(Object e) {
      boolean ret = inner.remove(e);
      updateInvariants();
      return ret;
    }

    @Override
    public AstToken remove(int index) {
      AstToken ret = inner.remove(index);
      updateInvariants();
      return ret;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      boolean ret = inner.removeAll(c);
      updateInvariants();
      return ret;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      boolean ret = inner.retainAll(c);
      updateInvariants();
      return ret;
    }

    @Override
    public AstToken set(int index, AstToken e) {
      AstToken ret = inner.set(index, e);
      updateInvariants();
      return ret;
    }

    @Override
    public int size() {
      return inner.size();
    }

    @Override
    public List<AstToken> subList(int fromIndex, int toIndex) {
      return new TokensSubList(inner.subList(fromIndex, toIndex));
    }

    @Override
    public Object[] toArray() {
      return inner.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
      return inner.toArray(a);
    }
  }

  public AstTokens(AstTree tree, Collection<AstToken> c) {
    this.inner = new ArrayList<>(c);
    this.tree = tree;
    updateInvariants();
  }

  @Override
  public boolean add(AstToken e) {
    boolean ret = inner.add(e);
    updateInvariants();
    return ret;
  }

  @Override
  public void add(int index, AstToken e) {
    inner.add(index, e);
    updateInvariants();
  }

  @Override
  public boolean addAll(Collection<? extends AstToken> c) {
    boolean ret = inner.addAll(c);
    updateInvariants();
    return ret;
  }

  @Override
  public boolean addAll(int index, Collection<? extends AstToken> c) {
    boolean ret = inner.addAll(index, c);
    updateInvariants();
    return ret;
  }

  @Override
  public void clear() {
    inner.clear();
    updateInvariants();
  }

  @Override
  public boolean contains(Object o) {
    return inner.contains(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return inner.containsAll(c);
  }

  @Override
  public boolean equals(Object o) {
    return inner.equals(o);
  }

  @Override
  public AstToken get(int index) {
    return inner.get(index);
  }

  @Override
  public int hashCode() {
    return inner.hashCode();
  }

  @Override
  public int indexOf(Object o) {
    return inner.indexOf(o);
  }

  @Override
  public boolean isEmpty() {
    return inner.isEmpty();
  }

  @Override
  public Iterator<AstToken> iterator() {
    return inner.iterator();
  }

  @Override
  public int lastIndexOf(Object o) {
    return inner.lastIndexOf(o);
  }

  @Override
  public ListIterator<AstToken> listIterator() {
    return inner.listIterator();
  }

  @Override
  public ListIterator<AstToken> listIterator(int index) {
    return inner.listIterator(index);
  }

  @Override
  public boolean remove(Object e) {
    boolean ret = inner.remove(e);
    updateInvariants();
    return ret;
  }

  @Override
  public AstToken remove(int index) {
    AstToken ret = inner.remove(index);
    updateInvariants();
    return ret;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    boolean ret = inner.removeAll(c);
    updateInvariants();
    return ret;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    boolean ret = inner.retainAll(c);
    updateInvariants();
    return ret;
  }

  @Override
  public AstToken set(int index, AstToken e) {
    AstToken ret = inner.set(index, e);
    updateInvariants();
    return ret;
  }

  @Override
  public int size() {
    return inner.size();
  }

  @Override
  public List<AstToken> subList(int fromIndex, int toIndex) {
    return new TokensSubList(inner.subList(fromIndex, toIndex));
  }

  @Override
  public Object[] toArray() {
    return inner.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return inner.toArray(a);
  }

  public void updateInvariants() {
    // recompute other indexing schemes and set token reference.
    int offset = 0;
    int priorNewlines = 0;
    for (int i = 0; i < size(); i++) {
      AstToken token = get(i);
      token.setTree(tree);
      token.range = new Range(offset, offset + token.code.length());
      token.priorNewlines = priorNewlines;
      token.index = i;
      offset = token.range.stop;
      if (token instanceof AstNewline) {
        priorNewlines++;
      }
    }
  }
}
