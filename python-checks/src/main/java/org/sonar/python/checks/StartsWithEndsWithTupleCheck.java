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
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.TriBool;
import org.sonar.plugins.python.api.quickfix.PythonQuickFix;
import org.sonar.plugins.python.api.tree.Argument;
import org.sonar.plugins.python.api.tree.BinaryExpression;
import org.sonar.plugins.python.api.tree.CallExpression;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.QualifiedExpression;
import org.sonar.plugins.python.api.tree.RegularArgument;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatcher;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatchers;
import org.sonar.python.checks.utils.CheckUtils;
import org.sonar.python.checks.utils.Expressions;
import org.sonar.python.quickfix.TextEditUtils;
import org.sonar.python.tree.TreeUtils;

@Rule(key = "S8513")
public class StartsWithEndsWithTupleCheck extends PythonSubscriptionCheck {

  private static final String STARTSWITH = "startswith";
  private static final String ENDSWITH = "endswith";

  private static final TypeMatcher STR_BYTES_OR_BYTEARRAY_METHOD_MATCHER = TypeMatchers.isFunctionOwnerSatisfying(
    TypeMatchers.any(
      TypeMatchers.isOrExtendsType("builtins.str"),
      TypeMatchers.isOrExtendsType("builtins.bytes"),
      TypeMatchers.isOrExtendsType("builtins.bytearray")));

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.OR, StartsWithEndsWithTupleCheck::checkOrExpression);
  }

  private static void checkOrExpression(SubscriptionContext ctx) {
    BinaryExpression orExpr = (BinaryExpression) ctx.syntaxNode();
    Tree ancestor = orExpr.parent();
    while (ancestor instanceof Expression expr && Expressions.removeParentheses(expr) != expr) {
      ancestor = ancestor.parent();
    }
    if (ancestor != null && ancestor.is(Tree.Kind.OR)) {
      return;
    }

    List<Expression> operands = flattenOrChain(orExpr);
    if (operands.size() < 2) {
      return;
    }

    Optional<List<CallExpression>> maybeCalls = extractCalls(operands, ctx);
    if (maybeCalls.isEmpty()) {
      return;
    }
    List<CallExpression> calls = maybeCalls.get();

    String methodName = extractUniformMethodName(calls);
    if (methodName == null) {
      return;
    }

    if (!allSameReceiver(calls)) {
      return;
    }

    if (anyReceiverHasNestedCall(calls)) {
      return;
    }

    Optional<List<String>> maybeArgTexts = extractStringLiteralArgs(calls);
    if (maybeArgTexts.isEmpty()) {
      return;
    }
    List<String> argTexts = maybeArgTexts.get();

    String message = String.format("Replace chained \"%s\" calls with a single call using a tuple argument.", methodName);
    String receiverText = receiverSourceText(calls.get(0));
    if (receiverText == null) {
      return;
    }
    String replacement = receiverText + "." + methodName + "((" + String.join(", ", argTexts) + "))";
    PythonQuickFix quickFix = PythonQuickFix.newQuickFix("Replace with a single call using a tuple argument",
      TextEditUtils.replace(orExpr, replacement));
    ctx.addIssue(orExpr, message).addQuickFix(quickFix);
  }

  private static List<Expression> flattenOrChain(BinaryExpression orExpr) {
    List<Expression> result = new ArrayList<>();
    flattenOrChainInto(orExpr, result);
    return result;
  }

  private static void flattenOrChainInto(Expression expr, List<Expression> result) {
    Expression stripped = Expressions.removeParentheses(expr);
    if (stripped.is(Tree.Kind.OR)) {
      BinaryExpression binExpr = (BinaryExpression) stripped;
      flattenOrChainInto(binExpr.leftOperand(), result);
      flattenOrChainInto(binExpr.rightOperand(), result);
    } else {
      result.add(stripped);
    }
  }

  private static Optional<List<CallExpression>> extractCalls(List<Expression> operands, SubscriptionContext ctx) {
    List<CallExpression> calls = new ArrayList<>();
    for (Expression operand : operands) {
      Expression stripped = Expressions.removeParentheses(operand);
      if (!stripped.is(Tree.Kind.CALL_EXPR)) {
        return Optional.empty();
      }
      CallExpression call = (CallExpression) stripped;
      if (!isStartsWithOrEndsWith(call, ctx)) {
        return Optional.empty();
      }
      calls.add(call);
    }
    return Optional.of(calls);
  }

  private static boolean isStartsWithOrEndsWith(CallExpression call, SubscriptionContext ctx) {
    if (!call.callee().is(Tree.Kind.QUALIFIED_EXPR)) {
      return false;
    }
    QualifiedExpression callee = (QualifiedExpression) call.callee();
    String name = callee.name().name();
    if (!STARTSWITH.equals(name) && !ENDSWITH.equals(name)) {
      return false;
    }
    return STR_BYTES_OR_BYTEARRAY_METHOD_MATCHER.evaluateFor(callee, ctx) != TriBool.FALSE;
  }

  private static String extractUniformMethodName(List<CallExpression> calls) {
    String methodName = null;
    for (CallExpression call : calls) {
      QualifiedExpression callee = (QualifiedExpression) call.callee();
      String name = callee.name().name();
      if (methodName == null) {
        methodName = name;
      } else if (!methodName.equals(name)) {
        return null;
      }
    }
    return methodName;
  }

  private static boolean allSameReceiver(List<CallExpression> calls) {
    Expression firstReceiver = getReceiver(calls.get(0));
    for (int i = 1; i < calls.size(); i++) {
      Expression receiver = getReceiver(calls.get(i));
      if (!CheckUtils.areEquivalent(firstReceiver, receiver)) {
        return false;
      }
    }
    return true;
  }

  private static Expression getReceiver(CallExpression call) {
    return ((QualifiedExpression) call.callee()).qualifier();
  }

  private static boolean anyReceiverHasNestedCall(List<CallExpression> calls) {
    for (CallExpression call : calls) {
      Expression receiver = getReceiver(call);
      if (receiver.is(Tree.Kind.CALL_EXPR) || TreeUtils.hasDescendant(receiver, t -> t.is(Tree.Kind.CALL_EXPR))) {
        return true;
      }
    }
    return false;
  }

  private static Optional<List<String>> extractStringLiteralArgs(List<CallExpression> calls) {
    List<String> argTexts = new ArrayList<>();
    for (CallExpression call : calls) {
      List<Argument> args = call.arguments();
      if (args.size() != 1) {
        return Optional.empty();
      }
      Argument arg = args.get(0);
      if (!(arg instanceof RegularArgument regularArg)) {
        return Optional.empty();
      }
      if (regularArg.keywordArgument() != null) {
        return Optional.empty();
      }
      Expression argExpr = regularArg.expression();
      if (!argExpr.is(Tree.Kind.STRING_LITERAL)) {
        return Optional.empty();
      }
      String argSource = TreeUtils.treeToString(argExpr, false);
      if (argSource == null) {
        return Optional.empty();
      }
      argTexts.add(argSource);
    }
    return Optional.of(argTexts);
  }

  private static String receiverSourceText(CallExpression call) {
    Expression receiver = getReceiver(call);
    return TreeUtils.treeToString(receiver, false);
  }
}
