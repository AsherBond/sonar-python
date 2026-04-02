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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.symbols.v2.SymbolV2;
import org.sonar.plugins.python.api.symbols.v2.UsageV2;
import org.sonar.plugins.python.api.tree.CallExpression;
import org.sonar.plugins.python.api.tree.Decorator;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.FunctionDef;
import org.sonar.plugins.python.api.tree.Name;
import org.sonar.plugins.python.api.tree.Parameter;
import org.sonar.plugins.python.api.tree.ParameterList;
import org.sonar.plugins.python.api.tree.QualifiedExpression;
import org.sonar.plugins.python.api.tree.RegularArgument;
import org.sonar.plugins.python.api.tree.SubscriptionExpression;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.tree.TypeAnnotation;
import org.sonar.plugins.python.api.types.v2.FunctionType;
import org.sonar.plugins.python.api.types.v2.ParameterV2;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatcher;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatchers;
import org.sonar.python.checks.utils.Expressions;
import org.sonar.python.checks.utils.FunctionParameterUtils;
import org.sonar.python.checks.utils.FunctionParameterUtils.FunctionParameterInfo;
import org.sonar.python.tree.TreeUtils;

@Rule(key = "S8411")
public class FastAPIPathParametersCheck extends PythonSubscriptionCheck {

  private static final String MISSING_PARAM_MESSAGE = "Add path parameter \"%s\" to the function signature.";
  private static final String POSITIONAL_ONLY_MESSAGE = "Path parameter \"%s\" should not be positional-only.";

  private static final List<String> HTTP_METHODS = List.of(
    "get", "post", "put", "delete", "patch", "options", "head", "trace");

