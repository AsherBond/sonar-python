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
package org.sonar.python.types.v2.matchers;

import java.util.Objects;
import java.util.Optional;
import org.sonar.plugins.python.api.TriBool;
import org.sonar.plugins.python.api.types.v2.FullyQualifiedNameHelper;
import org.sonar.plugins.python.api.types.v2.PythonType;

public record HasFQNPredicate(String fullyQualifiedName) implements TypePredicate {
  @Override
  public TriBool check(PythonType type, TypePredicateContext ctx) {
    return Optional.of(type)
      .flatMap(FullyQualifiedNameHelper::getFullyQualifiedName)
      .map(typeFqn -> Objects.equals(fullyQualifiedName, typeFqn) ? TriBool.TRUE : TriBool.FALSE)
      .orElse(TriBool.UNKNOWN);
  }
}

