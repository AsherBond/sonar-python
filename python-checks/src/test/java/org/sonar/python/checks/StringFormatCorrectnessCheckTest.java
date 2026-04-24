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

import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.python.api.tree.ArgList;
import org.sonar.plugins.python.api.tree.CallExpression;
import org.sonar.plugins.python.api.tree.RegularArgument;
import org.sonar.plugins.python.api.tree.StringLiteral;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.python.checks.utils.PythonCheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StringFormatCorrectnessCheckTest {

  @Test
  void test() {
    PythonCheckVerifier.verify("src/test/resources/checks/stringFormatCorrectness.py", new StringFormatCorrectnessCheck());
  }

  @Test
  void enclosing_call_expression_returns_empty_when_regular_argument_parent_is_not_arg_list() throws Exception {
    StringLiteral literal = mock(StringLiteral.class);
    RegularArgument argument = mock(RegularArgument.class);
    Tree nonArgListParent = mock(Tree.class);

    when(literal.parent()).thenReturn(argument);
    when(argument.parent()).thenReturn(nonArgListParent);

    assertThat(invokeEnclosingCallExpression(literal)).isEmpty();
  }

  @Test
  void enclosing_call_expression_returns_empty_when_arg_list_parent_is_not_call_expression() throws Exception {
    StringLiteral literal = mock(StringLiteral.class);
    RegularArgument argument = mock(RegularArgument.class);
    ArgList argList = mock(ArgList.class);
    Tree nonCallParent = mock(Tree.class);

    when(literal.parent()).thenReturn(argument);
    when(argument.parent()).thenReturn(argList);
    when(argList.parent()).thenReturn(nonCallParent);

    assertThat(invokeEnclosingCallExpression(literal)).isEmpty();
  }

  @SuppressWarnings("unchecked")
  private static Optional<CallExpression> invokeEnclosingCallExpression(StringLiteral literal) throws Exception {
    Method method = StringFormatCorrectnessCheck.class.getDeclaredMethod("enclosingCallExpression", StringLiteral.class);
    method.setAccessible(true);
    return (Optional<CallExpression>) method.invoke(null, literal);
  }
}
