# Python Reinforcement Learning Trading Model Documentation

## Overview
This module represents the intelligence layer of the trading system. It is a Reinforcement Learning (RL) agent designed to act as a market maker or algorithmic trader. It learns optimal trading strategies by interacting with a simulated market environment using historical price and volume data.

The system is built using **Gymnasium** for environment standardization and **Stable-Baselines3 (SB3)** for the implementation of the Proximal Policy Optimization (PPO) algorithm.

---

## 1. Environment (`env/market_env.py`)
The `HistoricalMarketEnv` is a custom OpenAI Gym (Gymnasium) environment. It acts as the bridge between the market data and the RL agent. 
- **Data Intake:** It takes historical CSV data (specifically `Close` and `Volume` columns) to simulate market states.
- **Episodes:** Training is broken down into episodes (default 500 steps). The starting index for each episode is randomized to prevent the agent from simply memorizing the chronological order of data (preventing overfitting).
- **Step Execution:** On every step, it passes the agent's action to the simulator, calculates the reward, updates the portfolio, and returns the next observation.

## 2. Action Space (`env/actions.py`)
The agent can interact with the market using a **Discrete** action space of size 3:
- `0` (HOLD): Do nothing.
- `1` (BUY): Purchase a fraction of the asset.
- `2` (SELL): Liquidate a fraction of the asset.

## 3. Observation Space (`env/observation.py`)
The observation space is what the agent "sees" at every timestep. It is a Continuous `Box` space combining two distinct sets of data:
1. **Market Features:** Current `Close` price and `Volume`.
2. **Portfolio State:** Normalized values of current `Cash`, `Inventory`, `Unrealized PnL`, `Realized PnL`, and `Total Portfolio Value`.

Combining external market conditions with internal portfolio state allows the agent to make context-aware decisions (e.g., holding off on buying if it already has maximum inventory).

## 4. Simulator (`env/simulator.py`)
This module introduces real-world trading frictions to make the agent robust and realistic:
- **Fractional Trading:** It doesn't bet the whole balance at once. Actions execute a trade representing a specific `trade_fraction` (e.g., 20%) of the available capacity.
- **Commission & Slippage:** Every trade is penalized with a `commission_rate` (default 0.1%) and `slippage_pct` (default 0.05%). For a BUY, execution happens at a slightly higher price than the current close; for a SELL, it executes slightly lower.

## 5. Portfolio Management (`env/portfolio.py`)
A ledger system that tracks:
- Fiat `cash` and asset `inventory`.
- Calculates moving average purchase price to determine **Unrealized PnL**.
- Determines **Realized PnL** after a sell execution.
- Includes normalization methods so neural networks can digest the raw financial numbers without exploding gradients.

## 6. Reward Function (`env/reward.py`)
The reward function is the core driver of the agent's learning process. It is a dense reward function that provides feedback on every step:
- **Base Reward:** Change in total portfolio value (Delta PnL %).
- **Inventory Penalty:** Penalizes the agent for holding too much inventory, discouraging it from turning into a pure "buy and hold" mechanism instead of active trading.
- **Drawdown Penalty:** Penalizes the agent if the portfolio value drops below its historical peak, enforcing risk aversion.
- **Step Penalty:** A tiny negative reward applied on every step to discourage the agent from remaining completely idle.

## 7. The Agent & Training (`agents/train.py`)
- **Algorithm:** Uses **Proximal Policy Optimization (PPO)**. PPO is an actor-critic policy gradient method known for its stability, reliability, and sample efficiency in continuous/discrete control tasks like finance.
- **Policy Network:** Uses an `MlpPolicy` (Multi-Layer Perceptron), meaning the underlying brain is a standard feedforward neural network.
- **Vectorized Environments:** Uses `DummyVecEnv` to wrap the environment, a standard practice in SB3 to streamline batch processing of observations during training.
- **Exploration:** The `ent_coef` (entropy coefficient) is set to 0.05 to encourage the agent to explore different strategies before converging on one.

---

## Technical Summary Stack
- **Core Python:** `pandas`, `numpy`, `copy`
- **RL Framework:** `gymnasium`, `stable-baselines3`
- **Architecture Type:** Episodic, Offline RL (training on historical static data).
