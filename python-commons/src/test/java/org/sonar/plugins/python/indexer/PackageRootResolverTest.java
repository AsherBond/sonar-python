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
package org.sonar.plugins.python.indexer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PackageRootResolverTest {

  @TempDir
  Path tempDir;

  // ─── Helper ────────────────────────────────────────────────────────────────

  private FileSystem mockFileSystem(File baseDir) {
    FileSystem fs = mock(FileSystem.class);
    when(fs.baseDir()).thenReturn(baseDir);
    return fs;
  }

  private Configuration noSonarSources() {
    Configuration config = mock(Configuration.class);
    when(config.getStringArray("sonar.sources")).thenReturn(new String[0]);
    return config;
  }

  private Configuration sonarSources(String... values) {
    Configuration config = mock(Configuration.class);
    when(config.getStringArray("sonar.sources")).thenReturn(values);
    return config;
  }

  // ─── resolve() — no build config files → LEGACY_INIT_PY ──────────────────

  @Test
  void resolve_noBuildConfigFiles_returnsLegacyInitPy() {
    File baseDir = tempDir.toFile();

    PackageResolutionResult result = PackageRootResolver.resolve(mockFileSystem(baseDir), noSonarSources());

    assertThat(result.roots()).isEmpty();
    assertThat(result.method()).isEqualTo(PackageResolutionResult.ResolutionMethod.LEGACY_INIT_PY);
  }

  @Test
  void resolve_noBuildConfigFiles_srcFolderPresent_stillReturnsLegacyInitPy() throws IOException {
    // Conventional folders are ignored when no build config files exist
    Files.createDirectory(tempDir.resolve("src"));
    File baseDir = tempDir.toFile();

    PackageResolutionResult result = PackageRootResolver.resolve(mockFileSystem(baseDir), noSonarSources());

    assertThat(result.roots()).isEmpty();
    assertThat(result.method()).isEqualTo(PackageResolutionResult.ResolutionMethod.LEGACY_INIT_PY);
  }

  @Test
  void resolve_noBuildConfigFiles_sonarSourcesIgnored() {
    // sonar.sources is also irrelevant when no build config files exist
    File baseDir = tempDir.toFile();

    PackageResolutionResult result = PackageRootResolver.resolve(mockFileSystem(baseDir), sonarSources("."));

    assertThat(result.roots()).isEmpty();
    assertThat(result.method()).isEqualTo(PackageResolutionResult.ResolutionMethod.LEGACY_INIT_PY);
  }

  @Test
  void resolve_buildFileExistsNoRoots_conventionalFoldersTakePriorityOverSonarSources() throws IOException {
    // pyproject.toml exists but provides no source roots; src/ exists; sonar.sources set
    // => conventional folders should win (build file present path)
    Files.createDirectory(tempDir.resolve("src"));
    Files.writeString(tempDir.resolve("pyproject.toml"), "[project]\nname = \"myproject\"\n");
    File baseDir = tempDir.toFile();

    PackageResolutionResult result = PackageRootResolver.resolve(mockFileSystem(baseDir), sonarSources("app"));

    assertThat(result.roots()).containsExactly(new File(baseDir, "src").getAbsolutePath());
    assertThat(result.method()).isEqualTo(PackageResolutionResult.ResolutionMethod.CONVENTIONAL_FOLDERS);
  }

  @Test
  void resolve_buildFileExistsNoRoots_fallsBackToSonarSources() throws IOException {
    // pyproject.toml exists but provides no source roots; no conventional folders; sonar.sources set
    Files.writeString(tempDir.resolve("pyproject.toml"), "[project]\nname = \"myproject\"\n");
    File baseDir = tempDir.toFile();

    PackageResolutionResult result = PackageRootResolver.resolve(mockFileSystem(baseDir), sonarSources("mysrc"));

    assertThat(result.roots()).containsExactly(new File(baseDir, "mysrc").getAbsolutePath());
    assertThat(result.method()).isEqualTo(PackageResolutionResult.ResolutionMethod.SONAR_SOURCES);
  }

  @Test
  void resolve_setupPyExistsNoRoots_conventionalFoldersTakePriorityOverSonarSources() throws IOException {
    // setup.py exists but provides no source roots; src/ exists; sonar.sources set
    Files.createDirectory(tempDir.resolve("src"));
    Files.writeString(tempDir.resolve("setup.py"), "# empty\n");
    File baseDir = tempDir.toFile();

    PackageResolutionResult result = PackageRootResolver.resolve(mockFileSystem(baseDir), sonarSources("app"));

    assertThat(result.roots()).containsExactly(new File(baseDir, "src").getAbsolutePath());
    assertThat(result.method()).isEqualTo(PackageResolutionResult.ResolutionMethod.CONVENTIONAL_FOLDERS);
  }

  // ─── toAbsolutePaths() — normalization ────────────────────────────────────

  @Test
  void toAbsolutePaths_normalPaths_resolvedCorrectly() {
    File baseDir = tempDir.toFile();
    List<String> result = PackageRootResolver.toAbsolutePaths(List.of("app", "core"), baseDir);

    assertThat(result).containsExactly(
      new File(baseDir, "app").getAbsolutePath(),
      new File(baseDir, "core").getAbsolutePath());
  }

  @Test
  void toAbsolutePaths_dotPath_normalizedToBaseDir() {
    File baseDir = tempDir.toFile();
    List<String> result = PackageRootResolver.toAbsolutePaths(List.of("."), baseDir);

    // "." must normalize to baseDir, not "/baseDir/."
    assertThat(result).containsExactly(baseDir.getAbsolutePath());
    assertThat(result.get(0)).doesNotEndWith("/.");
    assertThat(result.get(0)).doesNotEndWith("\\.");
  }

  // ─── toAbsolutePaths() — Windows path handling ───────────────────────────

  @Test
  void toAbsolutePaths_windowsAbsolutePath_notPrependedWithBaseDir() {
    // When sonar.sources contains a Windows-style absolute path (e.g. from a Windows scanner),
    // it must NOT be prepended with the base directory on a non-Windows system.
    File baseDir = new File("/project/base");
    String windowsAbsPath = "C:\\Users\\user\\mixed-language-build";

    List<String> result = PackageRootResolver.toAbsolutePaths(List.of(windowsAbsPath), baseDir);

    // Must not produce "/project/base/" prepended to the Windows path
    assertThat(result).containsExactly("C:\\Users\\user\\mixed-language-build");
  }

  @Test
  void toAbsolutePaths_windowsAbsolutePathWithForwardSlashes_notPrependedWithBaseDir() {
    // Windows paths may also use forward slashes; they are normalised by Path.normalize()
    File baseDir = new File("/project/base");
    String windowsAbsPath = "C:/Users/user/mixed-language-build";

    List<String> result = PackageRootResolver.toAbsolutePaths(List.of(windowsAbsPath), baseDir);

    // Must not produce "/project/base/" prepended to the Windows path
    // Forward slashes are preserved as-is by Path.normalize() on non-Windows systems
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isIn("C:\\Users\\user\\mixed-language-build","C:/Users/user/mixed-language-build");
  }

  @Test
  void trySonarSources_windowsAbsolutePath_usedDirectlyNotPrependedWithBaseDir() throws IOException {
    // Full integration: build file exists, sonar.sources has a Windows absolute path.
    // The resolved root should be derived from the Windows path, not baseDir + Windows path.
    Files.writeString(tempDir.resolve("pyproject.toml"), "[project]\nname = \"myproject\"\n");
    File baseDir = tempDir.toFile();
    String windowsAbsPath = "C:\\Users\\user\\mixed-language-build";

    PackageResolutionResult result = PackageRootResolver.resolve(mockFileSystem(baseDir), sonarSources(windowsAbsPath));

    assertThat(result.method()).isEqualTo(PackageResolutionResult.ResolutionMethod.SONAR_SOURCES);
    assertThat(result.roots()).hasSize(1);
    assertThat(result.roots()).containsExactly("C:\\Users\\user\\mixed-language-build");
  }

  // ─── adjustPackageRoot() — migrated from PythonIndexerTest ────────────────

  @Test
  void adjustPackageRoot_noInitPy(@TempDir Path localTempDir) {
    File baseDir = localTempDir.toFile();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    String result = PackageRootResolver.adjustPackageRoot(srcDir, baseDir);
    assertThat(result).isEqualTo(srcDir.getAbsolutePath());
  }

  @Test
  void adjustPackageRoot_withInitPy_walksUp(@TempDir Path localTempDir) throws Exception {
    File baseDir = localTempDir.toFile();
    File srcDir = new File(baseDir, "src");
    File packageDir = new File(srcDir, "mypackage");
    packageDir.mkdirs();
    new File(packageDir, "__init__.py").createNewFile();

    String result = PackageRootResolver.adjustPackageRoot(packageDir, baseDir);
    assertThat(result).isEqualTo(srcDir.getAbsolutePath());
  }

  @Test
  void adjustPackageRoot_nestedPackagesWithInitPy(@TempDir Path localTempDir) throws Exception {
    File baseDir = localTempDir.toFile();
    File srcDir = new File(baseDir, "src");
    File level1 = new File(srcDir, "level1");
    File level2 = new File(level1, "level2");
    File level3 = new File(level2, "level3");
    level3.mkdirs();

    new File(level1, "__init__.py").createNewFile();
    new File(level2, "__init__.py").createNewFile();
    new File(level3, "__init__.py").createNewFile();

    String result = PackageRootResolver.adjustPackageRoot(level3, baseDir);
    assertThat(result).isEqualTo(srcDir.getAbsolutePath());
  }

  @Test
  void adjustPackageRoot_stopsAtBaseDir(@TempDir Path localTempDir) throws Exception {
    File baseDir = localTempDir.toFile();
    File level1 = new File(baseDir, "level1");
    File level2 = new File(level1, "level2");
    level2.mkdirs();
    new File(baseDir, "__init__.py").createNewFile();
    new File(level1, "__init__.py").createNewFile();
    new File(level2, "__init__.py").createNewFile();

    String result = PackageRootResolver.adjustPackageRoot(level2, baseDir);
    assertThat(result).isEqualTo(baseDir.getAbsolutePath());
  }

  @Test
  void adjustPackageRoot_rootEqualsBaseDir(@TempDir Path localTempDir) throws Exception {
    File baseDir = localTempDir.toFile();
    new File(baseDir, "__init__.py").createNewFile();

    String result = PackageRootResolver.adjustPackageRoot(baseDir, baseDir);
    assertThat(result).isEqualTo(baseDir.getAbsolutePath());
  }

  @Test
  void adjustPackageRoot_partialInitPyChain(@TempDir Path localTempDir) throws Exception {
    File baseDir = localTempDir.toFile();
    File srcDir = new File(baseDir, "src");
    File withInit = new File(srcDir, "withInit");
    File withoutInit = new File(withInit, "withoutInit");
    File deepPackage = new File(withoutInit, "deepPackage");
    deepPackage.mkdirs();

    new File(withInit, "__init__.py").createNewFile();
    new File(deepPackage, "__init__.py").createNewFile();

    String result = PackageRootResolver.adjustPackageRoot(deepPackage, baseDir);
    assertThat(result).isEqualTo(withoutInit.getAbsolutePath());
  }

  @Test
  void adjustPackageRoot_emptyDirectory(@TempDir Path localTempDir) {
    File baseDir = localTempDir.toFile();
    File emptyDir = new File(baseDir, "empty");
    emptyDir.mkdir();

    String result = PackageRootResolver.adjustPackageRoot(emptyDir, baseDir);
    assertThat(result).isEqualTo(emptyDir.getAbsolutePath());
  }

  @Test
  void adjustPackageRoot_singleLevelWithInitPy(@TempDir Path localTempDir) throws Exception {
    File baseDir = localTempDir.toFile();
    File packageDir = new File(baseDir, "mypackage");
    packageDir.mkdir();
    new File(packageDir, "__init__.py").createNewFile();

    String result = PackageRootResolver.adjustPackageRoot(packageDir, baseDir);
    assertThat(result).isEqualTo(baseDir.getAbsolutePath());
  }
}
