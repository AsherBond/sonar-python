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

import com.sonar.plugins.security.api.PythonRules;
import com.sonarsource.plugins.architecturepythonfrontend.api.ArchitecturePythonRules;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.BuiltInActiveRule;
import org.sonar.plugins.python.editions.OpenSourceRepositoryInfoProvider;
import org.sonar.plugins.python.editions.RepositoryInfoProvider;

import static org.assertj.core.api.Assertions.assertThat;

class AIQualityProfileTest {

  @BeforeEach
  void setUp() {
    PythonRules.throwOnCall = false;
    PythonRules.getRuleKeys().clear();
    com.sonarsource.plugins.dbd.api.PythonRules.throwOnCall = false;
    com.sonarsource.plugins.dbd.api.PythonRules.getDataflowBugDetectionRuleKeys().clear();
    ArchitecturePythonRules.throwOnCall = false;
    ArchitecturePythonRules.getRuleKeys().clear();
  }

  public BuiltInQualityProfilesDefinition.BuiltInQualityProfile getProfile() {
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    new AIQualityProfile(new RepositoryInfoProvider[]{new OpenSourceRepositoryInfoProvider()}).define(context);
    return context.profile("py", "Sonar agentic AI");
  }

  @Test
  void profile() {
    BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile = getProfile();
    assertThat(profile.rules()).extracting("repoKey").containsOnly("python");
    assertThat(profile.rules()).hasSizeGreaterThan(25);
    assertThat(profile.rules()).extracting(BuiltInActiveRule::ruleKey).contains("S100");
  }

  @Test
  void should_contain_security_rules_when_available() {
    PythonRules.getRuleKeys().add("S3649");
    try {
      BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile = getProfile();
      assertThat(profile.rules()).extracting("repoKey").contains("pythonsecurity");
      assertThat(profile.rules()).extracting(BuiltInActiveRule::ruleKey).contains("S3649");
    } finally {
      PythonRules.getRuleKeys().clear();
    }
  }

  @Test
  void should_contain_dataflow_bug_detection_rules_when_available() {
    com.sonarsource.plugins.dbd.api.PythonRules.getDataflowBugDetectionRuleKeys().add("S2259");
    try {
      BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile = getProfile();
      assertThat(profile.rules()).extracting("repoKey").contains("dbd-repo-key");
      assertThat(profile.rules()).extracting(BuiltInActiveRule::ruleKey).contains("S2259");
    } finally {
      com.sonarsource.plugins.dbd.api.PythonRules.getDataflowBugDetectionRuleKeys().clear();
    }
  }

  @Test
  void should_contain_architecture_rules_when_available() {
    ArchitecturePythonRules.getRuleKeys().add("S7134");
    try {
      BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile = getProfile();
      assertThat(profile.rules()).extracting("repoKey").contains("pythonarchitecture");
      assertThat(profile.rules()).extracting(BuiltInActiveRule::ruleKey).contains("S7134");
    } finally {
      ArchitecturePythonRules.getRuleKeys().clear();
    }
  }
}
