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

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Configuration;

/**
 * Resolves package root directories for Python projects.
 *
 * <p>This class is the single source of truth for package root resolution. It handles:
 * <ul>
 *   <li>Extraction from pyproject.toml build system configurations</li>
 *   <li>Extraction from setup.py configurations</li>
 *   <li>When no build config files exist: returns empty roots so that FQN resolution falls back
 *       to legacy __init__.py-based detection (see
 *       {@link org.sonar.python.semantic.SymbolUtils#pythonPackageName})</li>
 *   <li>Fallback when build files exist but provide no roots: conventional folders (src/, lib/),
 *       then sonar.sources, then base directory</li>
 * </ul>
 */
public class PackageRootResolver {

  private static final Logger LOG = LoggerFactory.getLogger(PackageRootResolver.class);

  static final String SONAR_SOURCES_KEY = "sonar.sources";
  static final List<String> CONVENTIONAL_FOLDERS = List.of("src", "lib");

  private PackageRootResolver() {
  }

  /**
   * Resolves package root directories for the project.
   *
   * <p>Attempts to extract source roots from pyproject.toml and setup.py build system configurations.
   * When no build config files exist, returns empty roots so that FQN resolution falls back to
   * legacy __init__.py-based detection. When build config files exist but provide no source roots,
   * conventional folders take priority over sonar.sources, then base directory as last resort.
   *
   * @param fileSystem the Sonar file system providing the base directory
   * @param config the Sonar configuration
   * @return resolution result including resolved root absolute paths and method information
   */
  public static PackageResolutionResult resolve(FileSystem fileSystem, Configuration config) {
    File baseDir = fileSystem.baseDir();

    // Discover build config files
    List<File> pyprojectFiles = findFilesRecursively(fileSystem, "pyproject.toml");
    List<File> setupPyFiles = findFilesRecursively(fileSystem, "setup.py");
    boolean hasBuildConfigFiles = !pyprojectFiles.isEmpty() || !setupPyFiles.isEmpty();

    // When no build config files exist, FQN resolution relies on legacy __init__.py detection.
    // Returning empty roots causes SymbolUtils.pythonPackageName to use pythonPackageNameLegacy().
    if (!hasBuildConfigFiles) {
      LOG.debug("No build config files found; using legacy __init__.py-based package detection");
      return PackageResolutionResult.fromLegacyInitPy();
    }

    // Extract source roots from discovered files
    List<PyProjectExtractionResult> pyprojectResults = pyprojectFiles.stream()
      .map(PyProjectTomlSourceRoots::extractWithBuildSystem)
      .filter(PyProjectExtractionResult::hasRoots)
      .toList();
    List<ConfigSourceRoots> setupPyRoots = setupPyFiles.stream()
      .map(SetupPySourceRoots::extractWithLocation)
      .filter(csr -> !csr.relativeRoots().isEmpty())
      .toList();

    boolean hasPyproject = pyprojectResults.stream().anyMatch(PyProjectExtractionResult::hasRoots);
    boolean hasSetupPy = !setupPyRoots.isEmpty();

    List<String> combinedRoots = Stream.concat(
        pyprojectResults.stream().map(PyProjectExtractionResult::configRoots).flatMap(crs -> crs.toAbsolutePaths().stream()),
        setupPyRoots.stream().flatMap(csr -> csr.toAbsolutePaths().stream()))
      .distinct()
      .toList();

    List<String> adjustedRoots = adjustRoots(combinedRoots, baseDir);
    LOG.debug("Resolved package roots from build configuration: {}", adjustedRoots);

    if (hasPyproject && hasSetupPy) {
      return PackageResolutionResult.fromBothPyProjectAndSetupPy(adjustedRoots, getCombinedBuildSystem(pyprojectResults));
    }

    if (hasPyproject) {
      return PackageResolutionResult.fromPyProjectToml(adjustedRoots, getCombinedBuildSystem(pyprojectResults));
    }

    if (hasSetupPy) {
      return PackageResolutionResult.fromSetupPy(adjustedRoots);
    }

    return resolveFallback(config, baseDir);
  }

  /**
   * Resolves fallback package roots when build config files exist but provide no source roots.
   *
   * <p>Priority order: conventional folders (src/, lib/), then sonar.sources, then base directory.
   */
  private static PackageResolutionResult resolveFallback(Configuration config, File baseDir) {
    List<BiFunction<Configuration, File, Optional<PackageResolutionResult>>> candidates =
      List.of(PackageRootResolver::tryConventionalFolders, PackageRootResolver::trySonarSources);
    for (BiFunction<Configuration, File, Optional<PackageResolutionResult>> candidate : candidates) {
      Optional<PackageResolutionResult> result = candidate.apply(config, baseDir);
      if (result.isPresent()) {
        return result.get();
      }
    }

    LOG.debug("Using project base directory as package root (fallback)");
    return PackageResolutionResult.fromBaseDir(List.of(baseDir.getAbsolutePath()));
  }

  private static Optional<PackageResolutionResult> tryConventionalFolders(Configuration config, File baseDir) {
    List<String> conventionalFolders = findConventionalFolders(baseDir);
    if (conventionalFolders.isEmpty()) {
      return Optional.empty();
    }
    List<String> adjustedRoots = adjustRoots(toAbsolutePaths(conventionalFolders, baseDir), baseDir);
    LOG.debug("Resolved package roots from fallback (conventional folders): {}", adjustedRoots);
    return Optional.of(PackageResolutionResult.fromConventionalFolders(adjustedRoots));
  }

