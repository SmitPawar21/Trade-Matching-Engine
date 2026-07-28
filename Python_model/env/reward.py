from env.portfolio import Portfolio

class RewardCalculator:
    def __init__(self, inventory_penalty_weight: float = 0.0, drawdown_penalty_weight: float = 0.0, step_penalty: float = 0.0001):
        self.inventory_penalty_weight = inventory_penalty_weight
        self.drawdown_penalty_weight = drawdown_penalty_weight
        self.step_penalty = step_penalty
        self.peak_portfolio_value = 0.0
        self.initial_value = 1.0
        
    def reset(self, initial_value: float):
        self.peak_portfolio_value = initial_value
        self.initial_value = initial_value if initial_value > 0 else 1.0
        
    def calculate(self, portfolio_before: Portfolio, portfolio_after: Portfolio, trade_costs: float, current_price: float) -> float:
        """
        Calculates dense reward based on scaled ΔPortfolioValue - Costs - Penalties.
        """
        # Calculate Delta Portfolio Value
        val_before = portfolio_before.get_portfolio_value(current_price)
        val_after = portfolio_after.get_portfolio_value(current_price)
        
        # PnL change as a percentage of initial value
        delta_pnl_pct = (val_after - val_before) / self.initial_value
        
        # Inventory Penalty (scaled)
        inventory_val = portfolio_after.inventory * current_price
        inventory_penalty = self.inventory_penalty_weight * (inventory_val / self.initial_value)
        
        # Drawdown Penalty (scaled)
        self.peak_portfolio_value = max(self.peak_portfolio_value, val_after)
        drawdown = max(0, self.peak_portfolio_value - val_after)
        drawdown_penalty = self.drawdown_penalty_weight * (drawdown / self.initial_value)
        
        # Final reward incorporates a small step penalty to discourage sitting idle forever
        reward = delta_pnl_pct - inventory_penalty - drawdown_penalty - self.step_penalty
        return reward
