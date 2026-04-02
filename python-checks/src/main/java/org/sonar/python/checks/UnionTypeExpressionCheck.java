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

import java.util.function.Predicate;
import org.sonar.check.Rule;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.PythonVersionUtils;
import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.tree.TypeAnnotation;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatcher;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatchers;
import org.sonar.python.tree.TreeUtils;

@Rule(key = "S6546")
public class UnionTypeExpressionCheck extends PythonSubscriptionCheck {

  private static final String MESSAGE = "Use a union type expression for this type hint.";
  private static final TypeMatcher TYPING_UNION_MATCHER = TypeMatchers.isType("typing.Union");

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.PARAMETER_TYPE_ANNOTATION, UnionTypeExpressionCheck::checkTypeAnnotation);
    context.registerSyntaxNodeConsumer(Tree.Kind.RETURN_TYPE_ANNOTATION, UnionTypeExpressionCheck::checkTypeAnnotation);
    context.registerSyntaxNodeConsumer(Tree.Kind.VARIABLE_TYPE_ANNOTATION, UnionTypeExpressionCheck::checkTypeAnnotation);
  }

  private static void checkTypeAnnotation(SubscriptionContext ctx) {
    if (!supportsUnionTypeExpressions(ctx)) {
      return;
    }

    TypeAnnotation typeAnnotation = (TypeAnnotation) ctx.syntaxNode();
    Expression expression = typeAnnotation.expression();

    Predicate<Tree> isUnionName = n -> n.is(Tree.Kind.NAME) && TYPING_UNION_MATCHER.isTrueFor((Expression) n, ctx);
    if (isUnionName.test(expression) || TreeUtils.hasDescendant(expression, isUnionName)) {
      ctx.addIssue(expression, MESSAGE);
    }
  }

  private static boolean supportsUnionTypeExpressions(SubscriptionContext ctx) {
    return PythonVersionUtils.areSourcePythonVersionsGreaterOrEqualThan(ctx.sourcePythonVersions(), PythonVersionUtils.Version.V_310);
  }
}
