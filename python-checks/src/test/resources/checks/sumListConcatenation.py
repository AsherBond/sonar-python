list_of_lists = [[1, 2], [3, 4]]


def noncompliant_empty_list_as_start():
    sum(list_of_lists, [])  # Noncompliant {{Use "itertools.chain.from_iterable()" instead of "sum()" to flatten or concatenate lists.}}
#   ^^^^^^^^^^^^^^^^^^^^^^


def noncompliant_start_keyword_argument():
    sum(list_of_lists, start=[])  # Noncompliant {{Use "itertools.chain.from_iterable()" instead of "sum()" to flatten or concatenate lists.}}
#   ^^^^^^^^^^^^^^^^^^^^^^^^^^^^


def compliant_callee_is_not_builtin_sum(other_sum):
    def sum(iterable, start):
        result = start
        for item in iterable:
            result = result + item
        return result

    sum([[1, 2]], [])
    other_sum([[1, 2]], [])


def compliant_no_start_argument():
    sum([1, 2, 3])
    sum()


def compliant_start_not_empty_list_literal():
    sum(list_of_lists, [0])


def compliant_start_not_list_literal():
    sum(list_of_lists, 10)


def fn_start_not_syntactic_empty_list():  # FN: only literal []; Name, list(), **kwargs, starred unpack
    empty = []
    sum(list_of_lists, empty)
    sum(list_of_lists, list())
    kwargs = {"start": []}
    sum(list_of_lists, **kwargs)
    sum(list_of_lists, *[[]])
