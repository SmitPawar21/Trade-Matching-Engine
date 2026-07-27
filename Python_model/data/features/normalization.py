import pandas as pd
import numpy as np

def z_score_normalize(df: pd.DataFrame, columns: list) -> pd.DataFrame:
    """
    Applies Z-score normalization to specified columns.
    Z = (X - \mu) / \sigma
    """
    df = df.copy()
    for col in columns:
        if col in df.columns:
            mean = df[col].mean()
            std = df[col].std()
            if std != 0:
                df[col] = (df[col] - mean) / std
    return df

def min_max_normalize(df: pd.DataFrame, columns: list) -> pd.DataFrame:
    """
    Applies Min-Max normalization to specified columns.
    X_scaled = (X - X_min) / (X_max - X_min)
    """
    df = df.copy()
    for col in columns:
        if col in df.columns:
            min_val = df[col].min()
            max_val = df[col].max()
            if max_val != min_val:
                df[col] = (df[col] - min_val) / (max_val - min_val)
    return df

def calculate_log_returns(df: pd.DataFrame, column: str = 'Close') -> pd.DataFrame:
    """
    Calculates log returns for a given price column.
    R_t = ln(P_t / P_{t-1})
    """
    df = df.copy()
    if column in df.columns:
        df[f'{column}_log_return'] = np.log(df[column] / df[column].shift(1))
    return df

def calculate_pct_returns(df: pd.DataFrame, column: str = 'Close') -> pd.DataFrame:
    """
    Calculates simple percentage returns for a given price column.
    R_t = (P_t - P_{t-1}) / P_{t-1}
    """
    df = df.copy()
    if column in df.columns:
        df[f'{column}_pct_return'] = df[column].pct_change()
    return df
