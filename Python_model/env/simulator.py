from env.portfolio import Portfolio
from env.actions import TradeAction

class Simulator:
    def __init__(self, commission_rate: float = 0.001, slippage_pct: float = 0.0005, fixed_trade_size: float = 1.0):
        self.commission_rate = commission_rate
        self.slippage_pct = slippage_pct
        self.fixed_trade_size = fixed_trade_size # E.g., trade 1 unit of asset at a time

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
            
            trade_size = min(self.fixed_trade_size, max_size_possible)
            
            if trade_size > 0:
                # Apply slippage (buy higher)
                fill_price = current_price * (1 + self.slippage_pct)
                commission = (trade_size * fill_price) * self.commission_rate
                
                trade_costs = commission + ((fill_price - current_price) * trade_size)
                
                portfolio.update_from_trade('BUY', trade_size, fill_price, commission)
                
        elif action == TradeAction.SELL:
            # Check if enough inventory to sell
            trade_size = min(self.fixed_trade_size, portfolio.inventory)
            
            if trade_size > 0:
                # Apply slippage (sell lower)
                fill_price = current_price * (1 - self.slippage_pct)
                commission = (trade_size * fill_price) * self.commission_rate
                
                trade_costs = commission + ((current_price - fill_price) * trade_size)
                
                portfolio.update_from_trade('SELL', trade_size, fill_price, commission)
                
        return trade_costs
