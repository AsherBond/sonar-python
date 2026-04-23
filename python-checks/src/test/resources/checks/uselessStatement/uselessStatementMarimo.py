import marimo

app = marimo.App()


@app.cell
def _(combined_df):
    combined_df
    return


@app.cell
def _(_fig):
    _fig
    return


@app.cell(hide_code=True)
def _(result_df):
    result_df
    return


@app.cell
def _(df):
    df


@app.cell
def _(x, y):
    x  # Noncompliant
    y
    return


@app.cell
def _(df):
    df
    if True:  # control flow after cell output
        pass


@app.cell
def _(x, y):
    x  # Noncompliant
    if True:  # control flow after non-last expression
        pass
    y


@app.cell
def _(x):
    if True:
        x  # Noncompliant


@app.function
def helper(x):
    x  # Noncompliant
    return x


def not_marimo(x):
    x  # Noncompliant
    return x
