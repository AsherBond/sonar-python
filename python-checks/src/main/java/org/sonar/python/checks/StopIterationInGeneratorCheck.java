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
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.FunctionDef;
import org.sonar.plugins.python.api.tree.RaiseStatement;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatcher;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatchers;
import org.sonar.python.tree.TreeUtils;

@Rule(key = "S8493")
public class StopIterationInGeneratorCheck extends PythonSubscriptionCheck {

  private static final String MESSAGE = "Replace this \"raise StopIteration\" with a \"return\" statement.";

  private final TypeMatcher stopIterationMatcher = TypeMatchers.any(
    TypeMatchers.isObjectOfType("StopIteration"),
    TypeMatchers.isType("StopIteration")
  );

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.RAISE_STMT, this::checkRaise);
  }

  private void checkRaise(SubscriptionContext ctx) {
    RaiseStatement raiseStatement = (RaiseStatement) ctx.syntaxNode();
    if (raiseStatement.expressions().isEmpty()) {
      return;
    }
    Expression expression = raiseStatement.expressions().get(0);
    if (!stopIterationMatcher.isTrueFor(expression, ctx)) {
      return;
    }
    FunctionDef enclosingFunction = (FunctionDef) TreeUtils.firstAncestorOfKind(raiseStatement, Tree.Kind.FUNCDEF);
    if (enclosingFunction == null) {
      return;
    }
    if (!ReturnCheckUtils.ReturnStmtCollector.collect(enclosingFunction).containsYield()) {
      return;
    }
    ctx.addIssue(raiseStatement, MESSAGE);
  }
}
