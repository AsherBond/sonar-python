
def noncompliant_basic():
    lst = ['apple', 'banana', 'cherry']
    for i, _ in enumerate(lst):  # Noncompliant {{Unpack the value from 'enumerate()' directly instead of using an index lookup.}}
#               ^^^^^^^^^^^^^^
        print(lst[i])


def noncompliant_start_zero_keyword():
    colors = ['red', 'green', 'blue']
    for i, _ in enumerate(colors, start=0):  # Noncompliant
        print(colors[i])


def noncompliant_rhs_of_augmented_assignment():
    items = "abcdef"
    result = ""
    for ind, _ in enumerate(items):  # Noncompliant
        result += items[ind]


def compliant_proper_unpacking():
    fruits = ['apple', 'banana', 'cherry']
    for i, fruit in enumerate(fruits):
        print(f"Index {i}: {fruit}")


def compliant_multiple_iterables():
    lst = [1, 2, 3]
    for i in lst, lst:
        print(i)


def compliant_plain_iterable_not_call():
    pairs = [(1, 'a'), (2, 'b'), (3, 'c')]
    for i, v in pairs:
        print(i, v)


def compliant_non_enumerate_call():
    items = [1, 2, 3]
    others = [4, 5, 6]
    for i, v in zip(items, others):
        print(items[i])


def compliant_single_loop_variable():
    items = ['a', 'b', 'c']
    for item in enumerate(items):
        print(item)


def compliant_tuple_unpacking_first_var():
    pairs = [(1, 'a'), (2, 'b')]
    for (a, b), value in enumerate(pairs):
        print(a, b, value)


def compliant_nonzero_start():
    items = ['a', 'b', 'c']
    for i, _ in enumerate(items, start=1):
        print(items[i - 1])


def compliant_variable_start():
    items = ['a', 'b', 'c']
    n = 0
    for i, _ in enumerate(items, n):
        print(items[i])


def compliant_no_arguments():
    for i, _ in enumerate():
        print(i)


def compliant_inline_literal_iterable():
    for i, _ in enumerate([1, 2, 3]):
        pass


def compliant_undeclared_iterable():
    for i, _ in enumerate(undeclared_var):
        print(undeclared_var[i])


def compliant_write_assignment():
    items = ['a', 'b', 'c']
    for i, item in enumerate(items):
        items[i] = item.upper()


def compliant_write_augmented_assignment():
    counts = [0, 0, 0]
    for i, val in enumerate(counts):
        counts[i] += 1


def compliant_write_del():
    items = ['a', 'b', 'c']
    for i, item in enumerate(items):
        if item == 'b':
            del items[i]
            break


def compliant_different_list():
    keys = ['x', 'y', 'z']
    values = [1, 2, 3]
    for i, key in enumerate(keys):
        print(values[i])


def compliant_different_index_var():
    lst = ['a', 'b', 'c']
    j = 0
    for i, _ in enumerate(lst):
        print(lst[j])


def compliant_modified_index():
    items = ['a', 'b', 'c']
    for i, _ in enumerate(items):
        if i + 1 < len(items):
            print(items[i + 1])


def compliant_multi_index_subscript():
    import numpy as np
    matrix = np.array([[1, 2], [3, 4]])
    items = [10, 20]
    for i, _ in enumerate(items):
        print(matrix[i, 0])


def compliant_subscript_object_not_name():
    def get_list():
        return ['x', 'y', 'z']
    lst = ['a', 'b', 'c']
    for i, _ in enumerate(lst):
        print(get_list()[i])
