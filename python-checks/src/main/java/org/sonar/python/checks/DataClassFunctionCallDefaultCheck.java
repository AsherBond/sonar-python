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

import java.util.Set;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.tree.AnnotatedAssignment;
import org.sonar.plugins.python.api.tree.CallExpression;
import org.sonar.plugins.python.api.tree.ClassDef;
import org.sonar.plugins.python.api.tree.Decorator;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.QualifiedExpression;
import org.sonar.plugins.python.api.tree.RegularArgument;
import org.sonar.plugins.python.api.tree.SubscriptionExpression;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.tree.TypeAnnotation;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatcher;
import org.sonar.plugins.python.api.types.v2.matchers.TypeMatchers;
import org.sonar.python.tree.TreeUtils;

@Rule(key = "S8685")
public class DataClassFunctionCallDefaultCheck extends PythonSubscriptionCheck {

  private static final String MESSAGE = "Use \"field(default_factory=...)\" instead of a function call as a default value.";
  private static final String DEFAULT_KEYWORD = "default";

  private static final TypeMatcher IS_DATACLASS_DECORATOR = TypeMatchers.isType("dataclasses.dataclass");
  private static final TypeMatcher IS_DATACLASSES_FIELD = TypeMatchers.isType("dataclasses.field");
  private static final TypeMatcher IS_CLASS_VAR = TypeMatchers.isType("typing.ClassVar");

  // Allowlist of callables whose result should be re-evaluated per dataclass instance.
  // Calls to anything outside this list are assumed safe (e.g. user-defined helpers, frozen-dataclass
  // constructors, wrappers around dataclasses.field(default_factory=...)) — we prefer FNs over FPs here.
  //
  // NOTE: random.* top-level functions are intentionally absent. In typeshed they are aliased from a
  // module-level Random() instance (`randint = _inst.randint`, ...) and the V2 typeshed serializer
  // flattens the bound-method type to CallableType[builtins.function], so isType("random.randint")
  // resolves through Unknown. They are matched separately below via the random module qualifier.
  private static final TypeMatcher IS_PROBLEMATIC_FACTORY = TypeMatchers.any(Stream.of(
    // Current time / clock readings
    "datetime.datetime.now",
    "datetime.datetime.utcnow",
    "datetime.datetime.today",
    "datetime.datetime.fromtimestamp",
    "datetime.datetime.utcfromtimestamp",
    "datetime.date.today",
    "datetime.date.fromtimestamp",
    "time.time",
    "time.time_ns",
    "time.monotonic",
    "time.monotonic_ns",
    "time.perf_counter",
    "time.perf_counter_ns",
    "time.process_time",
    "time.process_time_ns",
    "time.localtime",
    "time.gmtime",
    // UUIDs
    "uuid.uuid1",
    "uuid.uuid3",
    "uuid.uuid4",
    "uuid.uuid5",
    // Secrets
    "secrets.token_hex",
    "secrets.token_bytes",
    "secrets.token_urlsafe",
    "secrets.choice",
    "secrets.randbelow",
    "secrets.randbits",
    // OS randomness
    "os.urandom",
    // Mutable container constructors — shared across all instances otherwise
    "builtins.list",
    "builtins.dict",
    "builtins.set",
    "builtins.bytearray",
    "collections.defaultdict",
    "collections.OrderedDict",
    "collections.Counter",
    "collections.deque").map(TypeMatchers::isType));

  // The random module type itself is known, even though its top-level function members all resolve
  // to Unknown (see NOTE on IS_PROBLEMATIC_FACTORY). We match `<random>.<name>` syntactically.
  private static final TypeMatcher IS_RANDOM_MODULE = TypeMatchers.isType("random");

