package core.ast;

import core.ast.api.AstList;
import core.ast.api.AstListOptional;
import core.ast.api.AstNode;
import core.ast.api.AstOptional;
import core.ast.api.AstParent;
import core.ast.api.AstTuple;
import core.ast.api.DefaultAstParent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class DartAst {

  public static class Extension extends Ast.Class_ {}

  public static class Function extends Ast.Function {

    @Override
    public Optional<NamedParameterListOptional> namedParameterListOptional() {
      return find(NamedParameterListOptional.class).findFirst();
    }

    @Override
    public Optional<PositionalParameterListOptional> positionalParameterListOptional() {
      return find(PositionalParameterListOptional.class).findFirst();
    }
  }

  public static class Getter extends Ast.Property {}

  public static class Method extends Ast.Method {

    @Override
    public Optional<NamedParameterListOptional> namedParameterListOptional() {
      return find(NamedParameterListOptional.class).findFirst();
    }

    @Override
    public Optional<PositionalParameterListOptional> positionalParameterListOptional() {
      return find(PositionalParameterListOptional.class).findFirst();
    }
  }

  public static class Mixin extends DefaultAstParent {

    public Ast.ClassMemberList memberList() {
      return child(Ast.EnclosedBody.class).get().child(Ast.ClassMemberList.class).get();
    }

    public Optional<Ast.ModifierList> modifierList() {
      return child(Ast.ModifierList.class);
    }

    public Optional<OnType> onType() {
      return child(OnOptional.class).flatMap(e -> e.optional());
    }
  }

  public static class MixinList extends AstList<MixinType> {

    @Override
    public String containerType() {
      return "MixinList";
    }

    @Override
    public String delimiter() {
      return ",";
    }

    @Override
    public Class<? extends MixinType> elementType() {
      return MixinType.class;
    }
  }

  public static class MixinListOptional extends AstListOptional {

    @Override
    public String containerType() {
      return "MixinListOptional";
    }

    @Override
    public Class<MixinList> elementType() {
      return MixinList.class;
    }

    @Override
    public List<Class<? extends AstNode>> innerElementTypes() {
      return Arrays.asList(MixinType.class);
    }

    @Override
    protected String prefix() {
      return "with ";
    }
  }

  public static class MixinType extends DefaultAstParent {}

  public static class NamedParameter extends Ast.Parameter {

    @Override
    public Optional<Ast.Identifier> name() {
      return child(Ast.Parameter.class).get().child(Ast.Identifier.class);
    }

    @Override
    public Optional<Ast.ModifierList> modifierList() {
      return child(Ast.Parameter.class).get().child(Ast.ModifierList.class);
    }

    @Override
    public Optional<Ast.TypeOptional> typeOptional() {
      return child(Ast.Parameter.class).get().child(Ast.TypeOptional.class);
    }
  }

  public static class NamedParameterList extends Ast.ParameterList {}

  public static class NamedParameterListOptional extends AstListOptional {

    @Override
    public String containerType() {
      return "NamedParameterListOptional";
    }

    @Override
    public Class<? extends AstList> elementType() {
      return NamedParameterList.class;
    }

    @Override
    public List<Class<? extends AstNode>> innerElementTypes() {
      return Arrays.asList(Ast.Parameter.class);
    }

    @Override
    protected String prefix() {
      return "{";
    }

    @Override
    protected String postfix() {
      return "}";
    }
  }

  public static class OnOptional extends AstOptional<OnType> {

    @Override
    public String containerType() {
      return "OnOptional";
    }

    @Override
    public Class<? extends OnType> elementType() {
      return OnType.class;
    }

    @Override
    protected String prefix() {
      return "on ";
    }
  }

  public static class OnType extends DefaultAstParent {}

  public static class Parameters extends AstTuple {

    @Override
    public String containerType() {
      return "Parameters";
    }

    @Override
    protected String delimiter() {
      return ",";
    }
  }

  public static class Operator extends Ast.Method {}

  public static class PositionalParameter extends Ast.Parameter {

    @Override
    public Optional<Ast.Identifier> name() {
      return child(Ast.Parameter.class).get().child(Ast.Identifier.class);
    }

    @Override
    public Optional<Ast.ModifierList> modifierList() {
      return child(Ast.Parameter.class).get().child(Ast.ModifierList.class);
    }

    @Override
    public Optional<Ast.TypeOptional> typeOptional() {
      return child(Ast.Parameter.class).get().child(Ast.TypeOptional.class);
    }
  }

  public static class PositionalParameterList extends Ast.ParameterList {}

  public static class PositionalParameterListOptional extends AstListOptional {

    @Override
    public String containerType() {
      return "PositionalParameterListOptional";
    }

    @Override
    public Class<? extends AstList> elementType() {
      return PositionalParameterList.class;
    }

    @Override
    public List<Class<? extends AstNode>> innerElementTypes() {
      return Arrays.asList(Ast.Parameter.class);
    }

    @Override
    protected String prefix() {
      return "[";
    }

    @Override
    protected String postfix() {
      return "]";
    }
  }

  public static class Setter extends Ast.Property {}

  public static class TypeDeclarationList extends Ast.TypeDeclarationList {

    @Override
    public List<Class<? extends AstParent>> innerElementTypes() {
      List<Class<? extends AstParent>> types = new ArrayList<>(super.innerElementTypes());
      types.addAll(Arrays.asList(Ast.Function.class, Ast.Import.class, Mixin.class));
      return types;
    }
  }
}
