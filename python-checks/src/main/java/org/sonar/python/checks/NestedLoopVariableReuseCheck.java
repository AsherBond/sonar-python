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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.sonar.check.Rule;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.ForStatement;
import org.sonar.plugins.python.api.tree.Name;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.tree.Tuple;

@Rule(key = "S8510")
public class NestedLoopVariableReuseCheck extends PythonSubscriptionCheck {

  private static final String MESSAGE = "Rename this loop variable; it shadows the outer loop variable \"%s\".";
  private static final String SECONDARY_MESSAGE = "Outer loop variable.";

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.FOR_STMT, NestedLoopVariableReuseCheck::checkForStatement);
  }

  private static void checkForStatement(SubscriptionContext ctx) {
    ForStatement forStatement = (ForStatement) ctx.syntaxNode();
    List<Name> innerVarNames = extractLoopVarNames(forStatement);
    if (innerVarNames.isEmpty()) {
      return;
    }
    Map<Name, List<Name>> shadowedOuterNames = collectShadowedNames(forStatement, innerVarNames);
    reportIssues(ctx, shadowedOuterNames);
  }

  private static Map<Name, List<Name>> collectShadowedNames(ForStatement forStatement, List<Name> innerVarNames) {
    Map<Name, List<Name>> shadowedOuterNames = new LinkedHashMap<>();
    for (Name innerName : innerVarNames) {
      shadowedOuterNames.put(innerName, new ArrayList<>());
    }
    Tree current = forStatement.parent();
    while (current != null) {
      if (current.is(Tree.Kind.FUNCDEF, Tree.Kind.CLASSDEF, Tree.Kind.LAMBDA,
        Tree.Kind.LIST_COMPREHENSION, Tree.Kind.SET_COMPREHENSION,
        Tree.Kind.DICT_COMPREHENSION, Tree.Kind.GENERATOR_EXPR)) {
        break;
      }
      if (current.is(Tree.Kind.FOR_STMT)) {
        collectMatchingNames((ForStatement) current, innerVarNames, shadowedOuterNames);
      }
      current = current.parent();
    }
    return shadowedOuterNames;
  }

  private static void collectMatchingNames(ForStatement outerLoop, List<Name> innerVarNames, Map<Name, List<Name>> shadowedOuterNames) {
    List<Name> outerVarNames = extractLoopVarNames(outerLoop);
    for (Name innerName : innerVarNames) {
      for (Name outerName : outerVarNames) {
        if (innerName.name().equals(outerName.name())) {
          shadowedOuterNames.get(innerName).add(outerName);
        }
      }
    }
  }

  private static void reportIssues(SubscriptionContext ctx, Map<Name, List<Name>> shadowedOuterNames) {
    for (Map.Entry<Name, List<Name>> entry : shadowedOuterNames.entrySet()) {
      Name innerName = entry.getKey();
      List<Name> outerNames = entry.getValue();
      if (!outerNames.isEmpty()) {
        var issue = ctx.addIssue(innerName, String.format(MESSAGE, innerName.name()));
        for (Name outerName : outerNames) {
          issue.secondary(outerName, SECONDARY_MESSAGE);
        }
      }
    }
  }

  private static List<Name> extractLoopVarNames(ForStatement forStatement) {
    List<Name> names = new ArrayList<>();
    for (Expression expr : forStatement.expressions()) {
      collectNames(expr, names);
    }
    return names;
  }

  private static void collectNames(Expression expr, List<Name> names) {
    if (expr.is(Tree.Kind.NAME)) {
      Name name = (Name) expr;
      if (!name.name().startsWith("_")) {
        names.add(name);
      }
    } else if (expr.is(Tree.Kind.TUPLE)) {
      for (Expression element : ((Tuple) expr).elements()) {
        collectNames(element, names);
      }
    }
  }
}
