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

import org.junit.jupiter.api.Test;
import org.sonar.python.checks.quickfix.PythonQuickFixVerifier;
import org.sonar.python.checks.utils.PythonCheckVerifier;

class SetUpdateOverForLoopCheckTest {

  private static final SetUpdateOverForLoopCheck check = new SetUpdateOverForLoopCheck();

  @Test
  void test() {
    PythonCheckVerifier.verify("src/test/resources/checks/setUpdateOverForLoop.py", check);
  }

  @Test
  void test_quick_fix() {
    String codeWithIssue = """
      my_set = set()
      other_collection = [4, 5, 6]
      for element in other_collection:
          my_set.add(element)
      """;

    PythonQuickFixVerifier.verifyQuickFixMessages(check, codeWithIssue, "Use \"set.update()\" instead");

    String correctCode = """
      my_set = set()
      other_collection = [4, 5, 6]
      my_set.update(other_collection)""";

    PythonQuickFixVerifier.verify(check, codeWithIssue, correctCode);
  }

  @Test
  void test_no_quick_fix_on_multiline_iterable() {
    String codeWithIssue = """
      my_set = set()
      for element in [
          1,
          2,
          3,
      ]:
          my_set.add(element)
      """;

    PythonQuickFixVerifier.verifyNoQuickFixes(check, codeWithIssue);
  }

  @Test
  void test_no_quick_fix_on_multiline_receiver() {
    String codeWithIssue = """
      my_set = set()
      items = [1, 2, 3]
      for element in items:
          (my_set
           ).add(element)
      """;

    PythonQuickFixVerifier.verifyNoQuickFixes(check, codeWithIssue);
  }
}
