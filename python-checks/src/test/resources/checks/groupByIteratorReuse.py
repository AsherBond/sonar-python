from itertools import groupby


def noncompliant_stored_in_dict():
    data = [1, 1, 2, 2, 3]
    groups = {}
    for key, group in groupby(data):
        groups[key] = group  # Noncompliant {{Convert this group iterator to a list.}}
#                     ^^^^^


def noncompliant_as_regular_argument():
    data = [1, 1, 2, 2, 3]
    result = []
    for key, group in groupby(data):
        result.append(group)  # Noncompliant {{Convert this group iterator to a list.}}
#                     ^^^^^


def noncompliant_map_without_safe_wrapper():
    from operator import itemgetter
    data = [1, 1, 2, 2, 3]
    for key, group in groupby(data):
        it = map(itemgetter(0), group)  # Noncompliant


def fn_group_as_map_callable():
    # FN: passing `group` as the callable to `map`/`filter` (instead of as the iterable) is a
    # clear bug, but it would also fail at runtime with a TypeError when the callable is invoked.
    data = [1, 1, 2, 2, 3]
    for key, group in groupby(data):
        result = list(map(group, [1, 2, 3]))


def fp_lazy_passthrough_as_keyword_arg():
    # FP: `list(iterable=map(...))` materializes `map(...)` synchronously, so `group` does not
    # escape — but the ancestor walk stops at the keyword-arg boundary because we don't resolve
    # parameter names to confirm `iterable` is the materializing slot.
    from operator import itemgetter
    data = [(1, "a"), (1, "b"), (2, "c")]
    for key, group in groupby(data):
        result = list(iterable=map(itemgetter(0), group))  # Noncompliant


def compliant_group_as_non_first_arg_of_safe_consumer():
    # Passing `group` at a non-iterable slot (e.g. as `key=` to sorted) is a TypeError at runtime
    data = [1, 1, 2, 2, 3]
    for key, group in groupby(data):
        result = sorted(key, group)


def compliant_nested_in_non_lazy_call():
    # `str(group)` doesn't advance or retain `group` (it just calls __repr__) and the reference
    # is dropped at end of statement, so `group` never escapes. 
    data = [1, 1, 2, 2, 3]
    for key, group in groupby(data):
        result = list(str(group))


def noncompliant_inside_lambda():
    data = [1, 1, 2, 2, 3]
    fns = []
    for key, group in groupby(data):
        fns.append(lambda: list(group))  # Noncompliant
#                               ^^^^^


def noncompliant_groupby_alias():
    from itertools import groupby as gb
    data = [1, 1, 2, 2, 3]
    groups = {}
    for key, group in gb(data):
        groups[key] = group  # Noncompliant


def noncompliant_unsafe_with_safe_sibling():
    data = [1, 1, 2, 2, 3]
    groups = {}
    for key, group in groupby(data):
        groups[key] = group  # Noncompliant
        for item in group:
            print(item)


def fp_group_as_keyword_arg():
    # FP: `list(iterable=group)` is semantically equivalent to `list(group)` and materializes
    # `group` synchronously, so it does not escape — but the rule conservatively rejects keyword
    # args because we don't resolve parameter names to confirm `iterable` is the materializing slot.
    data = [1, 1, 2, 2, 3]
    for key, group in groupby(data):
        result = list(iterable=group)  # Noncompliant


def compliant_immediate_list():
    data = [1, 1, 2, 2, 3]
    groups = {}
    for key, group in groupby(data):
        groups[key] = list(group)


def compliant_safe_consumers():
    # Exercises every entry of SAFE_CONSUMER_MATCHER to catch a potential type-resolution regression
    data = [1, 1, 2, 2, 3]
    for key, group in groupby(data):
        as_tuple = tuple(group)
    for key, group in groupby(data):
        as_set = set(group)
    for key, group in groupby(data):
        as_frozenset = frozenset(group)
    for key, group in groupby(data):
        ordered = sorted(group)
    for key, group in groupby(data):
        total = sum(group)
    for key, group in groupby(data):
        biggest = max(group)
    for key, group in groupby(data):
        smallest = min(group)
    for key, group in groupby(data):
        has_any = any(group)
    for key, group in groupby(data):
        has_all = all(group)


def compliant_generator_expression():
    data = [1, 1, 2, 2, 3]
    for key, group in groupby(data):
        count = sum(1 for _ in group)


def compliant_nested_for():
    data = [1, 1, 2, 2, 3]
    for key, group in groupby(data):
        for item in group:
            print(item)


def compliant_not_a_call():
    data = [(1, [10, 11]), (2, [20, 21])]
    groups = {}
    for key, group in data:
        groups[key] = group


