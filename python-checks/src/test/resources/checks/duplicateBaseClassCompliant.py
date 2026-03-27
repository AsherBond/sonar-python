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

class GenericSubscript(Generic[T]):
    pass

class TwoIdenticalSubscripts(Generic[T], Generic[T]):
    pass

class SubscriptAndName(Generic[T], Base):
    pass

class TwoCallBases(make_base(), make_base()):
    pass

class CallQualifiedBase(obj.method().Attr, obj.method().Attr):
    pass
