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

import org.sonar.plugins.python.api.TriBool;
import org.sonar.plugins.python.api.types.v2.PythonType;
import org.sonar.plugins.python.api.types.v2.SelfType;
import org.sonar.plugins.python.api.types.v2.UnknownType;

public class IsTypePredicate implements TypePredicate {
  private final String fullyQualifiedName;

  public IsTypePredicate(String fullyQualifiedName) {
    this.fullyQualifiedName = fullyQualifiedName;
  }

  @Override
  public TriBool check(PythonType type, TypePredicateContext ctx) {
    PythonType expectedType = ctx.typeTable().getType(fullyQualifiedName);
    
    if (type instanceof UnknownType || expectedType instanceof UnknownType
        || type instanceof SelfType || expectedType instanceof SelfType) {
      return TriBool.UNKNOWN;
    }
    
    return type.equals(expectedType) ? TriBool.TRUE : TriBool.FALSE;
  }
}

