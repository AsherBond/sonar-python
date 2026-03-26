from pg.connection import Connection
from pg.db import DB
from typing import Optional

def connect(
    dbname: Optional[str] = None,
    host: Optional[str] = None,
    port: int = -1,
    opt: Optional[str] = None,
    user: Optional[str] = None,
    passwd: Optional[str] = None,
    nowait: bool = False,
) -> Connection: ...
