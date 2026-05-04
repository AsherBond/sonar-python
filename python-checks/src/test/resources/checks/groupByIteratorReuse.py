from itertools import groupby


def noncompliant_assigned_to_dict_subscript():
    data = [1, 1, 2, 2, 3]
    groups = {}
    for key, group in groupby(data):
        groups[key] = group  # Noncompliant {{Consume this group iterator inside the loop, or materialize it into a collection.}}
#                     ^^^^^


def noncompliant_yielded():
    data = [1, 1, 2, 2, 3]
    for key, group in groupby(data):
        yield group  # Noncompliant


def noncompliant_inside_lambda_captured_by_outer_storage():
    data = [1, 1, 2, 2, 3]
    fns = []
    for key, group in groupby(data):
        fns.append(lambda: list(group))  # Noncompliant
#                               ^^^^^


def noncompliant_chain_top_lands_at_sink():
    data = [1, 1, 2, 2, 3]
    for key, group in groupby(data):
        it = enumerate(group)  # Noncompliant


def noncompliant_set_add():
    data = [1, 1, 2, 2, 3]
    seen = set()
    for key, group in groupby(data):
        seen.add(group)  # Noncompliant


def noncompliant_dict_setdefault():
    data = [1, 1, 2, 2, 3]
    cache = {}
    for key, group in groupby(data):
        cache.setdefault(key, group)  # Noncompliant


def noncompliant_lazy_passthrough_to_storing_method():
    from operator import itemgetter
    data = [(1, "a"), (1, "b"), (2, "c")]
    out = []
    for key, group in groupby(data):
        out.append(map(itemgetter(0), group))  # Noncompliant


def noncompliant_groupby_alias():
    from itertools import groupby as gb
    data = [1, 1, 2, 2, 3]
    groups = {}
    for key, group in gb(data):
        groups[key] = group  # Noncompliant


def compliant_immediate_list():
    data = [1, 1, 2, 2, 3]
    groups = {}
    for key, group in groupby(data):
        groups[key] = list(group)


def compliant_str_join():
    data = ["a", "a", "b"]
    for key, group in groupby(data):
        joined = ",".join(group)


def compliant_group_passed_to_runtime_class_object():
    data = [1, 1, 2, 2, 3]
    container_class = type([])
    for key, group in groupby(data):
        container = container_class(group)


def compliant_for_loop_iterable():
    data = [1, 1, 2, 2, 3]
    for key, group in groupby(data):
        for item in group:
            print(item)


def compliant_outer_group_consumed_by_nested_groupby():
    data = [(1, "a"), (1, "b"), (2, "c")]
    for key, group in groupby(data):
        for subkey, subgroup in groupby(group):
            print(subkey, list(subgroup))


def compliant_print():
    data = [1, 1, 2, 2, 3]
    for key, group in groupby(data):
        print(group)


def compliant_lazy_passthrough_consumed_by_outer_safe_consumer_map():
    from operator import itemgetter
    data = [(1, "a"), (1, "b"), (2, "c")]
    for key, group in groupby(data):
        keys = list(map(itemgetter(0), group))


def compliant_group_as_keyword_arg():
    data = [1, 1, 2, 2, 3]
    for key, group in groupby(data):
        result = list(iterable=group)


def compliant_extended_into_list():
    data = [1, 1, 2, 2, 3]
    out = []
    for key, group in groupby(data):
        out.extend(group)


def compliant_dict_updated_with_group():
    data = [(1, "a"), (1, "b"), (2, "c")]
    mapping = {}
    for key, group in groupby(data):
        mapping.update(group)


def compliant_returned():
    data = [1, 1, 2, 2, 3]
    for key, group in groupby(data):
        return group


def compliant_group_rebound_in_loop_body():
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


def fn_unresolved_callee():  # FN: unresolved callee assumed safe to avoid false positives
    from nonexistent_module import safe_consumer
    data = [1, 1, 2, 2, 3]
    for key, group in groupby(data):
        result = safe_consumer(group)


def fn_groupby_in_dict_comprehension():  # FN: check only handles FOR_STMT
    data = [1, 1, 2, 2, 3]
    groups = {k: g for k, g in groupby(data)}


def fn_in_container_literal():  # FN: container-literal sinks are not analyzed
    data = [1, 1, 2, 2, 3]
    for key, group in groupby(data):
        return [group]


def fn_in_else_clause():  # FN: only the for-statement body is analyzed
    data = [1, 1, 2, 2, 3]
    saved = None
    for key, group in groupby(data):
        pass
    else:
        saved = group


def fn_with_any_rebinding():  # FN: any rebinding of `group` in the body causes us to skip the loop
    data = [1, 1, 2, 2, 3]
    groups = {}
    for key, group in groupby(data):
        groups[key] = group
        group = list(group)
