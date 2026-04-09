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
import org.sonar.plugins.python.api.quickfix.PythonQuickFix;
import org.sonar.plugins.python.api.symbols.v2.SymbolV2;
import org.sonar.plugins.python.api.tree.CallExpression;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.ExpressionStatement;
import org.sonar.plugins.python.api.tree.ForStatement;
import org.sonar.plugins.python.api.tree.Name;
import org.sonar.plugins.python.api.tree.QualifiedExpression;
import org.sonar.plugins.python.api.tree.RegularArgument;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.tree.UnpackingExpression;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatcher;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatchers;
import org.sonar.python.quickfix.TextEditUtils;
import org.sonar.python.tree.TreeUtils;

@Rule(key = "S8502")
public class SetUpdateOverForLoopCheck extends PythonSubscriptionCheck {

  private static final TypeMatcher SET_TYPE_MATCHER = TypeMatchers.isObjectOfType("builtins.set");

  private static final String MESSAGE = "Use \"set.update()\" instead of a for-loop with \"add()\".";

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.FOR_STMT, SetUpdateOverForLoopCheck::checkForStatement);
  }

  private static void checkForStatement(SubscriptionContext ctx) {
    ForStatement forStatement = (ForStatement) ctx.syntaxNode();

    if (forStatement.expressions().size() != 1) {
      return;
    }
    if (forStatement.elseClause() != null) {
      return;
    }
    Expression loopVarExpr = forStatement.expressions().get(0);
    if (!(loopVarExpr instanceof Name loopVar)) {
      return;
    }
    if (forStatement.testExpressions().size() != 1) {
      return;
    }
    Expression iterable = forStatement.testExpressions().get(0);

    CallExpression callExpr = extractSingleBodyCallExpression(forStatement);
    if (callExpr == null) {
      return;
    }
    if (!(callExpr.callee() instanceof QualifiedExpression qualifiedExpr)) {
      return;
    }
    // TypeMatchers operate on types, not method names; the callee name must be checked syntactically
    if (!"add".equals(qualifiedExpr.name().name())) {
      return;
    }
    Expression receiver = qualifiedExpr.qualifier();

    // Exclude transient objects like get_set().add(item)
    if (receiver instanceof CallExpression) {
      return;
    }
    if (!SET_TYPE_MATCHER.isTrueFor(receiver, ctx)) {
      return;
    }
    if (!isLoopVariableTheOnlyArgument(callExpr, loopVar)) {
      return;
    }

    PreciseIssue issue = ctx.addIssue(qualifiedExpr, MESSAGE);
    addQuickFix(issue, forStatement, receiver, iterable);
  }

  private static boolean isLoopVariableTheOnlyArgument(CallExpression callExpr, Name loopVar) {
    if (callExpr.arguments().size() != 1) {
      return false;
    }
    if (!(callExpr.arguments().get(0) instanceof RegularArgument regularArgument)) {
      return false;
    }
    if (regularArgument.keywordArgument() != null) {
      return false;
    }
    Expression argExpr = regularArgument.expression();
    if (argExpr instanceof UnpackingExpression || !(argExpr instanceof Name argName)) {
      return false;
    }
    SymbolV2 loopVarSymbol = loopVar.symbolV2();
    return loopVarSymbol != null && loopVarSymbol.equals(argName.symbolV2());
  }

  private static CallExpression extractSingleBodyCallExpression(ForStatement forStatement) {
    if (forStatement.body().statements().size() != 1) {
      return null;
    }
    if (!(forStatement.body().statements().get(0) instanceof ExpressionStatement exprStmt)) {
      return null;
    }
    if (exprStmt.expressions().size() != 1) {
      return null;
    }
    if (!(exprStmt.expressions().get(0) instanceof CallExpression callExpr)) {
      return null;
    }
    return callExpr;
  }

  private static void addQuickFix(PreciseIssue issue, ForStatement forStatement, Expression receiver, Expression iterable) {
    String receiverText = TreeUtils.treeToString(receiver, false);
    String iterableText = TreeUtils.treeToString(iterable, false);
    if (receiverText == null || iterableText == null) {
      return;
    }

    String replacement = receiverText + ".update(" + iterableText + ")";

    PythonQuickFix quickFix = PythonQuickFix.newQuickFix("Use \"set.update()\" instead", TextEditUtils.replace(forStatement, replacement));
    issue.addQuickFix(quickFix);
  }
}
