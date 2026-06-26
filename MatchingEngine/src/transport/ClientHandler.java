package transport;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.ObjectMapper;

import agent.MarketStateProvider;
import agent.OrderBookStateProvider;
import agent.RLStateTracker;
import agent.RLTradeListener;
import engine.EngineManager;
import event.EngineResponsePublisher;
import model.MarketState;
import model.Order;
import model.OrderSide;
import model.OrderStatus;
import model.OrderType;
import transport.dto.EngineRequest;
import transport.dto.Envelope;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final EngineManager engineManager;
    private MarketStateProvider stateProvider;

    private final ObjectMapper mapper =
            new ObjectMapper();

    /** RL agent uses userId = 9999 to distinguish from real users */
    private static final long RL_USER_ID = 9999;

    /** Thread-safe order ID counter for RL agent orders (starts at 500,000) */
    private static final AtomicLong rlOrderIdCounter =
            new AtomicLong(500_000);

    /** Per-connection RL trade listener (created lazily per symbol) */
    private RLTradeListener rlTradeListener;

    public ClientHandler(
            Socket socket,
            EngineManager engineManager) {

        this.socket = socket;
        this.engineManager = engineManager;
        stateProvider = new OrderBookStateProvider(engineManager);
    }

    @Override
    public void run() {

        System.out.println("ClientHandler started");

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        socket.getInputStream()
                                )
                        );

                PrintWriter writer =
                        new PrintWriter(
                                socket.getOutputStream(),
                                true
                        )
        ) {

            EngineResponsePublisher publisher =
                    new SocketResponsePublisher(writer);

            String line;

            while ((line = reader.readLine()) != null) {

                System.out.println(
                        "RAW FROM CLIENT: " + line
                );

                Envelope envelope =
                        mapper.readValue(
                                line,
                                Envelope.class
                        );

                processRequest(
                        envelope,
                        publisher,
                        writer
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processRequest(
            Envelope envelope,
            EngineResponsePublisher publisher,
            PrintWriter writer) {

        String messageType =
                envelope.getType();

        EngineRequest request =
                envelope.getData();

        try {

            if ("NEW_ORDER".equals(messageType)) {

                Order order = new Order(
                        request.getOrderId(),
                        request.getSymbol(),
                        request.getUserId(),

                        OrderSide.valueOf(
                                request.getSide()
                        ),

                        OrderType.valueOf(
                                request.getOrderType()
                        ),

                        request.getPrice(),
                        request.getQuantity(),
                        request.getQuantity(),

                        OrderStatus.NEW,

                        new Date()
                );

                engineManager.submitOrder(
                        order,
                        publisher
                );
            }

            else if ("CANCEL_ORDER".equals(messageType)) {

                engineManager.cancelOrder(
                        request.getSymbol(),
                        request.getOrderId(),
                        publisher
                );
            }

            else if ("GET_STATE".equals(messageType)) {

                String symbol =
                        request.getSymbol();

                MarketState state =
                        stateProvider.getState(
                                symbol
                        );

                writer.println(
                        mapper.writeValueAsString(
                                state
                        )
                );
            }

            else if ("PLACE_RL_ORDER".equals(messageType)) {

                String symbol =
                        request.getSymbol();

                int actionIdx =
                        request.getActionIdx();

                // Ensure RL trade listener exists for this connection
                OrderBookStateProvider obsp = (OrderBookStateProvider) stateProvider;
                RLStateTracker tracker = obsp.getTracker(symbol);

                if (rlTradeListener == null) {
                    rlTradeListener = new RLTradeListener(publisher, tracker);
                }

                // Increment RL step counter
                tracker.incrementStep();

                processRLAction(
                        symbol,
                        actionIdx,
                        rlTradeListener,
                        tracker
                );
                
                MarketState state =
                        stateProvider.getState(symbol);

                writer.println(
                        mapper.writeValueAsString(state)
                );
            }

            else if ("RESET_RL".equals(messageType)) {

                String symbol =
                        request.getSymbol();

                // Reset the RL tracker for a new episode
                OrderBookStateProvider obsp = (OrderBookStateProvider) stateProvider;
                RLStateTracker tracker = obsp.getTracker(symbol);
                tracker.resetEpisode();

                MarketState state =
                        stateProvider.getState(symbol);

                writer.println(
                        mapper.writeValueAsString(state)
                );
            }

            else {

                writer.println(
                        "{\"status\":\"UNKNOWN_REQUEST\"}"
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            writer.println(
                    "{\"status\":\"ERROR\"}"
            );
        }
    }

    /**
     * Convert an RL action index into real orders submitted to the engine.
     * 
     * Actions:
     *   0 = TIGHT_SPREAD:    BID at mid-50,  ASK at mid+50
     *   1 = WIDE_SPREAD:     BID at mid-150, ASK at mid+150
     *   2 = AGGRESSIVE_BUY:  BID at bestAsk (crosses spread, will fill immediately)
     *   3 = AGGRESSIVE_SELL:  ASK at bestBid (crosses spread, will fill immediately)
     *   4 = CANCEL_QUOTES:   Cancel all active RL agent orders
     */
    private void processRLAction(
            String symbol,
            int action,
            RLTradeListener listener,
            RLStateTracker tracker) {

        MarketState currentState = stateProvider.getState(symbol);
        long midPrice = (long) currentState.getMidPrice();
        long bestBid = (long) currentState.getBestBid();
        long bestAsk = (long) currentState.getBestAsk();

        // Safety: if book is empty, skip
        if (midPrice == 0) {
            System.out.println(symbol + " -> RL action " + action + " skipped (empty book)");
            return;
        }

        switch (action) {

            case 0: // TIGHT_SPREAD
                System.out.println(symbol + " -> TIGHT_SPREAD");
                submitRLOrder(symbol, OrderSide.BUY,  midPrice - 50, 10, listener, tracker);
                submitRLOrder(symbol, OrderSide.SELL, midPrice + 50, 10, listener, tracker);
                break;

            case 1: // WIDE_SPREAD
                System.out.println(symbol + " -> WIDE_SPREAD");
                submitRLOrder(symbol, OrderSide.BUY,  midPrice - 150, 10, listener, tracker);
                submitRLOrder(symbol, OrderSide.SELL, midPrice + 150, 10, listener, tracker);
                break;

            case 2: // AGGRESSIVE_BUY
                System.out.println(symbol + " -> AGGRESSIVE_BUY");
                if (bestAsk > 0) {
                    submitRLOrder(symbol, OrderSide.BUY, bestAsk, 10, listener, tracker);
                }
                break;

            case 3: // AGGRESSIVE_SELL
                System.out.println(symbol + " -> AGGRESSIVE_SELL");
                if (bestBid > 0) {
                    submitRLOrder(symbol, OrderSide.SELL, bestBid, 10, listener, tracker);
                }
                break;

            case 4: // CANCEL_QUOTES
                System.out.println(symbol + " -> CANCEL_QUOTES");
                List<Long> activeIds = tracker.getAndClearActiveOrders();
                for (long orderId : activeIds) {
                    engineManager.cancelOrder(symbol, orderId, listener);
                }
                break;

            default:
                throw new IllegalArgumentException(
                        "Invalid RL Action: "
                        + action
                );
        }
    }

    /**
     * Submit a single RL agent order to the engine.
     */
    private void submitRLOrder(
            String symbol,
            OrderSide side,
            long price,
            long quantity,
            RLTradeListener listener,
            RLStateTracker tracker) {

        long orderId = rlOrderIdCounter.getAndIncrement();

        Order order = new Order(
                orderId,
                symbol,
                RL_USER_ID,
                side,
                OrderType.LIMIT,
                price,
                quantity,
                quantity,
                OrderStatus.NEW,
                new Date()
        );

        // Track the order ID so RLTradeListener can detect fills
        listener.trackOrderId(orderId, side);
        tracker.addActiveOrder(orderId);

        // Submit through the engine (async via event queue)
        engineManager.submitOrder(order, listener);
    }
}