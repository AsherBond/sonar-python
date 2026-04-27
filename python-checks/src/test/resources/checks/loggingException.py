import logging
import sys

logger = logging.getLogger(__name__)


def noncompliant_bare_except():
    try:
        risky()
    except:
        logging.error("Something went wrong")  # Noncompliant {{Use "logging.exception()" or explicitly pass "exc_info=False".}}


def noncompliant_typed_except():
    try:
        raise ValueError("bad value")
    except ValueError:
        logging.error("Value error occurred")  # Noncompliant


def noncompliant_with_exc_info_true():
    try:
        raise RuntimeError("runtime error")
    except RuntimeError:
        logging.error("Runtime error occurred", exc_info=True)  # Noncompliant


def compliant_with_exc_info_sys():
    try:
        risky()
    except Exception:
        logging.error("Exception occurred", exc_info=sys.exc_info())


def compliant_with_exc_info_variable(some_var):
    try:
        risky()
    except Exception:
        logging.error("Exception occurred", exc_info=some_var)


def compliant_with_exc_info_function_call():
    try:
        risky()
    except Exception:
        logging.error("Exception occurred", exc_info=should_log_traceback())


def noncompliant_named_logger():
    try:
        risky()
    except Exception:
        logger.error("Named logger error")  # Noncompliant {{Use "logging.exception()" or explicitly pass "exc_info=False".}}


def noncompliant_named_logger_exc_info():
    try:
        risky()
    except Exception:
        logger.error("Named logger error", exc_info=True)  # Noncompliant


def noncompliant_nested_except():
    try:
        risky()
    except ValueError:
        try:
            other()
        except TypeError:
            logging.error("Nested error")  # Noncompliant


def noncompliant_except_group():
    try:
        risky()
    except* ValueError:
        logging.error("Grouped error")  # Noncompliant


class MyService:
    def noncompliant_in_method(self):
        try:
            risky()
        except Exception:
            logging.error("Error in method")  # Noncompliant


def compliant_logging_exception():
    try:
        risky()
    except Exception:
        logging.exception("Exception occurred")


def compliant_named_logger_exception():
    try:
        risky()
    except Exception:
        logger.exception("Named logger exception")


def compliant_error_outside_except():
    logging.error("Not inside an except block")
    if some_condition:
        logging.error("Error in conditional")


def compliant_other_log_levels_in_except():
    try:
        risky()
    except Exception:
        logging.warning("Warning")
        logging.debug("Debug info")
        logging.critical("Critical failure")
        logging.info("Informational message")


def compliant_error_in_finally():
    try:
        risky()
    except Exception:
        logging.exception("Caught exception")
    finally:
        logging.error("Cleanup step")


def compliant_error_in_nested_function():
    try:
        risky()
    except Exception:
        def handle():
            logging.error("Inside nested function, not directly in except")
        handle()


def compliant_error_in_lambda():
    try:
        risky()
    except Exception:
        handler = lambda: logging.error("Inside lambda, not directly in except")
        handler()


def compliant_exc_info_false():
    try:
        risky()
    except Exception:
        logging.error("No traceback wanted", exc_info=False)


def compliant_named_logger_exc_info_false():
    try:
        risky()
    except Exception:
        logger.error("No traceback wanted", exc_info=False)


def compliant_exc_info_none():
    try:
        risky()
    except Exception:
        logging.error("No traceback wanted", exc_info=None)
