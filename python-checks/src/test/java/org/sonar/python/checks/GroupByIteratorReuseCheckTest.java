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

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.python.api.PythonCheck.PreciseIssue;
import org.sonar.plugins.python.api.PythonVisitorContext;
import org.sonar.python.SubscriptionVisitor;
import org.sonar.python.TestPythonVisitorRunner;
import org.sonar.python.checks.quickfix.PythonQuickFixVerifier;
import org.sonar.python.checks.utils.PythonCheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.python.checks.utils.CodeTestUtils.code;

class GroupByIteratorReuseCheckTest {

  @Test
  void test() {
    PythonCheckVerifier.verify("src/test/resources/checks/groupByIteratorReuse.py", new GroupByIteratorReuseCheck());
  }

  @Test
  void quick_fix_subscript_assignment() {
    String before = code(
      "from itertools import groupby",
      "data = [1, 1, 2, 2, 3]",
      "groups = {}",
      "for key, group in groupby(data):",
      "    groups[key] = group");
    String after = code(
      "from itertools import groupby",
      "data = [1, 1, 2, 2, 3]",
      "groups = {}",
      "for key, group in groupby(data):",
      "    groups[key] = list(group)");
    GroupByIteratorReuseCheck check = new GroupByIteratorReuseCheck();
    PythonQuickFixVerifier.verify(check, before, after);
    PythonQuickFixVerifier.verifyQuickFixMessages(check, before, "Wrap with \"list()\"");
  }

  @Test
  void quick_fix_direct_variable_assignment() {
    String before = code(
      "from itertools import groupby",
      "data = [1, 1, 2, 2, 3]",
      "for key, group in groupby(data):",
      "    saved = group");
    String after = code(
      "from itertools import groupby",
      "data = [1, 1, 2, 2, 3]",
      "for key, group in groupby(data):",
      "    saved = list(group)");
    PythonQuickFixVerifier.verify(new GroupByIteratorReuseCheck(), before, after);
  }

  @Test
  void no_quick_fix_when_multiple_reads_in_loop_body() {
    GroupByIteratorReuseCheck check = new GroupByIteratorReuseCheck();
    PythonVisitorContext context = TestPythonVisitorRunner
      .createContext(new File("src/test/resources/checks/groupByIteratorReuseMultipleReads.py"));
    SubscriptionVisitor.analyze(Collections.singletonList(check), context);

    // Sort issues by source position to make the test independent of the framework's internal ordering.
    List<PreciseIssue> issues = context.getIssues().stream()
      .sorted(Comparator.comparingInt((PreciseIssue i) -> i.primaryLocation().startLine())
        .thenComparingInt(i -> i.primaryLocation().startLineOffset()))
      .toList();

    assertThat(issues).hasSize(3);
    assertThat(issues).extracting(i -> i.primaryLocation().startLine()).containsExactly(12, 13, 14);
    assertThat(issues).extracting(i -> i.primaryLocation().message())
      .containsOnly("Consume this group iterator inside the loop, or materialize it into a collection.");

    // See fixture file header for the rationale on why no quickfix is attached.
    assertThat(issues).allSatisfy(issue -> assertThat(issue.quickFixes()).isEmpty());
  }

  @Test
  void no_quick_fix_when_safe_read_follows_unsafe_one() {
    GroupByIteratorReuseCheck check = new GroupByIteratorReuseCheck();
    PythonVisitorContext context = TestPythonVisitorRunner
      .createContext(new File("src/test/resources/checks/groupByIteratorReuseUnsafeWithSafeLaterRead.py"));
    SubscriptionVisitor.analyze(Collections.singletonList(check), context);

    List<PreciseIssue> issues = context.getIssues();
    // See fixture file header for the rationale on why no quickfix is attached.
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).quickFixes()).isEmpty();
  }

  @Test
  void quick_fix_append_to_list() {
    String before = code(
      "from itertools import groupby",
      "data = [1, 1, 2, 2, 3]",
      "result = []",
      "for key, group in groupby(data):",
      "    result.append(group)");
    String after = code(
      "from itertools import groupby",
      "data = [1, 1, 2, 2, 3]",
      "result = []",
      "for key, group in groupby(data):",
      "    result.append(list(group))");
    PythonQuickFixVerifier.verify(new GroupByIteratorReuseCheck(), before, after);
  }
}
