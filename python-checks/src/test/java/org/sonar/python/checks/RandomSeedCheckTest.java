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

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.python.checks.utils.PythonCheckVerifier;

class RandomSeedCheckTest {
  RandomSeedCheck check = new RandomSeedCheck();

  @Test
  void test_migration() {
    PythonCheckVerifier.verify("src/test/resources/checks/randomSeedMigration.py", check);
  }

  @Test
  void test_project_migration() {
    String root = "src/test/resources/checks/randomSeedMigrationProject/";
    PythonCheckVerifier.verify(List.of(root + "usage.py", root + "sklearn/__init__.py",
      root + "sklearn/linear_model/__init__.py", root + "sklearn/linear_model/_coordinate_descent.py",
      root + "sklearn/datasets/__init__.py", root + "sklearn/datasets/_samples_generator.py",
      root + "sklearn/datasets/usage.py"), check);
  }

  @Test
  void test_numpy() {
    PythonCheckVerifier.verify("src/test/resources/checks/randomSeedNumpy.py", check );
  }
  
  @Test
  void test_sklearn() {
    PythonCheckVerifier.verify("src/test/resources/checks/randomSeedSKlearn.py", check);
  }
}

