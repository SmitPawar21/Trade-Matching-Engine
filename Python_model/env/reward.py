from env.portfolio import Portfolio

class RewardCalculator:
    def __init__(self, inventory_penalty_weight: float = 0.0, drawdown_penalty_weight: float = 0.0):
        self.inventory_penalty_weight = inventory_penalty_weight
        self.drawdown_penalty_weight = drawdown_penalty_weight
        self.peak_portfolio_value = 0.0
        
    def reset(self, initial_value: float):
        self.peak_portfolio_value = initial_value
        
    def calculate(self, portfolio_before: Portfolio, portfolio_after: Portfolio, trade_costs: float, current_price: float) -> float:
        """
        Calculates dense reward based on ΔPortfolioValue - Costs - Penalties.
        """
        # Calculate Delta Portfolio Value
        val_before = portfolio_before.get_portfolio_value(current_price)
        val_after = portfolio_after.get_portfolio_value(current_price)
        
        # PnL change
        delta_pnl = val_after - val_before
        
        # We already accounted for costs in val_after because cash decreased, 
        # but if we wanted an explicit penalty, we could separate them. 
        # For simplicity, delta_pnl already includes the drop in cash from slippage and commission.
        
        # Inventory Penalty (e.g. penalize holding large positions to encourage turnover)
        inventory_penalty = self.inventory_penalty_weight * portfolio_after.inventory * current_price
        
        # Drawdown Penalty
        self.peak_portfolio_value = max(self.peak_portfolio_value, val_after)
        drawdown = max(0, self.peak_portfolio_value - val_after)
        drawdown_penalty = self.drawdown_penalty_weight * drawdown
        
        reward = delta_pnl - inventory_penalty - drawdown_penalty
        return reward
