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
package org.sonar.python.semantic.v2.converter;

import org.sonar.plugins.python.api.types.v2.PythonType;
import org.sonar.python.index.AliasDescriptor;
import org.sonar.python.index.Descriptor;

public class AliasDescriptorToPythonTypeConverter implements DescriptorToPythonTypeConverter {
  @Override
  public PythonType convert(ConversionContext ctx, Descriptor from) {
    AliasDescriptor aliasDescriptor = (AliasDescriptor) from;
    String originalFqn = aliasDescriptor.originalDescriptor().fullyQualifiedName();
    if (originalFqn != null && !originalFqn.startsWith(ctx.moduleFqn())) {
      return ctx.lazyTypesContext().getOrCreateLazyType(originalFqn);
    }
    return ctx.convert(((AliasDescriptor) from).originalDescriptor());
  }
}
