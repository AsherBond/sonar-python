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

import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.tree.CallExpression;
import org.sonar.plugins.python.api.tree.ListLiteral;
import org.sonar.plugins.python.api.tree.RegularArgument;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatcher;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatchers;
import org.sonar.python.tree.TreeUtils;

@Rule(key = "S8520")
public class SumListConcatenationCheck extends PythonSubscriptionCheck {

  private static final String MESSAGE = "Use \"itertools.chain.from_iterable()\" instead of \"sum()\" to flatten or concatenate lists.";
  private static final TypeMatcher SUM_MATCHER = TypeMatchers.isType("sum");

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.CALL_EXPR, SumListConcatenationCheck::checkCall);
  }

  private static void checkCall(SubscriptionContext ctx) {
    CallExpression call = (CallExpression) ctx.syntaxNode();
    if (!SUM_MATCHER.isTrueFor(call.callee(), ctx)) {
      return;
    }
    RegularArgument startArgument = TreeUtils.nthArgumentOrKeyword(1, "start", call.arguments());
    if (isEmptyListLiteral(startArgument)) {
      ctx.addIssue(call, MESSAGE);
    }
  }

  private static boolean isEmptyListLiteral(@Nullable RegularArgument argument) {
    if (argument == null) {
      return false;
    }
    if (!argument.expression().is(Tree.Kind.LIST_LITERAL)) {
      return false;
    }
    ListLiteral listLiteral = (ListLiteral) argument.expression();
    return listLiteral.elements().expressions().isEmpty();
  }
}
