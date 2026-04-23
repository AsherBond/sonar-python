def empty_literals(x):
    if x in []: ...  # Noncompliant {{Remove this membership test on an empty collection; it will always be the same value.}}
    #  ^^^^^^^
    if x in {}: ...  # Noncompliant
    #  ^^^^^^^
    if x in (): ...  # Noncompliant
    #  ^^^^^^^
    if x not in []: ...  # Noncompliant
    #  ^^^^^^^^^^^
    if x not in {}: ...  # Noncompliant
    if x not in (): ...  # Noncompliant


def empty_constructor_calls(x):
    if x in set(): ...  # Noncompliant
    #  ^^^^^^^^^^
    if x in tuple(): ...  # Noncompliant
    if x in frozenset(): ...  # Noncompliant
    if x not in set(): ...  # Noncompliant
    if x not in tuple(): ...  # Noncompliant
    if x not in frozenset(): ...  # Noncompliant


def outside_conditions(x, values):
    # The rule fires on any membership test, not only those used as a condition.
    result = x in []  # Noncompliant
    flag = x not in set()  # Noncompliant
    values = [y for y in range(10) if y in ()]  # Noncompliant


def compliant_non_empty_literals(x):
    if x in [1, 2, 3]: ...
    if x in {"a": 1}: ...
    if x in {1, 2}: ...
    if x in (1,): ...
    if x in (1, 2): ...
    if x not in [1, 2, 3]: ...


def compliant_constructor_calls_with_arguments(x, iterable):
    if x in set(iterable): ...
    if x in tuple(iterable): ...
    if x in frozenset(iterable): ...
    if x in list(iterable): ...
    if x in dict(iterable): ...


def compliant_list_dict_builtin_calls(x):
    # list() / dict() are not covered by this rule. Use [] / {} instead.
    if x in list(): ...
    if x in dict(): ...


def compliant_variables(x, values):
    if x in values: ...
    my_list = []
    if x in my_list: ...  # Empty but through a variable - out of scope here


def compliant_unpacking(x, p1, p2):
    if x in [*p1, *p2]: ...
    if x in {**p1}: ...
    if x in {*p1}: ...
    if x in (*p1,): ...


def compliant_shadowed_builtins(x):
    def set():  # user-defined function, shadows builtin
        return [1, 2]

    if x in set(): ...  # Not the builtin set
