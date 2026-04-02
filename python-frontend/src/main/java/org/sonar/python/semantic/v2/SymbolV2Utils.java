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
package org.sonar.python.semantic.v2;

import org.sonar.plugins.python.api.symbols.v2.UsageV2;

public class SymbolV2Utils {

  private SymbolV2Utils() {}

  public static boolean isDeclaration(UsageV2 usageV2) {
    return usageV2.kind() == UsageV2.Kind.FUNC_DECLARATION
      || usageV2.kind() == UsageV2.Kind.CLASS_DECLARATION
      || usageV2.kind() == UsageV2.Kind.IMPORT;
  }
}
