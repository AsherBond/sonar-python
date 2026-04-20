def noncompliant_chained_startswith():
    s = "https://example.com"
    if s.startswith("http://") or s.startswith("https://"):  # Noncompliant {{Replace chained "startswith" calls with a single call using a tuple argument.}}
#      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        pass


def noncompliant_chained_startswith_parenthesized_subgroup():
    s = "https://example.com"
    if (s.startswith("http://") or s.startswith("https://")) or s.startswith("ftp://"):  # Noncompliant {{Replace chained "startswith" calls with a single call using a tuple argument.}}
#      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        pass


def noncompliant_chained_endswith():
    filename = "report.pdf"
    if filename.endswith(".pdf") or filename.endswith(".doc"):  # Noncompliant {{Replace chained "endswith" calls with a single call using a tuple argument.}}
#      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        pass


class MyClass:
    def noncompliant_attribute_receiver(self):
        if self.name.startswith("a") or self.name.startswith("b"):  # Noncompliant {{Replace chained "startswith" calls with a single call using a tuple argument.}}
#          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
            pass


def compliant_different_receivers():
    a = "hello"
    b = "world"
    if a.startswith("he") or b.startswith("wo"):
        pass


def compliant_mixed_methods():
    filename = "photo.jpg"
    if filename.startswith("photo") or filename.endswith(".jpg"):
        pass


def compliant_mixed_startswith_endswith_three_calls():
    s = "example.txt"
    if s.startswith("ex") or s.endswith(".txt") or s.startswith("test"):
        pass


def compliant_operand_not_chainable():
    s = "https://example.com"
    if s.lower() or s.startswith("https://"):
        pass

    if s.startswith("http://") or s.upper():
        pass


def compliant_dynamic_argument():
    prefix = "http"
    s = "https://example.com"
    if s.startswith(prefix) or s.startswith("https://"):
        pass


def compliant_negated_calls():
    filename = "report.pdf"
    if not filename.endswith(".tmp") or not filename.endswith(".bak"):
        pass


def compliant_different_function_call_receivers():
    def get_value():
        return "https://example.com"
    def get_other():
        return "ftp://example.com"
    if get_value().startswith("http://") or get_other().startswith("https://"):
        pass


def compliant_extra_argument():
    s = "https://example.com"
    if s.startswith("http://") or s.startswith("https://", 0):
        pass


def compliant_no_argument():
    s = "https://example.com"
    if s.startswith() or s.startswith("https://"):
        pass


def compliant_function_call_receiver():
    def get_url():
        return "https://example.com"
    if get_url().startswith("http://") or get_url().startswith("https://"):
        pass


def compliant_nested_call_in_receiver():
    class Obj:
        def inner(self):
            return "https://example.com"
    def outer():
        return Obj()
    if outer().inner().startswith("http://") or outer().inner().startswith("https://"):
        pass


def compliant_unpacked_argument():
    prefixes = ("http://",)
    s = "https://example.com"
    if s.startswith(*prefixes) or s.startswith("https://"):
        pass


def compliant_unqualified_call_operand():
    s = "https://example.com"
    if startswith("http://") or startswith("https://"):
        pass


def compliant_keyword_argument():
    s = "https://example.com"
    if s.startswith("http://") or s.startswith(prefix="https://"):
        pass


def compliant_custom_startswith_on_non_string_type():
    class MyMatcher:
        def startswith(self, prefix):
            return False
    m = MyMatcher()
    if m.startswith("foo") or m.startswith("bar"):
        pass
