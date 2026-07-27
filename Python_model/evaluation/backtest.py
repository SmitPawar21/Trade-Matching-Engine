import sys
import os
from pathlib import Path
import pandas as pd
from stable_baselines3 import PPO

# Add project root to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from env.market_env import HistoricalMarketEnv
from env.actions import TradeAction
from evaluation.metrics import calculate_metrics
from evaluation.plots import plot_backtest_results

def run_backtest(model_path: str, data_path: str):
    print(f"Loading data from {data_path} for backtesting...")
    df = pd.read_csv(data_path)
    
    # We use the LAST 20% of the data for backtesting (assuming the model was trained on the first 80%)
    split_idx = int(len(df) * 0.8)
    test_df = df.iloc[split_idx:].copy()
    
    # Create the testing environment (deterministic, no random start)
    # We set max_steps to the length of the test data so it runs through the whole period
    env = HistoricalMarketEnv(data_df=test_df, initial_cash=10000.0, max_steps=len(test_df), trade_size=1.0)
    # Force start index to 0 for a clean backtest through the whole test set
    env.start_index = 0
    
    # Load model
    print(f"Loading model from {model_path}...")
    try:
        model = PPO.load(model_path)
    except Exception as e:
        print(f"Failed to load model: {e}")
        return
        
    obs, info = env.reset()
    env.current_step_idx = 0 # Force to 0 just in case
    
    # Data collection lists
    history = []
    trades = []
    portfolio_values = []
    
    print("Starting backtest simulation...")
    
    done = False
    step_count = 0
    while not done:
        # Use deterministic=True for evaluation to get the policy's best guess without exploration noise
        action, _states = model.predict(obs, deterministic=True)
        
        # We need to extract the scalar from action array if it is one
        action_scalar = int(action) if hasattr(action, '__int__') else int(action.item())
        action_name = TradeAction(action_scalar).name
        
        # Step the environment
        obs, reward, terminated, truncated, info = env.step(action_scalar)
        
        # Record history
        step_data = {
            'step': step_count,
            'price': env.prices[env.current_step_idx - 1], # The price *before* the step advanced time
            'portfolio_value': info['total_value'],
            'inventory': info['inventory'],
            'action': action_name,
            'reward': reward
        }
        history.append(step_data)
        portfolio_values.append(info['total_value'])
        
        if action_name in ['BUY', 'SELL']:
            trades.append(step_data)
            
        step_count += 1
        done = terminated or truncated
        
    print(f"Backtest complete. Ran for {step_count} steps.")
    
    # Convert history to DataFrame for plotting
    history_df = pd.DataFrame(history)
    
    # Calculate Metrics
    metrics = calculate_metrics(portfolio_values, trades, initial_cash=10000.0)
    print("\n=== Backtest Metrics ===")
    for k, v in metrics.items():
        if isinstance(v, float):
            print(f"{k}: {v:.2f}")
        else:
            print(f"{k}: {v}")
            
    # Generate Plots
    os.makedirs("evaluation", exist_ok=True)
    plot_backtest_results(history_df, save_path="evaluation/backtest_results.png")

if __name__ == "__main__":
    # Example usage
    data_file = "data/features/BTC_features.csv"
    model_file = "models/ppo_trading_model"
    
    run_backtest(model_file, data_file)
