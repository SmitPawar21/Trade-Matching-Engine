# Java Matching Engine Architecture & Low-Level Design

## Overview
The Java Matching Engine is a high-performance, event-driven limit order book (LOB) implementation designed for processing trades in a multi-symbol exchange environment. It receives orders via TCP sockets, matches them sequentially per symbol in dedicated background threads, and publishes real-time trade events back to connected clients. It also includes an embedded framework for running autonomous trading agents (such as Reinforcement Learning or Rule-Based Market Makers).

## High-Level Architecture
The system is built on a multi-threaded, non-blocking asynchronous architecture.

1.  **Transport Layer (TCP Socket Server)**: Handles incoming client connections. Clients submit JSON payloads representing order actions.
2.  **Engine Manager**: The central orchestrator that routes incoming requests to the appropriate processing thread based on the trading symbol.
3.  **Symbol Engines (Event Loop)**: Dedicated background threads per symbol (e.g., one for BTC, one for ETH). Each engine maintains a concurrent `BlockingQueue` for incoming events and a local `OrderBook`. This eliminates race conditions during matching.
4.  **Order Book**: The core matching logic, utilizing dual `TreeMap`s to maintain price-time priority.
5.  **Agent/Simulation Layer**: Embedded hooks for market-making agents, including an RL tracker and a custom MLP network inference engine that runs entirely on standard Java without external ML dependencies.

## Module Detailed Breakdown

### 1. `engine` Module (Core Processing)
This module acts as the orchestrator and asynchronous event loop for the system.
*   **`MatchingEngineApp.java`**: The main entry point. It initializes the `EngineManager`, registers trading symbols, seeds initial market liquidity synchronously, and spins up the TCP server and optional simulation runners.
*   **`EngineManager.java`**: Acts as a central router. It maps symbols (e.g., "BTC", "ETH") to their corresponding `SymbolEngine`. When an order is submitted via the network, the manager routes the order to the correct engine's internal queue.
*   **`SymbolEngine.java`**: Runs a dedicated `Thread` containing an infinite `runLoop()`. It polls a `LinkedBlockingQueue<EngineEvent>` to process `NewOrderEvent` and `CancelOrderEvent` sequentially. This single-threaded per-symbol design guarantees that state mutations on the internal `OrderBook` are completely thread-safe without needing expensive locking/synchronization mechanisms.

### 2. `Book` Module (The Order Book)
Handles the fundamental order matching and limit order storage using strictly maintained price-time priority.
*   **`OrderBook.java`**: Maintains dual `TreeMap<Long, PriceLevel>` structures.
    *   `bids`: Stored using `Comparator.reverseOrder()` so the highest price (Best Bid) is always at the top of the tree.
    *   `asks`: Stored in natural ascending order so the lowest price (Best Ask) is at the top of the tree.
    *   **Matching Algorithm**: An incoming order repeatedly attempts to cross the spread (`executeTrade`) by iterating over the opposing side's best `PriceLevel` until the order is fully filled or no crossing liquidity remains. If the incoming order is a limit order with remaining quantity, it's appended to the book.
    *   **Cancel Map**: An `O(1)` `HashMap<Long, OrderReference>` tracks orders by ID to support fast cancellations, though full removal involves some O(N) list traversal (with notes left in the source for future intrusive doubly-linked-list optimizations).
*   **`PriceLevel.java`**: Represents a single price point containing a `Deque` of `Order` objects to strictly preserve time priority (First-In, First-Out).
*   **`OrderReference.java`**: A helper class linking an Order ID to its `PriceLevel` for faster lookups during cancellation.

### 3. `transport` Module (Networking)
*   **`EngineSocketServer.java`**: A standard `ServerSocket` loop that listens for incoming TCP connections on a designated port and spawns a new `ClientHandler` thread for each client.
*   **`ClientHandler.java`**: Parses incoming raw string JSON payloads into `Envelope` wrappers using Jackson. It maps commands like `"NEW_ORDER"` or `"PLACE_RL_ORDER"` into Java POJO `EngineRequest`s and submits them to the `EngineManager`.
*   **`SocketResponsePublisher.java`**: Implements `EngineResponsePublisher`. When the matching engine executes trades or rejects orders, this class converts the resulting domain events back to JSON and pushes them asynchronously to the client's socket `PrintWriter`.

### 4. `model` Module (Domain Objects)
*   **`Order.java` & `Trade.java`**: Standard POJOs encapsulating the properties of an order (id, symbol, price, quantity, side, type) and a matched trade.
*   **`MarketState.java`**: A snapshot class representing the immediate state of the order book (best bid/ask, spread, volume, mid price) and agent-specific tracking metrics (inventory, PnL, imbalance).
*   **Enums**: `OrderSide` (BUY/SELL), `OrderType` (LIMIT), `OrderStatus`.

### 5. `event` Module (Messaging Bus)
An internal pub/sub event system to decouple the core matching logic from network/IO boundaries.
*   **Command Events**: Classes like `NewOrderEvent` and `CancelOrderEvent` act as commands sent *to* the engine.
*   **Response Events**: `OrderAcceptedEvent`, `TradeExecutedEvent`, `OrderFilledEvent`, and `OrderRejectedEvent` are emitted *from* the engine when actions complete. The engine pushes these to an `EngineResponsePublisher` interface, allowing flexible routing (e.g., to a socket, console log, or test framework).

### 6. `agent` & `simulation` Modules (Market Making & RL)
Designed to run autonomous agents directly against the in-memory engine.
*   **`RLStateTracker.java`**: A robust position-tracking system. It records every fill the agent receives, calculates Realized/Unrealized PnL via average-cost logic, and tracks total inventory to trigger Reinforcement Learning episode terminations (e.g., if inventory hits limits or PnL drops below a specific floor).
*   **`ONNXInferenceAgent.java`**: A custom, dependency-free Multilayer Perceptron (MLP) inference engine. Instead of requiring massive libraries like TensorFlow or ONNX Runtime in Java, it parses a JSON file of extracted weights/biases and performs mathematical forward passes (Matrix Multiplications + ReLU) to determine RL actions (Tight Spread, Wide Spread, Aggressive Buy/Sell).
*   **`MarketMakerRunner.java`**: A scheduled thread (running every 100ms) that pulls the latest `MarketState`, feeds it to a `MarketAgent` (rule-based or AI), and automatically places bids and asks based on the agent's decision.
