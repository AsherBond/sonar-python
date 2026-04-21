class Base:
    pass

class Mid(Base):
    pass

class Leaf(Mid):
    pass


class Broken(Base, Leaf):  # Noncompliant {{Reorder or remove base classes to fix this MRO conflict.}}
#     ^^^^^^ ^^^^< {{This base class is an ancestor of another listed base class appearing after it.}}
    pass


class CommonRoot:
    pass

class LeftChild(CommonRoot):
    pass

class RightChild(CommonRoot):
    pass

class LeftFirst(LeftChild, RightChild):
    pass

class RightFirst(RightChild, LeftChild):
    pass

class C3Conflict(LeftFirst, RightFirst):  # Noncompliant {{Reorder or remove base classes to fix this MRO conflict.}}
    pass


def not_a_class():
    pass


class CompliantPartialResolution(not_a_class, Base):
    pass


class OnlyMetaclass(metaclass=type):
    pass


class CompliantC3Order(LeftFirst, RightChild):
    pass


# --- Super types not fully resolved (heuristic path; full C3 is skipped) ---
from unknown_module import ExternalMixin, ExternalOther


# Compliant: later base not fully resolved for C3 (unknown import / incomplete hierarchy), and the
# heuristic does not show it extending Base.
class MidWithUnresolvedMixin(Base, ExternalMixin):
    pass


# Compliant: two unrelated imported bases (incomplete hierarchies); no "earlier ancestor of later" pair in the model
class CompliantTwoUnresolvedPeers(ExternalMixin, ExternalOther):
    pass


# Bases not fully resolved for C3 (second base has incomplete hierarchy), yet the heuristic still
# finds that the second base subclasses Base — flagged without running full C3.
class ConflictMid(Base, MidWithUnresolvedMixin):  # Noncompliant {{Reorder or remove base classes to fix this MRO conflict.}}
#     ^^^^^^^^^^^ ^^^^< {{This base class is an ancestor of another listed base class appearing after it.}}
    pass


# --- typeshed lies about built-in containers inheriting from collections.abc ABCs ---
# At runtime these built-ins only inherit from `object` and are merely virtual-subclass-registered
# against the corresponding ABCs (via abc.ABCMeta.register). Python's MRO computation succeeds, so
# the rule must NOT flag these.
from collections.abc import (
    MutableMapping,
    Mapping,
    MutableSequence,
    Sequence,
    MutableSet,
    Set as AbstractSet,
)


class CompliantDictMixin(MutableMapping, dict):
    pass


class CompliantMappingDict(Mapping, dict):
    pass


class CompliantListMixin(MutableSequence, list):
    pass


class CompliantTupleMixin(Sequence, tuple):
    pass


class CompliantSetMixin(MutableSet, set):
    pass


class CompliantFrozenSetMixin(AbstractSet, frozenset):
    pass


class CompliantBytearrayMixin(MutableSequence, bytearray):
    pass


# Smoke tests for str/bytes — typeshed lists extra ABCs (Sequence, Hashable, …) that don't appear
# in their runtime MRO.
class CompliantStrMixin(Sequence, str):
    pass


class CompliantBytesMixin(Sequence, bytes):
    pass


# collections.deque has the same typeshed-vs-runtime mismatch as the builtin containers:
# typeshed declares `class deque(MutableSequence[_T])` but at runtime deque.__bases__ == (object,),
# with MutableSequence registered as a virtual subclass via abc.ABCMeta.register().
from collections import deque


class CompliantDequeMixin(MutableSequence, deque):
    pass


# Real conflict that involves a built-in container must still be flagged: dict before its own
# user subclass is a genuine "earlier-is-ancestor-of-later" situation that Python rejects.
class _UserDictSubclass(dict):
    pass


class StillBrokenWithUserDict(dict, _UserDictSubclass):  # Noncompliant {{Reorder or remove base classes to fix this MRO conflict.}}
#     ^^^^^^^^^^^^^^^^^^^^^^^ ^^^^< {{This base class is an ancestor of another listed base class appearing after it.}}
    pass


# The reverse order is valid Python (dict comes after its subclass) and must not be flagged.
class CompliantUserDictThenDict(_UserDictSubclass, dict):
    pass


# Compliant: ABC paired with a user-defined dict subclass. typeshed (falsely) lists MutableMapping
# as ancestor of dict; the rule must not infer the same ancestry through `_UserDictSubclass`.
# Fully-resolved bases — exercises the C3 path (wouldHaveValidMro).
class CompliantAbcThenUserDict(MutableMapping, _UserDictSubclass):
    pass


# Same scenario but with an extra unresolved external mixin to force the heuristic path
# (findAncestorConflictIndex / isOrExtendsClassAtRuntime).
class _UserDictWithUnresolvedMixin(dict, ExternalMixin):
    pass


class CompliantAbcThenUserDictUnresolved(MutableMapping, _UserDictWithUnresolvedMixin):
    pass
