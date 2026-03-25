/*
 * SonarQube Python Plugin
 * Copyright (C) 2011-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.python;

import org.junit.jupiter.api.Test;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.BuiltInActiveRule;
import org.sonar.plugins.python.editions.OpenSourceRepositoryInfoProvider;
import org.sonar.plugins.python.editions.RepositoryInfoProvider;

import static org.assertj.core.api.Assertions.assertThat;

class AIQualityProfileTest {

  public BuiltInQualityProfilesDefinition.BuiltInQualityProfile getProfile() {
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    new AIQualityProfile(new RepositoryInfoProvider[]{new OpenSourceRepositoryInfoProvider()}).define(context);
    return context.profile("py", "AI quality profile");
  }

  @Test
  void profile() {
    BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile = getProfile();
    assertThat(profile.rules()).extracting("repoKey").containsOnly("python");
    assertThat(profile.rules()).hasSizeGreaterThan(25);
    assertThat(profile.rules()).extracting(BuiltInActiveRule::ruleKey).contains("S100");
  }
}
