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
package org.sonar.plugins.python.api.tree;

import javax.annotation.CheckForNull;

/**
 * <pre>
 *   {@link #name()} {@link #typeAnnotation()} = {@link #defaultValue()}
 * </pre>
 *
 *
 * or
 *
 * <pre>
 *   {@link #starToken()} {@link #name()}
 * </pre>
 *
 * See https://docs.python.org/3/reference/compound_stmts.html#function-definitions
 */
public interface Parameter extends AnyParameter {

  /**
   * Represents both '*' and '**' as well as the '/'
   */
  @CheckForNull
  Token starToken();

  @CheckForNull
  Name name();

  @CheckForNull
  TypeAnnotation typeAnnotation();

  @CheckForNull
  Token equalToken();

  @CheckForNull
  Expression defaultValue();
}
