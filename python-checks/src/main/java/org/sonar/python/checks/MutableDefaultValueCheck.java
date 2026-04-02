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

import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.tree.Argument;
import org.sonar.plugins.python.api.tree.CallExpression;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.Name;
import org.sonar.plugins.python.api.tree.QualifiedExpression;
import org.sonar.plugins.python.api.tree.RegularArgument;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatcher;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatchers;
import org.sonar.python.tree.TreeUtils;

@Rule(key = "S8508")
public class MutableDefaultValueCheck extends PythonSubscriptionCheck {

  private static final String MESSAGE = "Replace this mutable value with an immutable default to avoid shared state.";

  private static final TypeMatcher IS_DICT_TYPE = TypeMatchers.isType("builtins.dict");
  private static final TypeMatcher IS_CONTEXT_VAR = TypeMatchers.isType("contextvars.ContextVar");
  private static final TypeMatcher IS_MUTABLE_CONSTRUCTOR = TypeMatchers.any(
    TypeMatchers.isType("builtins.list"),
    TypeMatchers.isType("builtins.dict"),
    TypeMatchers.isType("builtins.set"),
    TypeMatchers.isType("builtins.bytearray")
  );

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.CALL_EXPR, MutableDefaultValueCheck::checkCall);
  }

  private static void checkCall(SubscriptionContext ctx) {
    CallExpression callExpression = (CallExpression) ctx.syntaxNode();
    Expression callee = callExpression.callee();
    if (isDictFromkeys(callee, ctx)) {
      checkDictFromkeys(ctx, callExpression);
    } else if (IS_CONTEXT_VAR.isTrueFor(callee, ctx)) {
      checkContextVar(ctx, callExpression);
    }
  }

  private static boolean isDictFromkeys(Expression callee, SubscriptionContext ctx) {
    if (!(callee instanceof QualifiedExpression qualifiedExpr)) {
      return false;
    }
    return "fromkeys".equals(qualifiedExpr.name().name())
      && IS_DICT_TYPE.isTrueFor(qualifiedExpr.qualifier(), ctx);
  }

  private static void checkDictFromkeys(SubscriptionContext ctx, CallExpression callExpression) {
    List<Argument> arguments = callExpression.arguments();
    if (arguments.size() < 2) {
      return;
    }
    Argument secondArg = arguments.get(1);
    if (secondArg instanceof RegularArgument regularArg && regularArg.keywordArgument() == null) {
      checkAndReportIfMutable(regularArg.expression(), ctx);
    }
  }

  private static void checkContextVar(SubscriptionContext ctx, CallExpression callExpression) {
    RegularArgument defaultArg = TreeUtils.argumentByKeyword("default", callExpression.arguments());
    if (defaultArg != null) {
      checkAndReportIfMutable(defaultArg.expression(), ctx);
    }
  }

  private static void checkAndReportIfMutable(Expression value, SubscriptionContext ctx) {
    if (isMutableValue(value, ctx)) {
      ctx.addIssue(value, MESSAGE);
    }
  }

  private static boolean isMutableValue(Expression expression, SubscriptionContext ctx) {
    if (expression.is(Tree.Kind.LIST_LITERAL)
      || expression.is(Tree.Kind.DICTIONARY_LITERAL)
      || expression.is(Tree.Kind.SET_LITERAL)) {
      return true;
    }
    if (expression instanceof CallExpression callExpr) {
      return IS_MUTABLE_CONSTRUCTOR.isTrueFor(callExpr.callee(), ctx);
    }
    if (expression instanceof Name name) {
      Set<Expression> values = ctx.valuesAtLocation(name);
      return !values.isEmpty() && values.stream().allMatch(v -> isMutableValue(v, ctx));
    }
    return false;
  }
}
