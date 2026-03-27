from os import PathLike
import my_class
from my_class import A, B, C, X, mixin
import a
import mod
# Examples with issues:

# Duplicate base with a resolved fully-qualified symbol (exercises the FQN path in expressionKey)
class ResolvedSymbolDuplicate(PathLike, PathLike): # Noncompliant {{Remove this duplicate base class.}}
#                             ^^^^^^^^  ^^^^^^^^< {{Already listed here.}}
    pass

class SimpleEndDuplicate(A, B, A): # Noncompliant {{Remove this duplicate base class.}}
#                        ^     ^< {{Already listed here.}}
    pass

class MidAndEndDuplicate(A, B, my_class.B): # Noncompliant {{Remove this duplicate base class.}}
#                           ^  ^^^^^^^^^^< {{Already listed here.}}
    pass

class QualifiedDuplicate(mod.Base, Other, mod.Base): # Noncompliant {{Remove this duplicate base class.}}
#                        ^^^^^^^^         ^^^^^^^^< {{Already listed here.}}
    pass

# One issue with two secondaries (2nd and 3rd are duplicates of 1st)
class TripleDuplicate(A, A, A): # Noncompliant {{Remove this duplicate base class.}}
#                     ^  ^< {{Already listed here.}}
#                           ^@-1< {{Already listed here.}}
    pass

# metaclass keyword argument is skipped, duplicate among positional bases still flagged
class WithMetaclassDuplicate(A, B, A, metaclass=Meta): # Noncompliant
    pass

# Two separate groups of duplicates (2 issues: one for A, one for B)
class TwoPairsDuplicates(A, B, A, B): # Noncompliant 2
    pass

class ManyBasesDuplicate(A, B, C, D, B): # Noncompliant
    pass

class DeepQualifiedDuplicate(a.b.C, D, a.b.C): # Noncompliant
    pass

# One issue with two secondaries (2nd and 3rd are duplicates of 1st)
class ThreeTimeDuplicate(mixin, mixin, mixin): # Noncompliant
#                        ^^^^^  ^^^^^< ^^^^^<
    pass

# Star-unpacking is skipped; duplicate among remaining RegularArguments is still detected
class StarAndDuplicate(*extra, A, A): # Noncompliant
    pass

# Name with a symbol but no fully-qualified name (parameter symbol has no FQN)
def make_class(other):
    class Inner(other, other): # FN
        pass

# Compliant examples:

class NoBaseClass:
    pass

class SingleBase(A):
    pass

class MultipleUnique(A, B, C):
    pass

class MultipleUniqueQualified(mod.A, mod.B, mod.C):
    pass

class OnlyMetaclass(metaclass=Meta):
    pass

class UniqueBasesWithMetaclass(A, B, metaclass=Meta):
    pass

# Subscript expressions yield a null key, so duplicates are not detected
class GenericSubscript(Generic[T]):
    pass

class TwoIdenticalSubscripts(Generic[T], Generic[T]):
    pass

class SubscriptAndName(Generic[T], Base):
    pass

# Call expressions yield a null key, so duplicates are not detected
class TwoCallBases(make_base(), make_base()):
    pass

# Qualified name whose qualifier is a call result — null key, no issue
class CallQualifiedBase(obj.method().Attr, obj.method().Attr):
    pass

# Deep qualified expression where an intermediate qualifier is a call result — null key via recursive path
class DeepCallQualifiedBase(a.b().c.D, a.b().c.D):
    pass
