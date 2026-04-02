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
import org.sonar.plugins.python.api.tree.CallExpression;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.NumericLiteral;
import org.sonar.plugins.python.api.tree.RegularArgument;
import org.sonar.plugins.python.api.tree.SubscriptionExpression;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.tree.UnaryExpression;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatcher;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatchers;
import org.sonar.python.checks.hotspots.CommonValidationUtils;
import org.sonar.python.checks.utils.Expressions;
import org.sonar.python.tree.TreeUtils;

@Rule(key = "S8517")
public class SortedMinMaxCheck extends PythonSubscriptionCheck {

  private static final TypeMatcher IS_SORTED = TypeMatchers.isType("sorted");

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.SUBSCRIPTION, SortedMinMaxCheck::check);
  }

  private static void check(SubscriptionContext ctx) {
    SubscriptionExpression subscriptionExpression = (SubscriptionExpression) ctx.syntaxNode();

    if (subscriptionExpression.subscripts().expressions().size() != 1) {
      return;
    }

    Expression subscript = subscriptionExpression.subscripts().expressions().get(0);
    Boolean isFirstElement = isIndexZeroOrMinusOne(subscript);
    if (isFirstElement == null) {
      return;
    }

    Expression object = subscriptionExpression.object();
    if (!(object instanceof CallExpression callExpression)) {
      return;
    }

    if (!IS_SORTED.isTrueFor(callExpression.callee(), ctx)) {
      return;
    }

    Boolean isReversed = isReversed(callExpression);
    if (isReversed == null) {
      // Cannot determine reverse truthiness, skip to avoid wrong suggestions
      return;
    }

    String replacement = getReplacementFunction(isFirstElement, isReversed);
    ctx.addIssue(subscriptionExpression, "Use \"" + replacement + "()\" instead of sorting to find this value.");
  }

  /**
   * Returns true if the expression is the integer literal 0 (first element),
   * false if it is -1 (last element), or null if it is neither.
   */
  @Nullable
  private static Boolean isIndexZeroOrMinusOne(Expression subscript) {
    if (subscript instanceof NumericLiteral numericLiteral && CommonValidationUtils.isEqualTo(numericLiteral, 0)) {
      return true;
    }
    if (subscript instanceof UnaryExpression unaryExpression
      && unaryExpression.is(Tree.Kind.UNARY_MINUS)
      && unaryExpression.expression() instanceof NumericLiteral numericLiteral
      && CommonValidationUtils.isEqualTo(numericLiteral, 1)) {
      return false;
    }
    return null;
  }

  /**
   * Returns {@code Boolean.TRUE} if the reverse argument is truthy, {@code Boolean.FALSE} if it is absent or falsy,
   * or {@code null} if the reverse argument is present but its truthiness cannot be determined.
   */
  @Nullable
  private static Boolean isReversed(CallExpression callExpression) {
    RegularArgument reverseArg = TreeUtils.argumentByKeyword("reverse", callExpression.arguments());
    if (reverseArg == null) {
      return Boolean.FALSE;
    }
    Expression value = reverseArg.expression();
    if (Expressions.isTruthy(value)) {
      return Boolean.TRUE;
    }
    if (Expressions.isFalsy(value)) {
      return Boolean.FALSE;
    }
    return null;
  }

  private static String getReplacementFunction(boolean isFirstElement, boolean isReversed) {
    // sorted()[0]          -> min (smallest first without reverse)
    // sorted()[-1]         -> max (largest last without reverse)
    // sorted(reverse=True)[0]  -> max (largest first with reverse)
    // sorted(reverse=True)[-1] -> min (smallest last with reverse)
    if (isFirstElement) {
      return isReversed ? "max" : "min";
    } else {
      return isReversed ? "min" : "max";
    }
  }
}
