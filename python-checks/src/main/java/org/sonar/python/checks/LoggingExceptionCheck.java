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

import org.sonar.check.Rule;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.tree.CallExpression;
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

  private static final String MESSAGE = "Use \"logging.exception()\" or explicitly pass \"exc_info=False\".";

  private static final TypeMatcher LOGGING_ERROR_MATCHER = TypeMatchers.any(
    TypeMatchers.isType("logging.error"),
    TypeMatchers.isType("logging.Logger.error"),
    TypeMatchers.isType("logging.LoggerAdapter.error")
  );

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
    if (!isDirectlyInsideExceptClause(callExpr)) {
      return;
    }
    RegularArgument excInfoArg = TreeUtils.argumentByKeyword("exc_info", callExpr.arguments());
    if (excInfoArg != null && !Expressions.isTruthy(excInfoArg.expression())) {
      return;
    }
    Name errorName = qualifiedCallee.name();
    ctx.addIssue(errorName, MESSAGE);
  }

  private static boolean isDirectlyInsideExceptClause(Tree tree) {
    Tree parent = tree.parent();
    while (parent != null) {
      if (parent.is(Tree.Kind.FUNCDEF, Tree.Kind.LAMBDA)) {
        return false;
      }
      if (parent.is(Tree.Kind.EXCEPT_CLAUSE, Tree.Kind.EXCEPT_GROUP_CLAUSE)) {
        return true;
      }
      parent = parent.parent();
    }
    return false;
  }
}
