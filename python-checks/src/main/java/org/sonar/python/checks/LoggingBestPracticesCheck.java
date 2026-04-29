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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.tree.BinaryExpression;
import org.sonar.plugins.python.api.tree.CallExpression;
import org.sonar.plugins.python.api.tree.DictionaryLiteral;
import org.sonar.plugins.python.api.tree.DictionaryLiteralElement;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.KeyValuePair;
import org.sonar.plugins.python.api.tree.RegularArgument;
import org.sonar.plugins.python.api.tree.StringLiteral;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatcher;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatchers;
import org.sonar.python.tree.TreeUtils;

@Rule(key = "S8554")
public class LoggingBestPracticesCheck extends PythonSubscriptionCheck {

  private static final TypeMatcher LOGGING_CALL_MATCHER = TypeMatchers.any(
    TypeMatchers.isType("logging.debug"),
    TypeMatchers.isType("logging.info"),
    TypeMatchers.isType("logging.warning"),
    TypeMatchers.isType("logging.warn"),
    TypeMatchers.isType("logging.error"),
    TypeMatchers.isType("logging.exception"),
    TypeMatchers.isType("logging.critical"),
    TypeMatchers.isType("logging.Logger.debug"),
    TypeMatchers.isType("logging.Logger.info"),
    TypeMatchers.isType("logging.Logger.warning"),
    TypeMatchers.isType("logging.Logger.warn"),
    TypeMatchers.isType("logging.Logger.error"),
    TypeMatchers.isType("logging.Logger.exception"),
    TypeMatchers.isType("logging.Logger.critical")
  );

  private static final TypeMatcher DEPRECATED_WARN_MATCHER = TypeMatchers.any(
    TypeMatchers.isType("logging.warn"),
    TypeMatchers.isType("logging.Logger.warn")
  );

  private static final TypeMatcher STR_FORMAT_MATCHER = TypeMatchers.isType("str.format");

  private static final Set<String> LOG_RECORD_ATTRIBUTES = new HashSet<>(Arrays.asList(
    "name", "msg", "args", "created", "filename", "funcName", "levelname", "levelno",
    "lineno", "module", "msecs", "message", "pathname", "process", "processName",
    "relativeCreated", "thread", "threadName", "exc_info", "exc_text", "stack_info",
    "taskName", "asctime"
  ));

  private static final String EAGER_FORMAT_MESSAGE =
    "Pass formatting arguments to the logging call instead of pre-formatting the message string.";
  private static final String DEPRECATED_WARN_MESSAGE =
    "Use \"warning\" instead of the deprecated \"warn\" method.";
  private static final String EXTRA_COLLISION_MESSAGE =
    "Remove or rename this key; it overrides a built-in LogRecord attribute.";

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.CALL_EXPR, LoggingBestPracticesCheck::checkLoggingCall);
  }

  private static void checkLoggingCall(SubscriptionContext ctx) {
    CallExpression call = (CallExpression) ctx.syntaxNode();
    if (!LOGGING_CALL_MATCHER.isTrueFor(call.callee(), ctx)) {
      return;
    }

    if (DEPRECATED_WARN_MATCHER.isTrueFor(call.callee(), ctx)) {
      ctx.addIssue(call.callee(), DEPRECATED_WARN_MESSAGE);
    }

    List<RegularArgument> positionalArgs = call.arguments().stream()
      .flatMap(TreeUtils.toStreamInstanceOfMapper(RegularArgument.class))
      .filter(arg -> arg.keywordArgument() == null)
      .toList();

    if (!positionalArgs.isEmpty()) {
      checkEagerFormatting(ctx, positionalArgs.get(0).expression());
    }

    checkExtraAttributeCollision(ctx, call);
  }

  private static void checkEagerFormatting(SubscriptionContext ctx, Expression expr) {
    if (expr instanceof StringLiteral literal) {
      boolean isFString = literal.stringElements().stream()
        .anyMatch(e -> e.prefix().toLowerCase(Locale.ENGLISH).contains("f") && !e.formattedExpressions().isEmpty());
      if (isFString) {
        ctx.addIssue(expr, EAGER_FORMAT_MESSAGE);
      }
    } else if (expr instanceof CallExpression innerCall && STR_FORMAT_MATCHER.isTrueFor(innerCall.callee(), ctx)) {
      ctx.addIssue(expr, EAGER_FORMAT_MESSAGE);
    } else if (expr.is(Tree.Kind.MODULO) && containsStringLiteral(((BinaryExpression) expr).leftOperand())) {
      ctx.addIssue(expr, EAGER_FORMAT_MESSAGE);
    } else if (expr.is(Tree.Kind.PLUS) && containsStringLiteral(expr)) {
      ctx.addIssue(expr, EAGER_FORMAT_MESSAGE);
    }
  }

  private static boolean containsStringLiteral(Expression expr) {
    if (expr instanceof StringLiteral) {
      return true;
    }
    if (expr.is(Tree.Kind.PLUS)) {
      BinaryExpression plus = (BinaryExpression) expr;
      return containsStringLiteral(plus.leftOperand()) || containsStringLiteral(plus.rightOperand());
    }
    return false;
  }

  private static void checkExtraAttributeCollision(SubscriptionContext ctx, CallExpression call) {
    RegularArgument extraArg = TreeUtils.argumentByKeyword("extra", call.arguments());
    if (extraArg == null || !(extraArg.expression() instanceof DictionaryLiteral dict)) {
      return;
    }
    for (DictionaryLiteralElement element : dict.elements()) {
      if (element instanceof KeyValuePair kvp
        && kvp.key() instanceof StringLiteral key
        && LOG_RECORD_ATTRIBUTES.contains(key.trimmedQuotesValue())) {
        ctx.addIssue(key, EXTRA_COLLISION_MESSAGE);
      }
    }
  }
}
