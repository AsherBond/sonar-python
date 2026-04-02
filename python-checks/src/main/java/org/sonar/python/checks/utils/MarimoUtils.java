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
package org.sonar.python.checks.utils;

import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.tree.CallExpression;
import org.sonar.plugins.python.api.tree.Decorator;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.FunctionDef;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.tree.Tree.Kind;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatcher;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatchers;
import org.sonar.python.tree.TreeUtils;

public class MarimoUtils {

  private static final TypeMatcher APP_CELL_MATCHER = TypeMatchers.isType("marimo.App.cell");
  private static final TypeMatcher APP_FUNCTION_MATCHER = TypeMatchers.isType("marimo.App.function");

  private MarimoUtils() {
  }

  public static boolean isTreeInMarimoDecoratedFunction(Tree tree, SubscriptionContext ctx) {
    FunctionDef functionDef = (FunctionDef) TreeUtils.firstAncestorOfKind(tree, Kind.FUNCDEF);
    if (functionDef == null) {
      return false;
    }
    return functionDef.decorators().stream().anyMatch(decorator -> isAppCellDecorator(decorator, ctx));
  }

  private static boolean isAppCellDecorator(Decorator decorator, SubscriptionContext ctx) {
    Expression expr = decorator.expression();
    if (expr instanceof CallExpression callExpr) {
      expr = callExpr.callee();
    }
    return APP_CELL_MATCHER.isTrueFor(expr, ctx) || APP_FUNCTION_MATCHER.isTrueFor(expr, ctx);
  }
}
