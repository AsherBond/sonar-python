import typing
from fastapi import FastAPI, APIRouter, Depends
from typing import Annotated

app = FastAPI()
router = APIRouter()

@app.get("/items/{item_id}")
def noncompliant_missing_path_param():  # Noncompliant {{Add path parameter "item_id" to the function signature.}}
    return {"message": "Hello"}

@app.get("/users/{user_id}/items/{item_id}")
def noncompliant_missing_one_of_multiple_params(user_id: int):  # Noncompliant {{Add path parameter "item_id" to the function signature.}}
    return {"user_id": user_id}

@app.get("/items/{item_id}")
def noncompliant_positional_only_param(item_id: int, /):  # Noncompliant {{Path parameter "item_id" should not be positional-only.}}
    return {"item_id": item_id}

@app.get("/things/{thing_id}")
async def noncompliant_async_missing_param(query: str):  # Noncompliant 
    return {"query": query}

@app.get("/users/{user_id}/posts/{post_id}")
def noncompliant_multiple_positional_only(user_id: int, post_id: int, /):  # Noncompliant 2
    return {"user_id": user_id}

@router.put("/items/{item_id}")
def noncompliant_router_missing_param():  # Noncompliant 
    return {"updated": True}

@app.post("/items/{item_id}")
def noncompliant_post_missing():  # Noncompliant 
    pass

@app.put("/items/{item_id}")
def noncompliant_put_missing():  # Noncompliant 
    pass

@app.delete("/items/{item_id}")
def noncompliant_delete_missing():  # Noncompliant 
    pass

@app.patch("/items/{item_id}")
def noncompliant_patch_missing():  # Noncompliant 
    pass

@app.options("/items/{item_id}")
def noncompliant_options_missing():  # Noncompliant 
    pass

@app.head("/items/{item_id}")
def noncompliant_head_missing():  # Noncompliant 
    pass

@app.trace("/items/{item_id}")
def noncompliant_trace_missing():  # Noncompliant 
    pass

@app.get("/items/{item_id:int}")
def noncompliant_with_converter_missing():  # Noncompliant 
    pass

@app.get("/users/{user_id}/items/{item_id}")
def noncompliant_mixed_positional_only_and_missing(user_id: int, /):  # Noncompliant 2
    pass

@app.get("/a/{x}/b/{y}/c/{z}")
def noncompliant_all_params_missing():  # Noncompliant 3
    pass

@app.get(path="/items/{item_id}")
def noncompliant_path_keyword_missing():  # Noncompliant 
    return {}

@app.get("/items/{item_id}")
def compliant_basic(item_id: int):
    return {"item_id": item_id}

@app.get("/users/{user_id}/items/{item_id}")
def compliant_multiple_params(user_id: int, item_id: int):
    return {"user_id": user_id, "item_id": item_id}

@app.get("/users/{user_id}/items/{item_id}")
def compliant_reordered_params(item_id: int, user_id: int):
    return {"user_id": user_id, "item_id": item_id}

@app.get("/things/{thing_id}")
async def compliant_async_with_query(thing_id: int, query: str):
    return {"thing_id": thing_id, "query": query}

@app.get("/items")
def compliant_static_route():
    return {"items": []}

@router.put("/items/{item_id}")
def compliant_router_with_param(item_id: int):
    return {"updated": True}

@app.get("/items/{item_id}")
def compliant_keyword_only(*, item_id: int):
    return {"item_id": item_id}

@app.post("/items/{item_id}")
def compliant_post(item_id: int):
    pass

@app.put("/items/{item_id}")
def compliant_put(item_id: int):
    pass

@app.delete("/items/{item_id}")
def compliant_delete(item_id: int):
    pass

@app.get("/items/{item_id}")
def compliant_with_default(item_id: int = 1):
    return {"item_id": item_id}

@app.get("/items/{item_id:int}")
def compliant_with_converter(item_id: int):
    return {"item_id": item_id}

@app.get("/items/{item_id}")
def compliant_with_kwargs(**kwargs):
    return kwargs

