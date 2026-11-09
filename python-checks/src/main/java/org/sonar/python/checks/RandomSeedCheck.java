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

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.tree.CallExpression;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.Name;
import org.sonar.plugins.python.api.tree.RegularArgument;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.tree.StringLiteral;
import org.sonar.plugins.python.api.types.v2.ClassType;
import org.sonar.plugins.python.api.types.v2.FunctionType;
import org.sonar.plugins.python.api.types.v2.FullyQualifiedNameHelper;
import org.sonar.plugins.python.api.types.v2.PythonType;
import org.sonar.python.checks.cdk.CdkPredicate;
import org.sonar.python.checks.utils.ReachingValues;
import org.sonar.python.tree.TreeUtils;

@Rule(key = "S6709")
public class RandomSeedCheck extends PythonSubscriptionCheck {

  private static final String NUMPY_SEED_ARG_NAME = "seed";

  private static final Map<String, String> SEED_METHODS_TO_CHECK = Map.of(
    "numpy.seed", NUMPY_SEED_ARG_NAME,
    "numpy.random.seed", NUMPY_SEED_ARG_NAME,
    "numpy.random.default_rng", NUMPY_SEED_ARG_NAME,
    "numpy.random.SeedSequence", "entropy",
    "numpy.random.PCG64", NUMPY_SEED_ARG_NAME,
    "numpy.random.PCG64DXSM", NUMPY_SEED_ARG_NAME,
    "numpy.random.MT19937", NUMPY_SEED_ARG_NAME,
    "numpy.random.SFC64", NUMPY_SEED_ARG_NAME,
    "numpy.random.Philox", NUMPY_SEED_ARG_NAME);

  private static final String SKLEARN_FQN = "sklearn";
  private static final String SKLEARN_ARG_NAME = "random_state";

  private static final String MESSAGE = "Provide a seed for this random generator.";
  private static final String SKLEARN_MESSAGE = "Provide a seed for the random_state parameter.";

  private static BiPredicate<CallExpression, SubscriptionContext> keywordAbsentOrNotIn(String keyword, String... restrictedValues) {
    Set<String> restrictedValueSet = Set.of(restrictedValues);
    return (call, ctx) -> {
      var arg = TreeUtils.argumentByKeyword(keyword, call.arguments());
      if (arg == null) {
        return true;
      }
      String expressionString = singleStringValue(arg.expression(), ctx);
      return restrictedValueSet.stream().noneMatch(expressionString::equals);
    };
  }
  private static BiPredicate<CallExpression, SubscriptionContext> probabilityArgAbsent() {
    return (call, ctx) -> {
      var probabilityArg = TreeUtils.argumentByKeyword("probability", call.arguments());
      return probabilityArg == null || CdkPredicate.isFalse().test(probabilityArg.expression());
    };
  }

  private static final BiPredicate<CallExpression, SubscriptionContext> SOLVER_NOT_SAG_SAGA = keywordAbsentOrNotIn("solver", "sag", "saga");
  private static final BiPredicate<CallExpression, SubscriptionContext> SELECTION_NOT_RANDOM = keywordAbsentOrNotIn("selection", "random");

  private static final Map<String, BiPredicate<CallExpression, SubscriptionContext>> SKLEARN_EXCEPTIONS = Map.ofEntries(
    Map.entry("sklearn.svm._classes.SVC", probabilityArgAbsent()),
    Map.entry("sklearn.linear_model._logistic.LogisticRegression", SOLVER_NOT_SAG_SAGA),
    Map.entry("sklearn.linear_model._ridge.Ridge", SOLVER_NOT_SAG_SAGA),
    Map.entry("sklearn.linear_model._coordinate_descent.Lasso", SELECTION_NOT_RANDOM),
    Map.entry("sklearn.linear_model._coordinate_descent.ElasticNet", SELECTION_NOT_RANDOM));

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.CALL_EXPR, RandomSeedCheck::checkEmptySeedCall);
  }

  private static void checkEmptySeedCall(SubscriptionContext ctx) {
    CallExpression call = (CallExpression) ctx.syntaxNode();
    PythonType calleeType = call.callee().typeV2();
    // NumPy stubs are excluded from serialization. The V2 helper also supports unresolved imports.
    Optional<String> calleeName = FullyQualifiedNameHelper.getFullyQualifiedName(calleeType);
    calleeName
      .map(SEED_METHODS_TO_CHECK::get)
      .filter(argName -> isArgumentAbsentOrNone(TreeUtils.nthArgumentOrKeyword(0, argName, call.arguments()), ctx))
      .map(arg -> MESSAGE)
      .or(() -> calleeName
        .filter(name -> name.startsWith(SKLEARN_FQN))
        .filter(name -> !(calleeType instanceof FunctionType function
          && function.isInstanceMethod() && "__init__".equals(function.name())))
        .filter(name -> isArgumentAbsentOrNone(TreeUtils.argumentByKeyword(SKLEARN_ARG_NAME, call.arguments()), ctx))
        .filter(name -> hasRandomStateParameter(calleeType))
        .filter(name -> !isSKLearnException(name, call, ctx))
        .map(name -> SKLEARN_MESSAGE))
      .ifPresent(message -> ctx.addIssue(call.callee(), message));
  }

  private static boolean hasRandomStateParameter(PythonType type) {
    if (type instanceof ClassType classType) {
      var constructors = classType.members().stream().filter(member -> "__init__".equals(member.name())).toList();
      return constructors.size() == 1 && hasRandomStateParameter(constructors.get(0).type());
    }
    return type instanceof FunctionType function && function.parameters().stream()
      .anyMatch(parameter -> SKLEARN_ARG_NAME.equals(parameter.name()));
  }

  private static String singleStringValue(Expression expression, SubscriptionContext ctx) {
    Set<Expression> values = ReachingValues.resolvedValues(expression, ctx);
    if (values.size() != 1) {
      return "";
    }
    Expression value = values.iterator().next();
    if (value instanceof StringLiteral literal) {
      return literal.trimmedQuotesValue();
    }
    return "";
  }

  private static boolean isArgumentAbsentOrNone(@Nullable RegularArgument arg, SubscriptionContext ctx) {
    return arg == null || arg.expression().is(Tree.Kind.NONE) || isAssignedNone(arg.expression(), ctx);
  }

  private static boolean isAssignedNone(Expression exp, SubscriptionContext ctx) {
    return Optional.of(exp)
      .flatMap(TreeUtils.toOptionalInstanceOfMapper(Name.class))
      .map(ctx::valuesAtLocation)
      .filter(Predicate.not(Set::isEmpty))
      .filter(values -> values.stream().allMatch(value -> value.is(Tree.Kind.NONE))).isPresent();
  }

  private static boolean isSKLearnException(String calleeName, CallExpression call, SubscriptionContext ctx) {
    var exception = SKLEARN_EXCEPTIONS.get(calleeName);
    return exception != null && exception.test(call, ctx);
  }
}
