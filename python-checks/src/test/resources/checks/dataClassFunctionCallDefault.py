import dataclasses
import random
import uuid
import time
import secrets
import os
from dataclasses import dataclass, field
from datetime import datetime, date
from typing import ClassVar


# Known problematic factory calls are flagged

@dataclass
class Event:
    timestamp: datetime = datetime.now()  # Noncompliant {{Use "field(default_factory=...)" instead of a function call as a default value.}}
#                         ^^^^^^^^^^^^^^
    event_id: str = uuid.uuid4()  # Noncompliant
    today: date = date.today()  # Noncompliant
    started_at: float = time.time()  # Noncompliant
    token: str = secrets.token_hex(16)  # Noncompliant
    rand_bytes: bytes = os.urandom(16)  # Noncompliant


# random.* top-level functions are aliased from a Random() instance in typeshed
# (`randint = _inst.randint`), so isType("random.randint") resolves to Unknown.
# We work around this by matching on the random module qualifier and the function name.

@dataclass
class EventWithRandom:
    number: int = random.randint(1, 100)  # Noncompliant
#                 ^^^^^^^^^^^^^^^^^^^^^^
    pick: int = random.choice([1, 2, 3])  # Noncompliant
    coin: float = random.random()  # Noncompliant


# Aliased imports: `from random import randint as r` makes the callee just `r`,
# not a QualifiedExpression on the random module — accepted FN.

from random import randint as _aliased_randint

@dataclass
class EventWithAliasedRandom:
    number: int = _aliased_randint(1, 100)  # Compliant: accepted FN (no random qualifier)


# Method calls on a Random() instance: the callee qualifier is not the random module,
# so the syntactic workaround does not fire. The full type chain DOES resolve here
# (random.Random.randint is a known FunctionType) but it is not in the allowlist.

@dataclass
class EventWithRandomInstance:
    rng: random.Random = random.Random()  # Compliant: constructor of a frozen-ish helper
    n: int = random.Random().randint(1, 100)  # Compliant: accepted FN (qualifier is a Random instance)


# Mutable container constructor calls are flagged — each instance would share the same container

@dataclass
class EventWithContainers:
    items: list = list()  # Noncompliant
    mapping: dict = dict()  # Noncompliant
    tags: set = set()  # Noncompliant
    buffer: bytearray = bytearray()  # Noncompliant


# Mutable literal defaults are flagged

@dataclass
class EventWithLiterals:
    items: list = []  # Noncompliant
#                 ^^
    mapping: dict = {}  # Noncompliant
    tags: set = {1, 2, 3}  # Noncompliant


# dataclasses.dataclass qualified name

@dataclasses.dataclass
class Record:
    created: datetime = datetime.now()  # Noncompliant


# dataclass with arguments (frozen, eq, ...)

@dataclass(frozen=True)
class FrozenEvent:
    timestamp: datetime = datetime.now()  # Noncompliant


@dataclasses.dataclass(eq=True, order=True)
class OrderedEvent:
    created: datetime = datetime.now()  # Noncompliant


# Compliant: call to a user-defined function — not in the allowlist.
# The allowlist-based approach accepts FNs here in exchange for FP-free behavior.

def compute_default():
    return 42

@dataclass
class WithUserCall:
    value: int = compute_default()  # Compliant: accepted FN


# Compliant: field(default_factory=...)

@dataclass
class CompliantEvent:
    timestamp: datetime = field(default_factory=datetime.now)
    event_id: str = field(default_factory=lambda: str(uuid.uuid4()))
    items: list = field(default_factory=list)


# Compliant: field(default=...) with a constant

@dataclass
class FieldWithDefault:
    value: int = field(default=0)
    name: str = field(default="foo")


# field(default=problematic_call()) is flagged — the inner call is evaluated once at
# class definition, same bug as writing the call directly

@dataclass
class FieldWithProblematicDefault:
    timestamp: datetime = field(default=datetime.now())  # Noncompliant
