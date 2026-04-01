# Tests: noncompliant cases


def calculate_stats(numbers): # Noncompliant {{Refactor this function to always return tuples of the same length.}}
#   ^^^^^^^^^^^^^^^
    if not numbers:
        return (0,)
#       ^^^^^^^^^^^< {{Returns a 1-tuple.}}
    total = sum(numbers)
    average = total / len(numbers)
    return (total, average)
#   ^^^^^^^^^^^^^^^^^^^^^^^< {{Returns a 2-tuple.}}


def early_guard(x): # Noncompliant {{Refactor this function to always return tuples of the same length.}}
#   ^^^^^^^^^^^
    if x < 0:
        return (x,)
#       ^^^^^^^^^^^< {{Returns a 1-tuple.}}
    result = x * 2
    extra = x + 1
    return (x, result, extra)
#   ^^^^^^^^^^^^^^^^^^^^^^^^^< {{Returns a 3-tuple.}}


def classify_value(x): # Noncompliant {{Refactor this function to always return tuples of the same length.}}
#   ^^^^^^^^^^^^^^
    if x > 0:
        return (x, "positive", True)
#       ^^^^^^^^^^^^^^^^^^^^^^^^^^^^< {{Returns a 3-tuple.}}
    elif x < 0:
        return (x, "negative", False)
#       ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^< {{Returns a 3-tuple.}}
    else:
        return (x,)
#       ^^^^^^^^^^^< {{Returns a 1-tuple.}}


def resolve_address(host, port, secure): # Noncompliant {{Refactor this function to always return tuples of the same length.}}
#   ^^^^^^^^^^^^^^^
    if secure:
        return (host, port, "https")
#       ^^^^^^^^^^^^^^^^^^^^^^^^^^^^< {{Returns a 3-tuple.}}
    return (host, port)
#   ^^^^^^^^^^^^^^^^^^^< {{Returns a 2-tuple.}}


# Class method with inconsistent returns
class DataProcessor:
    def extract(self, data): # Noncompliant {{Refactor this function to always return tuples of the same length.}}
#       ^^^^^^^
        if not data:
            return (None, 0)
#           ^^^^^^^^^^^^^^^^< {{Returns a 2-tuple.}}
        value = data[0]
        count = len(data)
        label = str(value)
        return (value, count, label)
#       ^^^^^^^^^^^^^^^^^^^^^^^^^^^^< {{Returns a 3-tuple.}}


# Async function with inconsistent returns
async def fetch_resource(url, timeout): # Noncompliant {{Refactor this function to always return tuples of the same length.}}
#         ^^^^^^^^^^^^^^
    if timeout <= 0:
        return (url, "error")
#       ^^^^^^^^^^^^^^^^^^^^^< {{Returns a 2-tuple.}}
    content = "data"
    status = 200
    latency = 0.1
    return (url, content, status, latency)
#   ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^< {{Returns a 4-tuple.}}


# Implicit tuple syntax vs explicit tuple syntax
def implicit_vs_explicit(items): # Noncompliant {{Refactor this function to always return tuples of the same length.}}
#   ^^^^^^^^^^^^^^^^^^^^
    for i, item in enumerate(items):
        if item == 0:
            return i, item
#           ^^^^^^^^^^^^^^< {{Returns a 2-tuple.}}
    return (len(items), items[-1], "last")
#   ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^< {{Returns a 3-tuple.}}


# Try/except with inconsistent returns
def safe_divide(a, b): # Noncompliant {{Refactor this function to always return tuples of the same length.}}
#   ^^^^^^^^^^^
    try:
        result = a / b
        return (result, None)
#       ^^^^^^^^^^^^^^^^^^^^^< {{Returns a 2-tuple.}}
    except ZeroDivisionError as e:
        return (None, str(e), True)
#       ^^^^^^^^^^^^^^^^^^^^^^^^^^^< {{Returns a 3-tuple.}}


# Multiple branches, one outlier
def parse_config(config): # Noncompliant {{Refactor this function to always return tuples of the same length.}}
#   ^^^^^^^^^^^^
    if "host" not in config:
        return ("localhost", 8080)
#       ^^^^^^^^^^^^^^^^^^^^^^^^^^< {{Returns a 2-tuple.}}
    host = config["host"]
    port = config.get("port", 80)
    if "user" in config:
        return (host, port, config["user"])
#       ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^< {{Returns a 3-tuple.}}
    return (host, port)
#   ^^^^^^^^^^^^^^^^^^^< {{Returns a 2-tuple.}}


# Loop body with inconsistent returns
def find_pair(items, target): # Noncompliant {{Refactor this function to always return tuples of the same length.}}
#   ^^^^^^^^^
    for i, item in enumerate(items):
        if item == target:
            return (i, item)
#           ^^^^^^^^^^^^^^^^< {{Returns a 2-tuple.}}
        if item > target:
            return (i,)
#           ^^^^^^^^^^^< {{Returns a 1-tuple.}}
    return (len(items), None)
