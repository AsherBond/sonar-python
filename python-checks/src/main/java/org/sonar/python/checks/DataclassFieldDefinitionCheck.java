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
import org.sonar.plugins.python.api.tree.AssignmentStatement;
import org.sonar.plugins.python.api.tree.CallExpression;
import org.sonar.plugins.python.api.tree.ClassDef;
import org.sonar.plugins.python.api.tree.Decorator;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatcher;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatchers;

@Rule(key = "S8514")
public class DataclassFieldDefinitionCheck extends PythonSubscriptionCheck {

  private static final String MESSAGE_MISSING_ANNOTATION = "Add a type annotation to this dataclass attribute.";

  private static final TypeMatcher IS_DATACLASS = TypeMatchers.isType("dataclasses.dataclass");

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.CLASSDEF, DataclassFieldDefinitionCheck::checkClassDef);
  }

  private static void checkClassDef(SubscriptionContext ctx) {
    ClassDef classDef = (ClassDef) ctx.syntaxNode();

    if (!isDataclass(classDef, ctx)) {
      return;
    }

    classDef.body().statements().forEach(statement -> checkStatement(ctx, statement));
  }

  private static boolean isDataclass(ClassDef classDef, SubscriptionContext ctx) {
    return classDef.decorators().stream().anyMatch(decorator -> isDataclassDecorator(decorator, ctx));
  }

  private static boolean isDataclassDecorator(Decorator decorator, SubscriptionContext ctx) {
    Expression expression = decorator.expression();
    if (expression instanceof CallExpression callExpr) {
      return IS_DATACLASS.isTrueFor(callExpr.callee(), ctx);
    }
    return IS_DATACLASS.isTrueFor(expression, ctx);
  }

  private static void checkStatement(SubscriptionContext ctx, Tree statement) {
    if (statement instanceof AssignmentStatement assignmentStatement) {
      ctx.addIssue(assignmentStatement, MESSAGE_MISSING_ANNOTATION);
    }
  }
}
