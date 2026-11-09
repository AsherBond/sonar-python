import numpy as np
import pandas as pd
import random
from sklearn.datasets import make_blobs
from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import LogisticRegression


def v2_resolves_final_import():
    from sklearn.svm import SVC as operation
    from sklearn.datasets import make_blobs as operation
    operation()  # Noncompliant


def existing_contract(frame: pd.DataFrame):
    np.random.default_rng()  # Noncompliant
    np.random.default_rng(0)
    make_blobs(random_state=None)  # Noncompliant
    make_blobs(random_state=0)
    RandomForestClassifier()  # Noncompliant
    RandomForestClassifier(random_state=0)
    solver = "sag"
    LogisticRegression(solver=solver)  # Noncompliant
    default_solver = "lbfgs"
    LogisticRegression(solver=default_solver)
    value = None
    make_blobs(random_state=value)  # Noncompliant
    frame.sample()
    generator = random.Random()
    generator.random()


def deferred_improvements(config, data):
    from sklearn.model_selection import train_test_split
    np.random.default_rng(**config)  # Noncompliant
    np.random.Philox(key=0)  # Noncompliant
    np.random.seed(0)
    RandomForestClassifier()  # Noncompliant
    train_test_split(data, random_state=0)
    # Positional values bind to *arrays, so random_state is still missing.
    train_test_split(data, test_size, train_size, 0)  # Noncompliant


def assigned_solver_values(unknown):
    solver = "saga"
    alias = solver
    LogisticRegression(solver=alias)  # Noncompliant
    LogisticRegression(solver=unknown)
    first = second
    second = first
    LogisticRegression(solver=first)
    variable = "sag"
    variable = "lbfgs"
    LogisticRegression(solver=variable)
    reassigned = "lbfgs"
    reassigned = "saga"
    LogisticRegression(solver=reassigned)  # Noncompliant
    either = "saga" if unknown else "lbfgs"
    LogisticRegression(solver=either)
