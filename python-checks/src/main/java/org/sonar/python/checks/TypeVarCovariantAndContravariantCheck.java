/*
 * SonarQube Python Plugin
 * Copyright (C) 2011-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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
import org.sonar.plugins.python.api.tree.RegularArgument;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatcher;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatchers;
import org.sonar.python.checks.utils.Expressions;
import org.sonar.python.tree.TreeUtils;

@Rule(key = "S8515")
public class TypeVarCovariantAndContravariantCheck extends PythonSubscriptionCheck {

  private static final String MESSAGE = "Remove either \"covariant=True\" or \"contravariant=True\"; a TypeVar cannot be both covariant and contravariant.";
  private static final TypeMatcher TYPEVAR_MATCHER = TypeMatchers.any(
    TypeMatchers.isType("typing.TypeVar"),
    TypeMatchers.isType("typing_extensions.TypeVar")
  );

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.CALL_EXPR, TypeVarCovariantAndContravariantCheck::checkTypeVarCall);
  }

  private static void checkTypeVarCall(SubscriptionContext ctx) {
    CallExpression call = (CallExpression) ctx.syntaxNode();
    if (!TYPEVAR_MATCHER.isTrueFor(call.callee(), ctx)) {
      return;
    }
    RegularArgument covariantArg = TreeUtils.argumentByKeyword("covariant", call.arguments());
    if (covariantArg == null) {
      return;
    }
    RegularArgument contravariantArg = TreeUtils.argumentByKeyword("contravariant", call.arguments());
    if (contravariantArg == null) {
      return;
    }
    if (Expressions.isTruthy(covariantArg.expression()) && Expressions.isTruthy(contravariantArg.expression())) {
      ctx.addIssue(call.callee(), MESSAGE);
    }
  }
}
