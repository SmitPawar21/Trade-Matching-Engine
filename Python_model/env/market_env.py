import gymnasium as gym
import pandas as pd
import numpy as np
import copy
from typing import Optional

from env.portfolio import Portfolio
from env.simulator import Simulator
from env.reward import RewardCalculator
from env.observation import ObservationBuilder
from env.actions import get_action_space

class HistoricalMarketEnv(gym.Env):
    """
    Historical Trading Environment for training RL agents.
    Iterates over static historical data (features) and simulates trades.
    """
    metadata = {"render_modes": ["human"]}
    
    def __init__(self, data_path: Optional[str] = None, data_df: Optional[pd.DataFrame] = None, initial_cash: float = 10000.0, max_steps: int = 500, trade_fraction: float = 0.2):
        super().__init__()
        
        # Load and prepare data
        if data_df is not None:
            self.df = data_df.copy()
            self.df.reset_index(drop=True, inplace=True)
        elif data_path is not None:
            self.df = pd.read_csv(data_path)
        else:
            raise ValueError("Must provide either data_path or data_df")
        
        # Define the price column (assume 'Close' is the execution price for simplicity)
        if 'Close' not in self.df.columns:
            raise ValueError("Data must contain a 'Close' column for pricing.")
            
        self.prices = self.df['Close'].values
        
        # Extract features for observation
        exclude_cols = [col for col in self.df.columns if 'time' in col.lower() or 'date' in col.lower()]
        self.feature_columns = [col for col in self.df.columns if col not in exclude_cols]
        self.features_array = self.df[self.feature_columns].values
        
        # Environment configuration
        self.max_steps = max_steps
        self.current_step_idx = 0
        self.start_index = 0
        self.max_idx = len(self.df) - 1
        
        # Component Initialization
        self.portfolio = Portfolio(initial_cash=initial_cash)
        self.simulator = Simulator(trade_fraction=trade_fraction)
        self.reward_calc = RewardCalculator()
        self.obs_builder = ObservationBuilder(self.feature_columns)
        
        # Gym Spaces
        self.action_space = get_action_space()
        self.observation_space = self.obs_builder.get_observation_space()
        
    def reset(self, seed: Optional[int] = None, options: Optional[dict] = None):
        super().reset(seed=seed)
        
        # Determine start index randomly to prevent overfitting
        max_possible_start = max(0, self.max_idx - self.max_steps - 1)
        if max_possible_start > 0:
            self.start_index = self.np_random.integers(0, max_possible_start)
        else:
            self.start_index = 0
            
        self.current_step_idx = self.start_index
        
        # Reset components
        self.portfolio.reset()
        current_price = self.prices[self.current_step_idx]
        initial_val = self.portfolio.get_portfolio_value(current_price)
        self.reward_calc.reset(initial_val)
        
        # Build first observation
        obs = self.obs_builder.build_observation(
            self.features_array[self.current_step_idx], 
            self.portfolio, 
            current_price
        )
        
        info = self._get_info()
        return obs, info
        
    def step(self, action: int):
        current_price = self.prices[self.current_step_idx]
        
        # Deepcopy portfolio to compare before/after for reward calc if needed
        # (Though we can just use the internal val_before logic if we want, but portfolio changes in place)
        portfolio_before = copy.deepcopy(self.portfolio)
        
        # 1. Execute action
        trade_costs = self.simulator.execute_trade(action, current_price, self.portfolio)
        
        # 2. Advance time
        self.current_step_idx += 1
        
        # Get next price for evaluation
        next_price = self.prices[self.current_step_idx]
        
        # 3. Calculate reward
        reward = self.reward_calc.calculate(portfolio_before, self.portfolio, trade_costs, next_price)
        
        # 4. Build observation
        obs = self.obs_builder.build_observation(
            self.features_array[self.current_step_idx], 
            self.portfolio, 
            next_price
        )
        
        # 5. Check termination
        terminated = False
        if self.portfolio.cash <= 0 and self.portfolio.inventory <= 0:
            terminated = True # Bankrupt
            
        truncated = False
        if (self.current_step_idx - self.start_index) >= self.max_steps:
            truncated = True
        elif self.current_step_idx >= self.max_idx:
            truncated = True
            
        info = self._get_info()
        
        return obs, reward, terminated, truncated, info
        
    def _get_info(self):
        return {
            "step": self.current_step_idx,
            "cash": self.portfolio.cash,
            "inventory": self.portfolio.inventory,
            "realized_pnl": self.portfolio.realized_pnl,
            "unrealized_pnl": self.portfolio.unrealized_pnl,
            "total_value": self.portfolio.get_portfolio_value(self.prices[self.current_step_idx])
        }
        
    def render(self):
        print(f"Step: {self.current_step_idx} | Price: {self.prices[self.current_step_idx]:.2f} | "
              f"Value: {self.portfolio.get_portfolio_value(self.prices[self.current_step_idx]):.2f} | "
              f"Inv: {self.portfolio.inventory} | Cash: {self.portfolio.cash:.2f}")