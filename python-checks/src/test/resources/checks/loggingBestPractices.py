import logging

logger = logging.getLogger(__name__)

user = "Maria"
count = 42


# ==== Eager string formatting: f-string ====

logging.info(f"{user} - Something happened")  # Noncompliant {{Pass formatting arguments to the logging call instead of pre-formatting the message string.}}
logger.info(f"{user} logged in")  # Noncompliant {{Pass formatting arguments to the logging call instead of pre-formatting the message string.}}


# ==== Eager string formatting: .format() ====

logging.debug("{} - Something happened".format(user))  # Noncompliant {{Pass formatting arguments to the logging call instead of pre-formatting the message string.}}
logger.debug("{}".format(user))  # Noncompliant {{Pass formatting arguments to the logging call instead of pre-formatting the message string.}}


# ==== Eager string formatting: % operator ====

logging.warning("%s - Something happened" % user)  # Noncompliant {{Pass formatting arguments to the logging call instead of pre-formatting the message string.}}
logger.warning("%s err" % user)  # Noncompliant {{Pass formatting arguments to the logging call instead of pre-formatting the message string.}}


# ==== Eager string formatting: + concatenation ====

logging.error("Error: " + user)  # Noncompliant {{Pass formatting arguments to the logging call instead of pre-formatting the message string.}}
logging.critical(user + " triggered critical")  # Noncompliant {{Pass formatting arguments to the logging call instead of pre-formatting the message string.}}
logger.error("User: " + user + " failed")  # Noncompliant {{Pass formatting arguments to the logging call instead of pre-formatting the message string.}}


# ==== Deprecated warn method ====

logging.warn("Something happened")  # Noncompliant {{Use "warning" instead of the deprecated "warn" method.}}
logger.warn("Something happened")  # Noncompliant {{Use "warning" instead of the deprecated "warn" method.}}


# ==== Extra attribute name collisions ====

logging.info("msg", extra={"name": "override"})  # Noncompliant {{Remove or rename this key; it overrides a built-in LogRecord attribute.}}
logging.info("msg", extra={"message": "val"})  # Noncompliant {{Remove or rename this key; it overrides a built-in LogRecord attribute.}}
logging.info("msg", extra={"levelname": "DEBUG"})  # Noncompliant {{Remove or rename this key; it overrides a built-in LogRecord attribute.}}
logging.info("msg", extra={"filename": "app.py"})  # Noncompliant {{Remove or rename this key; it overrides a built-in LogRecord attribute.}}
logging.info("msg", extra={"lineno": 42})  # Noncompliant {{Remove or rename this key; it overrides a built-in LogRecord attribute.}}
logging.info("msg", extra={"thread": 1})  # Noncompliant {{Remove or rename this key; it overrides a built-in LogRecord attribute.}}
logging.info("msg", extra={"process": 2})  # Noncompliant {{Remove or rename this key; it overrides a built-in LogRecord attribute.}}
logging.info("msg", extra={"exc_text": "custom"})  # Noncompliant {{Remove or rename this key; it overrides a built-in LogRecord attribute.}}
logging.info("msg", extra={"stack_info": "custom"})  # Noncompliant {{Remove or rename this key; it overrides a built-in LogRecord attribute.}}
logging.info("msg", extra={"asctime": "custom"})  # Noncompliant {{Remove or rename this key; it overrides a built-in LogRecord attribute.}}


# ==== logging.exception ====

logging.exception(f"{user} failed")  # Noncompliant {{Pass formatting arguments to the logging call instead of pre-formatting the message string.}}
logger.exception("Error: " + user)  # Noncompliant {{Pass formatting arguments to the logging call instead of pre-formatting the message string.}}
logging.exception("msg", extra={"name": "override"})  # Noncompliant {{Remove or rename this key; it overrides a built-in LogRecord attribute.}}


# ==== Compliant: lazy formatting ====

logging.info("%s - Something happened", user)
logging.debug("User %s did %d actions", user, count)
logging.warning("Count is %d", count)
logger.info("%s logged in", user)
logger.debug("User %s did %d actions", user, count)
logger.error("Error for %s", user)
logger.critical("Critical for %s", user)


# ==== Compliant: plain string messages ====

logging.info("Application started")
logging.error("An unexpected error occurred")
logger.warning("Low memory warning")


# ==== Compliant: non-deprecated methods ====

logging.warning("Something happened")
logger.warning("Something happened")


# ==== Compliant: valid format specifiers ====

logging.info("Value: %s", user)
logging.debug("Count: %d, ratio: %f", count, 3.14)
logging.info("Hex: %x, oct: %o", count, count)
logging.debug("Repr: %r", user)
logging.info("100%% done")


# ==== Compliant: extra with custom (non-reserved) keys ====

logging.info("Request processed", extra={"request_id": "abc123", "duration_ms": 42})
logger.info("User action", extra={"user_id": 99, "action": "login"})


# ==== Compliant: exc_info keyword argument ====

try:
    pass
except Exception:
    logging.error("Something failed", exc_info=True)


# ==== Compliant: no positional arguments ====

logging.info()
logging.debug("Static message with no format args")


# ==== Compliant: extra with ** unpacking (FN accepted) ====

extra_data = {"name": "override"}
logging.info("msg", extra=extra_data)
logging.info("msg", extra={**extra_data})


# ==== Compliant: + with non-string operands ====

a = 1
b = 2
logging.info("%d", a + b)
logging.info("%d", a % b)


# ==== Compliant: empty f-string (no formatted expressions) ====

logging.info(f"static message")
