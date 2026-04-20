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
import org.sonar.plugins.python.api.tree.Decorator;
import org.sonar.plugins.python.api.tree.FunctionDef;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatcher;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatchers;

@Rule(key = "S8505")
public class SingleDispatchMixupCheck extends PythonSubscriptionCheck {

  private static final TypeMatcher SINGLEDISPATCH_MATCHER = TypeMatchers.isType("functools.singledispatch");
  private static final TypeMatcher SINGLEDISPATCHMETHOD_MATCHER = TypeMatchers.isType("functools.singledispatchmethod");
  private static final TypeMatcher STATICMETHOD_MATCHER = TypeMatchers.isType("builtins.staticmethod");

  private static final String MSG_USE_SINGLEDISPATCHMETHOD =
    "Use \"@singledispatchmethod\" instead of \"@singledispatch\" on methods defined in a class body.";
  private static final String MSG_USE_SINGLEDISPATCH =
    "Use \"@singledispatch\" instead of \"@singledispatchmethod\" on standalone functions.";

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.FUNCDEF, SingleDispatchMixupCheck::checkFunctionDef);
  }

  private static void checkFunctionDef(SubscriptionContext ctx) {
    FunctionDef functionDef = (FunctionDef) ctx.syntaxNode();
    List<Decorator> decorators = functionDef.decorators();
    if (decorators.isEmpty()) {
      return;
    }

    boolean isMethod = functionDef.isMethodDefinition();

    for (int i = 0; i < decorators.size(); i++) {
      Decorator decorator = decorators.get(i);
      if (SINGLEDISPATCH_MATCHER.isTrueFor(decorator.expression(), ctx)) {
        // @singledispatch on a method is broken: dispatch happens on self's/cls's type.
        // The only valid combination on a method is @staticmethod applied as an outer decorator
        if (isMethod && !isWrappedByOuterStaticmethod(decorators, i, ctx)) {
          ctx.addIssue(decorator, MSG_USE_SINGLEDISPATCHMETHOD);
        }
      } else if (SINGLEDISPATCHMETHOD_MATCHER.isTrueFor(decorator.expression(), ctx) && !isMethod) {
        ctx.addIssue(decorator, MSG_USE_SINGLEDISPATCH);
      }
    }
  }

  private static boolean isWrappedByOuterStaticmethod(List<Decorator> decorators, int innerIndex, SubscriptionContext ctx) {
    for (int i = 0; i < innerIndex; i++) {
      if (STATICMETHOD_MATCHER.isTrueFor(decorators.get(i).expression(), ctx)) {
        return true;
      }
    }
    return false;
  }
}
