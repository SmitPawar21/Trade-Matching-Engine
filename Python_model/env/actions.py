from enum import IntEnum
from gymnasium import spaces

class TradeAction(IntEnum):
    HOLD = 0
    BUY = 1
    SELL = 2

def get_action_space() -> spaces.Discrete:
    """
    Returns a Discrete action space for Hold (0), Buy (1), Sell (2).
    """
    return spaces.Discrete(3)
