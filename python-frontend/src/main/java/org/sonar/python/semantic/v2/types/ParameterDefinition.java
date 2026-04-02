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
package org.sonar.python.semantic.v2.types;

import org.sonar.plugins.python.api.symbols.v2.SymbolV2;
import org.sonar.plugins.python.api.tree.Name;
import org.sonar.plugins.python.api.types.v2.PythonType;

public class ParameterDefinition extends Propagation {
  private final PythonType parameterType;

  protected ParameterDefinition(SymbolV2 lhsSymbol, Name lhsName, PythonType parameterType) {
    super(lhsSymbol, lhsName);
    this.parameterType = parameterType;
  }


  @Override
  public PythonType rhsType() {
    return parameterType;
  }
}
