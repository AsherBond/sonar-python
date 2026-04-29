class OnlyLt:  # Noncompliant {{Add the missing comparison methods or use "functools.total_ordering".}}
#     ^^^^^^
    def __lt__(self, other):
#       ^^^^^^< {{"__lt__" is defined here.}}
        return self.age < other.age


class OnlyGt:  # Noncompliant
    def __gt__(self, other):
        return self.value > other.value


class OnlyLe:  # Noncompliant
    def __le__(self, other):
        return self.value <= other.value


class OnlyGe:  # Noncompliant
    def __ge__(self, other):
        return self.value >= other.value


class TwoMethods:  # Noncompliant
#     ^^^^^^^^^^
    def __lt__(self, other):
#       ^^^^^^< {{"__lt__" is defined here.}}
        return self.value < other.value

    def __gt__(self, other):
#       ^^^^^^< {{"__gt__" is defined here.}}
        return self.value > other.value


class ThreeMethods:  # Noncompliant
    def __lt__(self, other):
        return self.value < other.value

    def __le__(self, other):
        return self.value <= other.value

    def __gt__(self, other):
        return self.value > other.value


class EqPlusOneLt:  # Noncompliant
    def __eq__(self, other):
        return self.id == other.id

    def __lt__(self, other):
        return self.id < other.id


class WithUnrelated:  # Noncompliant
    def __len__(self):
        return 0

    def __lt__(self, other):
        return len(self) < len(other)


class OuterWithInner:
    class Inner:  # Noncompliant
        def __lt__(self, other):
            return self.x < other.x


class WithStaticMethod:  # Noncompliant
    @staticmethod
    def __lt__(other):
        return False


# A non-functools decorator does not suppress the rule
def my_ordering(cls):
    return cls

@my_ordering
class WithNonFunctoolsDecorator:  # Noncompliant
    def __lt__(self, other):
        return self.x < other.x


# Compliant: @total_ordering from functools
from functools import total_ordering

@total_ordering
class CompliantWithImport:
    def __eq__(self, other):
        return self.age == other.age

    def __lt__(self, other):
        return self.age < other.age


# Compliant: @functools.total_ordering (qualified)
import functools

@functools.total_ordering
class CompliantQualified:
    def __eq__(self, other):
        return self.degrees == other.degrees

    def __lt__(self, other):
        return self.degrees < other.degrees


# Compliant: @ft.total_ordering via aliased import
import functools as ft

@ft.total_ordering
class CompliantAlias:
    def __eq__(self, other):
        return self.x == other.x

    def __lt__(self, other):
        return self.x < other.x


class AllFourMethods:
    def __lt__(self, other):
        return self.value < other.value

    def __le__(self, other):
        return self.value <= other.value

    def __gt__(self, other):
        return self.value > other.value

    def __ge__(self, other):
        return self.value >= other.value


class NoOrderingMethods:
    def __eq__(self, other):
        return self.x == other.x

    def __ne__(self, other):
        return self.x != other.x


class EmptyClass:
    pass


class OuterCompliant:
    @total_ordering
    class Inner:
        def __eq__(self, other):
            return self.x == other.x

        def __lt__(self, other):
            return self.x < other.x


class WithInnerFunction:  # Noncompliant
    def __lt__(self, other):
        def helper():
            pass
        return self.x < other.x


class ThreeMethodsWithGe:  # Noncompliant
    def __lt__(self, other):
        return self.value < other.value

    def __gt__(self, other):
        return self.value > other.value

    def __ge__(self, other):
        return self.value >= other.value


@my_ordering
@total_ordering
class MultipleDecoratorsCompliant:
    def __eq__(self, other):
        return self.x == other.x

    def __lt__(self, other):
        return self.x < other.x


@total_ordering
@my_ordering
class TotalOrderingFirstCompliant:
    def __eq__(self, other):
        return self.x == other.x

    def __lt__(self, other):
        return self.x < other.x


class ParentWithAllFour:
    def __lt__(self, other):
        return self.value < other.value

    def __le__(self, other):
        return self.value <= other.value

    def __gt__(self, other):
        return self.value > other.value

    def __ge__(self, other):
        return self.value >= other.value


class ChildDefiningOnlyLt(ParentWithAllFour):  # Noncompliant
    def __lt__(self, other):
        return self.value < other.value


@my_ordering
@my_ordering
class TwoNonFunctoolsDecorators:  # Noncompliant
    def __lt__(self, other):
        return self.x < other.x


