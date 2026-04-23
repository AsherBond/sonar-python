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
import org.sonar.plugins.python.api.tree.DictionaryLiteral;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.InExpression;
import org.sonar.plugins.python.api.tree.ListLiteral;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.tree.Tuple;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatcher;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatchers;

@Rule(key = "S8503")
public class EmptyCollectionMembershipTestCheck extends PythonSubscriptionCheck {

  private static final String MESSAGE = "Remove this membership test on an empty collection; it will always be the same value.";

  private static final TypeMatcher EMPTY_COLLECTION_CONSTRUCTOR = TypeMatchers.any(
    TypeMatchers.isType("builtins.set"),
    TypeMatchers.isType("builtins.tuple"),
    TypeMatchers.isType("builtins.frozenset")
  );

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.IN, EmptyCollectionMembershipTestCheck::checkInExpression);
  }

  private static void checkInExpression(SubscriptionContext ctx) {
    InExpression inExpression = (InExpression) ctx.syntaxNode();
    Expression rightOperand = inExpression.rightOperand();
    if (isEmptyCollection(rightOperand, ctx)) {
      ctx.addIssue(inExpression, MESSAGE);
    }
  }

  private static boolean isEmptyCollection(Expression expression, SubscriptionContext ctx) {
    if (expression instanceof ListLiteral listLiteral) {
      return listLiteral.elements().expressions().isEmpty();
    }
    if (expression instanceof DictionaryLiteral dictionaryLiteral) {
      return dictionaryLiteral.elements().isEmpty();
    }
    if (expression instanceof Tuple tuple) {
      return tuple.elements().isEmpty();
    }
    if (expression instanceof CallExpression callExpression) {
      return callExpression.arguments().isEmpty()
        && EMPTY_COLLECTION_CONSTRUCTOR.isTrueFor(callExpression.callee(), ctx);
    }
    return false;
  }
}
