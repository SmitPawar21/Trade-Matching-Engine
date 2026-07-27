import matplotlib.pyplot as plt
import pandas as pd
import numpy as np

def plot_backtest_results(df: pd.DataFrame, save_path: str = "evaluation/backtest_results.png"):
    """
    Plots the equity curve, asset price with trades, and inventory.
    df must contain: 'step', 'price', 'portfolio_value', 'inventory', 'action'
    """
    # 1: HOLD, 2: BUY, 3: SELL (Assuming actions were mapped back to strings or we mapped them here)
    # Actually, in our env, 0: HOLD, 1: BUY, 2: SELL. We assume 'action' column has string 'BUY', 'SELL', 'HOLD'
    
    fig, (ax1, ax2, ax3) = plt.subplots(3, 1, figsize=(12, 12), gridspec_kw={'height_ratios': [2, 2, 1]})
    
    # 1. Price Chart with Trades
    ax1.plot(df['step'], df['price'], label='Asset Price', color='black', alpha=0.6)
    
    buys = df[df['action'] == 'BUY']
    sells = df[df['action'] == 'SELL']
    
    ax1.scatter(buys['step'], buys['price'], marker='^', color='green', s=100, label='Buy')
    ax1.scatter(sells['step'], sells['price'], marker='v', color='red', s=100, label='Sell')
    ax1.set_title("Price & Trades")
    ax1.legend()
    ax1.grid(True, alpha=0.3)
    
    # 2. Equity Curve
    ax2.plot(df['step'], df['portfolio_value'], label='Portfolio Value', color='blue', linewidth=2)
    ax2.set_title("Equity Curve")
    ax2.legend()
    ax2.grid(True, alpha=0.3)
    
    # 3. Inventory
    ax3.plot(df['step'], df['inventory'], label='Inventory (Asset qty)', color='purple', drawstyle='steps-post')
    ax3.set_title("Agent Inventory")
    ax3.legend()
    ax3.grid(True, alpha=0.3)
    
    plt.tight_layout()
    plt.savefig(save_path)
    print(f"Plot saved to {save_path}")
    plt.close()
