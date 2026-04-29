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

import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.symbols.v2.SymbolV2;
import org.sonar.plugins.python.api.tree.CallExpression;
import org.sonar.plugins.python.api.tree.ExceptClause;
import org.sonar.plugins.python.api.tree.FunctionDef;
import org.sonar.plugins.python.api.tree.LambdaExpression;
import org.sonar.plugins.python.api.tree.Name;
import org.sonar.plugins.python.api.tree.QualifiedExpression;
import org.sonar.plugins.python.api.tree.RegularArgument;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatcher;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatchers;
import org.sonar.python.checks.utils.Expressions;
import org.sonar.python.tree.TreeUtils;

@Rule(key = "S8572")
public class LoggingExceptionCheck extends PythonSubscriptionCheck {

  private static final String MESSAGE = "Use \"logging.exception()\" instead.";

  private static final TypeMatcher LOGGING_ERROR_MATCHER = TypeMatchers.any(
    TypeMatchers.isType("logging.error"),
    TypeMatchers.isType("logging.Logger.error"),
    TypeMatchers.isType("logging.LoggerAdapter.error")
  );

  private static final TypeMatcher TRACEBACK_FORMAT_EXC_MATCHER = TypeMatchers.isType("traceback.format_exc");

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.CALL_EXPR, LoggingExceptionCheck::checkCall);
  }

  private static void checkCall(SubscriptionContext ctx) {
    CallExpression callExpr = (CallExpression) ctx.syntaxNode();
    if (!(callExpr.callee() instanceof QualifiedExpression qualifiedCallee)) {
      return;
    }
    if (!LOGGING_ERROR_MATCHER.isTrueFor(qualifiedCallee, ctx)) {
      return;
    }
    ExceptClause exceptClause = findEnclosingExceptClause(callExpr);
    if (exceptClause == null) {
      return;
    }
    RegularArgument excInfoArg = TreeUtils.argumentByKeyword("exc_info", callExpr.arguments());
    if (excInfoArg != null) {
      if (!Expressions.isTruthy(excInfoArg.expression())) {
        return;
      }
    } else if (!isExceptionLoggedInArgs(callExpr, exceptClause, ctx)) {
      return;
    }
    Name errorName = qualifiedCallee.name();
    ctx.addIssue(errorName, MESSAGE);
  }

  @Nullable
  private static ExceptClause findEnclosingExceptClause(Tree tree) {
    Tree parent = tree.parent();
    while (parent != null) {
      if (parent instanceof FunctionDef || parent instanceof LambdaExpression) {
        return null;
      }
      if (parent instanceof ExceptClause exceptClause) {
        return exceptClause;
      }
      parent = parent.parent();
    }
    return null;
  }

  private static boolean isExceptionLoggedInArgs(CallExpression callExpr, ExceptClause exceptClause, SubscriptionContext ctx) {
    SymbolV2 exceptionSymbol = exceptClause.exceptionInstance() instanceof Name name ? name.symbolV2() : null;
    return callExpr.arguments().stream().anyMatch(arg -> logsException(arg, exceptionSymbol, ctx));
  }

  private static boolean logsException(Tree tree, @Nullable SymbolV2 exceptionSymbol, SubscriptionContext ctx) {
    return matchesExceptionLogging(tree, exceptionSymbol, ctx)
      || TreeUtils.hasDescendant(tree, t -> matchesExceptionLogging(t, exceptionSymbol, ctx));
  }

  private static boolean matchesExceptionLogging(Tree tree, @Nullable SymbolV2 exceptionSymbol, SubscriptionContext ctx) {
    if (exceptionSymbol != null && tree instanceof Name name && exceptionSymbol.equals(name.symbolV2())) {
      return true;
    }
    return tree instanceof CallExpression call && TRACEBACK_FORMAT_EXC_MATCHER.isTrueFor(call.callee(), ctx);
  }
}
