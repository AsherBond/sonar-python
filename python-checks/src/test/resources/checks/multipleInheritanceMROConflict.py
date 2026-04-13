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
