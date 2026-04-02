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
import org.sonar.plugins.python.api.tree.InExpression;
import org.sonar.plugins.python.api.tree.QualifiedExpression;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatcher;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatchers;

@Rule(key = "S8521")
public class DictKeysMembershipTestCheck extends PythonSubscriptionCheck {

  private static final String MESSAGE = "Remove this unnecessary \"keys()\" call.";
  private static final String KEYS_METHOD_NAME = "keys";
  private static final TypeMatcher DICT_OR_SUBCLASS_KEYS_MATCHER =
    TypeMatchers.isFunctionOwnerSatisfying(TypeMatchers.isOrExtendsType("builtins.dict"));

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.IN, DictKeysMembershipTestCheck::checkInExpression);
  }

  private static void checkInExpression(SubscriptionContext ctx) {
    InExpression inExpression = (InExpression) ctx.syntaxNode();
    if (!(inExpression.rightOperand() instanceof CallExpression callExpression)) {
      return;
    }
    if (!callExpression.arguments().isEmpty()) {
      return;
    }
    if (!isKeysCall(callExpression)) {
      return;
    }
    if (DICT_OR_SUBCLASS_KEYS_MATCHER.isTrueFor(callExpression.callee(), ctx)) {
      ctx.addIssue(callExpression, MESSAGE);
    }
  }

  private static boolean isKeysCall(CallExpression callExpression) {
    return callExpression.callee() instanceof QualifiedExpression qualifiedExpression
      && KEYS_METHOD_NAME.equals(qualifiedExpression.name().name());
  }
}
