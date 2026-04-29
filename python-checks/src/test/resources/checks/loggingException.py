import logging
import sys
import traceback

logger = logging.getLogger(__name__)


# ---------- Noncompliant: exc_info=True ----------

def noncompliant_with_exc_info_true():
    try:
        raise RuntimeError("runtime error")
    except RuntimeError:
        logging.error("Runtime error occurred", exc_info=True)  # Noncompliant {{Use "logging.exception()" instead.}}


def noncompliant_named_logger_exc_info_true():
    try:
        risky()
    except Exception:
        logger.error("Named logger error", exc_info=True)  # Noncompliant


def noncompliant_exc_info_true_with_as():
    try:
        risky()
    except Exception as ex:
        logging.error("Error", exc_info=True)  # Noncompliant


# ---------- Noncompliant: exception object referenced in args ----------

def noncompliant_fstring_reference():
    try:
        risky()
    except Exception as ex:
        logging.error(f"Error: {ex}")  # Noncompliant {{Use "logging.exception()" instead.}}


def noncompliant_percent_s_positional():
    try:
        risky()
    except ValueError as ex:
        logging.error("Error: %s", ex)  # Noncompliant


def noncompliant_percent_formatting():
    try:
        risky()
    except ValueError as ex:
        logging.error("Error: %s" % ex)  # Noncompliant


def noncompliant_format_method():
    try:
        risky()
    except ValueError as ex:
        logging.error("Error: {}".format(ex))  # Noncompliant


def noncompliant_direct_pass():
    try:
        risky()
    except Exception as ex:
        logging.error(ex)  # Noncompliant


def noncompliant_str_call_in_concat():
    try:
        risky()
    except Exception as ex:
        logging.error("Error: " + str(ex))  # Noncompliant


def noncompliant_nested_in_call():
    try:
        risky()
    except Exception as ex:
        logging.error(format_exception(ex))  # Noncompliant


def noncompliant_except_group_with_as():
    try:
        risky()
    except* ValueError as eg:
        logging.error(f"Grouped error: {eg}")  # Noncompliant


def noncompliant_named_logger_with_ref():
    try:
        risky()
    except Exception as ex:
        logger.error(f"Named logger: {ex}")  # Noncompliant


def noncompliant_with_extra_kwargs():
    try:
        risky()
    except Exception as ex:
        logging.error(f"Error: {ex}", extra={"key": "val"})  # Noncompliant


def noncompliant_in_nested_except_with_ref():
    try:
        risky()
    except ValueError:
        try:
            other()
        except TypeError as ex:
            logging.error(f"Nested: {ex}")  # Noncompliant


class MyService:
    def noncompliant_in_method_with_ref(self):
        try:
            risky()
        except Exception as ex:
            logging.error("Error in method: %s", ex)  # Noncompliant


# ---------- Noncompliant: traceback.format_exc() in args ----------

def noncompliant_traceback_format_exc():
    try:
        risky()
    except Exception:
        logger.error(traceback.format_exc())  # Noncompliant


def noncompliant_traceback_format_exc_in_fstring():
    try:
        risky()
    except Exception:
        logger.error(f"Error: {traceback.format_exc()}")  # Noncompliant


def noncompliant_traceback_format_exc_positional():
    try:
        risky()
    except Exception:
        logging.error("Error: %s", traceback.format_exc())  # Noncompliant


def noncompliant_traceback_format_exc_no_as():
    # No `as` clause, but traceback.format_exc() is logged → still raise.
    try:
        risky()
    except ValueError:
        logging.error(traceback.format_exc())  # Noncompliant


# ---------- Compliant: no exception involvement (FP fix anchor) ----------

def compliant_no_exception_ref_with_as():
    try:
        risky()
    except Exception as ex:
        logging.error("Cleanup failed")


def compliant_typed_except_no_as():
    try:
        raise ValueError("bad value")
    except ValueError:
        logging.error("Value error occurred")


def compliant_bare_except():
    try:
        risky()
    except:
        logging.error("Something went wrong")


def compliant_except_group_no_as():
    try:
        risky()
    except* ValueError:
        logging.error("Grouped error")


def compliant_nested_except_no_as():
    try:
        risky()
    except ValueError:
        try:
            other()
        except TypeError:
            logging.error("Nested error")


def compliant_named_logger_no_ref():
    try:
        risky()
    except Exception:
        logger.error("Named logger error")


class MyServiceCompliant:
    def compliant_in_method_no_ref(self):
        try:
            risky()
        except Exception:
            logging.error("Error in method")


def compliant_name_shadow(ex):
    try:
        risky()
    except Exception:
        logging.error(f"Context: {ex}")


# ---------- Compliant: exc_info explicitly falsy or unknown ----------

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


def compliant_exc_info_false_with_ref():
    try:
        risky()
    except Exception as ex:
        logging.error(f"Error: {ex}", exc_info=False)


# ---------- Compliant: not directly inside an except clause ----------

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
    except Exception as ex:
        def handle():
            logging.error(f"Inside nested function: {ex}")
        handle()


def compliant_error_in_lambda():
    try:
        risky()
    except Exception as ex:
        handler = lambda: logging.error(f"Inside lambda: {ex}")
        handler()