@app.get("/items/{item_id}")
def compliant_with_args_kwargs(*args, **kwargs):
    return {"args": args, "kwargs": kwargs}

@app.get(status_code=200, path="/items/{item_id}")
def compliant_path_keyword(item_id: int):
    return {"item_id": item_id}

# --- Edge cases ---

def some_other_decorator(path):
    def wrapper(func):
        return func
    return wrapper

@some_other_decorator("/items/{item_id}")
def compliant_not_fastapi_decorator():
    pass

@app.get("/static")
def compliant_no_path_params():
    pass

@app.get("/items")
def compliant_extra_query_params(query: str):
    return {"query": query}

path = "/items/{item_id}"
@app.get(path)
def noncompliant_dynamic_path(): # Noncompliant
    pass

@app.get
def compliant_decorator_without_call():
    pass

@app.get("")
def compliant_empty_path():
    pass


@app.get(True)
def compliant_path_is_not_a_string():
    pass

# --- Depends() path parameter delegation ---

def get_item(item_id: int):
    return {"item_id": item_id}

@router.get("/items/{item_id}")
def compliant_depends_default_value(item=Depends(get_item)):
    return item

@app.get("/items/{item_id}")
def compliant_depends_annotated(item: Annotated[dict, Depends(get_item)]):
    return item

@app.get("/users/{user_id}/items/{item_id}")
def compliant_depends_partial(user_id: int, item=Depends(get_item)):
    return {"user_id": user_id, "item": item}

async def get_item_async(item_id: int):
    return {"item_id": item_id}

@router.get("/items/{item_id}")
def compliant_depends_async_dependency(item=Depends(get_item_async)):
    return item

def dep_without_item_id(name: str):
    return name

@app.get("/items/{item_id}")
def noncompliant_depends_param_not_in_dep(item=Depends(dep_without_item_id)):  # Noncompliant {{Add path parameter "item_id" to the function signature.}}
    return item

@app.get("/items/{item_id}")
def compliant_depends_annotated_qualified(item: typing.Annotated[dict, Depends(get_item)]):
    return item

# --- Nested/recursive Depends() ---

def get_item_wrapper(item=Depends(get_item)):
    return item

@router.get("/items/{item_id}")
def compliant_nested_depends(item=Depends(get_item_wrapper)):
    return item

def get_item_annotated_wrapper(item: Annotated[dict, Depends(get_item)]):
    return item

@app.get("/items/{item_id}")
def compliant_nested_depends_annotated(item=Depends(get_item_annotated_wrapper)):
    return item

def dep_level2_no_id(name: str):
    return name

def dep_level1_no_id(x=Depends(dep_level2_no_id)):
    return x

@app.get("/items/{item_id}")
def noncompliant_nested_depends_not_covering(item=Depends(dep_level1_no_id)):  # Noncompliant {{Add path parameter "item_id" to the function signature.}}
    return item

# --- Circular dependency detection (cycle in Depends chain) ---
# dep_b_circular references dep_a_circular (defined below) - tests cycle detection at line 112
def dep_b_circular(dep=Depends(dep_a_circular)):
    return dep

def dep_a_circular(item_id: int, dep=Depends(dep_b_circular)):
    return item_id

@app.get("/items/{item_id}")
def compliant_circular_depends(dep=Depends(dep_a_circular)):
    return dep

# --- QualifiedExpression as Depends argument (tests lines 164-165 and 174) ---
class ItemDepsHolder:
    @staticmethod
    def get_item(item_id: int):
        return item_id

@app.get("/items/{item_id}")
def compliant_qualified_expr_depends(dep=Depends(ItemDepsHolder.get_item)):
    return dep

# --- Non-Name/non-QualifiedExpression as Depends argument (tests lines 167 and 170) ---
@app.get("/items/{item_id}")
def noncompliant_lambda_depends(dep=Depends(lambda: None)):  # Noncompliant {{Add path parameter "item_id" to the function signature.}}
    return dep
