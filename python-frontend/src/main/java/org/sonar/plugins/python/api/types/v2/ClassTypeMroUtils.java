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
package org.sonar.plugins.python.api.types.v2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;

/**
 * C3 linearization (Method Resolution Order) for {@link ClassType}.
 */
final class ClassTypeMroUtils {

  private ClassTypeMroUtils() {
  }

  /**
   * Computes the C3 MRO for {@code cls}, using a fresh memoisation cache.
   */
  static Optional<List<ClassType>> compute(ClassType cls) {
    return compute(cls, new HashMap<>(), new HashSet<>());
  }

  /**
   * Returns whether C3 linearization would succeed for a hypothetical class whose direct bases are
   * exactly {@code bases} in order — same behaviour as {@link ClassType#wouldHaveValidMro(List)}.
   */
  static boolean wouldHaveValidMro(List<ClassType> bases) {
    List<List<ClassType>> lists = new ArrayList<>();
    for (ClassType base : bases) {
      Optional<List<ClassType>> mro = base.mro();
      if (mro.isEmpty()) {
        return true;
      }
      lists.add(new ArrayList<>(mro.get()));
    }
    lists.add(new ArrayList<>(bases));
    return c3Merge(lists) != null;
  }

  /**
   * Recursively computes the C3 MRO for {@code cls}, memoising results in {@code cache}.
   * {@code visiting} guards against cycles.
   */
  private static Optional<List<ClassType>> compute(
    ClassType cls,
    Map<ClassType, Optional<List<ClassType>>> cache,
    Set<ClassType> visiting
  ) {
    if (cache.containsKey(cls)) {
      return cache.get(cls);
    }
    if (!visiting.add(cls)) {
      // Cycle detected — treat as failure.
      return Optional.empty();
    }

    List<ClassType> parentTypes = new ArrayList<>();
    for (TypeWrapper wrapper : cls.superClasses()) {
      if (wrapper.type() instanceof ClassType parent) {
        parentTypes.add(parent);
      }
      // Non-ClassType superclass is already filtered out by hasUnresolvedHierarchy()
    }

    List<List<ClassType>> lists = new ArrayList<>();
    for (ClassType parent : parentTypes) {
      Optional<List<ClassType>> parentMro = compute(parent, cache, visiting);
      if (parentMro.isEmpty()) {
        // A parent itself has an invalid MRO — Python would have raised TypeError
        // when that parent was defined, so this class is not the root cause.
        visiting.remove(cls);
        cache.put(cls, Optional.empty());
        return Optional.empty();
      }
      lists.add(new ArrayList<>(parentMro.get()));
    }
    lists.add(new ArrayList<>(parentTypes));

    List<ClassType> merged = c3Merge(lists);
    Optional<List<ClassType>> result;
    if (merged == null) {
      result = Optional.empty();
    } else {
      merged.add(0, cls);
      result = Optional.of(merged);
    }

    visiting.remove(cls);
    cache.put(cls, result);
    return result;
  }

  /**
   * Performs the C3 merge of the given lists. Returns the merged sequence, or {@code null} if no
   * valid merge exists (i.e. the MRO has a conflict).
   */
  @CheckForNull
  private static List<ClassType> c3Merge(List<List<ClassType>> lists) {
    List<ClassType> result = new ArrayList<>();
    while (true) {
      lists.removeIf(List::isEmpty);
      if (lists.isEmpty()) {
        return result;
      }
      ClassType candidate = findFirstValidMergeHead(lists);
      if (candidate == null) {
        return null;
      }
      result.add(candidate);
      removeHeadFromLists(lists, candidate);
    }
  }

  /**
   * Returns the first list head that does not appear as a non-head entry in any list, or {@code null}
   * if no such head exists (merge failure).
   */
  @CheckForNull
  private static ClassType findFirstValidMergeHead(List<List<ClassType>> lists) {
    for (List<ClassType> list : lists) {
      ClassType head = list.get(0);
      if (!headAppearsAsNonHeadInAnyList(head, lists)) {
        return head;
      }
    }
    return null;
  }

  private static boolean headAppearsAsNonHeadInAnyList(ClassType head, List<List<ClassType>> lists) {
    for (List<ClassType> other : lists) {
      for (int k = 1; k < other.size(); k++) {
        if (other.get(k) == head) {
          return true;
        }
      }
    }
    return false;
  }

  private static void removeHeadFromLists(List<List<ClassType>> lists, ClassType chosen) {
    for (List<ClassType> list : lists) {
      if (!list.isEmpty() && list.get(0) == chosen) {
        list.remove(0);
      }
    }
  }
}
