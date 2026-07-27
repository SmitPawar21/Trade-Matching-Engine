import numpy as np
from gymnasium import spaces
from env.portfolio import Portfolio

class ObservationBuilder:
    def __init__(self, feature_columns: list):
        self.feature_columns = feature_columns
        self.num_market_features = len(feature_columns)
        self.num_portfolio_features = 5 # [Cash, Inventory, UnrealizedPnL, RealizedPnL, TotalValue]
        
    def get_observation_space(self) -> spaces.Box:
        """
        Returns a flat Box space covering market features + portfolio state.
        We use np.inf for bounds as standard neural nets handle normalized unbounded data well.
        """
        total_features = self.num_market_features + self.num_portfolio_features
        return spaces.Box(low=-np.inf, high=np.inf, shape=(total_features,), dtype=np.float32)
        
    def build_observation(self, market_data_row: np.ndarray, portfolio: Portfolio, current_price: float) -> np.ndarray:
        """
        Combines current market candle features with the agent's internal state.
        """
        # Ensure market_data_row is flat
        market_obs = np.array(market_data_row, dtype=np.float32)
        
        # Get portfolio state
        portfolio_obs = portfolio.get_state_array(current_price)
        
        # Concatenate into a single flat array
        full_obs = np.concatenate([market_obs, portfolio_obs])
        return full_obs