  private static Optional<PackageResolutionResult> trySonarSources(Configuration config, File baseDir) {
    String[] sonarSources = config.getStringArray(SONAR_SOURCES_KEY);
    if (sonarSources.length == 0) {
      return Optional.empty();
    }
    List<String> adjustedRoots = adjustRoots(toAbsolutePaths(Arrays.asList(sonarSources), baseDir), baseDir);
    LOG.debug("Resolved package roots from fallback (sonar.sources): {}", adjustedRoots);
    return Optional.of(PackageResolutionResult.fromSonarSources(adjustedRoots));
  }

  /**
   * Converts path strings to normalized absolute paths.
   *
   * <p>Relative paths are resolved against the given base directory. Windows-style absolute paths
   * (e.g. {@code C:\path\to\src} or {@code C:/path/to/src}) are used as-is without prepending the
   * base directory, which would otherwise produce a nonsensical path on non-Windows systems.
   *
   * <p>Uses {@link Path#normalize()} to resolve {@code .} and {@code ..} components without
   * performing any I/O, so that {@code sonar.sources=.} correctly resolves to the base directory
   * rather than producing an un-normalized path like {@code /project/.}.
   */
  static List<String> toAbsolutePaths(List<String> paths, File baseDir) {
    return paths.stream()
      .map(path -> {
        File file = isWindowsAbsolutePath(path) ? new File(path) : new File(baseDir, path);
        return file.toPath().normalize().toString();
      })
      .toList();
  }

  /**
   * Returns {@code true} if the given path is a Windows-style absolute path (e.g. {@code C:\...}
   * or {@code C:/...}), regardless of the current operating system.
   */
  private static boolean isWindowsAbsolutePath(String path) {
    return path.length() >= 3
      && Character.isLetter(path.charAt(0))
      && path.charAt(1) == ':'
      && (path.charAt(2) == '\\' || path.charAt(2) == '/');
  }

  private static List<String> findConventionalFolders(File baseDir) {
    List<String> folders = new ArrayList<>();
    for (String folderName : CONVENTIONAL_FOLDERS) {
      File folder = new File(baseDir, folderName);
      if (folder.exists() && folder.isDirectory()) {
        folders.add(folderName);
      }
    }
    return folders;
  }

  private static List<String> adjustRoots(List<String> roots, File baseDir) {
    return roots.stream()
      .map(root -> {
        File rootAsFile = new File(root);
        if (rootAsFile.isAbsolute()) {
          // Native absolute path (works on any OS, including Windows running on Windows).
          return adjustPackageRoot(rootAsFile, baseDir);
        }
        if (isWindowsAbsolutePath(root)) {
          // Windows-style absolute path (e.g. C:\src) on a non-Windows system: File.isAbsolute()
          // returns false, so we must not pass it to new File(baseDir, root) or getAbsolutePath()
          // would prepend the JVM working directory. Return as-is; __init__.py traversal is not
          // meaningful for a foreign-OS path.
          return root;
        }
        return adjustPackageRoot(new File(baseDir, root), baseDir);
      })
      .distinct()
      .toList();
  }

  /**
   * Adjusts a package root by walking up the directory tree if it contains __init__.py.
   *
   * <p>If the root directory contains __init__.py, it's part of a package, not the package root.
   * We walk up to find the first parent directory without __init__.py.
   *
   * @param root    the potential package root directory
   * @param baseDir the project base directory (we don't walk above this)
   * @return the adjusted package root absolute path
   */
  @VisibleForTesting
  static String adjustPackageRoot(File root, File baseDir) {
    File current = root;
    String baseDirPath = baseDir.getAbsolutePath();
    while (current != null && !current.getAbsolutePath().equals(baseDirPath)) {
      File initFile = new File(current, "__init__.py");
      if (!initFile.exists()) {
        break;
      }
      current = current.getParentFile();
    }
    if (current == null) {
      return baseDirPath;
    }
    return current.getAbsolutePath();
  }

  /**
   * Recursively finds files with the given filename under the project base directory.
   */
  private static List<File> findFilesRecursively(FileSystem fileSystem, String filename) {
    try (Stream<Path> stream = Files.walk(fileSystem.baseDir().toPath())) {
      return stream
        .filter(Files::isRegularFile)
        .filter(path -> filename.equals(path.getFileName().toString()))
        .map(Path::toFile)
        .toList();
    } catch (IOException e) {
      return List.of();
    }
  }

  /**
   * Determines the combined build system across multiple pyproject.toml results.
   * If multiple files report different build systems, returns MULTIPLE.
   */
  private static PackageResolutionResult.BuildSystem getCombinedBuildSystem(List<PyProjectExtractionResult> pyprojectResults) {
    return pyprojectResults.stream()
      .map(PyProjectExtractionResult::buildSystem)
      .filter(bs -> bs != PackageResolutionResult.BuildSystem.NONE)
      .distinct()
      .reduce((a, b) -> PackageResolutionResult.BuildSystem.MULTIPLE)
      .orElse(PackageResolutionResult.BuildSystem.NONE);
  }
}
