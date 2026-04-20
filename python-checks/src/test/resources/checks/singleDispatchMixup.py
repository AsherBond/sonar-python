from functools import singledispatch, singledispatchmethod


class WithSingledispatch:
    @singledispatch  # Noncompliant {{Use "@singledispatchmethod" instead of "@singledispatch" on methods defined in a class body.}}
   #^^^^^^^^^^^^^^^
    def process(self, data):
        pass


class WithWrongStaticmethodOrder:
    @singledispatch  # Noncompliant {{Use "@singledispatchmethod" instead of "@singledispatch" on methods defined in a class body.}}
    @staticmethod
    def handle(data):
        pass


@singledispatchmethod  # Noncompliant {{Use "@singledispatch" instead of "@singledispatchmethod" on standalone functions.}}
#^[sc=1;ec=21]
def process_standalone(data):
    pass


@singledispatch
def compliant_standalone(data):
    pass


class WithSingledispatchmethod:
    @singledispatchmethod
    def process(self, data):
        pass


class WithValidStaticmethodOrder:
    @staticmethod
    @singledispatch
    def process(data):
        pass


class WithClassmethodAndSingledispatch:
    @singledispatch  # Noncompliant {{Use "@singledispatchmethod" instead of "@singledispatch" on methods defined in a class body.}}
    @classmethod
    def handle(cls, data):
        pass


class WithSingledispatchmethodAndClassmethod:
    @singledispatchmethod
    @classmethod
    def process(cls, data):
        pass


class WithNestedFunction:
    def outer(self):
        @singledispatch  # Compliant: nested function, not a method definition
        def inner(data):
            pass


def no_dispatch(data):
    pass
