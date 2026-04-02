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
import org.sonar.plugins.python.api.tree.FunctionDef;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatcher;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatchers;
import org.sonar.python.checks.utils.CheckUtils;

@Rule(key = "S8504")
public class PropertyMethodWithoutReturnCheck extends PythonSubscriptionCheck {

  private static final String MESSAGE = "Add a return statement to this property method.";
  private static final TypeMatcher PROPERTY_MATCHER = TypeMatchers.isType("builtins.property");
  private static final TypeMatcher ABSTRACT_METHOD_MATCHER = TypeMatchers.isType("abc.abstractmethod");

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.FUNCDEF, ctx -> {
      FunctionDef functionDef = (FunctionDef) ctx.syntaxNode();

      boolean isProperty = functionDef.decorators().stream()
        .anyMatch(d -> PROPERTY_MATCHER.isTrueFor(d.expression(), ctx));
      boolean isAbstract = functionDef.decorators().stream()
        .anyMatch(d -> ABSTRACT_METHOD_MATCHER.isTrueFor(d.expression(), ctx));

      if (!isProperty || isAbstract) {
        return;
      }

      if (functionDef.body().statements().stream().allMatch(CheckUtils::isEmptyStatement)) {
        return;
      }

      var collector = ReturnCheckUtils.ReturnStmtCollector.collect(functionDef);

      if (collector.getReturnStmts().isEmpty() && !collector.containsYield() && !collector.raisesExceptions()) {
        ctx.addIssue(functionDef.name(), MESSAGE);
      }
    });
  }
}
