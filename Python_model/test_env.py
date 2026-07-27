import sys
from pathlib import Path

# Add project root to path
sys.path.insert(0, str(Path(__file__).parent))

from env.market_env import HistoricalMarketEnv
from env.actions import TradeAction

def test_environment():
    print("Testing HistoricalMarketEnv...")
    
    # We will use one of the processed feature files
    data_path = "data/features/BTC_features.csv"
    if not Path(data_path).exists():
        print(f"Error: Need data file at {data_path} to test.")
        return
        
    env = HistoricalMarketEnv(
        data_path=data_path,
        initial_cash=10000.0,
        max_steps=100,
        trade_size=1.0 # Buy/sell 1 BTC
    )
    
    obs, info = env.reset()
    print(f"Initial Observation Shape: {obs.shape}")
    print(f"Observation space: {env.observation_space}")
    print(f"Action space: {env.action_space}")
    
    print("\n--- Starting Random Simulation ---")
    env.render()
    
    total_reward = 0
    for i in range(10):
        # Take a random action
        action = env.action_space.sample()
        action_name = TradeAction(action).name
        
        obs, reward, terminated, truncated, info = env.step(action)
        total_reward += reward
        
        print(f"Step {i+1}: Action={action_name}, Reward={reward:.2f}")
        env.render()
        
        if terminated or truncated:
            print("Episode ended.")
            break
            
    print(f"\nTest finished. Total Reward: {total_reward:.2f}")
    
if __name__ == "__main__":
    test_environment()
