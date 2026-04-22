from itertools import groupby

# Used by GroupByIteratorReuseCheckTest.no_quick_fix_when_safe_read_follows_unsafe_one
# Wrapping the unsafe read in list() would exhaust the iterator and silently break
# the subsequent `for item in group:` loop, so no quickfix should be attached.

data = [1, 1, 2, 2, 3]
groups = {}
for key, group in groupby(data):
    groups[key] = group  # Noncompliant
    for item in group:
        print(item)
