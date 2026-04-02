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
import java.util.List;
import java.util.OptionalInt;
import org.sonar.check.Rule;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.FunctionDef;
import org.sonar.plugins.python.api.tree.ReturnStatement;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.tree.Tuple;

@Rule(key = "S8495")
public class InconsistentTupleReturnCheck extends PythonSubscriptionCheck {

  private static final String MESSAGE = "Refactor this function to always return tuples of the same length.";
  private static final String SECONDARY_MESSAGE = "Returns a %d-tuple.";

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.FUNCDEF, InconsistentTupleReturnCheck::checkFunction);
  }

  private static void checkFunction(SubscriptionContext ctx) {
    FunctionDef functionDef = (FunctionDef) ctx.syntaxNode();

    ReturnCheckUtils.ReturnStmtCollector collector = ReturnCheckUtils.ReturnStmtCollector.collect(functionDef);

    if (collector.containsYield()) {
      return;
    }

    List<ReturnStatement> returnStmts = collector.getReturnStmts();

    List<ReturnWithLength> tupleReturns = new ArrayList<>();
    for (ReturnStatement returnStmt : returnStmts) {
      OptionalInt length = getTupleLengthIfTupleReturn(returnStmt);
      if (length.isPresent()) {
        tupleReturns.add(new ReturnWithLength(returnStmt, length.getAsInt()));
      }
    }

    if (tupleReturns.size() < 2) {
      return;
    }

    int firstLength = tupleReturns.get(0).length;
    boolean allSame = tupleReturns.stream().allMatch(r -> r.length == firstLength);
    if (allSame) {
      return;
    }

    PreciseIssue issue = ctx.addIssue(functionDef.name(), MESSAGE);
    for (ReturnWithLength tupleReturn : tupleReturns) {
      issue.secondary(tupleReturn.returnStmt, String.format(SECONDARY_MESSAGE, tupleReturn.length));
    }
  }

  private static boolean containsUnpacking(List<Expression> exprs) {
    return exprs.stream().anyMatch(e -> e.is(Tree.Kind.UNPACKING_EXPR));
  }

  private static OptionalInt getTupleLengthIfTupleReturn(ReturnStatement returnStmt) {
    List<Expression> expressions = returnStmt.expressions();
    if (expressions.isEmpty()) {
      return OptionalInt.empty();
    }
    if (expressions.size() > 1) {
      // Implicit tuple: return a, b — skip if any element is a star expression
      if (containsUnpacking(expressions)) {
        return OptionalInt.empty();
      }
      return OptionalInt.of(expressions.size());
    }
    // Single expression - check if it's an explicit tuple literal
    Expression expr = expressions.get(0);
    if (expr.is(Tree.Kind.TUPLE)) {
      Tuple tuple = (Tuple) expr;
      if (containsUnpacking(tuple.elements())) {
        return OptionalInt.empty();
      }
      return OptionalInt.of(tuple.elements().size());
    }
    return OptionalInt.empty();
  }

  private static class ReturnWithLength {
    final ReturnStatement returnStmt;
    final int length;

    ReturnWithLength(ReturnStatement returnStmt, int length) {
      this.returnStmt = returnStmt;
      this.length = length;
    }
  }
}
