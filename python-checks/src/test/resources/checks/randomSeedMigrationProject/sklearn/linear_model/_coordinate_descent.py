class Lasso:
    def __init__(self, *, random_state=None, selection="cyclic"):
        pass


class ElasticNet:
    def __init__(self, *, random_state=None, selection="cyclic"):
        pass


class Derived(Lasso):
    def __init__(self):
        super().__init__()


Lasso()
ElasticNet()
Lasso(selection="random")  # Noncompliant