#                                       ^^^^^^^^^^^^^^
    event_id: str = field(default=uuid.uuid4())  # Noncompliant
    today: date = dataclasses.field(default=date.today())  # Noncompliant
    items: list = field(default=[])  # Noncompliant
    mapping: dict = field(default={})  # Noncompliant
    more_items: list = field(default=list())  # Noncompliant
    number: int = field(default=random.randint(1, 100))  # Noncompliant


# Compliant: field(default=...) with a non-problematic call (user-defined helper not in allowlist)

@dataclass
class FieldWithSafeDefault:
    value: int = field(default=compute_default())  # Compliant: accepted FN


# Compliant: dataclasses.field(...) qualified

@dataclass
class QualifiedField:
    value: int = dataclasses.field(default_factory=int)


# Compliant: constant default values

@dataclass
class Constants:
    integer: int = 0
    string: str = "hello"
    flag: bool = True
    nothing: None = None
    floating: float = 1.5
    tuple_val: tuple = ()


# Compliant: no default value

@dataclass
class NoDefault:
    name: str
    age: int


# Compliant: non-dataclass class — rule only applies to dataclasses

class RegularClass:
    timestamp: datetime = datetime.now()
    event_id: str = str(uuid.uuid4())


# Compliant: class variable (ClassVar) — intentionally class-level

@dataclass
class WithClassVar:
    shared: ClassVar[datetime] = datetime.now()


# Nested dataclass

@dataclass
class Outer:
    name: str = "foo"

    @dataclass
    class Inner:
        timestamp: datetime = datetime.now()  # Noncompliant


# Compliant: attribute access (not a call) on a call result is not the top-level expression

CONSTANT = 42

@dataclass
class WithAttributeAccess:
    value: int = CONSTANT


# Compliant: arithmetic on constants

@dataclass
class WithBinaryOps:
    value: int = 1 + 2
    other: int = -1


# Method body: only attributes annotated at class level are considered

@dataclass
class NoAnnotation:
    name: str = "foo"

    def method(self):
        local_var = datetime.now()
        return local_var


# Unknown decorator (not dataclass) — compliant

@unknown_decorator
class NotADataclass:
    timestamp: datetime = datetime.now()


# Regular assignment (not annotated) — no type annotation, not a dataclass field

@dataclass
class RegularAssignment:
    x = datetime.now()  # Compliant: without type annotation, not a dataclass field


# Compliant: user-defined classmethod helper named "field" that wraps
# dataclasses.field(default_factory=...). Common in LibCST (CSTNode.field).
# Under the allowlist philosophy, this is naturally compliant because the callee's
# FQN is not in the problematic list.

class Base:
    @classmethod
    def field(cls, *args, **kwargs):
        return field(default_factory=lambda: cls(*args, **kwargs))


@dataclass(frozen=True)
class Derived(Base):
    value: str = ""


@dataclass
class UsingFieldHelper:
    derived: Derived = Derived.field("")  # Compliant: wrapper around dataclasses.field
    other: Derived = Base.field("")  # Compliant: wrapper around dataclasses.field


# Compliant: constructor call (user-defined frozen dataclass). The constructor is not in
# the allowlist, so we accept this as an FN. Frozen dataclass instances are immutable,
# so sharing the default across instances is usually safe anyway.

@dataclass(frozen=True)
class Shared:
    name: str = "x"


@dataclass
class UsesShared:
    inner: Shared = Shared()  # Compliant: accepted FN


# Time via aliased import is still flagged — FQN still resolves to time.time

from time import time as now_ts

@dataclass
class WithAliasedTime:
    started: float = now_ts()  # Noncompliant


# Compliant: function call whose FQN cannot be resolved — unknown callee means no match

def _maker():
    return datetime.now()

@dataclass
class WithUnknownCallee:
    ts: datetime = _maker()  # Compliant: accepted FN (callee not in allowlist)
