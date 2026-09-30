package simulation;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import agent.MarketStateProvider;
import engine.EngineManager;
import model.MarketState;
import model.Order;
import model.OrderSide;
import model.OrderStatus;
import model.OrderType;

/**
 * Simulates external order flow (retail/noise traders) with
 * trending regimes to create realistic bull/bear runs.
 *
 * Uses a "sentiment" model:
 *  - Sentiment drifts randomly over time (mean-reverting random walk).
 *  - Positive sentiment = more buy pressure (bull run).
 *  - Negative sentiment = more sell pressure (bear run).
 *  - This creates natural trending + mean-reverting price behavior.
 */
public class RetailTraderRunner {
    private final EngineManager engineManager;
    private final MarketStateProvider stateProvider;
    private final String symbol;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Random random = new Random();
    private long orderIdCounter = 300000;

    /**
     * Sentiment ranges from -1.0 (extreme bearish) to +1.0 (extreme bullish).
     * At sentiment = +0.5, there's a 75% chance of a buy order.
     * At sentiment = -0.5, there's a 75% chance of a sell order.
     */
    private double sentiment = 0.0;

    /** How much sentiment drifts each tick (volatility of regime changes) */
    private static final double SENTIMENT_DRIFT = 0.08;

    /** Mean-reversion strength: pulls sentiment back toward 0 over time */
    private static final double MEAN_REVERSION = 0.02;

    public RetailTraderRunner(EngineManager engineManager, MarketStateProvider stateProvider, String symbol) {
        this.engineManager = engineManager;
        this.stateProvider = stateProvider;
        this.symbol = symbol;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::placeRetailOrder, 2, 2, TimeUnit.SECONDS);
    }

    private void placeRetailOrder() {
        try {
            // Evolve sentiment: random drift + mean reversion
            sentiment += (random.nextGaussian() * SENTIMENT_DRIFT) - (MEAN_REVERSION * sentiment);
            sentiment = Math.max(-1.0, Math.min(1.0, sentiment)); // clamp

            MarketState state = stateProvider.getState(symbol);
            
            long bestBid = (long) state.getBestBid();
            long bestAsk = (long) state.getBestAsk();
            
            if (bestBid <= 0 || bestAsk <= 0) return;

            // Convert sentiment to buy probability: sentiment=0 -> 50%, sentiment=+1 -> ~100%
            double buyProbability = 0.5 + (sentiment * 0.4);
            boolean isBuy = random.nextDouble() < buyProbability;
            
            OrderSide side;
            long price;
            
            if (isBuy) {
                side = OrderSide.BUY;
                price = bestAsk; // cross the spread
            } else {
                side = OrderSide.SELL;
                price = bestBid; // cross the spread
            }
            
            // Quantity scales with sentiment strength (stronger conviction = larger orders)
            int baseQty = 1 + random.nextInt(15);
            double sentimentBoost = 1.0 + Math.abs(sentiment) * 1.5; // up to 2.5x
            long quantity = Math.round(baseQty * sentimentBoost);

            String regime = sentiment > 0.2 ? "BULL" : sentiment < -0.2 ? "BEAR" : "NEUTRAL";
            System.out.println(">>> RETAIL [" + regime + " s=" + String.format("%.2f", sentiment) + "]: " 
                    + side + " " + quantity + " " + symbol + " @ " + price);

            Order order = new Order(
                    orderIdCounter++,
                    symbol,
                    8888,
                    side,
                    OrderType.LIMIT,
                    price,
                    quantity,
                    quantity,
                    OrderStatus.NEW,
                    new Date()
            );

            engineManager.submitOrder(order, null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