  private static final Set<String> RANDOM_PROBLEMATIC_FUNCTION_NAMES = Set.of(
    "random",
    "randint",
    "randrange",
    "choice",
    "choices",
    "sample",
    "uniform",
    "gauss",
    "normalvariate",
    "triangular",
    "betavariate",
    "expovariate",
    "gammavariate",
    "lognormvariate",
    "paretovariate",
    "vonmisesvariate",
    "weibullvariate",
    "getrandbits",
    "randbytes");

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.CLASSDEF, DataClassFunctionCallDefaultCheck::checkClassDef);
  }

  private static void checkClassDef(SubscriptionContext ctx) {
    ClassDef classDef = (ClassDef) ctx.syntaxNode();
    if (!isDataclass(classDef, ctx)) {
      return;
    }
    classDef.body().statements().stream()
      .flatMap(TreeUtils.toStreamInstanceOfMapper(AnnotatedAssignment.class))
      .forEach(annotatedAssignment -> checkField(ctx, annotatedAssignment));
  }

  private static boolean isDataclass(ClassDef classDef, SubscriptionContext ctx) {
    for (Decorator decorator : classDef.decorators()) {
      Expression decoratorExpr = getDecoratorFunctionExpression(decorator);
      if (IS_DATACLASS_DECORATOR.isTrueFor(decoratorExpr, ctx)) {
        return true;
      }
    }
    return false;
  }

  private static Expression getDecoratorFunctionExpression(Decorator decorator) {
    Expression expr = decorator.expression();
    if (expr instanceof CallExpression callExpr) {
      return callExpr.callee();
    }
    return expr;
  }

  private static void checkField(SubscriptionContext ctx, AnnotatedAssignment annotatedAssignment) {
    Expression assignedValue = annotatedAssignment.assignedValue();
    if (assignedValue == null || isClassVar(annotatedAssignment.annotation(), ctx)) {
      return;
    }
    if (isMutableLiteral(assignedValue)) {
      ctx.addIssue(assignedValue, MESSAGE);
      return;
    }
    if (assignedValue instanceof CallExpression callExpression) {
      Tree problematic = problematicCall(callExpression, ctx);
      if (problematic != null) {
        ctx.addIssue(problematic, MESSAGE);
      }
    }
  }

  private static boolean isMutableLiteral(Expression expression) {
    return expression.is(Tree.Kind.LIST_LITERAL, Tree.Kind.DICTIONARY_LITERAL, Tree.Kind.SET_LITERAL);
  }

  // Returns the expression to flag, or null. Handles all of:
  //   x: T = datetime.now()                       -> the outer call
  //   x: T = field(default=datetime.now())        -> the inner default argument
  //   x: T = field(default=[])                    -> the inner mutable literal
  private static Tree problematicCall(CallExpression callExpression, SubscriptionContext ctx) {
    if (isProblematicFactoryCall(callExpression, ctx)) {
      return callExpression;
    }
    if (IS_DATACLASSES_FIELD.isTrueFor(callExpression.callee(), ctx)) {
      RegularArgument defaultArg = TreeUtils.argumentByKeyword(DEFAULT_KEYWORD, callExpression.arguments());
      if (defaultArg != null) {
        Expression defaultExpr = defaultArg.expression();
        if (isMutableLiteral(defaultExpr)) {
          return defaultExpr;
        }
        if (defaultExpr instanceof CallExpression innerCall && isProblematicFactoryCall(innerCall, ctx)) {
          return innerCall;
        }
      }
    }
    return null;
  }

  private static boolean isProblematicFactoryCall(CallExpression callExpression, SubscriptionContext ctx) {
    Expression callee = callExpression.callee();
    return IS_PROBLEMATIC_FACTORY.isTrueFor(callee, ctx) || isRandomModuleFunctionCall(callee, ctx);
  }

  // Workaround for the random.* alias gap in the V2 typeshed serializer (see NOTE on
  // IS_PROBLEMATIC_FACTORY): the random module type itself is known, so we recognise
  // a problematic call by checking the qualifier's type and the syntactic name.
  private static boolean isRandomModuleFunctionCall(Expression callee, SubscriptionContext ctx) {
    if (!(callee instanceof QualifiedExpression qualifiedExpression)) {
      return false;
    }
    return RANDOM_PROBLEMATIC_FUNCTION_NAMES.contains(qualifiedExpression.name().name())
      && IS_RANDOM_MODULE.isTrueFor(qualifiedExpression.qualifier(), ctx);
  }

  private static boolean isClassVar(TypeAnnotation annotation, SubscriptionContext ctx) {
    Expression annotationExpr = annotation.expression();
    if (annotationExpr instanceof SubscriptionExpression subscriptionExpr) {
      return IS_CLASS_VAR.isTrueFor(subscriptionExpr.object(), ctx);
    }
    return IS_CLASS_VAR.isTrueFor(annotationExpr, ctx);
  }
}
