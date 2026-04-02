/*
 * SonarQube Python Plugin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.python.checks;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.symbols.v2.SymbolV2;
import org.sonar.plugins.python.api.symbols.v2.UsageV2;
import org.sonar.plugins.python.api.types.v2.FunctionType;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatcher;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatchers;
import org.sonar.plugins.python.api.tree.BinaryExpression;
import org.sonar.plugins.python.api.tree.CallExpression;
import org.sonar.plugins.python.api.tree.ClassDef;
import org.sonar.plugins.python.api.tree.ConditionalExpression;
import org.sonar.plugins.python.api.tree.Decorator;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.FileInput;
import org.sonar.plugins.python.api.tree.FunctionDef;
import org.sonar.plugins.python.api.tree.Name;
import org.sonar.plugins.python.api.tree.QualifiedExpression;
import org.sonar.plugins.python.api.tree.StatementList;
import org.sonar.plugins.python.api.tree.StringLiteral;
import org.sonar.plugins.python.api.tree.Token;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.tree.Tree.Kind;
import org.sonar.plugins.python.api.tree.UnaryExpression;
import org.sonar.plugins.python.api.tree.WithItem;
import org.sonar.plugins.python.api.tree.WithStatement;
import org.sonar.python.tree.TreeUtils;

@Rule(key = "S905")
public class UselessStatementCheck extends PythonSubscriptionCheck {

  private static final boolean DEFAULT_REPORT_ON_STRINGS = false;
  private static final String DEFAULT_IGNORED_OPERATORS = "<<,>>,|";

  @RuleProperty(
    key = "reportOnStrings",
    description = "Enable issues on string literals which are not assigned. Set this parameter to \"false\" if you use strings as comments.",
    defaultValue = "" + DEFAULT_REPORT_ON_STRINGS)
  public boolean reportOnStrings = DEFAULT_REPORT_ON_STRINGS;

  @RuleProperty(
    key = "ignoredOperators",
    description = "Comma separated list of ignored operators",
    defaultValue = DEFAULT_IGNORED_OPERATORS)
  public String ignoredOperators = DEFAULT_IGNORED_OPERATORS;

  List<String> ignoredOperatorsList;

  private List<String> ignoredOperators() {
    if (ignoredOperatorsList == null) {
      ignoredOperatorsList = Stream.of(ignoredOperators.split(","))
        .map(String::trim).toList();
    }
    return ignoredOperatorsList;
  }

  private static final List<Kind> regularKinds = Arrays.asList(Kind.NUMERIC_LITERAL, Kind.LIST_LITERAL, Kind.SET_LITERAL, Kind.DICTIONARY_LITERAL,
    Kind.NONE, Kind.LAMBDA);

  private static final List<Kind> binaryExpressionKinds = Arrays.asList(Kind.AND, Kind.OR, Kind.PLUS, Kind.MINUS,
    Kind.MULTIPLICATION, Kind.DIVISION, Kind.FLOOR_DIVISION, Kind.MODULO, Kind.MATRIX_MULTIPLICATION, Kind.SHIFT_EXPR,
    Kind.BITWISE_AND, Kind.BITWISE_OR, Kind.BITWISE_XOR, Kind.COMPARISON, Kind.POWER);

  private static final List<Kind> unaryExpressionKinds = Arrays.asList(Kind.UNARY_PLUS, Kind.UNARY_MINUS, Kind.BITWISE_COMPLEMENT, Kind.NOT);

  private static final String MESSAGE = "Remove or refactor this statement; it has no side effects.";

  private static final TypeMatcher EXCEPTION_CLASS_TYPE_MATCHER = TypeMatchers.isOrExtendsType("builtins.BaseException");

  private static final TypeMatcher CONTEXTLIB_SUPPRESS_TYPE_MATCHER = TypeMatchers.isType("contextlib.suppress");

  private static final TypeMatcher AIRFLOW_TYPE_MATCHER = TypeMatchers.any(
    TypeMatchers.isObjectInstanceOf("airflow.models.baseoperator.BaseOperator"),
    TypeMatchers.isObjectInstanceOf("airflow.models.dag.DAG"),
    TypeMatchers.isObjectInstanceOf("airflow.models.taskmixin.DependencyMixin")
  );

  private static final TypeMatcher DAG_CLASS_TYPE_MATCHER = TypeMatchers.isType("airflow.models.dag.DAG");

  private static final TypeMatcher DAG_DECORATOR_TYPE_MATCHER = TypeMatchers.isType("airflow.decorators.dag");

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Kind.STRING_LITERAL, this::checkStringLiteral);
    context.registerSyntaxNodeConsumer(Kind.NAME, UselessStatementCheck::checkName);
    context.registerSyntaxNodeConsumer(Kind.QUALIFIED_EXPR, UselessStatementCheck::checkQualifiedExpression);
    context.registerSyntaxNodeConsumer(Kind.CONDITIONAL_EXPR, UselessStatementCheck::checkConditionalExpression);
    binaryExpressionKinds.forEach(b -> context.registerSyntaxNodeConsumer(b, this::checkBinaryExpression));
    unaryExpressionKinds.forEach(u -> context.registerSyntaxNodeConsumer(u, this::checkUnaryExpression));
    regularKinds.forEach(r -> context.registerSyntaxNodeConsumer(r, UselessStatementCheck::checkNode));
  }

  private static void checkNode(SubscriptionContext ctx) {
    if ("__manifest__.py".equals(ctx.pythonFile().fileName())) {
      return;
    }
    Tree tree = ctx.syntaxNode();
    Tree tryParent = TreeUtils.firstAncestorOfKind(tree, Kind.TRY_STMT);
    if (tryParent != null) {
      return;
    }
    if (isBooleanExpressionWithCalls(tree)) {
      return;
    }
    Tree parent = tree.parent();
    if (parent == null || !parent.is(Kind.EXPRESSION_STMT)) {
      return;
    }
    if (isWithinIgnoredContext(tree, ctx)) {
      return;
    }
    // Safe cast because the rule only subscribes to expressions
    if (isAnAirflowException((Expression) tree, ctx)) {
      return;
    }
    ctx.addIssue(tree, MESSAGE);
  }

  private static boolean isAnAirflowException(Expression expression, SubscriptionContext ctx) {
    if (isWithinAirflowContext(expression, ctx)) {
      StatementList statementList = (StatementList) TreeUtils.firstAncestorOfKind(expression, Kind.STATEMENT_LIST);
      return Optional.ofNullable(statementList).map(StatementList::statements).map(statements -> statements.get(statements.size() - 1))
        .filter(lastStatement -> lastStatement.equals(TreeUtils.firstAncestorOfKind(expression, Kind.EXPRESSION_STMT))).isPresent();
    }
    return false;
  }

  private static boolean isWithinIgnoredContext(Tree tree, SubscriptionContext ctx) {
    Tree withParent = TreeUtils.firstAncestorOfKind(tree, Kind.WITH_STMT);
    if (withParent != null) {
      WithStatement withStatement = (WithStatement) withParent;
      return withStatement.withItems().stream()
        .map(WithItem::test)
        .filter(item -> item.is(Kind.CALL_EXPR))
        .map(item -> ((CallExpression) item).callee())
        .anyMatch(callee -> CONTEXTLIB_SUPPRESS_TYPE_MATCHER.isTrueFor(callee, ctx));
    }
    return false;
  }

  private static boolean isWithinAirflowContext(Tree tree, SubscriptionContext ctx) {
    Tree withParent = TreeUtils.firstAncestorOfKind(tree, Kind.WITH_STMT);
    while (withParent != null) {
      WithStatement withStatement = (WithStatement) withParent;
      if (withStatement.withItems().stream()
        .map(WithItem::test)
        .filter(item -> item.is(Kind.CALL_EXPR))
        .map(item -> ((CallExpression) item).callee())
        .anyMatch(callee -> DAG_CLASS_TYPE_MATCHER.isTrueFor(callee, ctx))) {
        return true;
      }
      withParent = TreeUtils.firstAncestorOfKind(withParent, Kind.WITH_STMT);
    }
    FunctionDef funcParent = (FunctionDef) TreeUtils.firstAncestorOfKind(tree, Kind.FUNCDEF);
    return funcParent != null && funcParent.decorators().stream()
      .map(Decorator::expression)
      .anyMatch(expr -> DAG_DECORATOR_TYPE_MATCHER.isTrueFor(expr, ctx));
  }

  private static boolean isBooleanExpressionWithCalls(Tree tree) {
    return (tree.is(Kind.AND) || tree.is(Kind.OR) || tree.is(Kind.NOT)) && (TreeUtils.hasDescendant(tree, t -> t.is(Kind.CALL_EXPR)));
  }

  public static void checkConditionalExpression(SubscriptionContext ctx) {
    ConditionalExpression conditionalExpression = (ConditionalExpression) ctx.syntaxNode();
    if (TreeUtils.hasDescendant(conditionalExpression, t -> t.is(Kind.CALL_EXPR))) {
      return;
    }
    checkNode(ctx);
  }

  private void checkStringLiteral(SubscriptionContext ctx) {
    StringLiteral stringLiteral = (StringLiteral) ctx.syntaxNode();
    if (!reportOnStrings || isDocString(stringLiteral)) {
      return;
    }
    checkNode(ctx);
  }

  private static void checkName(SubscriptionContext ctx) {
    Name name = (Name) ctx.syntaxNode();
    // Creating an exception without raising it is covered by S3984
    if (EXCEPTION_CLASS_TYPE_MATCHER.isTrueFor(name, ctx)) {
      return;
    }
    // Avoid raising on useless statements made to suppress issues due to "unused" import
    SymbolV2 symbolV2 = name.symbolV2();
    if (symbolV2 != null && symbolV2.usages().stream().anyMatch(u -> u.kind() == UsageV2.Kind.IMPORT) && symbolV2.usages().size() == 2) {
      return;
    }
    if (AIRFLOW_TYPE_MATCHER.isTrueFor(name, ctx)) {
      return;
    }
    checkNode(ctx);
  }

  private static void checkQualifiedExpression(SubscriptionContext ctx) {
    QualifiedExpression qualifiedExpression = (QualifiedExpression) ctx.syntaxNode();
    // Only raise on functions; properties are already resolved to their return type by the type inference engine
    if (qualifiedExpression.typeV2() instanceof FunctionType) {
      checkNode(ctx);
    }
  }

  private void checkBinaryExpression(SubscriptionContext ctx) {
    BinaryExpression binaryExpression = (BinaryExpression) ctx.syntaxNode();
    Token operator = binaryExpression.operator();
    if (ignoredOperators().contains(operator.value())) {
      return;
    }
    if (couldBePython2PrintStatement(binaryExpression)) {
      return;
    }
    checkNode(ctx);
  }

  private static boolean couldBePython2PrintStatement(BinaryExpression binaryExpression) {
    return TreeUtils.hasDescendant(binaryExpression, t -> t instanceof CallExpression callExpression
      && callExpression.callee() instanceof Name name
      && "print".equals(name.name()));
  }

  private void checkUnaryExpression(SubscriptionContext ctx) {
    UnaryExpression unaryExpression = (UnaryExpression) ctx.syntaxNode();
    Token operator = unaryExpression.operator();
    if (ignoredOperators().contains(operator.value())) {
      return;
    }
    checkNode(ctx);
  }

  private static boolean isDocString(StringLiteral stringLiteral) {
    Tree parent = TreeUtils.firstAncestorOfKind(stringLiteral, Kind.FILE_INPUT, Kind.CLASSDEF, Kind.FUNCDEF);
    return Optional.ofNullable(parent)
      .map(p -> ((p.is(Kind.FILE_INPUT) && stringLiteral.equals(((FileInput) p).docstring()))
        || (p.is(Kind.CLASSDEF) && stringLiteral.equals(((ClassDef) p).docstring()))
        || (p.is(Kind.FUNCDEF) && stringLiteral.equals(((FunctionDef) p).docstring()))))
      .orElse(false);
  }
}
