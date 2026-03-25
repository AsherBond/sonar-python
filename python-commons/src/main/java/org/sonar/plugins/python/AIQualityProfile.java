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

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.plugins.python.editions.RepositoryInfoProvider;
import org.sonar.plugins.python.editions.RepositoryInfoProvider.RepositoryInfo;
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader;

public class AIQualityProfile implements BuiltInQualityProfilesDefinition {

  static final String PROFILE_NAME = "AI quality profile";

  private final RepositoryInfoProvider[] editionMetadataProviders;

  public AIQualityProfile(RepositoryInfoProvider[] editionMetadataProviders) {
    this.editionMetadataProviders = editionMetadataProviders;
  }

  @Override
  public void define(Context context) {
    NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile(PROFILE_NAME, Python.KEY);

    for (RepositoryInfoProvider repositoryInfoProvider : editionMetadataProviders) {
      registerRulesForEdition(repositoryInfoProvider, profile);
    }

    profile.done();
  }

  private static void registerRulesForEdition(RepositoryInfoProvider repositoryInfoProvider, NewBuiltInQualityProfile profile) {
    RepositoryInfo repositoryInfo = repositoryInfoProvider.getAIProfileInfo();
    BuiltInQualityProfileJsonLoader.load(profile, repositoryInfo.repositoryKey(), repositoryInfo.profileLocation());
    profile.activeRules().removeIf(rule -> repositoryInfo.disabledRules().contains(rule.ruleKey()));
  }
}
