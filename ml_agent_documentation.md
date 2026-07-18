# ML-Agent Architecture & Order Simulation

This document provides a comprehensive overview of the `ml-agent` directory. It explains how Python simulates market-making orders, interacts with the Java Matching Engine, and trains a Reinforcement Learning (RL) agent. It is structured to help you present the project clearly and confidently to an interviewer.

---

## 1. The Big Picture: Why Python?

In a high-frequency trading system, the **Java Matching Engine** acts as the exchange—it maintains the order book, matches buyers and sellers, and calculates market state. 

The **Python `ml-agent`** acts as an intelligent **Market Maker**. Market makers provide liquidity by continuously quoting buy and sell prices. The goal of the Python agent is to learn an optimal strategy using **Reinforcement Learning (PPO)** to maximize profit while managing inventory risk.

Instead of hardcoding rules (e.g., "always place a bid $10 below the mid-price"), the Python agent learns through trial and error by interacting with the Java engine in real-time over a TCP socket.

---

## 2. Core Components of the `ml-agent`

The directory is self-contained and heavily relies on a custom, from-scratch implementation of the Proximal Policy Optimization (PPO) algorithm using NumPy. 

### A. The Environment (`env/market_env.py`)
This file is the bridge between the Python RL algorithm and the Java engine. It wraps the Java TCP connection in a standard **Gymnasium** environment interface (`reset()`, `step()`).

- **Communication Protocol**: It uses a raw TCP socket (`localhost:9090`) to send JSON requests to Java and receive JSON responses.
- **The Observation (State)**: At every step, it extracts a 4-dimensional vector from the Java response to represent the market's current state:
  1. `spread`: The difference between the lowest ask and highest bid.
  2. `inventory`: How many units of the asset the agent currently holds.
  3. `imbalance`: The ratio of buy volume to sell volume in the order book.
  4. `midPrice`: The average of the best bid and best ask.
  
  *(Note: These values are normalized to keep neural network gradients stable).*

- **The Actions**: The agent can take 5 discrete actions. Python simply sends an integer (0 to 4) to Java, and Java executes the corresponding logic:
  - `0 (TIGHT_SPREAD)`: Places limit buy/sell orders very close to the mid-price.
  - `1 (WIDE_SPREAD)`: Places limit buy/sell orders further away from the mid-price.
  - `2 (AGGRESSIVE_BUY)`: Submits a market/limit order at the best ask to buy instantly.
  - `3 (AGGRESSIVE_SELL)`: Submits a market/limit order at the best bid to sell instantly.
  - `4 (CANCEL_QUOTES)`: Cancels all currently active orders.

- **The Reward Function**: The environment computes a scalar reward to teach the agent:
  - **+ Reward** for realized/unrealized profit (PnL).
  - **- Penalty** for taking on too much inventory risk (holding too much of the asset).
  - **+ Bonus** for providing liquidity (tightening the spread).

### B. The Brain (`ppo_numpy.py`)
To avoid heavy dependencies like PyTorch during production inference, the project implements a pure-NumPy version of PPO.
- **Neural Networks**: It defines a Multi-Layer Perceptron (MLP) with ReLU activations from scratch. 
- **Policy Network**: Takes the 4D observation and outputs probabilities for the 5 actions.
- **Value Network**: Estimates how good the current state is (used for computing advantages).
- **Optimizer**: Includes a custom Adam optimizer to perform gradient descent using analytically derived backpropagation gradients.

### C. The Training Loop (`train.py`)
This is the entry point for training. 
1. It initializes the `MarketMakingEnv` and the `PPOAgent`.
2. It runs a loop for thousands of timesteps where the agent plays out scenarios (episodes).
3. Every 256 steps, it pauses to compute Generalized Advantage Estimation (GAE) and performs an update on the neural network weights.
4. It periodically saves checkpoints to the `models/` directory as JSON files.

### D. Exporting for Production (`export_model.py` & `export_onnx.py`)
Once the Python agent is fully trained, it is inefficient to run Python alongside Java in a low-latency production environment.
- `export_model.py`: Extracts the raw weights and biases of the NumPy Policy Network and saves them as a JSON file. The Java engine can read this JSON and perform the matrix multiplications natively!
- `export_onnx.py`: Provides an alternative to export Stable Baselines 3 (SB3) models into the industry-standard ONNX format.

### E. Analytics (`visualize.py`)
A utility script that runs a trained agent (or a random agent) and uses `matplotlib` to plot its behavior over time. It generates graphs for Mid Price, Spread, Inventory, and Cumulative Reward, making it easy to visually verify if the agent is learning a profitable strategy.

---

## 3. How the Simulation Works (Step-by-Step Flow)

If an interviewer asks, *"Walk me through exactly how an order gets simulated in your ML pipeline,"* you can explain this loop:

1. **State Request**: Python sends a `PLACE_RL_ORDER` request (containing an `actionIdx`) via TCP socket to the Java Matching Engine.
2. **Java Translation**: The `ClientHandler.java` server receives the action index. For example, if it receives `0` (TIGHT_SPREAD), Java generates two Limit Orders (one Buy, one Sell) and injects them into the Order Book.
3. **Engine Matching**: The Java Matching Engine attempts to match these new limit orders against existing market liquidity. It updates the agent's inventory, Unrealized PnL, and Realized PnL.
4. **State Response**: Java calculates the new market state (Spread, Mid Price, Imbalance) and sends it back to Python as a JSON string.
5. **Python Processing**: `market_env.py` parses the JSON, normalizes the state array, calculates the reward based on PnL changes, and returns `(state, reward, done)` to the PPO algorithm.
6. **Learning**: The PPO agent stores this transition in its buffer. Once the buffer is full, it calculates gradients and updates its neural network to make better decisions in the future.

---

## 4. Key Talking Points for Interviews
- **Low-Latency Design**: By using TCP sockets for training and exporting the model to JSON for native Java inference, the architecture avoids the overhead of HTTP APIs or Python-to-Java bridges during live trading.
- **Custom ML Implementation**: Highlight that you didn't just use a black-box library; the neural networks and PPO algorithms are implemented from scratch in pure NumPy, demonstrating a deep understanding of ML math and backpropagation.
- **Risk Management**: Emphasize the reward function in `market_env.py`—it doesn't just chase profit, it actively penalizes quadratic inventory holding, mimicking real-world quant trading risk controls.