#   ^^^^^^^^^^^^^^^^^^^^^^^^^< {{Returns a 2-tuple.}}


# ##############################################################################
# Compliant patterns
# ##############################################################################


# All returns produce 2-tuples — consistent
def calculate_stats_fixed(numbers):
    if not numbers:
        return (0, 0)
    total = sum(numbers)
    average = total / len(numbers)
    return (total, average)


# Single return statement — no inconsistency possible
def make_pair(a, b):
    return (a, b)


# Only one tuple-returning return statement; the other returns None
def find_element(items, target):
    for i, item in enumerate(items):
        if item == target:
            return (i, item)
    return None


# Returns a scalar on all paths — rule does not apply (no tuple literals)
def double(x):
    if x > 0:
        return x * 2
    return 0


# Returns a list on both paths — rule does not apply (list is not a tuple)
def get_items(flag):
    if flag:
        return [1, 2, 3]
    return [4, 5]


# Empty tuple consistently — no inconsistency
def always_empty(condition):
    if condition:
        return ()
    return ()


# Generator function — rule does not apply
def generate_pairs(items):
    for i, item in enumerate(items):
        yield (i, item)


# Function with no return statements — no issue
def do_nothing():
    pass


# NamedTuple usage — returns a call expression, not a raw tuple literal
from typing import NamedTuple


class Stats(NamedTuple):
    total: float
    average: float


def calculate_named_stats(numbers):
    if not numbers:
        return Stats(total=0, average=0)
    total = sum(numbers)
    average = total / len(numbers)
    return Stats(total=total, average=average)


# Async function with consistent 2-tuples — no issue
async def fetch_resource_ok(url, timeout):
    if timeout <= 0:
        return (url, "timeout")
    content = "data"
    return (url, content)


# Nested function definitions — outer has consistent returns; inner is flagged independently
def outer_consistent(x):
    def inner_inconsistent(y): # Noncompliant {{Refactor this function to always return tuples of the same length.}}
#       ^^^^^^^^^^^^^^^^^^
        if y > 0:
            return (y,)
#           ^^^^^^^^^^^< {{Returns a 1-tuple.}}
        return (y, y * 2)
#       ^^^^^^^^^^^^^^^^^< {{Returns a 2-tuple.}}

    return (x, inner_inconsistent)


# Bare return plus one tuple return — only one tuple return, no inconsistency
def maybe_tuple(x):
    if x is None:
        return
    return (x, x + 1)


# Variable assigned a tuple and returned — rule only detects literal tuple syntax
# at the return site, so this is a false negative (accepted limitation)
def tuple_via_variable(a, b, c):
    t2 = (a, b)
    t3 = (a, b, c)
    if a > 0:
        return t2
    return t3


# Dynamic tuple construction — accepted false negative
def tuple_via_constructor(values):
    if values:
        return tuple(values)
    return tuple(range(3))


# Star-unpacking in tuple — length is dynamic, so rule does not flag this
def star_tuple(prefix, value):
    if prefix:
        return (*prefix, value)
    return (value,)


# ##############################################################################
# Edge cases
# ##############################################################################


# No arguments — function has no returns at all
def no_body():
    ...


# Only non-tuple returns — rule does not apply even though lengths would differ
# if interpreted as tuples
def non_tuple_paths(x):
    if x > 0:
        return x
    elif x < 0:
        return -x
    return 0


# Single tuple return alongside multiple non-tuple returns — only one tuple
def mixed_non_tuple_and_one_tuple(x):
    if x > 100:
        return None
    if x < 0:
        return -1
    return (x, x * 2)


# Lambda inside function — lambda returns must not be collected
def outer_with_lambda(items):
    transform = lambda x: (x,) if x > 0 else (x, x - 1)
    if items:
        return (items[0], len(items))
    return (None, 0)


# Nested function is itself noncompliant; outer is compliant — inner's returns
# must not pollute the outer function's analysis
def outer_compliant_inner_noncompliant(data):
    def inner(x): # Noncompliant {{Refactor this function to always return tuples of the same length.}}
#       ^^^^^
        if x:
            return (x,)
#           ^^^^^^^^^^^< {{Returns a 1-tuple.}}
        return (x, 0)
#       ^^^^^^^^^^^^^< {{Returns a 2-tuple.}}
    result = inner(data)
    if data:
        return (data, True)
    return (None, False)


# Implicit tuple with single element — `return x,` is a 1-tuple
def implicit_single_element(x):
    if x > 0:
        return x,
    return x,


# Try/except/else/finally — only tuple returns matter, bare returns are ignored
def try_with_bare_return(x):
    try:
        result = 1 / x
        return (result, "ok")
    except ZeroDivisionError:
        return
    finally:
        pass


# Multiple return values all the same length using implicit syntax
def implicit_consistent(a, b, c):
    if a > 0:
        return a, b
    if b > 0:
        return b, c
    return c, a


# Star-unpacking in implicit tuple — length is dynamic, so rule does not flag this
def star_implicit_tuple(prefix, value):
    if prefix:
        return *prefix, value
    return (value,)