# Compliant: __lt__ and __gt__ defined as lambda assignments, __le__ and __ge__ as `def`.
# Real-world example: nltk/probability.py (FreqDist).
class MixedDefAndLambda:
    def __le__(self, other):
        return self.value <= other.value

    def __ge__(self, other):
        return self.value >= other.value

    __lt__ = lambda self, other: self <= other and not self.value == other.value
    __gt__ = lambda self, other: self >= other and not self.value == other.value


# Compliant: one `def`-defined method and three lambda-assigned methods.
# Real-world example: nltk/tree/tree.py (Tree).
class OneDefThreeLambdas:
    def __lt__(self, other):
        return self.value < other.value

    __gt__ = lambda self, other: not (self < other or self.value == other.value)
    __le__ = lambda self, other: self < other or self.value == other.value
    __ge__ = lambda self, other: not self < other


# Compliant: all four methods defined via assignments (e.g. function references or partials).
def _lt(self, other): return self.value < other.value
def _le(self, other): return self.value <= other.value
def _gt(self, other): return self.value > other.value
def _ge(self, other): return self.value >= other.value

class AllFourViaAssignments:
    __lt__ = _lt
    __le__ = _le
    __gt__ = _gt
    __ge__ = _ge


# Compliant: annotated assignments with lambdas count toward the four.
from typing import Callable

class AnnotatedLambdaAssignments:
    __lt__: Callable = lambda self, other: self.value < other.value
    __le__: Callable = lambda self, other: self.value <= other.value
    __gt__: Callable = lambda self, other: self.value > other.value
    __ge__: Callable = lambda self, other: self.value >= other.value


# Only one distinct ordering method name even with a duplicate `def` and lambda — should raise.
# The `def` definition is collected first, so it carries the secondary location.
class OneDistinctNameWithDuplicates:  # Noncompliant
#     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    def __lt__(self, other):
#       ^^^^^^< {{"__lt__" is defined here.}}
        return self.value < other.value

    __lt__ = lambda self, other: self.value < other.value


# Duplicate `__lt__` definitions — three distinct names total — should still raise.
# Only the first occurrence of `__lt__` carries a secondary location.
class DuplicateLtMissingGe:  # Noncompliant
#     ^^^^^^^^^^^^^^^^^^^^
    def __lt__(self, other):
#       ^^^^^^< {{"__lt__" is defined here.}}
        return self.value < other.value

    def __lt__(self, other):
        return self.value < other.value

    def __le__(self, other):
#       ^^^^^^< {{"__le__" is defined here.}}
        return self.value <= other.value

    def __gt__(self, other):
#       ^^^^^^< {{"__gt__" is defined here.}}
        return self.value > other.value


# Compliant: duplicate `__lt__` definitions plus all four distinct names — must not raise.
class DuplicateLtAllFourPresent:
    def __lt__(self, other):
        return self.value < other.value

    def __lt__(self, other):
        return self.value < other.value

    def __le__(self, other):
        return self.value <= other.value

    def __gt__(self, other):
        return self.value > other.value

    def __ge__(self, other):
        return self.value >= other.value


# Known FP: rule does not consider comparison methods inherited from parent classes.
# Real-world example: nltk/tree/probabilistic.py (ProbabilisticTree) overrides only __lt__
# while inheriting __gt__/__le__/__ge__ as lambdas from its parent — all four comparisons
# are consistent at runtime, but the rule sees only one method in the subclass body.
# Tracked as a follow-up of SONARPY-3972.
class SubclassOverridingLtWithLambdaParent(MixedDefAndLambda):  # Noncompliant
    def __lt__(self, other):
        return self.value < other.value


# Class-body assignments that are not single-name targets must not affect detection.
# Chained assignments, tuple unpacking, and attribute targets exercise the early-return
# branches in the assignment-target name extraction.
class _Holder:
    attr = 0

class WithUnusualAssignments:
    a = b = 10
    x, y = 1, 2
    _Holder.attr = 5
    _Holder.annotated_attr: int = 5

    def __lt__(self, other):
        return self.value < other.value

    def __le__(self, other):
        return self.value <= other.value

    def __gt__(self, other):
        return self.value > other.value

    def __ge__(self, other):
        return self.value >= other.value


# Annotated assignment without an assigned value: must be ignored (treated as no ordering method).
class AnnotatedWithoutValue:
    __lt__: Callable


# Annotated assignment with a non-ordering method name: must not affect detection.
class AnnotatedNonOrdering:  # Noncompliant
    counter: int = 0

    def __lt__(self, other):
        return self.value < other.value
