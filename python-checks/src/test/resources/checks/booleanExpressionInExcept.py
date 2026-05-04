try:
    foo()
except ValueError:
    pass
except ValueError or TypeError:  # Noncompliant
    pass
except ValueError and TypeError:  # Noncompliant
    pass
except (ValueError or TypeError) as exception:  # Noncompliant
    pass
except (ValueError, TypeError) as exception:
    pass
except (ValueError):
    pass
except:
    pass

try:
    foo()
except* ValueError:
    pass
except* ValueError or TypeError:  # Noncompliant
    pass
except* ValueError and TypeError:  # Noncompliant
    pass
except* (ValueError or TypeError) as exception:  # Noncompliant
    pass
except* (ValueError, TypeError) as exception:
    pass
except* (ValueError):
    pass

# === `|` bitwise OR ===
try:
    foo()
except ValueError | TypeError:  # Noncompliant
    pass
except ValueError | TypeError as e:  # Noncompliant
    pass
except (ValueError | TypeError):  # Noncompliant
    pass
except ConnectionError | TimeoutError | OSError:  # Noncompliant
    pass

# === `&` bitwise AND ===
try:
    foo()
except ValueError & TypeError:  # Noncompliant
    pass
except ValueError & TypeError as e:  # Noncompliant
    pass
except (ValueError & TypeError):  # Noncompliant
    pass

# === `except*` variants for bitwise operators ===
try:
    foo()
except* ValueError | TypeError:  # Noncompliant
    pass
except* ValueError & TypeError:  # Noncompliant
    pass

# === Mixed: one valid handler, one with a bitwise operator ===
try:
    foo()
except TypeError:
    pass
except ValueError | KeyError:  # Noncompliant
    pass

try:
    foo()
except* TypeError:
    pass
except* ValueError | KeyError:  # Noncompliant
    pass

# === Arithmetic operators are out of scope (e.g. tuple + tuple is valid Python) ===
connection_errors = (ConnectionError, OSError)
channel_errors = (KeyError,)
try:
    foo()
except connection_errors + channel_errors as exc:
    pass
except connection_errors + channel_errors:
    pass
except (connection_errors + channel_errors):
    pass
