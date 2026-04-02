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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.tree.ArgList;
import org.sonar.plugins.python.api.tree.Argument;
import org.sonar.plugins.python.api.tree.ClassDef;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.RegularArgument;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.types.v2.FullyQualifiedNameHelper;

@Rule(key = "S8509")
public class DuplicateBaseClassCheck extends PythonSubscriptionCheck {

  private static final String MESSAGE = "Remove this duplicate base class.";
  private static final String SECONDARY_MESSAGE = "Already listed here.";

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.CLASSDEF, ctx -> checkClassDef((ClassDef) ctx.syntaxNode(), ctx));
  }

  private static void checkClassDef(ClassDef classDef, SubscriptionContext ctx) {
    ArgList args = classDef.args();
    if (args == null) {
      return;
    }
    var duplicates = gatherDuplicates(args);
    raiseOnDuplicates(duplicates, ctx);
  }

  private static Map<String, List<RegularArgument>> gatherDuplicates(ArgList args) {
    Map<String, List<RegularArgument>> groups = new LinkedHashMap<>();
    for (Argument argument : args.arguments()) {
      if (argument instanceof RegularArgument regularArgument) {
        if (regularArgument.keywordArgument() != null) {
          continue;
        }
        String key = expressionKey(regularArgument.expression());
        if (key != null) {
          groups.computeIfAbsent(key, k -> new ArrayList<>()).add(regularArgument);
        }
      }
    }
    return groups;
  }

  private static void raiseOnDuplicates(Map<String, List<RegularArgument>> duplicates, SubscriptionContext ctx) {
    for (List<RegularArgument> group : duplicates.values()) {
      if (group.size() > 1) {
        RegularArgument first = group.get(0);
        PreciseIssue issue = ctx.addIssue(first.expression(), MESSAGE);
        for (int i = 1; i < group.size(); i++) {
          issue.secondary(group.get(i).expression(), SECONDARY_MESSAGE);
        }
      }
    }
  }

  @CheckForNull
  private static String expressionKey(Expression expression) {
    return FullyQualifiedNameHelper.getFullyQualifiedName(expression.typeV2()).orElse(null);
  }
}