  private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{([^}:]+)(?::[^}]*)?\\}");

  private static final TypeMatcher FASTAPI_ROUTE_MATCHER = TypeMatchers.any(
    HTTP_METHODS.stream()
      .flatMap(method -> Stream.of(
        TypeMatchers.isType("fastapi.FastAPI." + method),
        TypeMatchers.isType("fastapi.APIRouter." + method)))
  );

  private static final TypeMatcher FASTAPI_DEPENDS_MATCHER = TypeMatchers.isType("fastapi.param_functions.Depends");
  private static final TypeMatcher TYPING_ANNOTATED_MATCHER = TypeMatchers.isType("typing.Annotated");

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.FUNCDEF, FastAPIPathParametersCheck::checkFunction);
  }

  private static void checkFunction(SubscriptionContext ctx) {
    FunctionDef functionDef = (FunctionDef) ctx.syntaxNode();
    for (Decorator decorator : functionDef.decorators()) {
      checkDecorator(ctx, decorator, functionDef);
    }
  }

  private static void checkDecorator(SubscriptionContext ctx, Decorator decorator, FunctionDef functionDef) {
    Expression expr = decorator.expression();
    if (!(expr instanceof CallExpression callExpr)) {
      return;
    }

    if (!FASTAPI_ROUTE_MATCHER.isTrueFor(callExpr.callee(), ctx)) {
      return;
    }

    Set<String> pathParams = extractPathParameters(callExpr);
    if (pathParams.isEmpty()) {
      return;
    }

    FunctionParameterInfo paramInfo = FunctionParameterUtils.extractFunctionParameters(functionDef);
    pathParams.removeAll(extractDependencyParameters(functionDef, ctx));
    reportIssues(ctx, functionDef, pathParams, paramInfo);
  }

  private static Set<String> extractDependencyParameters(FunctionDef functionDef, SubscriptionContext ctx) {
    return extractDependencyParameters(functionDef, ctx, new HashSet<>());
  }

  private static Set<String> extractDependencyParameters(FunctionDef functionDef, SubscriptionContext ctx, Set<FunctionDef> visited) {
    if (!visited.add(functionDef)) {
      return Set.of();
    }
    ParameterList parameterList = functionDef.parameters();
    if (parameterList == null) {
      return Set.of();
    }
    Set<String> dependencyParams = new HashSet<>();
    parameterList.nonTuple().stream()
      .map(param -> getDependsCall(param, ctx))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .forEach(dependsCall -> collectDependencyParams(dependsCall, dependencyParams, visited, ctx));
    return dependencyParams;
  }

  private static Optional<CallExpression> getDependsCall(Parameter param, SubscriptionContext ctx) {
    Expression defaultValue = param.defaultValue();
    if (defaultValue instanceof CallExpression callExpr && FASTAPI_DEPENDS_MATCHER.isTrueFor(callExpr.callee(), ctx)) {
      return Optional.of(callExpr);
    }
    TypeAnnotation typeAnnotation = param.typeAnnotation();
    if (typeAnnotation != null && typeAnnotation.expression() instanceof SubscriptionExpression subscriptionExpr
      && TYPING_ANNOTATED_MATCHER.isTrueFor(subscriptionExpr.object(), ctx)) {
      return subscriptionExpr.subscripts().expressions().stream()
        .filter(e -> e instanceof CallExpression ce && FASTAPI_DEPENDS_MATCHER.isTrueFor(ce.callee(), ctx))
        .map(e -> (CallExpression) e)
        .findFirst();
    }
    return Optional.empty();
  }

  private static void collectDependencyParams(CallExpression dependsCall, Set<String> dependencyParams, Set<FunctionDef> visited, SubscriptionContext ctx) {
    TreeUtils.nthArgumentOrKeywordOptional(0, "dependency", dependsCall.arguments())
      .map(RegularArgument::expression)
      .ifPresent(argExpr -> {
        if (argExpr.typeV2() instanceof FunctionType funcType) {
          funcType.parameters().stream()
            .filter(param -> !param.isVariadic())
            .map(ParameterV2::name)
            .filter(Objects::nonNull)
            .forEach(dependencyParams::add);
        }
        getFunctionDef(argExpr).ifPresent(depFuncDef ->
          dependencyParams.addAll(extractDependencyParameters(depFuncDef, ctx, visited))
        );
      });
  }

  private static Optional<FunctionDef> getFunctionDef(Expression expression) {
    Name name;
    if (expression instanceof Name n) {
      name = n;
    } else if (expression instanceof QualifiedExpression qe) {
      name = qe.name();
    } else {
      name = null;
    }
    if (name == null) {
      return Optional.empty();
    }
    SymbolV2 symbol = name.symbolV2();
    if (symbol == null) {
      return Optional.empty();
    }
    return symbol.usages().stream()
      .filter(u -> u.kind() == UsageV2.Kind.FUNC_DECLARATION)
      .map(UsageV2::tree)
      .map(tree -> TreeUtils.firstAncestorOfKind(tree, Tree.Kind.FUNCDEF))
      .filter(Objects::nonNull)
      .map(FunctionDef.class::cast)
      .findFirst();
  }

  private static Set<String> extractPathParameters(CallExpression callExpr) {
    Set<String> pathParams = new HashSet<>();
    String pathString = getPathArgument(callExpr).orElse("");
    Matcher matcher = PATH_PARAM_PATTERN.matcher(pathString);
    while (matcher.find()) {
      pathParams.add(matcher.group(1));
    }
    return pathParams;
  }

  private static Optional<String> getPathArgument(CallExpression callExpr) {
    return TreeUtils.nthArgumentOrKeywordOptional(0, "path", callExpr.arguments())
      .flatMap(arg -> extractStringValue(arg.expression()));
  }

  private static Optional<String> extractStringValue(Expression expression) {
    return Optional.ofNullable(Expressions.extractStringLiteral(expression))
      .map(Expressions::unescape);
  }

  private static void reportIssues(SubscriptionContext ctx, FunctionDef functionDef, Set<String> pathParams, FunctionParameterInfo paramInfo) {
    pathParams.stream()
      .filter(paramInfo::isMissingFromSignature)
      .forEach(param -> ctx.addIssue(functionDef.name(), String.format(MISSING_PARAM_MESSAGE, param)));

    pathParams.stream()
      .filter(paramInfo.positionalOnlyParams()::contains)
      .forEach(param -> ctx.addIssue(functionDef.name(), String.format(POSITIONAL_ONLY_MESSAGE, param)));
  }
}
