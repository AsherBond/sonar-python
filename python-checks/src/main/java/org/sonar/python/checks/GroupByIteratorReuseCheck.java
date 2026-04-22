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
import org.sonar.plugins.python.api.symbols.v2.SymbolV2;
import org.sonar.plugins.python.api.symbols.v2.UsageV2;
import org.sonar.plugins.python.api.tree.ArgList;
import org.sonar.plugins.python.api.tree.CallExpression;
import org.sonar.plugins.python.api.tree.ComprehensionFor;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.ForStatement;
import org.sonar.plugins.python.api.tree.Name;
import org.sonar.plugins.python.api.tree.RegularArgument;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatcher;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatchers;
import org.sonar.python.quickfix.TextEditUtils;
import org.sonar.python.tree.TreeUtils;

@Rule(key = "S8516")
public class GroupByIteratorReuseCheck extends PythonSubscriptionCheck {

  private static final String MESSAGE = "Convert this group iterator to a list.";
  private static final String QUICK_FIX_MESSAGE = "Wrap with \"list()\"";

  private static final TypeMatcher GROUPBY_MATCHER = TypeMatchers.isType("itertools.groupby");

  // SAFE_CONSUMER_MATCHER and CLASS_MATCHER are matched leniently (TRUE or UNKNOWN both pass) so
  // unresolved callees don't trigger false positives.
  private static final TypeMatcher SAFE_CONSUMER_MATCHER = TypeMatchers.any(
    TypeMatchers.isType("list"),
    TypeMatchers.isType("tuple"),
    TypeMatchers.isType("set"),
    TypeMatchers.isType("frozenset"),
    TypeMatchers.isType("sorted"),
    TypeMatchers.isType("sum"),
    TypeMatchers.isType("max"),
    TypeMatchers.isType("min"),
    TypeMatchers.isType("any"),
    TypeMatchers.isType("all")
  );

  // Any class constructor that accepts an iterable invariably materializes it inside __init__
  private static final TypeMatcher CLASS_MATCHER = TypeMatchers.isObjectOfType("type");

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.FOR_STMT, GroupByIteratorReuseCheck::checkForStatement);
  }

  private static void checkForStatement(SubscriptionContext ctx) {
    ForStatement forStatement = (ForStatement) ctx.syntaxNode();

    if (forStatement.testExpressions().size() != 1) {
      return;
    }

    if (!(forStatement.testExpressions().get(0) instanceof CallExpression callExpr)) {
      return;
    }

    if (!GROUPBY_MATCHER.isTrueFor(callExpr.callee(), ctx)) {
      return;
    }

    if (forStatement.expressions().size() != 2) {
      return;
    }

    if (!(forStatement.expressions().get(1) instanceof Name groupName)) {
      return;
    }

    SymbolV2 groupSymbol = groupName.symbolV2();
    if (groupSymbol == null) {
      return;
    }

    Tree loopBody = forStatement.body();

    // If `group` is rebound anywhere in the loop body (e.g. `group = list(group)`), we can't
    // tell from the AST alone which reads see the original iterator. We conservatively skip.
    boolean isReboundInLoopBody = groupSymbol.usages().stream()
      .filter(usage -> usage.kind() == UsageV2.Kind.ASSIGNMENT_LHS)
      .map(UsageV2::tree)
      .flatMap(TreeUtils.toStreamInstanceOfMapper(Name.class))
      .anyMatch(reboundName -> isInside(reboundName, loopBody));
    if (isReboundInLoopBody) {
      return;
    }

    List<Name> loopBodyReads = groupSymbol.usages().stream()
      .filter(usage -> !usage.isBindingUsage())
      .map(UsageV2::tree)
      .flatMap(TreeUtils.toStreamInstanceOfMapper(Name.class))
      .filter(nameUsage -> isInside(nameUsage, loopBody))
      .toList();

    List<Name> unsafeReads = loopBodyReads.stream()
      .filter(nameUsage -> !isSafeUsage(nameUsage, forStatement, ctx))
      .toList();

    // The quickfix wraps a single occurrence in `list(...)`. We only attach it when there is
    // exactly one read of `group` in the loop body — this does not affecting any other consumer.
    boolean canOfferQuickFix = loopBodyReads.size() == 1 && unsafeReads.size() == 1;
    for (Name nameUsage : unsafeReads) {
      var issue = ctx.addIssue(nameUsage, MESSAGE);
      if (canOfferQuickFix) {
        PythonQuickFix quickFix = PythonQuickFix.newQuickFix(QUICK_FIX_MESSAGE)
          .addTextEdit(TextEditUtils.insertBefore(nameUsage, "list("))
          .addTextEdit(TextEditUtils.insertAfter(nameUsage, ")"))
          .build();
        issue.addQuickFix(quickFix);
      }
    }
  }

  private static boolean isSafeUsage(Name nameUsage, ForStatement enclosingForStatement, SubscriptionContext ctx) {
    // A usage inside a nested function or lambda defined in the loop body is always unsafe
    if (isInsideNestedFunctionOrLambda(nameUsage, enclosingForStatement)) {
      return false;
    }

    if (nameUsage.parent() instanceof ComprehensionFor compFor && compFor.iterable() == nameUsage) {
      return true;
    }

    if (nameUsage.parent() instanceof ForStatement nestedFor
      && nestedFor.testExpressions().stream().anyMatch(e -> e == nameUsage)) {
      return true;
    }

    if (nameUsage.parent() instanceof RegularArgument regularArg && regularArg.keywordArgument() == null) {
      return hasSafeConsumerAncestor(regularArg, ctx);
    }

    return false;
  }

  // We raise when the group iterator escapes the current iteration and is read after the outer
  // `groupby` advances. A positional-arg call chain ending in a safe consumer cannot escape.
  private static boolean hasSafeConsumerAncestor(RegularArgument regularArg, SubscriptionContext ctx) {
    Optional<CallExpression> currentCall = owningCall(regularArg);
    while (currentCall.isPresent()) {
      CallExpression call = currentCall.get();
      if (isSafeConsumerCallee(call.callee(), ctx)) {
        return true;
      }
      if (!(call.parent() instanceof RegularArgument outerArg) || outerArg.keywordArgument() != null) {
        return false;
      }
      currentCall = owningCall(outerArg);
    }
    return false;
  }

  private static boolean isSafeConsumerCallee(Expression callee, SubscriptionContext ctx) {
    return !SAFE_CONSUMER_MATCHER.evaluateFor(callee, ctx).isFalse()
      || !CLASS_MATCHER.evaluateFor(callee, ctx).isFalse();
  }

  private static boolean isInsideNestedFunctionOrLambda(Name nameUsage, ForStatement enclosingForStatement) {
    Tree functionLikeAncestor = TreeUtils.firstAncestorOfKind(nameUsage, Tree.Kind.FUNCDEF, Tree.Kind.LAMBDA);
    return functionLikeAncestor != null
      && isInside(functionLikeAncestor, enclosingForStatement.body());
  }

  private static boolean isInside(Tree tree, Tree container) {
    return TreeUtils.firstAncestor(tree, ancestor -> ancestor == container) != null;
  }

  private static Optional<CallExpression> owningCall(RegularArgument regularArg) {
    if (regularArg.parent() instanceof ArgList argList && argList.parent() instanceof CallExpression callExpr) {
      return Optional.of(callExpr);
    }
    return Optional.empty();
  }
}
