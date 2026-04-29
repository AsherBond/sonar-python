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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.tree.AnnotatedAssignment;
import org.sonar.plugins.python.api.tree.AssignmentStatement;
import org.sonar.plugins.python.api.tree.BaseTreeVisitor;
import org.sonar.plugins.python.api.tree.ClassDef;
import org.sonar.plugins.python.api.tree.Decorator;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.ExpressionList;
import org.sonar.plugins.python.api.tree.FunctionDef;
import org.sonar.plugins.python.api.tree.Name;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatcher;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatchers;

@Rule(key = "S8500")
public class IncompleteComparisonMethodsCheck extends PythonSubscriptionCheck {

  private static final String MESSAGE = "Add the missing comparison methods or use \"functools.total_ordering\".";
  private static final String SECONDARY_MESSAGE = "\"%s\" is defined here.";

  private static final Set<String> ORDERING_METHODS = Set.of("__lt__", "__le__", "__gt__", "__ge__");

  private static final TypeMatcher TOTAL_ORDERING_MATCHER = TypeMatchers.withFQN("functools.total_ordering");

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.CLASSDEF, IncompleteComparisonMethodsCheck::checkClassDef);
  }

  private static void checkClassDef(SubscriptionContext ctx) {
    ClassDef classDef = (ClassDef) ctx.syntaxNode();

    CollectOrderingMethodNamesVisitor visitor = new CollectOrderingMethodNamesVisitor();
    classDef.body().accept(visitor);

    if (visitor.definitions.isEmpty() || visitor.definitions.size() == ORDERING_METHODS.size()) {
      return;
    }

    for (Decorator decorator : classDef.decorators()) {
      if (TOTAL_ORDERING_MATCHER.isTrueFor(decorator.expression(), ctx)) {
        return;
      }
    }

    PreciseIssue issue = ctx.addIssue(classDef.name(), MESSAGE);
    visitor.definitions.forEach((name, tree) -> issue.secondary(tree, String.format(SECONDARY_MESSAGE, name)));
  }

  /**
   * Collects ordering methods defined at the top level of a class body, whether they are
   * introduced by a {@code def} or by an assignment such as {@code __lt__ = lambda ...}.
   * The map preserves the first occurrence per name in source order, which keeps secondary
   * locations stable when a method is shadowed.
   * Mirrors the recursion-control pattern of {@code TreeUtils.CollectFunctionDefsVisitor}:
   * does not descend into nested classes or nested functions.
   */
  private static class CollectOrderingMethodNamesVisitor extends BaseTreeVisitor {
    private final Map<String, Tree> definitions = new LinkedHashMap<>();

    @Override
    public void visitClassDef(ClassDef nestedClass) {
      // Do not descend into nested classes
    }

    @Override
    public void visitFunctionDef(FunctionDef functionDef) {
      Name name = functionDef.name();
      if (ORDERING_METHODS.contains(name.name())) {
        definitions.putIfAbsent(name.name(), name);
      }
      // Do not descend into nested functions
    }

    @Override
    public void visitAssignmentStatement(AssignmentStatement assignment) {
      assignmentTargetName(assignment)
        .filter(name -> ORDERING_METHODS.contains(name.name()))
        .ifPresent(name -> definitions.putIfAbsent(name.name(), name));
      super.visitAssignmentStatement(assignment);
    }

    @Override
    public void visitAnnotatedAssignment(AnnotatedAssignment annotated) {
      if (annotated.assignedValue() != null && annotated.variable() instanceof Name name && ORDERING_METHODS.contains(name.name())) {
        definitions.putIfAbsent(name.name(), name);
      }
      super.visitAnnotatedAssignment(annotated);
    }

    private static Optional<Name> assignmentTargetName(AssignmentStatement assignment) {
      List<ExpressionList> lhsList = assignment.lhsExpressions();
      if (lhsList.size() != 1) {
        return Optional.empty();
      }
      List<Expression> expressions = lhsList.get(0).expressions();
      if (expressions.size() == 1 && expressions.get(0) instanceof Name name) {
        return Optional.of(name);
      }
      return Optional.empty();
    }
  }
}
