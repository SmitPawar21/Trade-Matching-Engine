from env.portfolio import Portfolio
from env.actions import TradeAction

class Simulator:
    def __init__(self, commission_rate: float = 0.001, slippage_pct: float = 0.0005, trade_fraction: float = 0.2):
        self.commission_rate = commission_rate
        self.slippage_pct = slippage_pct
        self.trade_fraction = trade_fraction # Trade 20% of available capacity at a time

    def execute_trade(self, action: int, current_price: float, portfolio: Portfolio):
        """
        Simulates a trade and updates the portfolio.
        Returns:
            trade_costs (float): The total fiat value lost to slippage and commission.
        """
        trade_costs = 0.0
        
        if action == TradeAction.HOLD:
            return trade_costs
            
        elif action == TradeAction.BUY:
            # Check if enough cash
            max_size_possible = portfolio.cash / (current_price * (1 + self.commission_rate + self.slippage_pct))
            
            # Trade a fraction instead of fixed size to allow scaling
            trade_size = max_size_possible * self.trade_fraction
            
            if trade_size > 0.00001:
                # Apply slippage (buy higher)
                fill_price = current_price * (1 + self.slippage_pct)
                commission = (trade_size * fill_price) * self.commission_rate
                
                trade_costs = commission + ((fill_price - current_price) * trade_size)
                
                portfolio.update_from_trade('BUY', trade_size, fill_price, commission)
                
        elif action == TradeAction.SELL:
            # Check if enough inventory to sell
            trade_size = portfolio.inventory * self.trade_fraction
            
            if trade_size > 0.00001:
                # Apply slippage (sell lower)
                fill_price = current_price * (1 - self.slippage_pct)
                commission = (trade_size * fill_price) * self.commission_rate
                
                trade_costs = commission + ((current_price - fill_price) * trade_size)
                
                portfolio.update_from_trade('SELL', trade_size, fill_price, commission)
                
        return trade_costs
