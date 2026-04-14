def generator_basic():
    yield 1
    yield 2
    raise StopIteration  # Noncompliant {{Replace this "raise StopIteration" with a "return" statement.}}

def generator_with_int_arg():
    yield 1
    raise StopIteration(42)  # Noncompliant {{Replace this "raise StopIteration" with a "return" statement.}}

def generator_with_str_arg():
    yield 1
    raise StopIteration("message")  # Noncompliant {{Replace this "raise StopIteration" with a "return" statement.}}

def generator_raise_in_if(condition):
    yield 1
    if condition:
        raise StopIteration  # Noncompliant

def generator_raise_in_else(condition):
    yield 1
    if condition:
        pass
    else:
        raise StopIteration  # Noncompliant

def generator_raise_in_for(items):
    for item in items:
        if item is None:
            raise StopIteration  # Noncompliant
        yield item

def generator_raise_in_while():
    count = 0
    while count < 10:
        if count == 5:
            raise StopIteration  # Noncompliant
        yield count
        count += 1

def generator_raise_in_try():
    yield 1
    try:
        raise StopIteration  # Noncompliant
    except ValueError:
        pass

def generator_raise_in_except():
    yield 1
    try:
        pass
    except StopIteration:
        raise StopIteration  # Noncompliant

def generator_raise_in_finally():
    yield 1
    try:
        pass
    finally:
        raise StopIteration  # Noncompliant

def generator_raise_chained():
    yield 1
    raise StopIteration from ValueError("cause")  # Noncompliant

async def async_generator_raise():
    yield 1
    raise StopIteration  # Noncompliant

def generator_after_yield_from():
    yield from range(3)
    raise StopIteration  # Noncompliant

def generator_nested_control_flow(data):
    for item in data:
        yield item
        if item > 100:
            if item > 1000:
                raise StopIteration  # Noncompliant


def generator_return():
    yield 1
    yield 2
    return

def generator_return_value():
    yield 1
    return 42

def generator_natural_exhaustion():
    yield 1
    yield 2

def regular_function():
    raise StopIteration

class ManualIterator:
    def __init__(self):
        self.count = 0

    def __next__(self):
        self.count += 1
        if self.count > 3:
            raise StopIteration
        return self.count

def helper_raises():
    raise StopIteration

def generator_calls_helper():
    yield 1
    helper_raises()

def outer_generator_nested_helper():
    def inner_helper():
        raise StopIteration
    yield 1
    inner_helper()

def generator_catches_stop_iteration():
    yield 1
    try:
        pass
    except StopIteration:
        return

def generator_raises_value_error():
    yield 1
    raise ValueError("not a stop iteration")

def generator_bare_raise():
    yield 1
    try:
        pass
    except Exception:
        raise

raise StopIteration

def generator_deeply_nested_helper():
    def inner_non_generator():
        def innermost():
            raise StopIteration
        innermost()
    yield 1
    inner_non_generator()

async def async_generator_catches():
    yield 1
    try:
        pass
    except StopIteration:
        return


class IteratorClass:
    def generator_method(self):
        yield 1
        raise StopIteration  # Noncompliant


def nested_generators():
    def inner_generator():
        yield 10
        raise StopIteration  # Noncompliant
    yield 1
    raise StopIteration  # Noncompliant


class CustomStopIteration(StopIteration):
    pass

def generator_raises_stop_iteration_subclass():
    yield 1
    raise CustomStopIteration


def generator_raises_stop_iteration_subclass_instance():
    yield 1
    raise CustomStopIteration("done")


def generator_raise_assigned_variable():
    yield 1
    exc = StopIteration("error")
    raise exc  # Noncompliant


async def async_generator_yield_from():
    yield from range(3)
    raise StopIteration  # Noncompliant


def generator_yield_in_nested_loop(items, stop_early):
    # Real-world pattern: yield inside nested for loop, raise StopIteration in elif branch
    for item in items:
        if isinstance(item, list):
            for x in item:
                yield x
        elif stop_early:
            raise StopIteration  # Noncompliant
        else:
            yield item
