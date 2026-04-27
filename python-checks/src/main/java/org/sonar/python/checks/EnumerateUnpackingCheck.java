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
import org.sonar.check.Rule;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.symbols.v2.SymbolV2;
import org.sonar.plugins.python.api.tree.AnnotatedAssignment;
import org.sonar.plugins.python.api.tree.AssignmentStatement;
import org.sonar.plugins.python.api.tree.CallExpression;
import org.sonar.plugins.python.api.tree.CompoundAssignmentStatement;
import org.sonar.plugins.python.api.tree.DelStatement;
import org.sonar.plugins.python.api.tree.ExpressionList;
import org.sonar.plugins.python.api.tree.ForStatement;
import org.sonar.plugins.python.api.tree.Name;
import org.sonar.plugins.python.api.tree.NumericLiteral;
import org.sonar.plugins.python.api.tree.RegularArgument;
import org.sonar.plugins.python.api.tree.SubscriptionExpression;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatcher;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatchers;
import org.sonar.python.tree.TreeUtils;

@Rule(key = "S8518")
public class EnumerateUnpackingCheck extends PythonSubscriptionCheck {

  private static final String MESSAGE = "Unpack the value from 'enumerate()' directly instead of using an index lookup.";
  private static final String SECONDARY_MESSAGE = "Replace this index lookup with the unpacked value.";
  private static final TypeMatcher ENUMERATE_MATCHER = TypeMatchers.isType("enumerate");

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.FOR_STMT, EnumerateUnpackingCheck::check);
  }

  private static void check(SubscriptionContext ctx) {
    ForStatement forStatement = (ForStatement) ctx.syntaxNode();

    if (forStatement.testExpressions().size() != 1) {
      return;
    }
    if (!(forStatement.testExpressions().get(0) instanceof CallExpression call)) {
      return;
    }
    if (!ENUMERATE_MATCHER.isTrueFor(call.callee(), ctx)) {
      return;
    }
    if (forStatement.expressions().size() != 2) {
      return;
    }
    if (!(forStatement.expressions().get(0) instanceof Name indexName)) {
      return;
    }
    SymbolV2 indexSymbol = indexName.symbolV2();
    if (indexSymbol == null) {
      return;
    }

    RegularArgument startArg = TreeUtils.nthArgumentOrKeyword(1, "start", call.arguments());
    if (startArg != null && !(startArg.expression() instanceof NumericLiteral num && "0".equals(num.valueAsString()))) {
      return;
    }

    RegularArgument iterableArg = TreeUtils.nthArgumentOrKeyword(0, "iterable", call.arguments());
    if (iterableArg == null || !(iterableArg.expression() instanceof Name iterableName)) {
      return;
    }
    SymbolV2 iterableSymbol = iterableName.symbolV2();
    if (iterableSymbol == null) {
      return;
    }

    List<SubscriptionExpression> matchingSubscripts = new ArrayList<>();
    collectMatchingSubscripts(forStatement.body(), indexSymbol, iterableSymbol, matchingSubscripts);

    if (matchingSubscripts.isEmpty()) {
      return;
    }
    if (matchingSubscripts.stream().anyMatch(EnumerateUnpackingCheck::isSubscriptWriteTarget)) {
      return;
    }

    PreciseIssue issue = ctx.addIssue(call, MESSAGE);
    matchingSubscripts.forEach(subscript -> issue.secondary(subscript, SECONDARY_MESSAGE));
  }

  private static void collectMatchingSubscripts(Tree tree, SymbolV2 indexSymbol, SymbolV2 iterableSymbol, List<SubscriptionExpression> result) {
    for (Tree child : tree.children()) {
      if (child instanceof SubscriptionExpression subscription && isMatchingSubscript(subscription, indexSymbol, iterableSymbol)) {
        result.add(subscription);
      }
      collectMatchingSubscripts(child, indexSymbol, iterableSymbol, result);
    }
  }

  private static boolean isMatchingSubscript(SubscriptionExpression subscription, SymbolV2 indexSymbol, SymbolV2 iterableSymbol) {
    if (subscription.subscripts().expressions().size() != 1) {
      return false;
    }
    if (!(subscription.subscripts().expressions().get(0) instanceof Name subscriptName)) {
      return false;
    }
    if (!indexSymbol.equals(subscriptName.symbolV2())) {
      return false;
    }
    if (!(subscription.object() instanceof Name objectName)) {
      return false;
    }
    return iterableSymbol.equals(objectName.symbolV2());
  }

  private static boolean isSubscriptWriteTarget(SubscriptionExpression subscription) {
    Tree parent = subscription.parent();
    if (parent instanceof DelStatement) {
      return true;
    }
    if (parent instanceof CompoundAssignmentStatement compound) {
      return compound.lhsExpression() == subscription;
    }
    if (parent instanceof AnnotatedAssignment annotated) {
      return annotated.variable() == subscription;
    }
    return parent instanceof ExpressionList exprList
      && exprList.parent() instanceof AssignmentStatement;
  }
}
