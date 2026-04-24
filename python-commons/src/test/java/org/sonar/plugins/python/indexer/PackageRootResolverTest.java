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

  // ─── resolve() — fallback: conventional folders ───────────────────────────

  @Test
  void resolve_noConfigFiles_fallsBackToSrcFolder() throws IOException {
    Files.createDirectory(tempDir.resolve("src"));
    File baseDir = tempDir.toFile();

    PackageResolutionResult result = PackageRootResolver.resolve(mockFileSystem(baseDir), noSonarSources());

    assertThat(result.roots()).containsExactly(new File(baseDir, "src").getAbsolutePath());
    assertThat(result.method()).isEqualTo(PackageResolutionResult.ResolutionMethod.CONVENTIONAL_FOLDERS);
  }

  @Test
  void resolve_noConfigFiles_fallsBackToLibFolder() throws IOException {
    Files.createDirectory(tempDir.resolve("lib"));
    File baseDir = tempDir.toFile();

    PackageResolutionResult result = PackageRootResolver.resolve(mockFileSystem(baseDir), noSonarSources());

    assertThat(result.roots()).containsExactly(new File(baseDir, "lib").getAbsolutePath());
    assertThat(result.method()).isEqualTo(PackageResolutionResult.ResolutionMethod.CONVENTIONAL_FOLDERS);
  }

  @Test
  void resolve_noConfigFiles_fallsBackToBothSrcAndLib() throws IOException {
    Files.createDirectory(tempDir.resolve("src"));
    Files.createDirectory(tempDir.resolve("lib"));
    File baseDir = tempDir.toFile();

    PackageResolutionResult result = PackageRootResolver.resolve(mockFileSystem(baseDir), noSonarSources());

    assertThat(result.roots()).containsExactly(
      new File(baseDir, "src").getAbsolutePath(),
      new File(baseDir, "lib").getAbsolutePath());
    assertThat(result.method()).isEqualTo(PackageResolutionResult.ResolutionMethod.CONVENTIONAL_FOLDERS);
  }

  @Test
  void resolve_noConfigFiles_sonarSourcesWinsOverConventionalFolders() throws IOException {
    // src/ exists AND sonar.sources is set — sonar.sources should win (no build files)
    Files.createDirectory(tempDir.resolve("src"));
    File baseDir = tempDir.toFile();

    PackageResolutionResult result = PackageRootResolver.resolve(mockFileSystem(baseDir), sonarSources("app"));

    assertThat(result.roots()).containsExactly(new File(baseDir, "app").getAbsolutePath());
    assertThat(result.method()).isEqualTo(PackageResolutionResult.ResolutionMethod.SONAR_SOURCES);
  }

  // ─── resolve() — fallback: sonar.sources ──────────────────────────────────

  @Test
  void resolve_noConfigFilesNoConventionalFolders_fallsBackToSonarSources() {
    File baseDir = tempDir.toFile();

    PackageResolutionResult result = PackageRootResolver.resolve(mockFileSystem(baseDir), sonarSources("sources", "lib"));

    assertThat(result.roots()).containsExactly(
      new File(baseDir, "sources").getAbsolutePath(),
      new File(baseDir, "lib").getAbsolutePath());
    assertThat(result.method()).isEqualTo(PackageResolutionResult.ResolutionMethod.SONAR_SOURCES);
  }

  @Test
  void resolve_dotSonarSources_returnsBaseDirAbsolutePath() {
    // sonar.sources=. must normalize to baseDir, not produce "/baseDir/."
    File baseDir = tempDir.toFile();

    PackageResolutionResult result = PackageRootResolver.resolve(mockFileSystem(baseDir), sonarSources("."));

    assertThat(result.roots()).containsExactly(baseDir.getAbsolutePath());
    assertThat(result.method()).isEqualTo(PackageResolutionResult.ResolutionMethod.SONAR_SOURCES);
  }

  // ─── resolve() — fallback: base directory ─────────────────────────────────

  @Test
  void resolve_noConfigFilesNoConventionalFoldersNoSonarSources_fallsBackToBaseDir() {
    File baseDir = tempDir.toFile();

    PackageResolutionResult result = PackageRootResolver.resolve(mockFileSystem(baseDir), noSonarSources());

    assertThat(result.roots()).containsExactly(baseDir.getAbsolutePath());
    assertThat(result.method()).isEqualTo(PackageResolutionResult.ResolutionMethod.BASE_DIR);
  }

  @Test
  void resolve_srcExistsAsFile_fallsBackToSonarSourcesThenBaseDir() throws IOException {
    // src is a file, not a dir — conventional folder check fails
    Files.createFile(tempDir.resolve("src"));
    File baseDir = tempDir.toFile();

    PackageResolutionResult result = PackageRootResolver.resolve(mockFileSystem(baseDir), noSonarSources());

    assertThat(result.roots()).containsExactly(baseDir.getAbsolutePath());
    assertThat(result.method()).isEqualTo(PackageResolutionResult.ResolutionMethod.BASE_DIR);
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
