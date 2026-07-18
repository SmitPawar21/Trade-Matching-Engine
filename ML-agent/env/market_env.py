import gymnasium as gym
import numpy as np
import socket
import json

from gymnasium import spaces


class MarketMakingEnv(gym.Env):
    """
    Gymnasium environment for RL market-making agent.

    Connects to the Java matching engine via TCP socket.
    Observation space: [spread, inventory, imbalance, midPrice]
    Action space: Discrete(5)
        0 = TIGHT_SPREAD
        1 = WIDE_SPREAD
        2 = AGGRESSIVE_BUY
        3 = AGGRESSIVE_SELL
        4 = CANCEL_QUOTES

    Episode terminates when Java returns done=True (triggered by
    max steps, inventory limit, or PnL floor exceeded).
    """

    metadata = {"render_modes": []}

    def __init__(self, symbol="BTC", host="localhost", port=9090):
        super().__init__()

        self.symbol = symbol
        self.host = host
        self.port = port

        # 5 discrete actions
        self.action_space = spaces.Discrete(5)

        # Observation: [spread, inventory, imbalance, midPrice]
        self.observation_space = spaces.Box(
            low=-np.inf,
            high=np.inf,
            shape=(4,),
            dtype=np.float32
        )

        self.sock = None
        self.buffer = ""
        self.prev_realized_pnl = 0.0
        self.current_step = 0

    def _connect(self):
        """Establish TCP connection to Java engine."""
        if self.sock is not None:
            try:
                self.sock.close()
            except Exception:
                pass

        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.connect((self.host, self.port))
        self.buffer = ""

    def reset(self, seed=None, options=None):
        super().reset(seed=seed)

        # Reconnect on each episode reset for clean state
        self._connect()

        # Send RESET_RL to Java to reset the RLStateTracker
        request = {
            "type": "RESET_RL",
            "data": {
                "symbol": self.symbol
            }
        }

        self._send(request)
        response = self._receive()

        self.prev_realized_pnl = 0.0
        self.current_step = 0

        state = self._extract_state(response)
        return state, {}

    def step(self, action):
        request = {
            "type": "PLACE_RL_ORDER",
            "data": {
                "symbol": self.symbol,
                "actionIdx": int(action)
            }
        }

        self._send(request)
        response = self._receive()

        self.current_step += 1

        # Extract observation
        state = self._extract_state(response)

        # --- Reward Computation ---
        spread = response.get("spread", 0)
        inventory = response.get("inventory", 0)
        realized_pnl = response.get("realizedPnL", 0)
        unrealized_pnl = response.get("unrealizedPnL", 0)
        recent_trades = response.get("recentTrades", 0)

        reward = 0.0

        # 1. Realized PnL delta (reward for profitable trades)
        pnl_delta = realized_pnl - self.prev_realized_pnl
        reward += pnl_delta * 0.01  # Scale down since prices are in 100000s range
        self.prev_realized_pnl = realized_pnl

        # 2. Unrealized PnL (mark-to-market)
        reward += unrealized_pnl * 0.001

        # 3. Inventory risk penalty (quadratic)
        reward -= (inventory ** 2) * 0.001

        # 4. Liquidity provision reward
        if recent_trades > 0:
            reward += 0.5

        # 5. Spread tightening bonus
        if spread > 0 and spread < 300:
            reward += 0.1

        # --- Episode Termination ---
        # Java side tracks done via RLStateTracker
        done = response.get("done", False)

        terminated = done
        truncated = False

        info = {
            "spread": spread,
            "inventory": inventory,
            "realized_pnl": realized_pnl,
            "unrealized_pnl": unrealized_pnl,
            "recent_trades": recent_trades,
            "step": self.current_step
        }

        return state, reward, terminated, truncated, info

    def _extract_state(self, response):
        """Extract the 4-dimensional observation vector from Java response."""
        return np.array([
            response.get("spread", 0) / 100.0,
            response.get("inventory", 0) / 100.0,
            response.get("imbalance", 0),
            response.get("midPrice", 0) / 100000.0
        ], dtype=np.float32)

    def _send(self, data):
        """Send a JSON message to Java engine (newline-delimited)."""
        message = json.dumps(data) + "\n"
        self.sock.sendall(message.encode())

    def _receive(self):
        """
        Receive a complete JSON line from Java engine.
        Handles TCP fragmentation by buffering until newline.
        """
        while "\n" not in self.buffer:
            chunk = self.sock.recv(4096).decode()
            if not chunk:
                raise ConnectionError("Java engine closed connection")
            self.buffer += chunk

        line, self.buffer = self.buffer.split("\n", 1)
        line = line.strip()

        if not line:
            return self._receive()

        return json.loads(line)

    def close(self):
        """Clean up socket connection."""
        if self.sock is not None:
            try:
                self.sock.close()
            except Exception:
                pass
            self.sock = None