import numpy as np

class Portfolio:
    def __init__(self, initial_cash: float = 10000.0):
        self.initial_cash = initial_cash
        self.cash = initial_cash
        self.inventory = 0.0
        self.average_price = 0.0
        self.realized_pnl = 0.0
        self.unrealized_pnl = 0.0
        
    def reset(self):
        self.cash = self.initial_cash
        self.inventory = 0.0
        self.average_price = 0.0
        self.realized_pnl = 0.0
        self.unrealized_pnl = 0.0
        
    def update_from_trade(self, action_type: str, trade_size: float, price: float, cost: float):
        """
        Updates portfolio based on a trade.
        action_type: 'BUY' or 'SELL'
        trade_size: positive amount of asset traded
        price: execution price
        cost: total cost (commission + slippage) in fiat
        """
        self.cash -= cost
        self.realized_pnl -= cost # Costs immediately hit realized PnL
        
        if action_type == 'BUY':
            cost_of_purchase = trade_size * price
            self.cash -= cost_of_purchase
            
            # Update average price (Value weighted)
            total_value = (self.inventory * self.average_price) + cost_of_purchase
            self.inventory += trade_size
            self.average_price = total_value / self.inventory
            
        elif action_type == 'SELL':
            revenue = trade_size * price
            self.cash += revenue
            
            # Calculate PnL on this trade
            trade_pnl = (price - self.average_price) * trade_size
            self.realized_pnl += trade_pnl
            
            self.inventory -= trade_size
            
            if self.inventory <= 0:
                self.inventory = 0.0
                self.average_price = 0.0
                
    def update_unrealized_pnl(self, current_price: float):
        if self.inventory > 0:
            self.unrealized_pnl = (current_price - self.average_price) * self.inventory
        else:
            self.unrealized_pnl = 0.0
            
    def get_portfolio_value(self, current_price: float) -> float:
        return self.cash + (self.inventory * current_price)
        
    def get_state_array(self, current_price: float) -> np.ndarray:
        self.update_unrealized_pnl(current_price)
        # Returns [Cash, Inventory, UnrealizedPnL, RealizedPnL, TotalValue]
        return np.array([
            self.cash,
            self.inventory,
            self.unrealized_pnl,
            self.realized_pnl,
            self.get_portfolio_value(current_price)
        ], dtype=np.float32)
