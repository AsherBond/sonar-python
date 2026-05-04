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
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.quickfix.PythonQuickFix;
import org.sonar.plugins.python.api.symbols.v2.SymbolV2;
import org.sonar.plugins.python.api.symbols.v2.UsageV2;
import org.sonar.plugins.python.api.tree.ArgList;
import org.sonar.plugins.python.api.tree.AssignmentStatement;
import org.sonar.plugins.python.api.tree.CallExpression;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.ForStatement;
import org.sonar.plugins.python.api.tree.Name;
import org.sonar.plugins.python.api.tree.QualifiedExpression;
import org.sonar.plugins.python.api.tree.RegularArgument;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.tree.YieldExpression;
import org.sonar.plugins.python.api.tree.YieldStatement;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatcher;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatchers;
import org.sonar.python.quickfix.TextEditUtils;
import org.sonar.python.tree.TreeUtils;

@Rule(key = "S8516")
public class GroupByIteratorReuseCheck extends PythonSubscriptionCheck {

  private static final String MESSAGE = "Consume this group iterator inside the loop, or materialize it into a collection.";
  private static final String QUICK_FIX_MESSAGE = "Wrap with \"list()\"";

  private static final TypeMatcher GROUPBY_MATCHER = TypeMatchers.isType("itertools.groupby");

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
    TypeMatchers.isType("all"),
    TypeMatchers.isType("next"),
    TypeMatchers.isType("len"),
    TypeMatchers.isType("str.join"),
    TypeMatchers.isType("bytes.join")
  );

  // Matches class objects produced at runtime via `type(...)` (e.g. `Cls = type(obj); Cls(group)`).
  // Direct class references (`MyClass`) are NOT matched here
  private static final TypeMatcher RUNTIME_CLASS_OBJECT_MATCHER = TypeMatchers.isObjectOfType("type");

  // Container methods that store their argument *as a single element* without iterating it.
  private static final Set<String> STORING_METHOD_NAMES = Set.of(
    "append", "add", "setdefault"
  );

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.FOR_STMT, GroupByIteratorReuseCheck::checkForStatement);
  }

  private static void checkForStatement(SubscriptionContext ctx) {
    ForStatement forStatement = (ForStatement) ctx.syntaxNode();
    Name groupName = extractGroupByLoopVariable(forStatement, ctx).orElse(null);
    if (groupName == null) {
      return;
    }
    SymbolV2 groupSymbol = groupName.symbolV2();
    if (groupSymbol == null) {
      return;
    }

    Tree loopBody = forStatement.body();

    // Bail on any rebinding of `group` in the body to avoid requiring a CFG
    boolean isReboundInLoopBody = namesInLoopBody(groupSymbol, loopBody,
      usage -> usage.kind() == UsageV2.Kind.ASSIGNMENT_LHS).findAny().isPresent();
    if (isReboundInLoopBody) {
      return;
    }

    List<Name> loopBodyReads = namesInLoopBody(groupSymbol, loopBody,
      usage -> !usage.isBindingUsage()).toList();

    List<Name> unsafeReads = loopBodyReads.stream()
      .filter(nameUsage -> isUnsafeRead(nameUsage, forStatement, ctx))
      .toList();

    // Quickfix only when there is a single read in the body: wrapping `group` in `list()`
    // consumes the iterator and would silently break any other read.
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

  // Matches `for key, group in groupby(...):` and returns the `group` name
  private static Optional<Name> extractGroupByLoopVariable(ForStatement forStatement, SubscriptionContext ctx) {
    if (forStatement.testExpressions().size() != 1
      || !(forStatement.testExpressions().get(0) instanceof CallExpression callExpr)
      || !GROUPBY_MATCHER.isTrueFor(callExpr.callee(), ctx)
      || forStatement.expressions().size() != 2
      || !(forStatement.expressions().get(1) instanceof Name groupName)) {
      return Optional.empty();
    }
    return Optional.of(groupName);
  }

  // Recognized escape sinks: lambda/nested-function capture, assignment rvalue, yield, and
  // positional argument of a known storing-method. Anything else is treated as safe.
  private static boolean isUnsafeRead(Name nameUsage, ForStatement enclosingForStatement, SubscriptionContext ctx) {
    if (isCapturedByNestedFunctionOrLambda(nameUsage, enclosingForStatement)) {
      return true;
    }
    return reachesSink(nameUsage, ctx);
  }

  private static boolean reachesSink(Expression expression, SubscriptionContext ctx) {
    Tree parent = expression.parent();
    if (parent instanceof AssignmentStatement assign && assign.assignedValue() == expression) {
      return true;
    }
    if (parent instanceof YieldExpression || parent instanceof YieldStatement) {
      return true;
    }
    // Keyword arguments are skipped (treated as safe): mapping them to the callee's parameter would
    // require signature resolution, and iterators are overwhelmingly passed positionally in practice.
    if (parent instanceof RegularArgument regularArg && regularArg.keywordArgument() == null) {
      return chainReachesSink(regularArg, ctx);
    }
    return false;
  }

  private static boolean chainReachesSink(RegularArgument arg, SubscriptionContext ctx) {
    CallExpression call = owningCall(arg).orElse(null);
    if (call == null) {
      return false;
    }
    if (isSafeConsumerCallee(call.callee(), ctx)) {
      return false;
    }
    if (isStoringMethodCall(call)) {
      return true;
    }
    return reachesSink(call, ctx);
  }

  private static boolean isSafeConsumerCallee(Expression callee, SubscriptionContext ctx) {
    return !SAFE_CONSUMER_MATCHER.evaluateFor(callee, ctx).isFalse()
      || !RUNTIME_CLASS_OBJECT_MATCHER.evaluateFor(callee, ctx).isFalse();
  }

  // Name-only on purpose: gating on the receiver type would silently miss the case where the
  // receiver's type cannot be resolved. Middle ground between FP risk and raising actual issues.  
  private static boolean isStoringMethodCall(CallExpression call) {
    return call.callee() instanceof QualifiedExpression qualified
      && STORING_METHOD_NAMES.contains(qualified.name().name());
  }

  private static boolean isCapturedByNestedFunctionOrLambda(Name nameUsage, ForStatement enclosingForStatement) {
    Tree functionLikeAncestor = TreeUtils.firstAncestorOfKind(nameUsage, Tree.Kind.FUNCDEF, Tree.Kind.LAMBDA);
    return functionLikeAncestor != null && isInside(functionLikeAncestor, enclosingForStatement.body());
  }

  private static Stream<Name> namesInLoopBody(SymbolV2 symbol, Tree loopBody, Predicate<UsageV2> usageFilter) {
    return symbol.usages().stream()
      .filter(usageFilter)
      .map(UsageV2::tree)
      .flatMap(TreeUtils.toStreamInstanceOfMapper(Name.class))
      .filter(name -> isInside(name, loopBody));
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
