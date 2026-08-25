# 🚀 High-Performance Matching Engine & RL Trading System

Welcome to the **Matching Engine Project**! This system is a high-performance, event-driven limit order book (LOB) exchange coupled with an autonomous Reinforcement Learning (RL) trading agent. It seamlessly bridges a robust Java-based matching engine, a Python-powered AI intelligence layer, and a MERN stack web interface.

## 🎯 Problem

Building a real-time cryptocurrency or stock exchange requires handling thousands of orders per second with **zero race conditions** and strict **price-time priority**. Furthermore, creating a realistic testing environment requires intelligent market-making agents that adapt to market conditions rather than following hardcoded, predictable rules.

This project solves this by separating the matching logic into isolated concurrent threads per symbol and integrating an offline-trained Reinforcement Learning agent to provide dynamic liquidity and autonomous trading.

## 🏗️ Architecture

The system is designed with a multi-threaded, non-blocking asynchronous architecture:

1. **Transport Layer**: A TCP Socket Server receives JSON payloads for order actions.
2. **Engine Manager**: Acts as the central orchestrator, routing incoming requests to the appropriate processing thread based on the trading symbol (e.g., BTC, ETH).
3. **Symbol Engines (Event Loop)**: Dedicated background threads per symbol ensure thread-safety. Orders are processed sequentially via a `LinkedBlockingQueue`, eliminating the need for expensive locking mechanisms.
4. **Order Book**: The core matching logic utilizes dual `TreeMap`s to maintain strict price-time priority.
5. **Agent/Simulation Layer**: An embedded hook for market-making agents. It features a custom MLP network inference engine running purely in Java without heavy ML dependencies.
6. **Intelligence Layer (Python)**: A custom Gymnasium environment trains the agent using the Proximal Policy Optimization (PPO) algorithm, blending market conditions with portfolio state.

## 💻 Tech Stack

* **Core Engine**: Java (Multithreading, Sockets, Concurrent Collections, Jackson)
* **Intelligence / AI Model**: Python, `gymnasium`, `stable-baselines3`, `pandas`, `numpy`
* **Web Ecosystem**: MERN Stack (MongoDB, Express, React, Node.js)

## ✨ Features

* **Blazing Fast Matching**: Lock-free order execution utilizing dedicated threads per symbol.
* **Strict Price-Time Priority**: Maintained effortlessly through a dual `TreeMap` structure.
* **Dependency-Free AI Inference**: Executes ONNX/MLP network matrix multiplications natively in Java for ultra-low latency.
* **Reinforcement Learning Agent**: Autonomous agent trained using PPO, capable of Fractional Trading.
* **Realistic Market Friction**: The simulator accounts for commissions, slippage, and inventory penalties to train robust models.
* **Event-Driven Messaging Bus**: Internal Pub/Sub system decoupling matching logic from network I/O.

## 🚀 How to Run

### 1. Start the Java Engine
Navigate to the engine directory and compile/run the main application:
```bash
# Example compilation and run commands
javac engine/MatchingEngineApp.java
java engine.MatchingEngineApp
```

### 2. Train the RL Agent (Python)
Navigate to the `Python_model` directory to begin training the PPO agent on historical data:
```bash
cd Python_model
python agents/train.py
```

### 3. Start the Web Interface (MERN)
*(Assuming standard MERN structure)*
```bash
# Backend
cd backend && npm install && npm run dev
# Frontend
cd frontend && npm install && npm start
```

## 🔐 Environment Variables

Create a `.env` file in the appropriate directories to configure your instances:

```env
# Java Engine
PORT=8080
TCP_PORT=9090

# MERN Backend
MONGO_URI=mongodb://localhost:27017/matching_engine
JWT_SECRET=your_super_secret_key

# Python Agent
DATA_PATH=./data/historical_data.csv
```

## 📖 API Documentation

The Java Engine uses a TCP Socket server for high-speed communication. Here are some of the standard JSON payloads:

**Send a New Order:**
```json
{
  "command": "NEW_ORDER",
  "symbol": "BTC",
  "price": 50000.0,
  "quantity": 1.5,
  "side": "BUY",
  "type": "LIMIT"
}
```

**Domain Events Received (Asynchronous):**
The server pushes events back to the client via the `EngineResponsePublisher`:
* `OrderAcceptedEvent`
* `TradeExecutedEvent`
* `OrderFilledEvent`
* `OrderRejectedEvent`

## 📸 Screenshots

*(Visuals from the CompliQ Main Assets Folder)*

![Screenshot 1](file:///b:/placement%20course/JAVA%20development/compliqMain/assets/screenshot1.png)
![Screenshot 2](file:///b:/placement%20course/JAVA%20development/compliqMain/assets/screenshot2.png)
![Screenshot 3](file:///b:/placement%20course/JAVA%20development/compliqMain/assets/screenshot3.png)
> **Note:** Ensure your image files are present in the `b:\placement course\JAVA development\compliqMain\assets` directory to view them properly. You can adjust the exact filenames in this README as needed.

## 🧠 Key Design Decisions

* **Thread-Per-Symbol Concurrency**: Instead of synchronizing the entire order book (which creates massive performance bottlenecks), orders are queued to an asynchronous event loop specific to each symbol. This guarantees state mutations are thread-safe and incredibly fast.
* **O(log N) Matching Algorithm**: Utilizing dual `TreeMap`s ensures that finding the best bid or ask takes logarithmic time, keeping the engine highly responsive even under heavy load.
* **Pure Java Inference Engine**: Instead of importing massive machine learning frameworks like TensorFlow into the Java engine, neural network weights are loaded via JSON. The forward pass is computed using raw Java math, saving memory and eliminating dependency bloat.
* **Dense RL Reward Function**: The Python agent is driven by a complex reward function that balances PnL, inventory penalties, and drawdown, preventing it from learning lazy "buy-and-hold" strategies.

## 🚀 Future Improvements

* **O(1) Order Cancellations**: Transitioning the order tracking system to an intrusive doubly-linked list to achieve instantaneous O(1) order cancellations.
* **WebSocket Integration**: Upgrading the transport layer to WebSockets for seamless, real-time integration directly with the React frontend.
* **Advanced Agent Strategies**: Training the PPO agent on order book imbalance and micro-structure data rather than just basic OHLCV candles to improve market-making decisions.
