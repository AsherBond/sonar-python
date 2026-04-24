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
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.quickfix.PythonQuickFix;
import org.sonar.plugins.python.api.tree.Argument;
import org.sonar.plugins.python.api.tree.CallExpression;
import org.sonar.plugins.python.api.tree.ExpressionStatement;
import org.sonar.plugins.python.api.tree.IfStatement;
import org.sonar.plugins.python.api.tree.InExpression;
import org.sonar.plugins.python.api.tree.QualifiedExpression;
import org.sonar.plugins.python.api.tree.RegularArgument;
import org.sonar.plugins.python.api.tree.Statement;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatcher;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatchers;
import org.sonar.python.checks.utils.CheckUtils;
import org.sonar.python.quickfix.TextEditUtils;
import org.sonar.python.tree.TreeUtils;

@Rule(key = "S8492")
public class SetDiscardOverRemoveCheck extends PythonSubscriptionCheck {

  private static final String MESSAGE = "Use \"discard()\" instead of checking membership before calling \"remove()\".";
  private static final String QUICK_FIX_MESSAGE = "Replace with \"discard()\"";
  private static final String REMOVE_METHOD_NAME = "remove";
  private static final TypeMatcher SET_TYPE_MATCHER = TypeMatchers.isObjectInstanceOf("builtins.set");

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.IF_STMT, SetDiscardOverRemoveCheck::checkIfStatement);
  }

  private static void checkIfStatement(SubscriptionContext ctx) {
    IfStatement ifStatement = (IfStatement) ctx.syntaxNode();
    if (ifStatement.isElif()
      || ifStatement.elseBranch() != null
      || !ifStatement.elifBranches().isEmpty()) {
      return;
    }
    if (!(ifStatement.condition() instanceof InExpression inExpression) || inExpression.notToken() != null) {
      return;
    }
    List<Statement> statements = ifStatement.body().statements();
    if (statements.size() != 1 || !(statements.get(0) instanceof ExpressionStatement expressionStatement)) {
      return;
    }
    if (expressionStatement.expressions().size() != 1
      || !(expressionStatement.expressions().get(0) instanceof CallExpression callExpression)) {
      return;
    }
    if (!(callExpression.callee() instanceof QualifiedExpression qualifiedExpression)
      || !REMOVE_METHOD_NAME.equals(qualifiedExpression.name().name())) {
      return;
    }
    List<Argument> arguments = callExpression.arguments();
    if (arguments.size() != 1 || !(arguments.get(0) instanceof RegularArgument regularArgument)
      || regularArgument.keywordArgument() != null) {
      return;
    }
    if (!CheckUtils.areEquivalent(qualifiedExpression.qualifier(), inExpression.rightOperand())
      || !CheckUtils.areEquivalent(regularArgument.expression(), inExpression.leftOperand())) {
      return;
    }
    if (!SET_TYPE_MATCHER.isTrueFor(qualifiedExpression.qualifier(), ctx)) {
      return;
    }
    var issue = ctx.addIssue(inExpression, MESSAGE);
    createQuickFix(ifStatement, qualifiedExpression, regularArgument).ifPresent(issue::addQuickFix);
  }

  private static Optional<PythonQuickFix> createQuickFix(IfStatement ifStatement, QualifiedExpression qualifiedExpression, RegularArgument argument) {
    String qualifierText = TreeUtils.treeToString(qualifiedExpression.qualifier(), false);
    String argumentText = TreeUtils.treeToString(argument.expression(), false);
    if (qualifierText == null || argumentText == null) {
      return Optional.empty();
    }
    String replacement = "%s.discard(%s)".formatted(qualifierText, argumentText);
    return Optional.of(PythonQuickFix.newQuickFix(QUICK_FIX_MESSAGE)
      .addTextEdit(TextEditUtils.replace(ifStatement, replacement))
      .build());
  }
}
