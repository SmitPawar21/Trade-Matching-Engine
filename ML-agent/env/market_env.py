import gymnasium as gym
import numpy as np
import socket
import json

from gymnasium import spaces

class MarketMakingEnv(gym.Env):

    def __init__(self):
        super().__init__()

        self.action_space = spaces.Discrete(5)
        self.observation_space = spaces.Box(
            low=-100000,
            high=100000,
            shape=(4,),
            dtype=np.float32
        )
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        
        self.sock.connect(
            ("localhost", 9005)
        )
        self.inventory = 0
    
    def reset(self,seed=None,options=None):
        request = {"type": "RESET_EPISODE"}

        self._send(request)
        response = self._receive()

        self.inventory = 0

        state = np.array([
            response["spread"],
            response["inventory"],
            response["imbalance"],
            response["midPrice"]
        ],dtype=np.float32)

        return state, {}

    def step(self, action):
        request = {
            "type": "RL_ACTION",
            "data": {
                "symbol": "BTC",
                "action": int(action)
            }
        }

        self._send(request)
        response = self._receive()

        # updated state from Java
        spread = response["spread"]
        inventory = response["inventory"]
        imbalance = response["imbalance"]
        mid_price = response["midPrice"]

        # execution info
        realized_pnl = response["realizedPnl"]
        filled_qty = response["filledQty"]
        done = response["done"]

        # reward
        reward = 0

        # pnl reward
        reward += realized_pnl

        # inventory risk penalty
        reward -= (abs(inventory) * 0.05)

        # encourage liquidity
        if filled_qty > 0:
            reward += 1

        next_state = np.array(
            [
                spread,
                inventory,
                imbalance,
                mid_price
            ],
            dtype=np.float32
        )

        return next_state, reward, done, False, {}
    
    
    def _send(self,data):
        message = json.dumps(data) + "\n"
        self.sock.sendall(message.encode())

    def _receive(self):
        data = self.sock.recv(4096).decode()
        return json.loads(data)
        