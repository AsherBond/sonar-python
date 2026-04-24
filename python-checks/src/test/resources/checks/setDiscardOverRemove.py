def basic_set_literal(x):
    my_set = {1, 2, 3}
    if x in my_set:  # Noncompliant {{Use "discard()" instead of checking membership before calling "remove()".}}
    #  ^^^^^^^^^^^
        my_set.remove(x)


def basic_set_constructor(x):
    my_set = set([1, 2, 3])
    if x in my_set:  # Noncompliant
        my_set.remove(x)


def multiple_operations_in_body(x):
    my_set = {1, 2, 3}
    if x in my_set:
        my_set.remove(x)
        print("removed")  # additional logic


def else_branch_present(x):
    my_set = {1, 2, 3}
    if x in my_set:
        my_set.remove(x)
    else:
        print("not found")


def elif_branch_present(x, y):
    my_set = {1, 2, 3}
    if x in my_set:
        my_set.remove(x)
    elif y in my_set:
        my_set.remove(y)


def not_in_condition(x):
    my_set = {1, 2, 3}
    if x not in my_set:
        my_set.remove(x)


def different_objects(x):
    set_a = {1, 2, 3}
    set_b = {4, 5, 6}
    if x in set_a:
        set_b.remove(x)


def different_elements(x, y):
    my_set = {1, 2, 3}
    if x in my_set:
        my_set.remove(y)


def list_not_set(x):
    my_list = [1, 2, 3]
    if x in my_list:
        my_list.remove(x)


def dict_not_set(x):
    my_dict = {"a": 1}
    if x in my_dict:
        my_dict.pop(x)


def frozenset_type(x):
    my_fset = frozenset([1, 2, 3])
    if x in my_fset:  # frozenset has no remove()
        ...


def not_a_remove_call(x):
    my_set = {1, 2, 3}
    if x in my_set:
        my_set.add(x + 1)


def complex_condition(x, flag):
    my_set = {1, 2, 3}
    if x in my_set and flag:
        my_set.remove(x)


def set_from_parameter(my_set: set, x):
    if x in my_set:  # Noncompliant
        my_set.remove(x)


def set_subclass(x):
    class MySet(set):
        pass
    s = MySet()
    if x in s:  # Noncompliant
        s.remove(x)


def method_call_on_self(x):
    class Container:
        def __init__(self):
            self.items = {1, 2, 3}

        def remove_item(self, value):
            if value in self.items:  # FN: type of self.items attribute is not inferred
                self.items.remove(value)


def nested_attribute_access(x):
    class Container:
        pass
    c = Container()
    c.items = {1, 2, 3}
    if x in c.items:  # FN: type of c.items attribute is not inferred
        c.items.remove(x)


def remove_with_wrong_number_of_args(x, y):
    my_set = {1, 2, 3}
    if x in my_set:
        my_set.remove(x, y)  # FN: invalid set.remove call, but not our rule's concern


def complex_element_expression(items, i):
    my_set = {1, 2, 3}
    if items[i] in my_set:  # Noncompliant
        my_set.remove(items[i])


def body_is_not_expression_statement(x):
    my_set = {1, 2, 3}
    if x in my_set:
        return


def body_is_pass_statement(x):
    my_set = {1, 2, 3}
    if x in my_set:
        pass


def body_is_assignment(x):
    my_set = {1, 2, 3}
    if x in my_set:
        y = my_set.remove(x)


def body_has_multiple_expressions(x):
    my_set = {1, 2, 3}
    def foo(): ...
    if x in my_set:
        my_set.remove(x), foo()


def bare_remove_call(x):
    def remove(value): ...
    my_set = {1, 2, 3}
    if x in my_set:
        remove(x)


def remove_with_unpacking_argument(x):
    my_set = {1, 2, 3}
    args = (x,)
    if x in my_set:
        my_set.remove(*args)


def remove_with_keyword_argument(x):
    my_set = {1, 2, 3}
    if x in my_set:
        my_set.remove(element=x)


def multiline_qualifier(x, cond):
    my_set = {1, 2, 3}
    other_set = {4, 5, 6}
    if x in (my_set  # Noncompliant
             if cond else
             other_set):
        (my_set
         if cond else
         other_set).remove(x)


def multiline_argument(items, i):
    my_set = {1, 2, 3}
    if items[  # Noncompliant
            i] in my_set:
        my_set.remove(items[
                          i])
