from typing import TypeVar, ParamSpec, NewType
from typing import TypeVar as TV
from typing import ParamSpec as PS
from typing import NewType as NT
from typing_extensions import TypeVar as ExtTV
from typing_extensions import ParamSpec as ExtPS
from typing_extensions import NewType as ExtNewType
import typing
import typing_extensions

MyType = TypeVar("T")  # Noncompliant {{Rename this string to match the variable name "MyType".}}
#                ^^^

MyParams = ParamSpec("P")  # Noncompliant {{Rename this string to match the variable name "MyParams".}}
#                    ^^^

MyInt = NewType("Integer", int)  # Noncompliant {{Rename this string to match the variable name "MyInt".}}
#               ^^^^^^^^^

AnotherType = typing.TypeVar("T")  # Noncompliant {{Rename this string to match the variable name "AnotherType".}}
#                            ^^^

MyPS = typing.ParamSpec("P")  # Noncompliant {{Rename this string to match the variable name "MyPS".}}
#                       ^^^

MyNewType = typing.NewType("OtherName", int)  # Noncompliant
#                          ^^^^^^^^^^^

MyExtType = typing_extensions.TypeVar("E")  # Noncompliant
#                                     ^^^

MyExtParams = typing_extensions.ParamSpec("P")  # Noncompliant
#                                         ^^^

MyExtInt = typing_extensions.NewType("ExtInteger", int)  # Noncompliant
#                                    ^^^^^^^^^^^^

MyAliasType = TV("T")  # Noncompliant
#                ^^^

MyAliasParams = PS("Params")  # Noncompliant
#                  ^^^^^^^^

MyAliasInt = NT("UserId", int)  # Noncompliant
#               ^^^^^^^^

MyExtAliasType = ExtTV("E")  # Noncompliant
#                      ^^^

MyExtAliasParams = ExtPS("Params")  # Noncompliant
#                        ^^^^^^^^

MyExtAliasInt = ExtNewType("OtherName", int)  # Noncompliant
#                          ^^^^^^^^^^^

MyVar = TypeVar("myVar")  # Noncompliant
#               ^^^^^^^

T_co = TypeVar("T_contra")  # Noncompliant
#              ^^^^^^^^^^

UserId = NewType("UserID", int)  # Noncompliant
#                ^^^^^^^^

ElementType = TypeVar("E")  # Noncompliant
#                     ^^^

NewName = TypeVar("OldName")  # Noncompliant
#                 ^^^^^^^^^

NumberType = TypeVar("Num", int, float)  # Noncompliant
#                    ^^^^^

ComparableType = TypeVar("Comparable", bound=int)  # Noncompliant
#                        ^^^^^^^^^^^^

MyCovariantType = TypeVar("T_co", covariant=True)  # Noncompliant
#                         ^^^^^^

MyContraType = TypeVar("T_contra", contravariant=True)  # Noncompliant
#                      ^^^^^^^^^^

EmptyStr = TypeVar("")  # Noncompliant
#                  ^^

MySingleQuoteType = TypeVar('MySingleQuoteTypo')  # Noncompliant
#                           ^^^^^^^^^^^^^^^^^^^


def noncompliant_inside_function():
    InnerType = TypeVar("T")  # Noncompliant
#                       ^^^
    InnerParams = ParamSpec("P")  # Noncompliant
#                           ^^^
    InnerNew = NewType("OtherName", int)  # Noncompliant
#                      ^^^^^^^^^^^


class MyGenericClass:
    ClassVar1 = TypeVar("T")  # Noncompliant
#                       ^^^
    ClassVar2 = ParamSpec("P")  # Noncompliant
#                         ^^^
    ClassVar3 = NewType("OtherName", int)  # Noncompliant
#                       ^^^^^^^^^^^


T = TypeVar("T")
P = ParamSpec("P")
UserId2 = NewType("UserId2", int)

MyType2 = TypeVar("MyType2")
MyParams2 = ParamSpec("MyParams2")
MyInt2 = NewType("MyInt2", int)

T_co2 = TypeVar("T_co2", covariant=True)
T_contra2 = TypeVar("T_contra2", contravariant=True)

T_bound = TypeVar("T_bound", bound=int)
T_constrained = TypeVar("T_constrained", int, str)

AnotherType2 = typing.TypeVar("AnotherType2")
MyPS2 = typing.ParamSpec("MyPS2")
MyNT2 = typing.NewType("MyNT2", str)

MyExtType2 = typing_extensions.TypeVar("MyExtType2")
MyExtParams2 = typing_extensions.ParamSpec("MyExtParams2")
MyExtInt2 = typing_extensions.NewType("MyExtInt2", int)

T_alias = TV("T_alias")
P_alias = PS("P_alias")
NT_alias = NT("NT_alias", int)
ExtT_alias = ExtTV("ExtT_alias")
ExtP_alias = ExtPS("ExtP_alias")
ExtNT_alias = ExtNewType("ExtNT_alias", int)

MySingleQuoteType2 = TypeVar('MySingleQuoteType2')

MyRawType = TypeVar(r"MyRawType")
MyTripleType = TypeVar("""MyTripleType""")

MyRawTypeBad = TypeVar(r"WrongRaw")  # Noncompliant {{Rename this string to match the variable name "MyRawTypeBad".}}
#                      ^^^^^^^^^^^
MyTripleTypeBad = TypeVar("""WrongTriple""")  # Noncompliant {{Rename this string to match the variable name "MyTripleTypeBad".}}
#                         ^^^^^^^^^^^^^^^^^

_name = "T"
T_dynamic = TypeVar(_name)

T_computed = TypeVar("T".lower())

result = list(TypeVar("T"))


def make_typevar():
    return TypeVar("T")


def compliant_inside_function():
    InnerT = TypeVar("InnerT")
    InnerP = ParamSpec("InnerP")
    InnerNT = NewType("InnerNT", int)


class CompliantGenericClass:
    ClassT = TypeVar("ClassT")
    ClassP = ParamSpec("ClassP")
    ClassNT = NewType("ClassNT", int)


try:
    T_noargs = TypeVar()  # noqa
except TypeError:
    pass

_kwargs = {"covariant": True}
T_unpacked = TypeVar(**_kwargs)

A = B = TypeVar("A")

C, D = TypeVar("C"), TypeVar("D")

(G, H) = TypeVar("T")

I, J = TypeVar("T")

_d = {}
_d["key"] = TypeVar("T")


class _Holder:
    pass


_obj = _Holder()
_obj.attr = TypeVar("T")

_flag = True
T_ternary = TypeVar("T_ternary") if _flag else TypeVar("T_ternary")

E, F = TypeVar("T"), TypeVar("T")

T_keyword_match = TypeVar(name="T_keyword_match")
T_keyword_mismatch = TypeVar(name="WrongName")  # Noncompliant {{Rename this string to match the variable name "T_keyword_mismatch".}}
#                                 ^^^^^^^^^^^

import typing as _typing
# Annotated assignments (AnnotatedAssignment node) are not AssignmentStatement, so they are skipped.
T_annotated: _typing.TypeAlias = TypeVar("WrongName")

if walrus := TypeVar("T"):
    pass
