import numpy as np
import pandas as pd

def calculate_metrics(portfolio_values: list, trades: list, initial_cash: float):
    """
    Calculates standard financial metrics from a backtest run.
    portfolio_values: List of portfolio values at each step.
    trades: List of dicts representing trades executed. e.g., {'action': 'BUY', 'price': 50000, 'size': 1.0, 'cost': 10}
    """
    if len(portfolio_values) < 2:
        return {}
        
    final_value = portfolio_values[-1]
    
    # 1. Total PnL
    total_pnl = final_value - initial_cash
    
    # 2. Returns (%)
    total_return_pct = (total_pnl / initial_cash) * 100
    
    # Calculate daily (or step-wise) returns for Sharpe
    values_array = np.array(portfolio_values)
    step_returns = np.diff(values_array) / values_array[:-1]
    
    # 3. Sharpe Ratio (assuming risk-free rate = 0, annualized assuming steps are hours/days - let's just use raw step sharpe for now)
    mean_return = np.mean(step_returns)
    std_return = np.std(step_returns)
    sharpe_ratio = 0.0
    if std_return > 0:
        # Scale by roughly sqrt(365*24) if hourly, or sqrt(252) if daily. 
        # For simplicity, we just return the raw step sharpe or scaled by sqrt(N)
        sharpe_ratio = (mean_return / std_return) * np.sqrt(len(step_returns))
        
    # 4. Max Drawdown
    peak = values_array[0]
    max_drawdown = 0.0
    for val in values_array:
        if val > peak:
            peak = val
        drawdown = (peak - val) / peak
        if drawdown > max_drawdown:
            max_drawdown = drawdown
            
    # 5. Trades & Win Rate
    total_trades = len(trades)
    
    # Approximating win rate by looking at successive PnL or trade profitability.
    # In a real environment, you match BUYs with SELLs. For this simple phase, we'll just count
    # how many times portfolio value increased after a trade action was taken vs decreased.
    # Or, we can just return the raw counts of buy/sell actions for now.
    
    buy_count = sum(1 for t in trades if t['action'] == 'BUY')
    sell_count = sum(1 for t in trades if t['action'] == 'SELL')
    
    return {
        "Total PnL": total_pnl,
        "Total Return (%)": total_return_pct,
        "Sharpe Ratio": sharpe_ratio,
        "Max Drawdown (%)": max_drawdown * 100,
        "Total Trades": total_trades,
        "Buys": buy_count,
        "Sells": sell_count
    }
