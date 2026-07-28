import pandas as pd
import numpy as np
import pandas_ta as ta

# Disable Numba to prevent Python 3.12 compatibility crash in pandas_ta
if hasattr(ta, 'pbase'):
    ta.pbase.use_numba = False

def add_basic_indicators(df: pd.DataFrame) -> pd.DataFrame:
    """
    Adds basic technical indicators to the dataframe using pandas-ta.
    Assumes dataframe has OHLCV columns: Open, High, Low, Close, Volume.
    """
    # Copy to avoid SettingWithCopyWarning
    df = df.copy()

    # Ensure required columns exist
    required_cols = ['Open', 'High', 'Low', 'Close', 'Volume']
    missing_cols = [col for col in required_cols if col not in df.columns]
    if missing_cols:
        raise ValueError(f"Missing required columns for indicators: {missing_cols}")

    # RSI
    df.ta.rsi(length=14, append=True)
    
    # MACD
    df.ta.macd(fast=12, slow=26, signal=9, append=True)
    
    # Bollinger Bands
    df.ta.bbands(length=20, std=2, append=True)
    
    # Moving Averages
    df.ta.sma(length=20, append=True)
    df.ta.ema(length=50, append=True)
    df.ta.ema(length=200, append=True)
    
    # ATR (Average True Range) for volatility
    df.ta.atr(length=14, append=True)

    return df
