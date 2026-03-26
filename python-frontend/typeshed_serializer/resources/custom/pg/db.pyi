from SonarPythonAnalyzerFakeStub import CustomStubBase
from typing import Optional

class DB(CustomStubBase):
    def __init__(
        self,
        dbname: Optional[str] = None,
        host: Optional[str] = None,
        port: int = -1,
        opt: Optional[str] = None,
        user: Optional[str] = None,
        passwd: Optional[str] = None,
    ) -> None: ...
    def query(self, command: str, *args) -> None: ...
    def close(self) -> None: ...
