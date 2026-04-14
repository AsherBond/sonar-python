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
import org.sonar.plugins.python.api.tree.AssignmentStatement;
import org.sonar.plugins.python.api.tree.CallExpression;
import org.sonar.plugins.python.api.tree.ExpressionList;
import org.sonar.plugins.python.api.tree.Name;
import org.sonar.plugins.python.api.tree.RegularArgument;
import org.sonar.plugins.python.api.tree.StringLiteral;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatcher;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatchers;
import org.sonar.python.tree.TreeUtils;

@Rule(key = "S8507")
public class TypeVarNamingConventionCheck extends PythonSubscriptionCheck {

  private static final String MESSAGE = "Rename this string to match the variable name \"%s\".";

  private static final TypeMatcher TYPING_CONSTRUCT_MATCHER = TypeMatchers.any(
    TypeMatchers.isType("typing.TypeVar"),
    TypeMatchers.isType("typing_extensions.TypeVar"),
    TypeMatchers.isType("typing.ParamSpec"),
    TypeMatchers.isType("typing_extensions.ParamSpec"),
    TypeMatchers.isType("typing.NewType"),
    TypeMatchers.isType("typing_extensions.NewType")
  );

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.CALL_EXPR, TypeVarNamingConventionCheck::checkCall);
  }

  private static void checkCall(SubscriptionContext ctx) {
    CallExpression call = (CallExpression) ctx.syntaxNode();
    if (!TYPING_CONSTRUCT_MATCHER.isTrueFor(call.callee(), ctx)) {
      return;
    }
    RegularArgument firstArg = TreeUtils.nthArgumentOrKeyword(0, "name", call.arguments());
    if (firstArg == null || !firstArg.expression().is(Tree.Kind.STRING_LITERAL)) {
      return;
    }
    StringLiteral stringLiteral = (StringLiteral) firstArg.expression();
    String stringName = stringLiteral.trimmedQuotesValue();

    Tree parent = call.parent();
    if (!(parent instanceof AssignmentStatement assignment)) {
      return;
    }
    if (assignment.lhsExpressions().size() != 1) {
      return;
    }
    ExpressionList lhsExprList = assignment.lhsExpressions().get(0);
    if (lhsExprList.expressions().size() != 1) {
      return;
    }
    Tree lhsExpr = lhsExprList.expressions().get(0);
    if (!lhsExpr.is(Tree.Kind.NAME)) {
      return;
    }
    String variableName = ((Name) lhsExpr).name();
    if (!stringName.equals(variableName)) {
      ctx.addIssue(stringLiteral, String.format(MESSAGE, variableName));
    }
  }
}
