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
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.tree.AnnotatedAssignment;
import org.sonar.plugins.python.api.tree.AssignmentStatement;
import org.sonar.plugins.python.api.tree.ClassDef;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.ExpressionList;
import org.sonar.plugins.python.api.tree.Name;
import org.sonar.plugins.python.api.tree.Statement;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.tree.Tree.Kind;

@Rule(key = "S8512")
public class ClassFieldDefinedMultipleTimesCheck extends PythonSubscriptionCheck {

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Kind.CLASSDEF, ctx -> checkClass(ctx, (ClassDef) ctx.syntaxNode()));
  }

  private static void checkClass(SubscriptionContext ctx, ClassDef classDef) {
    Map<String, List<Tree>> fieldDefinitions = new LinkedHashMap<>();

    for (Statement stmt : classDef.body().statements()) {
      collectFieldDefinition(stmt, fieldDefinitions);
    }

    reportDuplicateDefinitions(ctx, fieldDefinitions);
  }

  private static void collectFieldDefinition(Statement stmt, Map<String, List<Tree>> fieldDefinitions) {
    if (stmt.is(Kind.ASSIGNMENT_STMT)) {
      getAssignmentName((AssignmentStatement) stmt)
        .ifPresent(name -> fieldDefinitions.computeIfAbsent(name.name(), k -> new ArrayList<>()).add(name));
    } else if (stmt.is(Kind.ANNOTATED_ASSIGNMENT)) {
      getAnnotatedAssignmentName((AnnotatedAssignment) stmt)
        .ifPresent(name -> fieldDefinitions.computeIfAbsent(name.name(), k -> new ArrayList<>()).add(name));
    }
  }

  private static Optional<Name> getAssignmentName(AssignmentStatement assignment) {
    List<ExpressionList> lhsList = assignment.lhsExpressions();
    if (lhsList.size() != 1) {
      return Optional.empty();
    }
    List<Expression> expressions = lhsList.get(0).expressions();
    if (expressions.size() == 1 && expressions.get(0).is(Kind.NAME)) {
      return Optional.of((Name) expressions.get(0));
    }
    return Optional.empty();
  }

  private static Optional<Name> getAnnotatedAssignmentName(AnnotatedAssignment annotated) {
    if (annotated.variable().is(Kind.NAME) && annotated.assignedValue() != null) {
      return Optional.of((Name) annotated.variable());
    }
    return Optional.empty();
  }

  private static void reportDuplicateDefinitions(SubscriptionContext ctx, Map<String, List<Tree>> fieldDefinitions) {
    fieldDefinitions.forEach((name, definitions) -> {
      for (int i = 0; i < definitions.size() - 1; i++) {
        Tree current = definitions.get(i);
        Tree next = definitions.get(i + 1);
        String message = String.format("Remove this assignment; \"%s\" is assigned again on line %d.", name, next.firstToken().line());
        ctx.addIssue(current, message).secondary(next, "Reassignment.");
      }
    });
  }
}
