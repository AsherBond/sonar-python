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
import org.sonar.check.Rule;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.quickfix.PythonQuickFix;
import org.sonar.plugins.python.api.tree.QualifiedExpression;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatcher;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatchers;
import org.sonar.python.quickfix.TextEditUtils;

@Rule(key = "S6730")
public class DeprecatedNumpyTypesCheck extends PythonSubscriptionCheck {

  private static final String MESSAGE = "Replace this deprecated \"numpy\" type alias with the builtin type \"%s\".";
  private static final String QUICK_FIX_MESSAGE = "Replace with %s.";

  private record DeprecatedType(TypeMatcher matcher, String replacement) {}

  private static final List<DeprecatedType> DEPRECATED_TYPES = List.of(
    new DeprecatedType(TypeMatchers.withFQN("numpy.int"), "int"),
    new DeprecatedType(TypeMatchers.withFQN("numpy.float"), "float"),
    new DeprecatedType(TypeMatchers.withFQN("numpy.complex"), "complex"),
    new DeprecatedType(TypeMatchers.withFQN("numpy.object"), "object"),
    new DeprecatedType(TypeMatchers.withFQN("numpy.str"), "str"),
    new DeprecatedType(TypeMatchers.withFQN("numpy.long"), "int"),
    new DeprecatedType(TypeMatchers.withFQN("numpy.unicode"), "str"));

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.QUALIFIED_EXPR,
        DeprecatedNumpyTypesCheck::checkForDeprecatedTypesNames);
  }

  private static void checkForDeprecatedTypesNames(SubscriptionContext ctx) {
    QualifiedExpression expression = (QualifiedExpression) ctx.syntaxNode();
    for (DeprecatedType deprecatedType : DEPRECATED_TYPES) {
      if (deprecatedType.matcher().isTrueFor(expression, ctx)) {
        raiseIssue(expression, deprecatedType.replacement(), ctx);
        return;
      }
    }
  }

  private static void raiseIssue(QualifiedExpression expression, String replacementType, SubscriptionContext ctx) {
    PreciseIssue issue = ctx.addIssue(expression, String.format(MESSAGE, replacementType));
    PythonQuickFix quickFix = PythonQuickFix.newQuickFix(
        String.format(QUICK_FIX_MESSAGE, replacementType),
        TextEditUtils.replace(expression, replacementType));
    issue.addQuickFix(quickFix);
  }
}
