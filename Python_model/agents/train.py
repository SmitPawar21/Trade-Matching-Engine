import sys
import os
from pathlib import Path
import pandas as pd
from stable_baselines3 import PPO
from stable_baselines3.common.vec_env import DummyVecEnv
from stable_baselines3.common.monitor import Monitor

# Add project root to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from env.market_env import HistoricalMarketEnv

def train_agent(data_path: str, model_save_path: str, total_timesteps: int = 100000):
    print(f"Loading data from {data_path}...")
    df = pd.read_csv(data_path)
    
    # 80/20 Train/Test Split
    split_idx = int(len(df) * 0.8)
    train_df = df.iloc[:split_idx].copy()
    test_df = df.iloc[split_idx:].copy()
    print(f"Train data size: {len(train_df)}, Test data size: {len(test_df)}")
    
    # Setup Environment
    # We use a Monitor wrapper to log episode rewards and lengths
    def make_env():
        # max_steps limits each episode length
        env = HistoricalMarketEnv(data_df=train_df, initial_cash=10000.0, max_steps=500, trade_fraction=0.2)
        return Monitor(env)
        
    vec_env = DummyVecEnv([make_env])
    
    # Setup PPO Model
    print("Initializing PPO model...")
    model = PPO(
        "MlpPolicy", 
        vec_env, 
        verbose=1,
        ent_coef=0.05, # Encourage exploration
        tensorboard_log="./logs/ppo_market_maker/"
    )
    
    # Train
    print(f"Starting training for {total_timesteps} timesteps...")
    model.learn(total_timesteps=total_timesteps)
    
    # Save
    os.makedirs(Path(model_save_path).parent, exist_ok=True)
    model.save(model_save_path)
    print(f"Model saved to {model_save_path}")
    
    return train_df, test_df

if __name__ == "__main__":
    # Example usage
    data_file = "data/features/BTC_features.csv"
    model_file = "models/ppo_trading_model"
    
    train_agent(data_file, model_file, total_timesteps=500000)
