from typing import TypeVar
from typing import TypeVar as TV
from typing_extensions import TypeVar as ExtTV
import typing
import typing_extensions

T1 = TypeVar('T1', covariant=True, contravariant=True)  # Noncompliant
#    ^^^^^^^
T2 = TypeVar('T2', contravariant=True, covariant=True)  # Noncompliant
#    ^^^^^^^
T3 = TypeVar('T3', int, str, covariant=True, contravariant=True)  # Noncompliant
#    ^^^^^^^
T4 = TypeVar('T4', bound=int, covariant=True, contravariant=True)  # Noncompliant
#    ^^^^^^^
T5 = typing.TypeVar('T5', covariant=True, contravariant=True)  # Noncompliant
#    ^^^^^^^^^^^^^^
T6 = typing_extensions.TypeVar('T6', covariant=True, contravariant=True)  # Noncompliant
#    ^^^^^^^^^^^^^^^^^^^^^^^^^

cov = True
T7 = TypeVar('T7', covariant=cov, contravariant=True)  # Noncompliant
#    ^^^^^^^

contra = True
T8 = TypeVar('T8', covariant=True, contravariant=contra)  # Noncompliant
#    ^^^^^^^

cov2 = True
contra2 = True
T9 = TypeVar('T9', covariant=cov2, contravariant=contra2)  # Noncompliant
#    ^^^^^^^


T_co = TypeVar('T_co', covariant=True)
T_contra = TypeVar('T_contra', contravariant=True)
T_inv = TypeVar('T_inv')
T_inv2 = TypeVar('T_inv2', covariant=False, contravariant=False)
T_inv3 = TypeVar('T_inv3', covariant=True, contravariant=False)
T_inv4 = TypeVar('T_inv4', covariant=False, contravariant=True)
T_ext_co = typing_extensions.TypeVar('T_ext_co', covariant=True)
T_ext_contra = typing_extensions.TypeVar('T_ext_contra', contravariant=True)


def get_flag():
    return True


T_unk = TypeVar('T_unk', covariant=get_flag(), contravariant=True)
T_unk2 = TypeVar('T_unk2', covariant=True, contravariant=get_flag())


def my_func(name, covariant=False, contravariant=False):
    pass


my_func('X', covariant=True, contravariant=True)


cov3 = False
cov3 = True
T_fn = TypeVar('T_fn', covariant=cov3, contravariant=True)

cov_false = False
T_false_name = TypeVar('T_false_name', covariant=cov_false, contravariant=True)

T_num_noncompliant = TypeVar('T_num_noncompliant', covariant=1, contravariant=1)  # Noncompliant
T_num_mixed = TypeVar('T_num_mixed', covariant=1, contravariant=True)  # Noncompliant

T_num_zero_cov = TypeVar('T_num_zero_cov', covariant=0, contravariant=True)
T_num_zero_contra = TypeVar('T_num_zero_contra', covariant=True, contravariant=0)
T_num_zero_both = TypeVar('T_num_zero_both', covariant=0, contravariant=0)

T_float_zero_cov = TypeVar('T_float_zero_cov', covariant=0.0, contravariant=True)
T_float_zero_contra = TypeVar('T_float_zero_contra', covariant=True, contravariant=0.0)

T_complex_zero_cov = TypeVar('T_complex_zero_cov', covariant=0j, contravariant=True)
T_complex_zero_contra = TypeVar('T_complex_zero_contra', covariant=True, contravariant=0j)

T_none_cov = TypeVar('T_none_cov', covariant=None, contravariant=True)
T_none_contra = TypeVar('T_none_contra', covariant=True, contravariant=None)

T_str_noncompliant = TypeVar('T_str_noncompliant', covariant="yes", contravariant=True)  # Noncompliant
T_str_empty_cov = TypeVar('T_str_empty_cov', covariant="", contravariant=True)

T_list_empty_cov = TypeVar('T_list_empty_cov', covariant=[], contravariant=True)
T_list_empty_contra = TypeVar('T_list_empty_contra', covariant=True, contravariant=[])
T_list_nonempty = TypeVar('T_list_nonempty', covariant=[1], contravariant=True)  # Noncompliant

T_tuple_empty_cov = TypeVar('T_tuple_empty_cov', covariant=(), contravariant=True)
T_tuple_empty_contra = TypeVar('T_tuple_empty_contra', covariant=True, contravariant=())
T_tuple_nonempty = TypeVar('T_tuple_nonempty', covariant=(1,), contravariant=True)  # Noncompliant

T_dict_empty_cov = TypeVar('T_dict_empty_cov', covariant={}, contravariant=True)
T_dict_empty_contra = TypeVar('T_dict_empty_contra', covariant=True, contravariant={})
T_dict_nonempty = TypeVar('T_dict_nonempty', covariant={1: 2}, contravariant=True)  # Noncompliant

T_set_nonempty = TypeVar('T_set_nonempty', covariant={1, 2}, contravariant=True)  # Noncompliant

T_ext_inv = typing_extensions.TypeVar('T_ext_inv', covariant=False, contravariant=False)

T_alias = TV('T_alias', covariant=True, contravariant=True)  # Noncompliant
#         ^^
T_ext_alias = ExtTV('T_ext_alias', covariant=True, contravariant=True)  # Noncompliant
#             ^^^^^

T_str_contra_truthy = TypeVar('T_str_contra_truthy', covariant=True, contravariant="yes")  # Noncompliant
#                     ^^^^^^^
T_str_empty_contra = TypeVar('T_str_empty_contra', covariant=True, contravariant="")

kwargs_with_covariant = {'covariant': True, 'contravariant': True}
T_kwargs_unpacked = TypeVar('T_kwargs_unpacked', **kwargs_with_covariant)
