import abc
from abc import abstractmethod


class Rectangle:
    def __init__(self, width, height):
        self._width = width
        self._height = height
        self._area = None

    @property
    def area(self):  # Noncompliant {{Add a return statement to this property method.}}
        self._width * self._height

    @property
    def empty(self):  # Compliant - pass body is intentionally empty (stub/hook pattern)
        pass

    @property
    def ellipsis_body(self):  # Compliant - ellipsis body is intentionally empty (stub/hook pattern)
        ...

    @property
    def cached_area(self):  # Noncompliant {{Add a return statement to this property method.}}
        result = self._width * self._height
        self._area = result

    @property
    def description(self):  # Noncompliant {{Add a return statement to this property method.}}
        if self._width > self._height:
            value = "landscape"
        else:
            value = "portrait"

    @property
    def perimeter_sum(self):  # Noncompliant {{Add a return statement to this property method.}}
        total = 0
        for side in [self._width, self._height, self._width, self._height]:
            total += side

    @property
    def label(self):  # Noncompliant {{Add a return statement to this property method.}}
        print(f"Rectangle({self._width}x{self._height})")
        self._area = self._width * self._height

    @property
    def dimensions(self):  # Noncompliant {{Add a return statement to this property method.}}
        w = self._width
        h = self._height
        label = f"{w}x{h}"
        self._area = w * h


class Circle:
    def __init__(self, radius):
        self._radius = radius
        self._cache = {}

    @property
    def radius(self):
        return self._radius

    @property
    def diameter(self):
        if self._radius > 0:
            return self._radius * 2
        return 0

    @property
    def area(self):
        raise NotImplementedError("Subclass must implement area")

    @property
    def none_value(self):
        return None

    @property
    def cached_value(self):
        try:
            return self._cache["value"]
        except KeyError:
            return self._radius * 2

    @property
    def early_return(self):
        if self._radius <= 0:
            return 0
        return self._radius * 3.14159

    def compute_area(self):
        self._radius * self._radius * 3.14159


class AbstractShape(abc.ABC):
    @property
    @abstractmethod
    def area(self):
        pass

    @property
    @abc.abstractmethod
    def perimeter(self):
        pass

    @property
    @abstractmethod
    def volume(self):  # Compliant - @abstractmethod with non-empty body
        self._volume = 0

    @property
    @abc.abstractmethod
    def surface(self):  # Compliant - @abc.abstractmethod with non-empty body
        self._surface = 0


class DataStream:
    def __init__(self, data):
        self._data = data

    @property
    def values(self):
        yield from self._data

    @property
    def pairs(self):
        for i, v in enumerate(self._data):
            yield i, v


class NestedFunctionInProperty:
    def __init__(self, value):
        self._value = value

    @property
    def computed(self):  # Noncompliant {{Add a return statement to this property method.}}
        def helper():
            return self._value * 2

    @property
    def with_nested_and_return(self):
        def helper():
            return self._value * 2
        return helper()


class PropertyWithSetterDeleter:
    def __init__(self):
        self._x = 0

    @property
    def x(self):
        return self._x

    @x.setter
    def x(self, value):  # Compliant - setter does not need a return statement
        self._x = value

    @x.deleter
    def x(self):  # Compliant - deleter does not need a return statement
        del self._x


class BareReturnCases:
    def __init__(self):
        self._value = 0

    @property
    def only_bare_return(self):
        return  # Compliant - has a return statement (returns None implicitly)

    @property
    def bare_return_in_branch(self):
        if self._value < 0:
            return  # Compliant - has a return statement
        self._value = 0


class TryCasesWithoutReturn:
    def __init__(self):
        self._data = {}

    @property
    def no_return_in_try(self):  # Noncompliant {{Add a return statement to this property method.}}
        try:
            self._data["key"]  # result not returned, no raise in except
        except KeyError:
            print("missing")

    @property
    def raise_only_in_except(self):  # Compliant - raises in except branch
        try:
            return self._data["key"]
        except KeyError:
            raise ValueError("key not found")

    @property
    def via_finally(self):  # Compliant - return in finally block
        try:
            pass
        finally:
            return self._data.get("key")


def property(func):
    """A user-defined decorator that happens to be named 'property'."""
    return func


class UserDefinedPropertyDecorator:
    def __init__(self):
        self._value = 0

    @property
    def value(self):  # Compliant - built-in 'property' is shadowed by the user-defined decorator above; type matcher correctly excludes this
        self._value * 2


import functools


class FunctoolsCachedProperty:
    def __init__(self, radius):
        self._radius = radius

    @functools.cached_property
    def area(self):  # Compliant - @functools.cached_property is out of scope for this rule
        self._radius * self._radius * 3.14159


# ---- Protocol exemptions ----
from typing import Protocol


class ReadBuffer(Protocol):
    @property
    def mode(self) -> str:
        ...  # Compliant - Protocol stub with ellipsis body

    @property
    def closed(self) -> bool:
        pass  # Compliant - Protocol stub with pass body

    @property
    def name(self) -> str:
        """The name of the buffer."""
        ...  # Compliant - Protocol stub with docstring + ellipsis


class ReadBufferWithMissingReturn(Protocol):
    @property
    def mode(self) -> str:  # Noncompliant {{Add a return statement to this property method.}}
        self._mode = "r"


# ---- ABC subclass exemptions (no @abstractmethod, body is pass/...) ----
class AbstractBase(abc.ABC):
    @property
    def value(self):
        ...  # Compliant - ABC subclass with ellipsis body, conventionally abstract

    @property
    def label(self):
        pass  # Compliant - ABC subclass with pass body, conventionally abstract

    @property
    def description(self):
        """Returns a description."""
        ...  # Compliant - ABC subclass with docstring + ellipsis

    @property
    def computed(self):  # Noncompliant {{Add a return statement to this property method.}}
        result = self._x * 2


class PlainClassWithEmptyBody:
    """A plain class (not ABC, not Protocol) - empty body is still compliant."""
    def __init__(self):
        self._value = 0

    @property
    def stub(self):
        pass  # Compliant - pass body is always exempt regardless of class type

    @property
    def stub_ellipsis(self):
        ...  # Compliant - ellipsis body is always exempt regardless of class type

    @property
    def real_body(self):  # Noncompliant {{Add a return statement to this property method.}}
        self._value * 2
