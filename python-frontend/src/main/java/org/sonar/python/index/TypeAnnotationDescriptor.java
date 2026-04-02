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
package org.sonar.python.index;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class TypeAnnotationDescriptor {

  String prettyPrintedName;
  TypeKind kind;
  List<TypeAnnotationDescriptor> args;
  String fullyQualifiedName;
  Boolean isSelf;

  public TypeAnnotationDescriptor(String prettyPrintedName, TypeKind kind, List<TypeAnnotationDescriptor> args, @Nullable String fullyQualifiedName, Boolean isSelf) {
    this.prettyPrintedName = prettyPrintedName;
    this.kind = kind;
    this.args = args;
    this.fullyQualifiedName = fullyQualifiedName;
    this.isSelf = isSelf;
  }

  public String prettyPrintedName() {
    return prettyPrintedName;
  }

  public TypeKind kind() {
    return kind;
  }

  public List<TypeAnnotationDescriptor> args() {
    return args;
  }

  @CheckForNull
  public String fullyQualifiedName() {
    return fullyQualifiedName;
  }

  public Boolean isSelf(){
    return isSelf;
  }

  public enum TypeKind {
    INSTANCE,
    UNION,
    TYPE,
    TUPLE,
    TYPE_VAR,
    ANY,
    NONE,
    TYPE_ALIAS,
    CALLABLE,
    LITERAL,
    UNINHABITED,
    UNBOUND,
    TYPED_DICT
  }
}
