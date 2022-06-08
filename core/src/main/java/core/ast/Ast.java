package core.ast;

import core.ast.api.AstIndentAligner;
import core.ast.api.AstList;
import core.ast.api.AstListOptional;
import core.ast.api.AstListWithLinePadding;
import core.ast.api.AstMultilineOptional;
import core.ast.api.AstNode;
import core.ast.api.AstOptional;
import core.ast.api.AstParent;
import core.ast.api.AstToken;
import core.ast.api.AstTrailingClauseList;
import core.ast.api.DefaultAstParent;
import core.ast.api.IndentationUtils;
import core.util.Range;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Ast {

  public abstract static class FunctionDeclaration extends DefaultAstParent {

    public Optional<AttributeList> attributeList() {
      return child(AttributeList.class);
    }

    public Optional<DecoratorList> decoratorList() {
      return child(DecoratorList.class);
    }

    public Optional<ElseClauseList> elseClauseList() {
      return child(ElseClauseList.class);
    }

    public Optional<EnsureClauseList> ensureClauseList() {
      return child(EnsureClauseList.class);
    }

    public Optional<FieldInitializerList> fieldInitializerList() {
      return child(FieldInitializerList.class);
    }

    public Optional<FunctionModifierList> functionModifierList() {
      return child(FunctionModifierList.class);
    }

    public Optional<ModifierList> modifierList() {
      return child(ModifierList.class);
    }

    public Optional<ModifierList> modifierListSecond() {
      return child(ModifierList.class, 1);
    }

    public Optional<Identifier> name() {
      return child(Identifier.class);
    }

    public Optional<Parameter> parameter() {
      return child(Ast.Parameter.class);
    }

    public Optional<ParameterList> parameterList() {
      return child(ParameterListOptional.class).isPresent()
        ? child(ParameterListOptional.class).get().optional().map(ParameterList.class::cast)
        : child(ParameterList.class);
    }

    public Optional<ParameterListOptional> parameterListOptional() {
      return child(ParameterListOptional.class);
    }

    public Optional<ReceiverArgumentOptional> receiverArgumentOptional() {
      return child(ReceiverArgumentOptional.class);
    }

    public Optional<RescueClauseList> rescueClauseList() {
      return child(RescueClauseList.class);
    }

    public Optional<ReturnValue> returnValue() {
      return child(Ast.ReturnValue.class);
    }

    public Optional<ReturnValueNameListOptional> returnValueNameListOptional() {
      return child(ReturnValueNameListOptional.class);
    }

    public Optional<DefaultMethodClause> defaultMethodClause() {
      return child(DefaultMethodClause.class);
    }

    public Optional<DeleteMethodClause> deleteMethodClause() {
      return child(DeleteMethodClause.class);
    }

    public Optional<LambdaCaptureSpecifier> lambdaCaptureSpecifier() {
      return child(LambdaCaptureSpecifier.class);
    }

    public Optional<Statement> statement() {
      return child(Statement.class);
    }

    public Optional<StatementList> statementList() {
      return child(EnclosedBody.class).flatMap(c -> c.child(StatementList.class));
    }

    public Optional<ThrowsType> throwsType() {
      return child(ThrowsOptional.class).flatMap(e -> e.optional());
    }

    public Optional<TypeDecoratorList> typeDecoratorList() {
      return child(TypeDecoratorList.class);
    }

    public Optional<TypeParameterConstraintListOptional> typeParameterConstraintsOptional() {
      return child(TypeParameterConstraintListOptional.class);
    }

    public Optional<TypeParameterList> typeParameterList() {
      Optional<TypeParameterListOptional> typeParameterListOptional = child(
        TypeParameterListOptional.class
      );
      if (typeParameterListOptional.isPresent()) {
        return typeParameterListOptional
          .get()
          .optional()
          .map(astList -> (TypeParameterList) astList);
      }
      return child(TypeParameterList.class);
    }

    public Optional<TypeOptional> typeOptional() {
      return child(TypeOptional.class);
    }

    // Ideally we don't have these in the DartAst specifically, but there may be too many
    // concepts to make it worth migrating over to the main Ast.
    public Optional<DartAst.NamedParameterListOptional> namedParameterListOptional() {
      return Optional.empty();
    }

    public Optional<DartAst.PositionalParameterListOptional> positionalParameterListOptional() {
      return Optional.empty();
    }
  }

  public abstract static class MemberList extends AstListWithLinePadding<AstParent> {

    @Override
    protected int innerMinimumBlankLines(AstNode innerElement) {
      if (innerElement instanceof Method || innerElement instanceof TypeDeclaration) {
        return 1;
      }

      return 0;
    }

    @Override
    public Class<? extends AstParent> elementType() {
      return Member.class;
    }

    @Override
    public List<Class<? extends AstParent>> innerElementTypes() {
      return Arrays.asList(Class_.class, Enum.class, Interface.class, Method.class, Property.class);
    }
  }

  // We use this as a base for all the class-like Ast constructs (e.g. struct, enum, etc.)
  // We don't typically create instances of this class directly.
  public abstract static class TypeDeclaration extends DefaultAstParent {

    public Optional<DecoratorList> decoratorList() {
      return child(DecoratorList.class);
    }

    public Optional<ElseClauseList> elseClauseList() {
      return child(ElseClauseList.class);
    }

    public Optional<EnsureClauseList> ensureClauseList() {
      return child(EnsureClauseList.class);
    }

    public Optional<ExtendsListOptional> extendsListOptional() {
      return child(ExtendsListOptional.class);
    }

    public Optional<AstList> extendsList() {
      return child(ExtendsListOptional.class).flatMap(c -> c.optional());
    }

    public Optional<ExtendsOptional> extendsOptional() {
      return child(ExtendsOptional.class);
    }

    public Optional<ExtendsType> extendsType() {
      return child(ExtendsOptional.class).flatMap(c -> c.optional());
    }

    public Optional<ImplementsListOptional> implementsOptional() {
      return child(ImplementsListOptional.class);
    }

    public Optional<AstList> implementsList() {
      return child(ImplementsListOptional.class).flatMap(c -> c.optional());
    }

    public Optional<ModifierList> modifierList() {
      return child(ModifierList.class);
    }

    public Optional<RescueClauseList> rescueClauseList() {
      return child(RescueClauseList.class);
    }

    public Optional<TypeParameterConstraintListOptional> typeParameterConstraintsOptional() {
      return child(TypeParameterConstraintListOptional.class);
    }

    public Optional<TypeParameterList> typeParameterList() {
      Optional<TypeParameterListOptional> typeParameterListOptional = child(
        TypeParameterListOptional.class
      );
      if (typeParameterListOptional.isPresent()) {
        return typeParameterListOptional.flatMap(c -> c.child(TypeParameterList.class));
      }
      return child(TypeParameterList.class);
    }
  }

  public static class AnonymousProperty extends DefaultAstParent {

    public Optional<ModifierList> modifierList() {
      return child(ModifierList.class);
    }

    public Optional<TypeOptional> typeOptional() {
      return child(TypeOptional.class);
    }
  }

  public static class AnonymousPropertyList extends AstList<AnonymousProperty> {

    @Override
    public String containerType() {
      return "AnonymousPropertyList";
    }

    @Override
    public String delimiter() {
      return ",";
    }

    @Override
    public Class<? extends AnonymousProperty> elementType() {
      return AnonymousProperty.class;
    }
  }

  public static class Argument extends DefaultAstParent {}

  public static class ArgumentListOptional extends AstListOptional {

    public Optional<ArgumentList> argumentList() {
      return child(ArgumentList.class);
    }

    @Override
    public String containerType() {
      return "ArgumentListOptional";
    }

    @Override
    public Class<ArgumentList> elementType() {
      return ArgumentList.class;
    }

    @Override
    public List<Class<? extends AstNode>> innerElementTypes() {
      return Arrays.asList(Argument.class);
    }

    @Override
    protected String prefix() {
      return "(";
    }

    @Override
    protected String postfix() {
      return ")";
    }
  }

  public static class ArgumentList extends AstList<Argument> {

    @Override
    public String containerType() {
      return "ArgumentList";
    }

    @Override
    public String delimiter() {
      return ",";
    }

    @Override
    public Class<? extends Argument> elementType() {
      return Argument.class;
    }
  }

  public static class ArrayPatternList extends AstList<PatternListElement> {

    @Override
    public String containerType() {
      return "PatternList";
    }

    @Override
    public String delimiter() {
      return ",";
    }

    @Override
    public Class<? extends PatternListElement> elementType() {
      return PatternListElement.class;
    }
  }

  public static class Assert extends DefaultAstParent {}

  public static class Assignment extends DefaultAstParent {

    public Optional<AssignmentVariableList> assignmentVariableList() {
      return child(AssignmentVariableList.class);
    }

    public Optional<AssignmentValueListOptional> assignmentValueListOptional() {
      return child(AssignmentValueListOptional.class);
    }

    public Optional<TypeOptional> typeOptional() {
      return child(TypeOptional.class);
    }
  }

  public static class AssignmentList extends AstList<Assignment> {

    @Override
    public String containerType() {
      return "AssignmentList";
    }

    @Override
    public String delimiter() {
      return ",";
    }

    @Override
    public Class<? extends Assignment> elementType() {
      return Assignment.class;
    }
  }

  public static class AssignmentValue extends DefaultAstParent {}

  public static class AssignmentValueList extends AstList<AssignmentValue> {

    @Override
    public String containerType() {
      return "AssignmentValueList";
    }

    @Override
    public String delimiter() {
      return ",";
    }

    @Override
    public Class<? extends AssignmentValue> elementType() {
      return AssignmentValue.class;
    }
  }

  public static class AssignmentValueListOptional extends AstListOptional {

    @Override
    public String containerType() {
      return "AssignmentValueListOptional";
    }

    @Override
    public Class<AssignmentValueList> elementType() {
      return AssignmentValueList.class;
    }

    @Override
    public List<Class<? extends AstNode>> innerElementTypes() {
      return Arrays.asList(AssignmentValue.class);
    }

    public Optional<AssignmentValueList> assignmentValueList() {
      return child(AssignmentValueList.class);
    }
  }

  public static class AssignmentVariable extends DefaultAstParent {

    public Optional<TypeOptional> typeOptional() {
      return child(TypeOptional.class);
    }
  }

  public static class AssignmentVariableList extends AstList<AssignmentVariable> {

    @Override
    public String containerType() {
      return "AssignmentVariableList";
    }

    @Override
    public String delimiter() {
      return ",";
    }

    @Override
    public Class<? extends AssignmentVariable> elementType() {
      return AssignmentVariable.class;
    }
  }

  public static class Attribute extends DefaultAstParent {}

  public static class AttributeList extends AstList<Attribute> {

    @Override
    public String containerType() {
      return "AttributeList";
    }

    @Override
    public Class<? extends Attribute> elementType() {
      return Attribute.class;
    }
  }

  public static class Begin extends DefaultAstParent {

    @Override
    public void propagateOrFinalizeRemoval(AstNode child) {
      remove();
    }

    public Optional<BeginClause> clause() {
      return child(BeginClause.class);
    }

    public Optional<ElseClauseList> elseClauseList() {
      return child(ElseClauseList.class);
    }

    public Optional<EnsureClauseList> ensureClauseList() {
      return child(EnsureClauseList.class);
    }

    public Optional<RescueClauseList> rescueClauseList() {
      return child(RescueClauseList.class);
    }
  }

  public static class BeginClause extends DefaultAstParent {

    public Optional<StatementList> statementList() {
      return child(EnclosedBody.class).flatMap(c -> c.child(StatementList.class));
    }
  }

  public static class BlockCollection extends DefaultAstParent {}

  public static class BlockInitializer extends DefaultAstParent {}

  public static class BlockInitializerOptional extends AstOptional<BlockInitializer> {

    @Override
    public String containerType() {
      return "BlockInitializerOptional";
    }

    @Override
    public Class<? extends BlockInitializer> elementType() {
      return BlockInitializer.class;
    }
  }

  public static class BlockIterator extends DefaultAstParent {}

  public static class BlockUpdate extends DefaultAstParent {}

  public static class BlockUpdateOptional extends AstOptional<BlockUpdate> {

    @Override
    public String containerType() {
      return "BlockUpdateOptional";
    }

    @Override
    public Class<? extends BlockUpdate> elementType() {
      return BlockUpdate.class;
    }
  }

  public static class Break extends DefaultAstParent {}

  public static class Call extends DefaultAstParent {

    public Identifier identifier() {
      return child(Identifier.class).get();
    }
  }

  public static class Case extends DefaultAstParent {}

  public static class CatchClause extends DefaultAstParent {

    public Optional<StatementList> statementList() {
      return child(EnclosedBody.class).flatMap(c -> c.child(StatementList.class));
    }

    public Optional<Statement> statement() {
      return child(Statement.class);
    }

    public Optional<CatchParameter> parameter() {
      return Stream
        .concat(
          child(CatchParameter.class).stream(),
          parameterOptional().flatMap(e -> e.optional()).stream()
        )
        .findFirst();
    }

    public Optional<CatchParameterOptional> parameterOptional() {
      return child(CatchParameterOptional.class);
    }

    public Optional<CatchFilterOptional> catchFilterOptional() {
      return child(CatchFilterOptional.class);
    }
  }

  public static class CatchFilter extends DefaultAstParent {}

  public static class CatchFilterOptional extends AstOptional<CatchFilter> {

    @Override
    public String containerType() {
      return "CatchFilterOptional";
    }

    @Override
    public Class<? extends CatchFilter> elementType() {
      return CatchFilter.class;
    }

    @Override
    protected String prefix() {
      return "when (";
    }

    @Override
    protected String postfix() {
      return ")";
    }
  }

  public static class CatchClauseList extends AstTrailingClauseList<CatchClause> {

    @Override
    public String containerType() {
      return "CatchClauseList";
    }

    @Override
    public Class<? extends CatchClause> elementType() {
      return CatchClause.class;
    }
  }

  public static class CatchClauseListWithBracesOnSameLine extends CatchClauseList {

    @Override
    public boolean isMultiline() {
      return false;
    }
  }

  public static class CatchParameter extends DefaultAstParent {}

  public static class CatchParameterOptional extends AstOptional<CatchParameter> {

    @Override
    public String containerType() {
      return "CatchParameterOptional";
    }

    @Override
    public Class<? extends CatchParameter> elementType() {
      return CatchParameter.class;
    }
  }

  public static class Class_ extends TypeDeclaration {

    public Optional<ClassMemberList> memberList() {
      return child(Ast.EnclosedBody.class)
        .flatMap(body -> (Optional<ClassMemberList>) body.child(ClassMemberList.class));
    }
  }

  public static class ClassMemberList extends MemberList {

    @Override
    public String containerType() {
      return "ClassMemberList";
    }
  }

  public static class Comment extends DefaultAstParent {

    public CommentText text() {
      return child(CommentText.class).get();
    }

    @Override
    public void remove() {
      this.tree().removeComment(this);
    }
  }

  public static class CommentText extends Text {}

  public static class Condition extends DefaultAstParent {}

  public static class ConditionOptional extends AstOptional<Condition> {

    @Override
    public String containerType() {
      return "ConditionOptional";
    }

    @Override
    public Class<? extends Condition> elementType() {
      return Condition.class;
    }
  }

  public static class Constructor extends Method {}

  public static class Continue extends DefaultAstParent {}

  public static class CssInclude extends DefaultAstParent {

    public Optional<Ast.StatementList> blockStatementList() {
      return child(Ast.EnclosedBody.class).flatMap(c -> c.child(Ast.StatementList.class));
    }

    public Optional<Ast.ArgumentList> argumentList() {
      return child(Ast.ArgumentListOptional.class)
        .flatMap(c -> ((ArgumentListOptional) c).argumentList());
    }
  }

  public static class CssMedia extends DefaultAstParent {}

  public static class CssMixin extends DefaultAstParent {

    public Optional<Ast.StatementList> blockStatementList() {
      return child(Ast.EnclosedBody.class).flatMap(c -> c.child(Ast.StatementList.class));
    }

    public Optional<Ast.ParameterList> parameterList() {
      return child(Ast.ParameterListOptional.class)
        .flatMap(c -> c.optional())
        .map(ParameterList.class::cast);
    }
  }

  public static class CssRuleset extends DefaultAstParent {

    public Optional<Ast.StatementList> blockStatementList() {
      return child(Ast.EnclosedBody.class).flatMap(c -> c.child(Ast.StatementList.class));
    }

    public Optional<CssSelectorList> selectorList() {
      return child(CssSelectorList.class);
    }
  }

  public static class CssSelector extends DefaultAstParent {}

  public static class CssSelectorList extends AstList {

    @Override
    public String containerType() {
      return "CssSelectorList";
    }

    @Override
    public Class<? extends AstNode> elementType() {
      return CssSelector.class;
    }
  }

  public static class Debugger extends DefaultAstParent {}

  public static class Decorator extends DefaultAstParent {

    public Optional<DecoratorExpression> value() {
      return child(DecoratorExpression.class);
    }
  }

  public static class DecoratorList extends AstList<Decorator> {

    @Override
    public String containerType() {
      return "DecoratorList";
    }

    @Override
    public boolean boundedAbove() {
      return false;
    }

    @Override
    public boolean isMultiline() {
      return true;
    }

    @Override
    public Class<? extends Decorator> elementType() {
      return Decorator.class;
    }
  }

  public static class DecoratorExpression extends DefaultAstParent {}

  public static class DefaultMethodClause extends DefaultAstParent {}

  public static class Defer extends DefaultAstParent {}

  public static class Delete extends DefaultAstParent {}

  public static class DeleteMethodClause extends DefaultAstParent {}

  public static class Dictionary extends AstIndentAligner {}

  public static class DoWhile extends DefaultAstParent {}

  public static class EnsureClause extends DefaultAstParent {

    public Optional<StatementList> statementList() {
      return child(EnclosedBody.class).flatMap(c -> c.child(StatementList.class));
    }
  }

  public static class EnsureClauseList extends AstTrailingClauseList {

    @Override
    public String containerType() {
      return "RescueClauseList";
    }

    @Override
    public Class<? extends AstNode> elementType() {
      return RescueElseEnsureElement.class;
    }

    @Override
    public List<Class<? extends AstNode>> innerElementTypes() {
      return Arrays.asList(EnsureClause.class);
    }
  }

  public static class ElseClause extends DefaultAstParent {

    public Optional<StatementList> statementList() {
      return child(EnclosedBody.class).flatMap(c -> c.child(StatementList.class));
    }

    public Optional<Statement> statement() {
      return child(Statement.class);
    }
  }

  public static class ElseClauseList extends AstTrailingClauseList {

    @Override
    public String containerType() {
      return "RescueClauseList";
    }

    @Override
    public Class<? extends AstNode> elementType() {
      return RescueElseEnsureElement.class;
    }

    @Override
    public List<Class<? extends AstNode>> innerElementTypes() {
      return Arrays.asList(ElseClause.class);
    }
  }

  public static class ElseClauseOptional extends AstMultilineOptional {

    @Override
    public String containerType() {
      return "ElseClauseOptional";
    }

    @Override
    public Class<? extends AstNode> elementType() {
      return ElseClause.class;
    }
  }

  public static class ElseClauseOptionalWithBracesOnSameLine extends ElseClauseOptional {

    @Override
    protected boolean isInlineWithSiblings() {
      return true;
    }
  }

  public static class ElseIfClause extends DefaultAstParent {

    public Optional<BlockInitializerOptional> blockInitializerOptional() {
      return child(Ast.BlockInitializerOptional.class);
    }

    public Optional<Condition> condition() {
      return child(Ast.Condition.class);
    }

    public Optional<ModifierList> modifierList() {
      return child(ModifierList.class);
    }

    public Optional<StatementList> statementList() {
      return child(EnclosedBody.class).flatMap(c -> c.child(StatementList.class));
    }

    public Optional<Statement> statement() {
      return child(Statement.class);
    }
  }

  public static class ElseIfClauseList extends AstTrailingClauseList {

    @Override
    public String containerType() {
      return "ElseIfClauseList";
    }

    @Override
    public Class<? extends AstNode> elementType() {
      return ElseIfClause.class;
    }
  }

  public static class ElseIfClauseListWithBracesOnSameLine extends ElseIfClauseList {

    @Override
    public boolean isMultiline() {
      return false;
    }
  }

  public static class EnclosedBody extends AstIndentAligner {

    public AstNode inner() {
      return children().get(1);
    }
  }

  public static class Enum extends TypeDeclaration {

    public Optional<EnumMemberList> memberList() {
      return child(Ast.EnclosedBody.class).flatMap(body -> body.child(EnumMemberList.class));
    }
  }

  public static class EnumConstant extends DefaultAstParent {}

  public static class EnumMemberList extends MemberList {

    @Override
    public String containerType() {
      return "EnumMemberList";
    }

    @Override
    public String delimiter() {
      return ",";
    }

    @Override
    public List<Class<? extends AstParent>> innerElementTypes() {
      return Stream
        .concat(super.innerElementTypes().stream(), Arrays.asList(EnumConstant.class).stream())
        .collect(Collectors.toList());
    }
  }

  public static class ExtendsList extends AstList<ExtendsType> {

    @Override
    public String containerType() {
      return "ExtendsList";
    }

    @Override
    public String delimiter() {
      return ",";
    }

    @Override
    public Class<? extends ExtendsType> elementType() {
      return ExtendsType.class;
    }
  }

  public static class ExtendsListOptional extends AstListOptional {

    @Override
    public String containerType() {
      return "ExtendsListOptional";
    }

    @Override
    public Class<ExtendsList> elementType() {
      return ExtendsList.class;
    }

    @Override
    public List<Class<? extends AstNode>> innerElementTypes() {
      return Arrays.asList(ExtendsType.class);
    }

    @Override
    protected String prefix() {
      // there's no space here because the AST will add spaces to optional lists
      return "extends";
    }
  }

  public static class ExtendsOptional extends AstOptional<ExtendsType> {

    @Override
    public String containerType() {
      return "ExtendsOptional";
    }

    @Override
    public Class<? extends ExtendsType> elementType() {
      return ExtendsType.class;
    }

    @Override
    protected String prefix() {
      return "extends ";
    }
  }

  public static class ExtendsType extends DefaultAstParent {}

  public static class FieldInitializerList extends AstList {

    @Override
    public String containerType() {
      return "FieldInitializerList";
    }

    @Override
    public String delimiter() {
      return ",";
    }

    @Override
    public Class<? extends AstNode> elementType() {
      return FieldInitializer.class;
    }
  }

  public static class FieldInitializer extends DefaultAstParent {}

  public static class FinallyClause extends DefaultAstParent {

    public Optional<StatementList> statementList() {
      return child(EnclosedBody.class).flatMap(c -> c.child(StatementList.class));
    }
  }

  public static class FinallyClauseOptional extends AstMultilineOptional<FinallyClause> {

    @Override
    public String containerType() {
      return "FinallyClauseOptional";
    }

    @Override
    public Class<FinallyClause> elementType() {
      return FinallyClause.class;
    }
  }

  public static class FinallyClauseOptionalWithBracesOnSameLine extends FinallyClauseOptional {

    @Override
    protected boolean isInlineWithSiblings() {
      return true;
    }
  }

  public static class For extends DefaultAstParent {

    public Optional<LoopLabelOptional> loopLabelOptional() {
      return child(LoopLabelOptional.class);
    }

    public Optional<ForClause> forClause() {
      return child(ForClause.class);
    }

    public Optional<ForEachClause> forEachClause() {
      return child(ForEachClause.class);
    }

    public Optional<ElseClauseOptional> elseClauseOptional() {
      return child(ElseClauseOptional.class);
    }
  }

  public static class ForClause extends DefaultAstParent {

    public Optional<BlockInitializerOptional> initializerOptional() {
      return child(BlockInitializerOptional.class);
    }

    public Optional<ConditionOptional> conditionOptional() {
      return child(ConditionOptional.class);
    }

    public Optional<ModifierList> modifierList() {
      return child(ModifierList.class);
    }

    public Optional<BlockUpdateOptional> updateOptional() {
      return child(BlockUpdateOptional.class);
    }

    public Optional<StatementList> statementList() {
      return child(EnclosedBody.class).flatMap(c -> c.child(StatementList.class));
    }

    public Optional<Statement> statement() {
      return child(Statement.class);
    }
  }

  public static class ForEachClause extends DefaultAstParent {

    public Optional<BlockIterator> blockIterator() {
      return child(BlockIterator.class);
    }

    public Optional<BlockCollection> blockCollection() {
      return child(BlockCollection.class);
    }

    public Optional<ModifierList> modifierList() {
      return child(ModifierList.class);
    }

    public boolean of() {
      return (
        child(ForEachSeparator.class).isPresent() &&
        child(ForEachSeparator.class).get().code().equals("of")
      );
    }

    public Optional<StatementList> statementList() {
      return child(EnclosedBody.class).flatMap(c -> c.child(StatementList.class));
    }

    public Optional<Statement> statement() {
      return child(Statement.class);
    }
  }

  public static class ForEachSeparator extends DefaultAstParent {}

  public static class Function extends FunctionDeclaration {}

  public static class FunctionModifierList extends AstList {

    @Override
    public String containerType() {
      return "FunctionModifierList";
    }

    @Override
    public String delimiter() {
      return "";
    }

    @Override
    public Class<? extends AstNode> elementType() {
      return Modifier.class;
    }
  }

  public static class Generator extends DefaultAstParent {}

  public static class Identifier extends DefaultAstParent {}

  public static class If extends DefaultAstParent {

    @Override
    public void propagateOrFinalizeRemoval(AstNode child) {
      remove();
    }

    public Optional<ElseIfClauseList> elseIfClauseList() {
      return child(ElseIfClauseList.class);
    }

    public Optional<ElseClauseOptional> elseClauseOptional() {
      return child(ElseClauseOptional.class);
    }

    public Optional<IfClause> ifClause() {
      return child(IfClause.class);
    }
  }

  public static class IfClause extends DefaultAstParent {

    public Optional<BlockInitializerOptional> blockInitializerOptional() {
      return child(BlockInitializerOptional.class);
    }

    public Optional<Condition> condition() {
      return child(Condition.class);
    }

    public Optional<ModifierList> modifierList() {
      return child(ModifierList.class);
    }

    public Optional<StatementList> statementList() {
      return child(EnclosedBody.class).flatMap(c -> c.child(StatementList.class));
    }

    public Optional<Statement> statement() {
      return child(Statement.class);
    }
  }

  public static class Implementation extends TypeDeclaration {

    public Optional<ImplementationMemberList> memberList() {
      return child(Ast.EnclosedBody.class)
        .flatMap(body -> body.child(ImplementationMemberList.class));
    }
  }

  public static class ImplementationMemberList extends MemberList {

    @Override
    public String containerType() {
      return "ImplementationMemberList";
    }
  }

  public static class ImplementsList extends AstList<ImplementsType> {

    @Override
    public String containerType() {
      return "ImplementsList";
    }

    @Override
    public String delimiter() {
      return ",";
    }

    @Override
    public Class<? extends ImplementsType> elementType() {
      return ImplementsType.class;
    }
  }

  public static class ImplementsListOptional extends AstListOptional {

    @Override
    public String containerType() {
      return "ImplementsListOptional";
    }

    @Override
    public Class<ImplementsList> elementType() {
      return ImplementsList.class;
    }

    @Override
    public List<Class<? extends AstNode>> innerElementTypes() {
      return Arrays.asList(ImplementsType.class);
    }

    @Override
    protected String prefix() {
      return "implements ";
    }
  }

  public static class ImplementsType extends DefaultAstParent {}

  public static class Import extends TerminatedElement {}

  public static class ImportList extends AstListWithLinePadding<AstParent> {

    @Override
    public String containerType() {
      return "ImportList";
    }

    @Override
    public Class<? extends AstParent> elementType() {
      return Statement.class;
    }

    @Override
    public List<Class<? extends AstParent>> innerElementTypes() {
      return Arrays.asList(Import.class);
    }
  }

  public static class ImportSpecifier extends DefaultAstParent {}

  public static class ImportSpecifierList extends AstList<ImportSpecifier> {

    @Override
    public String containerType() {
      return "ImportSpecifierList";
    }

    @Override
    public String delimiter() {
      return ",";
    }

    @Override
    public Class<? extends ImportSpecifier> elementType() {
      return ImportSpecifier.class;
    }
  }

  public static class InitializerExpression extends DefaultAstParent {}

  public static class InitializerExpressionList extends AstList<InitializerExpression> {

    @Override
    public String containerType() {
      return "InitializerExpressionList";
    }

    @Override
    public String delimiter() {
      return ",";
    }

    @Override
    public Class<? extends InitializerExpression> elementType() {
      return InitializerExpression.class;
    }
  }

  public static class Interface extends TypeDeclaration {

    public Optional<InterfaceMemberList> memberList() {
      return child(EnclosedBody.class).flatMap(body -> body.child(InterfaceMemberList.class));
    }
  }

  public static class InterfaceMemberList extends MemberList {

    @Override
    public String containerType() {
      return "InterfaceMemberList";
    }

    @Override
    protected int minimumBlankLines(AstNode e) {
      // methods definitions aren't separated by spaces like methods are in
      // class member lists.
      return 0;
    }
  }

  public static class JsxEmbeddedExpression extends DefaultAstParent {}

  public static class KeyValuePair extends DefaultAstParent {

    public Optional<KeyValuePairKey> key() {
      return child(KeyValuePairKey.class);
    }

    public Optional<KeyValuePairValue> value() {
      return child(KeyValuePairValue.class);
    }

    public static String build(String key, Optional<String> value) {
      return key + value.map(val -> ": " + val).orElse("");
    }
  }

  public static class KeyValuePairKey extends DefaultAstParent {}

  public static class KeyValuePairList extends AstListWithLinePadding<AstParent> {

    @Override
    public String containerType() {
      return "KeyValuePairList";
    }

    @Override
    public Class<? extends AstParent> elementType() {
      return KeyValuePair.class;
    }

    @Override
    public String delimiter() {
      return ",";
    }

    @Override
    public List<Class<? extends AstParent>> innerElementTypes() {
      return Arrays.asList(Splat.class, KeyValuePairKey.class, KeyValuePairValue.class);
    }

    public List<KeyValuePair> keyValues() {
      return children()
        .stream()
        .filter(KeyValuePair.class::isInstance)
        .map(KeyValuePair.class::cast)
        .collect(Collectors.toList());
    }
  }

  public static class KeyValuePairValue extends DefaultAstParent {}

  public static class Lambda extends FunctionDeclaration {}

  public static class LambdaCaptureSpecifier extends DefaultAstParent {}

  public static class List_ extends AstList<ListElement> {

    @Override
    public String containerType() {
      return "List_";
    }

    @Override
    public String delimiter() {
      return ",";
    }

    @Override
    public Class<? extends ListElement> elementType() {
      return ListElement.class;
    }
  }

  public static class ListElement extends DefaultAstParent {}

  public static class Loop extends DefaultAstParent {

    public Optional<LoopLabelOptional> loopLabelOptional() {
      return child(LoopLabelOptional.class);
    }

    public Optional<StatementList> statementList() {
      return child(EnclosedBody.class).flatMap(c -> c.child(StatementList.class));
    }
  }

  public static class LoopLabelOptional extends AstList<AstParent> {

    @Override
    public String containerType() {
      return "LoopLabelOptional";
    }

    @Override
    public Class<? extends AstParent> elementType() {
      return AstParent.class;
    }
  }

  public static class MarkupAttribute extends DefaultAstParent {

    public Optional<MarkupAttributeName> attributeName() {
      return child(MarkupAttributeName.class);
    }

    public Optional<MarkupAttributeValue> attributeValue() {
      return child(MarkupAttributeValue.class);
    }
  }

  public static class MarkupAttributeName extends DefaultAstParent {}

  public static class MarkupAttributeValue extends DefaultAstParent {

    public Range expression() {
      if (child(Ast.String_.class).isPresent()) {
        return child(Ast.String_.class).get().text().rangeWithCommentsAndWhitespace();
      } else if (child(EnclosedBody.class).isPresent()) {
        return child(EnclosedBody.class).get().inner().range();
      }
      return this.range();
    }
  }

  public static class MarkupAttributeList extends AstList<MarkupAttribute> {

    @Override
    public String containerType() {
      return "MarkupAttributeList";
    }

    @Override
    public Class<? extends MarkupAttribute> elementType() {
      return MarkupAttribute.class;
    }
  }

  public static class MarkupElement extends AstIndentAligner {

    public Optional<MarkupClosingTag> closingTag() {
      return child(MarkupClosingTag.class);
    }

    public Optional<Ast.Identifier> name() {
      return Stream
        .of(singletonTag().stream(), openingTag().stream(), closingTag().stream())
        .flatMap(e -> e)
        .findFirst()
        .flatMap(t -> t.name());
    }

    public Optional<MarkupOpeningTag> openingTag() {
      return child(MarkupOpeningTag.class);
    }

    public Optional<MarkupSingletonTag> singletonTag() {
      return child(MarkupSingletonTag.class);
    }

    public Optional<MarkupContentList> contentList() {
      return child(MarkupContentList.class);
    }

    public Optional<StatementList> externalProgram() {
      // Code from embedded CSS or JSX
      return child(Program.class).flatMap(p -> p.child(StatementList.class));
    }

    public Optional<MarkupText> text() {
      // Maybe technically unnested text segments shouldn't be considered an element, but
      // they're allowed inside of an MarkupElementContent.
      return child(MarkupText.class);
    }
  }

  public static class MarkupContent extends DefaultAstParent {}

  public static class MarkupContentList extends AstListWithLinePadding<AstParent> {

    @Override
    public String containerType() {
      return "MarkupContentList";
    }

    @Override
    public Class<? extends AstParent> elementType() {
      return MarkupContent.class;
    }

    @Override
    public List<Class<? extends AstParent>> innerElementTypes() {
      return Arrays.asList(MarkupElement.class, MarkupText.class);
    }

    @Override
    public void clear() {
      clearWithoutNewline();
    }
  }

  public static class MarkupClosingTag extends MarkupTag {}

  public static class MarkupOpeningTag extends MarkupTag {}

  public static class MarkupSingletonTag extends MarkupTag {}

  public static class MarkupTag extends DefaultAstParent {

    public Optional<MarkupAttributeList> attributeList() {
      return child(MarkupAttributeList.class);
    }

    public Optional<ElseClauseList> elseClauseList() {
      return child(ElseClauseList.class);
    }

    public Optional<EnsureClauseList> ensureClauseList() {
      return child(EnsureClauseList.class);
    }

    public Optional<RescueClauseList> rescueClauseList() {
      return child(RescueClauseList.class);
    }
  }

  public static class MarkupText extends DefaultAstParent {}

  public static class Member extends TerminatedElement {

    @Override
    public void propagateOrFinalizeRemoval(AstNode child) {
      remove();
    }
  }

  public static class Method extends FunctionDeclaration {}

  public static class Modifier extends DefaultAstParent {}

  public static class ModifierList extends AstList<Modifier> {

    @Override
    public String containerType() {
      return "ModifierList";
    }

    @Override
    public String delimiter() {
      return "";
    }

    @Override
    public Class<? extends Modifier> elementType() {
      return Modifier.class;
    }
  }

  public static class Namespace extends DefaultAstParent {

    public Optional<ElseClauseList> elseClauseList() {
      return child(ElseClauseList.class);
    }

    public Optional<EnsureClauseList> ensureClauseList() {
      return child(EnsureClauseList.class);
    }

    public Optional<RescueClauseList> rescueClauseList() {
      return child(RescueClauseList.class);
    }

    public Optional<StatementList> statementList() {
      return child(EnclosedBody.class).flatMap(c -> c.child(StatementList.class));
    }

    public Optional<MemberList> memberList() {
      return child(EnclosedBody.class).flatMap(c -> c.child(MemberList.class));
    }
  }

  public static class NamespaceMemberList extends MemberList {

    @Override
    public String containerType() {
      return "NamespaceMemberList";
    }

    @Override
    public List<Class<? extends AstParent>> innerElementTypes() {
      return Stream
        .concat(super.innerElementTypes().stream(), Arrays.asList(Statement.class).stream())
        .collect(Collectors.toList());
    }
  }

  public static class ReceiverArgument extends DefaultAstParent {}

  public static class ReceiverArgumentOptional extends AstOptional<ReceiverArgument> {

    @Override
    public String containerType() {
      return "ReceiverArgumentOptional";
    }

    @Override
    protected String prefix() {
      return "(";
    }

    @Override
    protected String postfix() {
      return ")";
    }

    @Override
    public Class<? extends ReceiverArgument> elementType() {
      return ReceiverArgument.class;
    }
  }

  public static class RescueClause extends DefaultAstParent {

    public Optional<RescueParameter> parameter() {
      return Stream
        .concat(
          child(RescueParameter.class).stream(),
          parameterOptional().flatMap(e -> e.optional()).stream()
        )
        .findFirst();
    }

    public Optional<RescueParameterOptional> parameterOptional() {
      return child(RescueParameterOptional.class);
    }

    public Optional<StatementList> statementList() {
      return child(EnclosedBody.class).flatMap(c -> c.child(StatementList.class));
    }
  }

  public static class RescueClauseList extends AstTrailingClauseList {

    @Override
    public String containerType() {
      return "RescueClauseList";
    }

    @Override
    public Class<? extends AstNode> elementType() {
      return RescueElseEnsureElement.class;
    }

    @Override
    public List<Class<? extends AstNode>> innerElementTypes() {
      return Arrays.asList(RescueClause.class);
    }
  }

  public static class RescueElseEnsureElement extends DefaultAstParent {}

  public static class RescueParameter extends DefaultAstParent {}

  public static class RescueParameterOptional extends AstOptional<RescueParameter> {

    @Override
    public String containerType() {
      return "RescueParameterOptional";
    }

    @Override
    public Class<? extends RescueParameter> elementType() {
      return RescueParameter.class;
    }
  }

  public static class ReturnValueName extends DefaultAstParent {}

  public static class ReturnValueNameList extends AstList<ReturnValueName> {

    @Override
    public String containerType() {
      return "ReturnValueNameList";
    }

    @Override
    public String delimiter() {
      return ",";
    }

    @Override
    public Class<? extends ReturnValueName> elementType() {
      return ReturnValueName.class;
    }
  }

  public static class ReturnValueNameListOptional extends AstListOptional {

    @Override
    public String containerType() {
      return "ReturnValueNameListOptional";
    }

    @Override
    public Class<ReturnValueNameList> elementType() {
      return ReturnValueNameList.class;
    }

    @Override
    public List<Class<? extends AstNode>> innerElementTypes() {
      return Arrays.asList(ReturnValueName.class);
    }

    @Override
    protected String prefix() {
      return "(";
    }

    @Override
    protected String postfix() {
      return ")";
    }
  }

  public static class ObjectPatternList extends AstList<PatternListElement> {

    @Override
    public String containerType() {
      return "PatternList";
    }

    @Override
    public String delimiter() {
      return ",";
    }

    @Override
    public Class<? extends PatternListElement> elementType() {
      return PatternListElement.class;
    }
  }

  public static class Package extends DefaultAstParent {}

  public static class PackageOptional extends AstOptional<Package> {

    @Override
    public String containerType() {
      return "PackageOptional";
    }

    @Override
    public Class<? extends Package> elementType() {
      return Package.class;
    }
  }

  public static class Parameter extends DefaultAstParent {

    @Override
    public Optional<Identifier> name() {
      return child(Identifier.class);
    }

    public Optional<DecoratorList> decoratorList() {
      return child(DecoratorList.class);
    }

    public Optional<ModifierList> modifierList() {
      return child(ModifierList.class);
    }

    public Optional<ModifierList> modifierListSecond() {
      return child(ModifierList.class, 1);
    }

    public Optional<TypeOptional> typeOptional() {
      return child(TypeOptional.class);
    }

    public Optional<Splat> splat() {
      return Optional.empty();
    }

    public Optional<ParameterValue> value() {
      return child(ParameterValue.class);
    }
  }

  public static class ParameterList extends AstList<Parameter> {

    @Override
    public String containerType() {
      return "ParameterList";
    }

    @Override
    public String delimiter() {
      return ",";
    }

    @Override
    public Class<? extends Parameter> elementType() {
      return Parameter.class;
    }
  }

  public static class ParameterListOptional extends AstListOptional {

    @Override
    public String containerType() {
      return "ParameterListOptional";
    }

    @Override
    public Class<ParameterList> elementType() {
      return ParameterList.class;
    }

    @Override
    public List<Class<? extends AstNode>> innerElementTypes() {
      return Arrays.asList(Parameter.class);
    }

    @Override
    protected String prefix() {
      return "(";
    }

    @Override
    protected String postfix() {
      return ")";
    }
  }

  public static class ParameterValue extends DefaultAstParent {}

  public static class ParameterValueOptional extends AstOptional<ParameterValue> {

    @Override
    public String containerType() {
      return "ParameterValueOptional";
    }

    @Override
    public Class<ParameterValue> elementType() {
      return ParameterValue.class;
    }

    @Override
    protected String prefix() {
      return "=";
    }
  }

  public static class PatternListElement extends DefaultAstParent {}

  public static class Placeholder extends DefaultAstParent {}

  public static class Program extends DefaultAstParent {}

  public static class Property extends DefaultAstParent {

    public Optional<DecoratorList> decoratorList() {
      return child(DecoratorList.class);
    }

    public Optional<ModifierList> modifierList() {
      return child(ModifierList.class);
    }

    public Optional<ModifierList> modifierListSecond() {
      return child(ModifierList.class, 1);
    }

    public Optional<TypeOptional> typeOptional() {
      return child(TypeOptional.class);
    }

    public Optional<AssignmentList> assignmentList() {
      return child(AssignmentList.class);
    }

    public Optional<PropertyAccessorListOptional> propertyAccessorListOptional() {
      return child(PropertyAccessorListOptional.class);
    }

    public Optional<PropertyTagOptional> tagOptional() {
      return child(PropertyTagOptional.class);
    }
  }

  public static class PropertyAccessorListOptional extends AstOptional<PropertyAccessorList> {

    @Override
    public String containerType() {
      return "PropertyAccessorListOptional";
    }

    @Override
    public Class<? extends PropertyAccessorList> elementType() {
      return PropertyAccessorList.class;
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

  public static class PropertyAccessorList extends DefaultAstParent {}

  public static class PropertyTag extends String_ {}

  public static class PropertyTagOptional extends AstOptional<PropertyTag> {

    @Override
    public String containerType() {
      return "PropertyTagOptional";
    }

    @Override
    public Class<? extends PropertyTag> elementType() {
      return PropertyTag.class;
    }
  }

  public static class Prototype extends FunctionDeclaration {}

  public static class RestArgument extends DefaultAstParent {}

  public static class RestArgumentOptional extends AstOptional<RestArgument> {

    @Override
    public String containerType() {
      return "RestArgumentOptional";
    }

    @Override
    public Class<? extends RestArgument> elementType() {
      return RestArgument.class;
    }
  }

  public static class RestParameter extends DefaultAstParent {}

  public static class RestParameterOptional extends AstOptional<RestParameter> {

    @Override
    public String containerType() {
      return "RestParameterOptional";
    }

    @Override
    public Class<? extends RestParameter> elementType() {
      return RestParameter.class;
    }
  }

  public static class Return extends DefaultAstParent {}

  public static class ReturnTypeOptional extends TypeOptional {}

  public static class ReturnValue extends DefaultAstParent {}

  public static class ReturnValueOptional extends AstOptional<ReturnValue> {

    @Override
    public String containerType() {
      return "ReturnValueOptional";
    }

    @Override
    public Class<? extends ReturnValue> elementType() {
      return ReturnValue.class;
    }
  }

  public static class Set_ extends DefaultAstParent {}

  public static class Splat extends DefaultAstParent {}

  public static class Statement extends TerminatedElement {

    @Override
    public void propagateOrFinalizeRemoval(AstNode child) {
      remove();
    }
  }

  public static class StatementList extends AstListWithLinePadding<Statement> {

    @Override
    public String containerType() {
      return "StatementList";
    }

    @Override
    protected int wrapperMinimumBlankLines(AstNode element) {
      return 0;
    }

    @Override
    protected int innerMinimumBlankLines(AstNode innerElement) {
      return 0;
    }

    @Override
    public Class<? extends Statement> elementType() {
      return Statement.class;
    }

    @Override
    public List<Class<? extends AstParent>> innerElementTypes() {
      return Arrays.asList(
        Begin.class,
        Class_.class,
        Enum.class,
        For.class,
        Function.class,
        Interface.class,
        Implementation.class,
        If.class,
        Loop.class,
        Namespace.class,
        Prototype.class,
        Struct.class,
        Trait.class,
        Try.class,
        Until.class,
        Using.class,
        While.class
      );
    }
  }

  public static class String_ extends DefaultAstParent {

    public StringText text() {
      return child(StringText.class).get();
    }
  }

  public static class StringText extends Text {

    public String value() {
      return codeWithCommentsAndWhitespace();
    }
  }

  public static class Struct extends TypeDeclaration {

    public Optional<StructMemberList> memberList() {
      return child(Ast.EnclosedBody.class)
        .flatMap(body -> (Optional<StructMemberList>) body.child(StructMemberList.class));
    }

    public Optional<AnonymousPropertyList> anonymousPropertyList() {
      return child(Ast.EnclosedBody.class)
        .flatMap(body -> (Optional<AnonymousPropertyList>) body.child(AnonymousPropertyList.class));
    }
  }

  public static class StructMemberList extends MemberList {

    @Override
    public String containerType() {
      return "StructMemberList";
    }
  }

  public static class Switch extends DefaultAstParent {}

  public static class SwitchDefault extends DefaultAstParent {}

  public static class Synchronized extends DefaultAstParent {}

  public static class TemplateArgumentList extends DefaultAstParent {}

  public static class TerminatedElement extends DefaultAstParent {

    public Optional<AstNode> inner() {
      return children().stream().filter(child -> !child.isToken(";")).findFirst();
    }
  }

  public static class TraitBoundsOptional extends DefaultAstParent {}

  public static class Text extends DefaultAstParent {}

  public static class Throw extends DefaultAstParent {}

  public static class ThrowsOptional extends AstOptional<ThrowsType> {

    @Override
    public String containerType() {
      return "ThrowsOptional";
    }

    @Override
    public Class<ThrowsType> elementType() {
      return ThrowsType.class;
    }

    @Override
    protected String prefix() {
      return "throws ";
    }
  }

  public static class ThrowsType extends DefaultAstParent {}

  public static class Trait extends TypeDeclaration {

    public Optional<TraitMemberList> traitMemberList() {
      return child(EnclosedBody.class)
        .flatMap(body -> (Optional<TraitMemberList>) body.child(TraitMemberList.class));
    }
  }

  public static class TraitMemberList extends MemberList {

    @Override
    public String containerType() {
      return "TraitMemberList";
    }
  }

  public static class Try extends DefaultAstParent {

    public Optional<TryClause> clause() {
      return child(TryClause.class);
    }

    public Optional<CatchClauseList> catchClauseList() {
      return child(CatchClauseList.class);
    }

    public Optional<FinallyClauseOptional> finallyOptional() {
      return child(FinallyClauseOptional.class);
    }

    public Optional<ElseClauseOptional> elseClauseOptional() {
      return child(ElseClauseOptional.class);
    }
  }

  public static class TryClause extends DefaultAstParent {

    public Optional<StatementList> statementList() {
      return child(EnclosedBody.class).flatMap(c -> c.child(StatementList.class));
    }

    public Optional<Statement> statement() {
      return child(Statement.class);
    }
  }

  public static class Tuple extends DefaultAstParent {}

  public static class Type extends DefaultAstParent {}

  public static class TypeAlias extends DefaultAstParent {}

  public static class TypeArgument extends DefaultAstParent {}

  public static class TypeArgumentList extends AstList<TypeArgument> {

    @Override
    public String containerType() {
      return "TypeArgumentList";
    }

    @Override
    public String delimiter() {
      return ",";
    }

    @Override
    public Class<? extends TypeArgument> elementType() {
      return TypeArgument.class;
    }
  }

  public static class TypeArgumentListOptional extends AstListOptional {

    @Override
    public String containerType() {
      return "TypeArgumentListOptional";
    }

    @Override
    protected String prefix() {
      return "<";
    }

    @Override
    protected String postfix() {
      return ">";
    }

    @Override
    public Class<TypeArgumentList> elementType() {
      return TypeArgumentList.class;
    }

    @Override
    public List<Class<? extends AstNode>> innerElementTypes() {
      return Arrays.asList(TypeArgument.class);
    }
  }

  public static class TypeDeclarationList extends AstListWithLinePadding<AstParent> {

    @Override
    public String containerType() {
      return "TypeDeclarationList";
    }

    @Override
    public Class<? extends AstParent> elementType() {
      return Statement.class;
    }

    @Override
    public List<Class<? extends AstParent>> innerElementTypes() {
      return Arrays.asList(Class_.class, Enum.class, Interface.class);
    }
  }

  public static class TypeDecoratorList extends AstList<AstParent> {

    @Override
    public String containerType() {
      return "TypeDecoratorList";
    }

    @Override
    public boolean boundedAbove() {
      return false;
    }

    @Override
    public boolean isMultiline() {
      return false;
    }

    @Override
    public Class<? extends AstParent> elementType() {
      return Decorator.class;
    }
  }

  public static class TypeOptional extends AstOptional<Type> {

    @Override
    public String containerType() {
      return "TypeOptional";
    }

    @Override
    public Class<? extends Type> elementType() {
      return Type.class;
    }
  }

  public static class TypeParameter extends DefaultAstParent {}

  public static class TypeParameterConstraintList extends AstList<TypeParameterConstraintType> {

    @Override
    public String containerType() {
      return "TypeParameterConstraintList";
    }

    @Override
    public Class<? extends TypeParameterConstraintType> elementType() {
      return TypeParameterConstraintType.class;
    }
  }

  public static class TypeParameterConstraintListOptional extends AstListOptional {

    @Override
    public String containerType() {
      return "TypeParameterConstraintListOptional";
    }

    @Override
    public Class<TypeParameterConstraintList> elementType() {
      return TypeParameterConstraintList.class;
    }

    @Override
    public List<Class<? extends AstNode>> innerElementTypes() {
      return Arrays.asList(TypeParameterConstraintType.class);
    }

    @Override
    protected String prefix() {
      return "where ";
    }
  }

  public static class TypeParameterConstraintType extends DefaultAstParent {}

  public static class TypeParameterList extends AstList<TypeParameter> {

    @Override
    public String containerType() {
      return "TypeParameterList";
    }

    @Override
    public String delimiter() {
      return ",";
    }

    @Override
    public Class<? extends TypeParameter> elementType() {
      return TypeParameter.class;
    }
  }

  public static class TypeParameterListOptional extends AstListOptional {

    @Override
    public String containerType() {
      return "TypeParameterListOptional";
    }

    @Override
    protected String prefix() {
      return "<";
    }

    @Override
    protected String postfix() {
      return ">";
    }

    @Override
    public Class<TypeParameterList> elementType() {
      return TypeParameterList.class;
    }

    @Override
    public List<Class<? extends AstNode>> innerElementTypes() {
      return Arrays.asList(TypeParameter.class);
    }
  }

  public static class Using extends DefaultAstParent {}

  public static class UseClause extends DefaultAstParent {}

  public static class UseClauseList extends AstList<UseClause> {

    @Override
    public String containerType() {
      return "UseClauseList";
    }

    @Override
    public String delimiter() {
      return ",";
    }

    @Override
    public Class<? extends UseClause> elementType() {
      return UseClause.class;
    }
  }

  public static class Until extends DefaultAstParent {

    public Optional<UntilClause> clause() {
      return child(UntilClause.class);
    }
  }

  public static class UntilClause extends DefaultAstParent {

    public Optional<StatementList> statementList() {
      return child(EnclosedBody.class).flatMap(c -> c.child(StatementList.class));
    }

    public Optional<Condition> condition() {
      return child(Condition.class);
    }
  }

  public static class VariableDeclaration extends DefaultAstParent {}

  public static class While extends DefaultAstParent {

    public Optional<LoopLabelOptional> loopLabelOptional() {
      return child(LoopLabelOptional.class);
    }

    public Optional<WhileClause> whileClause() {
      return child(WhileClause.class);
    }

    public Optional<ElseClauseOptional> elseClauseOptional() {
      return child(ElseClauseOptional.class);
    }
  }

  public static class WhileClause extends DefaultAstParent {

    public Optional<StatementList> statementList() {
      return child(EnclosedBody.class).flatMap(c -> c.child(StatementList.class));
    }

    public Optional<Statement> statement() {
      return child(Statement.class);
    }

    public Optional<Condition> condition() {
      return child(Condition.class);
    }
  }

  public static class With extends DefaultAstParent {

    public Optional<StatementList> statementList() {
      return child(EnclosedBody.class).flatMap(c -> c.child(StatementList.class));
    }

    public Optional<Statement> statement() {
      return child(Statement.class);
    }

    public Optional<WithItemList> itemList() {
      return child(WithItemList.class);
    }

    public Optional<ModifierList> modifierList() {
      return child(ModifierList.class);
    }
  }

  public static class WithItem extends DefaultAstParent {

    public Optional<WithItemValue> value() {
      return child(WithItemValue.class);
    }
  }

  public static class WithItemAlias extends DefaultAstParent {}

  public static class WithItemAliasOptional extends AstOptional<WithItemAlias> {

    @Override
    public String containerType() {
      return "WithItemAliasOptional";
    }

    @Override
    public Class<? extends WithItemAlias> elementType() {
      return WithItemAlias.class;
    }

    @Override
    protected String prefix() {
      return "as ";
    }
  }

  public static class WithItemList extends AstList<WithItem> {

    @Override
    public String containerType() {
      return "WithItemList";
    }

    @Override
    public Class<? extends WithItem> elementType() {
      return WithItem.class;
    }
  }

  public static class WithItemValue extends DefaultAstParent {}
}
