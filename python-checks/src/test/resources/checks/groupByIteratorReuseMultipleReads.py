from itertools import groupby

# Used by GroupByIteratorReuseCheckTest.no_quick_fix_when_multiple_reads_in_loop_body
# Asserts every unsafe read is reported and none get a quickfix (it would exhaust the iterator
# before the remaining reads execute).

data = [1, 1, 2, 2, 3]
first = []
second = []
third = []
for key, group in groupby(data):
    first.append(group)   # Noncompliant
    second.append(group)  # Noncompliant
    third.append(group)   # Noncompliant
