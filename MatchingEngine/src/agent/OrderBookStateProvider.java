package agent;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import Book.OrderBook;
import Book.PriceLevel;
import engine.EngineManager;
import engine.SymbolEngine;
import model.MarketState;
import model.Order;

/**
 * Computes full MarketState from the live OrderBook + RLStateTracker.
 * 
 * Fields populated:
 *  - bestBid, bestAsk, spread, midPrice  (from OrderBook)
 *  - bidVolume, askVolume                 (sum of top 3 price levels)
 *  - imbalance                            (bidVol-askVol)/(bidVol+askVol)
 *  - inventory, realizedPnL, unrealizedPnL, recentTrades, lastTradePrice (from RLStateTracker)
 *  - done                                 (episode termination flag)
 */
public class OrderBookStateProvider implements MarketStateProvider {
	private final EngineManager engineManager;
	private final Map<String, RLStateTracker> trackers = new HashMap<>();

	public OrderBookStateProvider(EngineManager engineManager) {
		this.engineManager = engineManager;
	}
	
	/**
	 * Get or create the RLStateTracker for a symbol.
	 */
	public RLStateTracker getTracker(String symbol) {
		return trackers.computeIfAbsent(symbol, s -> new RLStateTracker(s));
	}
	
	@Override
    public MarketState getState(String symbol) {
		SymbolEngine engine = engineManager.getEngine(symbol);
		if (engine == null) {
            throw new RuntimeException(
                    "No engine for symbol: "
                            + symbol
            );
        }
		
		OrderBook orderBook =
                engine.getOrderBook();

        MarketState state = new MarketState();
        
        // --- Best Bid / Best Ask ---
        if (orderBook.getBestBid() != null) {
            state.setBestBid(orderBook.getBestBid().getPrice());
        }
        
        if (orderBook.getBestAsk() != null) {
            state.setBestAsk(orderBook.getBestAsk().getPrice());
        }
        
        double bestBid = state.getBestBid();
        double bestAsk = state.getBestAsk();
        
        // --- Spread & Mid Price ---
        if (bestBid > 0 && bestAsk > 0) {
            state.setSpread(bestAsk - bestBid);
            state.setMidPrice((bestBid + bestAsk) / 2.0);
        }

        // --- Volume from top 3 price levels ---
        int bidVolume = computeVolume(orderBook.getBids(), 3);
        int askVolume = computeVolume(orderBook.getAsks(), 3);
        state.setBidVolume(bidVolume);
        state.setAskVolume(askVolume);

        // --- Order Book Imbalance ---
        if (bidVolume + askVolume > 0) {
            state.setImbalance(
                (double)(bidVolume - askVolume) / (bidVolume + askVolume)
            );
        }

        // --- RL Agent State from Tracker ---
        RLStateTracker tracker = getTracker(symbol);
        state.setInventory(tracker.getInventory());
        state.setRealizedPnL(tracker.getRealizedPnL());
        state.setUnrealizedPnL(tracker.computeUnrealizedPnL(state.getMidPrice()));
        state.setRecentTrades(tracker.getRecentTradeCount());
        state.setLastTradePrice(tracker.getLastTradePrice());

        // --- Episode Termination ---
        state.setDone(tracker.isEpisodeDone());

        return state;
	}

	/**
	 * Sum the remaining quantities across the top N price levels.
	 */
	private int computeVolume(TreeMap<Long, PriceLevel> side, int topN) {
		int volume = 0;
		int count = 0;
		for (PriceLevel level : side.values()) {
			if (count >= topN) break;
			for (Order order : level.getOrders()) {
				volume += order.getRemainingQuantity();
			}
			count++;
		}
		return volume;
	}
}
