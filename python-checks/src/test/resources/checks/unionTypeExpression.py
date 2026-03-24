from typing import Annotated, Union
from typing import Union as u
import typing
import typing as t

def function_return() -> Union[str, int]: # Noncompliant {{Use a union type expression for this type hint.}}
                        #^^^^^^^^^^^^^^^
    pass

def typing_union() -> typing.Union[int, str]: # Noncompliant
                     #^^^^^^^^^^^^^^^^^^^^^^
    pass

def from_import_alias() -> u[str, float]: # Noncompliant
                          #^^^^^^^^^^^^^
    pass

def import_alias() -> t.Union[float, int]: # Noncompliant
                     #^^^^^^^^^^^^^^^^^^^
    pass

def function_param(param: Union[str, int]): # Noncompliant
                         #^^^^^^^^^^^^^^^
    pass

def local_variable():
    variable : Union[int, str] # Noncompliant
              #^^^^^^^^^^^^^^^

top_level_variable : Union[int, str] # Noncompliant
                    #^^^^^^^^^^^^^^^

class MyClass:

    instance_variable: Union[int, str] # Noncompliant
                      #^^^^^^^^^^^^^^^

    def instance_method() -> Union[int, str]: # Noncompliant
                            #^^^^^^^^^^^^^^^
        pass


def union_in_generic_type() -> list[Union[int, str]]: # Noncompliant
    pass

def ok(param: int | str) -> int | str:
    variable : int | str
    variable = param
    return variable

def not_union_type() -> None:
    pass

def str_type() -> str:
    pass

def not_name_or_subscript() -> int or str:
    pass

def subscript_but_not_name() -> (int or str)[int, str]:
    pass

def unknown_return_type() -> unknown:
    pass

def unknown_return_type_subscript() -> unknown[int, str]:
    pass

variable: Annotated[int | str, "metadata"]

top_level: Annotated[float | bytes, "info"]

def annotated_param(value: Annotated[int | str, "description"]) -> None:
    pass

def annotated_return() -> Annotated[int | str, "result"]:
    return 42
