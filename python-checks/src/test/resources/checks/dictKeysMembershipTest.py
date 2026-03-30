def noncompliant_basic():
    my_dict = {'a': 1, 'b': 2}
    if 'a' in my_dict.keys():  # Noncompliant {{Remove this unnecessary "keys()" call.}}
#             ^^^^^^^^^^^^^^
        print('Found')


def noncompliant_not_in():
    my_dict = {'a': 1, 'b': 2}
    if 'a' not in my_dict.keys():  # Noncompliant {{Remove this unnecessary "keys()" call.}}
#                 ^^^^^^^^^^^^^^
        print('Not found')


def compliant_direct_membership():
    my_dict = {'a': 1, 'b': 2}
    if 'a' in my_dict:
        print('Found')


def compliant_keys_with_argument():
    my_dict = {'a': 1}
    if 'a' in my_dict.keys('unexpected'):
        print('Not flagged')


def compliant_values_call():
    my_dict = {'a': 1, 'b': 2}
    if 1 in my_dict.values():
        print('Found value')


def compliant_custom_class():
    class CustomMapping:
        def keys(self):
            return ['x', 'y']

    obj = CustomMapping()
    if 'x' in obj.keys():
        print('Found')
