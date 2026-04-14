from dataclasses import dataclass
from enum import Enum, IntEnum, Flag, IntFlag, StrEnum
import dataclasses


# @dataclass on Enum - Noncompliant cases

@dataclass  # Noncompliant {{Remove this "@dataclass" decorator; it is incompatible with Enum classes.}}
class Status(Enum):
    PENDING = 1
    APPROVED = 2
    REJECTED = 3


# @dataclass with arguments

@dataclass(frozen=True)  # Noncompliant
class Color(Enum):
    RED = 1
    GREEN = 2
    BLUE = 3


# Using dataclasses.dataclass qualified name

@dataclasses.dataclass  # Noncompliant
class Direction(Enum):
    NORTH = "N"
    SOUTH = "S"
    EAST = "E"
    WEST = "W"


@dataclasses.dataclass(eq=True, order=True)  # Noncompliant
class Priority(Enum):
    LOW = 1
    MEDIUM = 2
    HIGH = 3


# @dataclass on IntEnum and Flag (transitively extend Enum)

@dataclass  # Noncompliant
class ErrorCode(IntEnum):
    NOT_FOUND = 404
    SERVER_ERROR = 500


@dataclass  # Noncompliant
class Permission(Flag):
    READ = 1
    WRITE = 2
    EXECUTE = 4


@dataclass  # Noncompliant
class FileMode(IntFlag):
    READ = 4
    WRITE = 2
    EXECUTE = 1


# @dataclass on StrEnum (Python 3.11+)

@dataclass  # Noncompliant
class HttpMethod(StrEnum):
    GET = "GET"
    POST = "POST"


# @dataclass on an indirect Enum subclass

class BaseStatus(Enum):
    pass

@dataclass  # Noncompliant
class ExtendedStatus(BaseStatus):
    PENDING = 1
    ACTIVE = 2


# @dataclass among multiple decorators

def my_decorator(cls):
    return cls

@my_decorator
@dataclass  # Noncompliant
class State(Enum):
    OPEN = "open"
    CLOSED = "closed"


@dataclass  # Noncompliant
@my_decorator
class Stage(Enum):
    START = 1
    END = 2


# @dataclass with multiple keyword arguments

@dataclass(repr=False, eq=False)  # Noncompliant
class Suit(Enum):
    HEARTS = 1
    DIAMONDS = 2
    CLUBS = 3
    SPADES = 4


# @dataclass on class with multiple bases including Enum

class Mixin:
    pass

@dataclass  # Noncompliant
class Mixed(Mixin, Enum):
    FIRST = 1
    SECOND = 2


# Compliant: Enum without @dataclass

class CompliantStatus(Enum):
    PENDING = 1
    APPROVED = 2
    REJECTED = 3


class CompliantErrorCode(IntEnum):
    NOT_FOUND = 404
    SERVER_ERROR = 500


class CompliantPermission(Flag):
    READ = 1
    WRITE = 2
    EXECUTE = 4


class CompliantHttpMethod(StrEnum):
    GET = "GET"
    POST = "POST"


# Compliant: @dataclass on a plain class (not Enum)

@dataclass
class Point:
    x: float
    y: float


@dataclass(frozen=True)
class Config:
    host: str
    port: int


@dataclasses.dataclass
class Rectangle:
    width: float
    height: float


# Compliant: custom (non-dataclass) decorator on an Enum

@my_decorator
class DecoratedStatus(Enum):
    ACTIVE = 1
    INACTIVE = 2


# Compliant: Enum subclass without @dataclass

class CompliantExtended(BaseStatus):
    ACTIVE = 1
    INACTIVE = 2


# Compliant: unknown base class — cannot confirm Enum inheritance

@dataclass
class UnknownBase(UnknownParent):
    value: int = 0


# Compliant: unknown decorator on an Enum — not a dataclass

@unknown_decorator
class StatusWithUnknownDecorator(Enum):
    ACTIVE = 1
    INACTIVE = 2
