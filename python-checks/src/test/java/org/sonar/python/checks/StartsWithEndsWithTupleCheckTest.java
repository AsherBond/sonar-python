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

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.python.checks.quickfix.PythonQuickFixVerifier;
import org.sonar.python.checks.utils.PythonCheckVerifier;

class StartsWithEndsWithTupleCheckTest {

  @Test
  void test() {
    PythonCheckVerifier.verify("src/test/resources/checks/startsWithEndsWithTupleCheck.py", new StartsWithEndsWithTupleCheck());
  }

  static Stream<Arguments> quickFixTestCases() {
    return Stream.of(
      Arguments.of(
        """
          s = "https://example.com"
          s.startswith("http://") or s.startswith("https://")
          """,
        """
          s = "https://example.com"
          s.startswith(("http://", "https://"))
          """,
        "Replace with a single call using a tuple argument"),
      Arguments.of(
        """
          filename = "photo.jpg"
          filename.endswith(".jpg") or filename.endswith(".png")
          """,
        """
          filename = "photo.jpg"
          filename.endswith((".jpg", ".png"))
          """,
        "Replace with a single call using a tuple argument"),
      Arguments.of(
        """
          data = b"hello world"
          data.startswith(b"hello") or data.startswith(b"hi")
          """,
        """
          data = b"hello world"
          data.startswith((b"hello", b"hi"))
          """,
        "Replace with a single call using a tuple argument"),
      Arguments.of(
        """
          paths = ["/usr/bin", "/etc/conf"]
          paths[0].startswith("/usr") or paths[0].startswith("/etc")
          """,
        """
          paths = ["/usr/bin", "/etc/conf"]
          paths[0].startswith(("/usr", "/etc"))
          """,
        "Replace with a single call using a tuple argument"),
      Arguments.of(
        """
          filename = "report.pdf"
          filename.endswith(".pdf") or filename.endswith(".doc") or filename.endswith(".txt")
          """,
        """
          filename = "report.pdf"
          filename.endswith((".pdf", ".doc", ".txt"))
          """,
        "Replace with a single call using a tuple argument"),
      Arguments.of(
        """
          class MyClass:
              def check(self):
                  self.name.startswith("a") or self.name.startswith("b")
          """,
        """
          class MyClass:
              def check(self):
                  self.name.startswith(("a", "b"))
          """,
        "Replace with a single call using a tuple argument"),
      Arguments.of(
        """
          s = "https://example.com"
          if (s.startswith("http://")
                  or s.startswith("https://")):
              pass
          """,
        """
          s = "https://example.com"
          if (s.startswith(("http://", "https://"))):
              pass
          """,
        "Replace with a single call using a tuple argument"),
      Arguments.of(
        """
          s = "https://example.com"
          if s.startswith("http://") \\
                  or s.startswith("https://"):
              pass
          """,
        """
          s = "https://example.com"
          if s.startswith(("http://", "https://")):
              pass
          """,
        "Replace with a single call using a tuple argument")
    );
  }

  @ParameterizedTest
  @MethodSource("quickFixTestCases")
  void testQuickFix(String before, String after, String expectedMessage) {
    var check = new StartsWithEndsWithTupleCheck();
    PythonQuickFixVerifier.verify(check, before, after);
    PythonQuickFixVerifier.verifyQuickFixMessages(check, before, expectedMessage);
  }

}