def compliant_non_groupby_call():
    def my_groupby(data):
        return iter([])

    data = [1, 1, 2, 2, 3]
    groups = {}
    for key, group in my_groupby(data):
        groups[key] = group


def compliant_groupby_full_module_path():
    import itertools
    data = [1, 1, 2, 2, 3]
    groups = {}
    for key, group in itertools.groupby(data):
        groups[key] = list(group)


def compliant_list_of_map():
    from operator import itemgetter
    data = [(1, "a"), (1, "b"), (2, "c")]
    for key, group in groupby(data):
        keys = list(map(itemgetter(0), group))


def compliant_list_of_filter():
    data = [1, 1, 2, 2, 3]
    for key, group in groupby(data):
        odd = list(filter(lambda x: x % 2, group))


def compliant_list_of_map_of_filter():
    data = [1, 1, 2, 2, 3]
    for key, group in groupby(data):
        result = list(map(str, filter(bool, group)))


def compliant_list_of_zip_of_enumerate():
    data = [1, 1, 2, 2, 3]
    for key, group in groupby(data):
        result = list(zip([10, 20], enumerate(group)))


def compliant_list_of_enumerate():
    data = [1, 1, 2, 2, 3]
    for key, group in groupby(data):
        indexed = list(enumerate(group))


def compliant_list_of_zip_first_arg():
    data = [1, 1, 2, 2, 3]
    for key, group in groupby(data):
        zipped = list(zip(group, [10, 20, 30]))


def compliant_list_of_zip_second_arg():
    data = [1, 1, 2, 2, 3]
    for key, group in groupby(data):
        zipped = list(zip([10, 20, 30], group))


def compliant_list_of_islice():
    from itertools import islice
    data = [1, 1, 2, 2, 3]
    for key, group in groupby(data):
        head = list(islice(group, 2))


def noncompliant_enumerate_without_safe_wrapper():
    data = [1, 1, 2, 2, 3]
    for key, group in groupby(data):
        it = enumerate(group)  # Noncompliant


def compliant_enumerate_at_wrong_arg_index():
    # `list(...)` materializes synchronously so `group` cannot escape
    data = [1, 1, 2, 2, 3]
    for key, group in groupby(data):
        result = list(enumerate([10, 20], group))


def compliant_group_passed_to_variable_class():
    data = [1, 1, 2, 2, 3]
    container_class = type([])
    for key, group in groupby(data):
        container = container_class(group)


def compliant_group_rebound():
    data = [1, 1, 2, 2, 3]
    for key, group in groupby(data):
        group = list(group)
        print(group)


def compliant_multiple_iterables():
    data1 = [1, 1, 2]
    data2 = [3, 3, 4]
    for key, group in groupby(data1), groupby(data2):
        saved = group


def compliant_tuple_pattern_loop_var():
    data = [(1, "a"), (1, "b"), (2, "c")]
    for key, (a, b) in groupby(data):
        saved = a


def compliant_single_loop_variable():
    data = [(1, 2), (1, 3)]
    for pair in groupby(data):
        saved = pair


def fn_unresolved_callee():  # FN: unresolved callee assumed safe to avoid false positives
    from nonexistent_module import safe_consumer
    data = [1, 1, 2, 2, 3]
    for key, group in groupby(data):
        result = safe_consumer(group)


def fn_groupby_in_dict_comprehension():  # FN: check only handles FOR_STMT
    data = [1, 1, 2, 2, 3]
    groups = {k: g for k, g in groupby(data)}


def fp_outer_group_consumed_by_nested_groupby():
    # FP: the inner `for ... in groupby(group):` fully consumes `group` before the outer
    # iterator advances, so it does not escape — but `itertools.groupby` is not a safe consumer,
    # so the ancestor walk does not recognize the chain as safe.
    data = [(1, "a"), (1, "b"), (2, "c")]
    for key, group in groupby(data):
        for subkey, subgroup in groupby(group):  # Noncompliant
            print(subkey, list(subgroup))


def fn_rebinding_in_conditional_branch(cond):
    # FN: as soon as `group` is rebound anywhere in the loop body, the rule conservatively
    # skips the loop to keep the rule purely AST-based.
    data = [1, 1, 2, 2, 3]
    groups = {}
    for key, group in groupby(data):
        if cond:
            group = list(group)
        groups[key] = group  # FN: on the `cond is False` path, this stores the raw iterator


def fn_unsafe_read_before_unconditional_rebinding():
    # FN: same reasoning as above.
    data = [1, 1, 2, 2, 3]
    groups = {}
    for key, group in groupby(data):
        groups[key] = group  # FN: real bug, but suppressed by the rebinding below
        group = list(group)
