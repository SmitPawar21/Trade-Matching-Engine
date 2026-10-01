package simulation;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import agent.MarketAgent;
import agent.MarketStateProvider;
import engine.EngineManager;
import model.AgentAction;
import model.MarketState;
import model.Order;
import model.OrderSide;
import model.OrderStatus;
import model.OrderType;

public class MarketMakerRunner {
	private final EngineManager engineManager;

	private final MarketAgent agent;

	private final MarketStateProvider stateProvider;

	private final String symbol;
	
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	private long orderIdCounter = 100000;

	/**
	 * Remember the last valid mid price so we can recover
	 * if one side of the book gets completely drained.
	 */
	private long lastKnownMidPrice = 0;

	public MarketMakerRunner(EngineManager engineManager, MarketAgent agent, MarketStateProvider stateProvider,
			String symbol) {
		super();
		this.engineManager = engineManager;
		this.agent = agent;
		this.stateProvider = stateProvider;
		this.symbol = symbol;
	}
	
	public void start() {
        scheduler.scheduleAtFixedRate(this::runAgent, 0, 1000, TimeUnit.MILLISECONDS);
    }
	
	private void runAgent() {
		try {
			MarketState state = stateProvider.getState(symbol);
			AgentAction action = agent.decide(state);
			
			System.out.println("STATE OF SYMBOL ->"+
				" Symbol: "+symbol+
				" | BestBid: "+state.getBestBid()+
				" | BestAsk: "+state.getBestAsk()+
				" | Spread: "+state.getSpread()+
				" | Mid Price: "+state.getMidPrice()
			);
			
			placeQuotes(state, action);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void placeQuotes(MarketState state, AgentAction action) {
		// CANCEL_QUOTES means do nothing — don't place any orders
		if (action == AgentAction.CANCEL_QUOTES) {
			return;
		}

		long bidPrice = (long) state.getBestBid();
		long askPrice = (long) state.getBestAsk();

		// Track the last valid mid price for recovery
		if (bidPrice > 0 && askPrice > 0) {
			lastKnownMidPrice = (bidPrice + askPrice) / 2;
		}

		// Recovery: if one or both sides are empty, re-seed around last known mid
		if (bidPrice <= 0 || askPrice <= 0) {
			if (lastKnownMidPrice <= 0) {
				return; // No history to recover from
			}
			System.out.println(symbol + " -> RECOVERY: re-seeding book around last mid=" + lastKnownMidPrice);
			long halfSpread = Math.max(50, lastKnownMidPrice / 1000); // ~0.1% spread
			submitOrder(OrderSide.BUY,  lastKnownMidPrice - halfSpread);
			submitOrder(OrderSide.SELL, lastKnownMidPrice + halfSpread);
			return;
		}
		
		switch(action) {
			case TIGHT_SPREAD:
			    long tightOffset = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 11);
				bidPrice += tightOffset;
	            askPrice -= tightOffset;
	            break;
            
            case WIDE_SPREAD:
                long wideOffset = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 11);
                bidPrice -= wideOffset;
                askPrice += wideOffset;
                break;
                
			case AGGRESSIVE_BUY:
                // Cross the spread to buy immediately from the ask
                bidPrice = askPrice;
                break;
                
			case AGGRESSIVE_SELL:
                // Cross the spread to sell immediately to the bid
                askPrice = bidPrice;
                break;
                
			default:
                break;
		}
		
		// Only submit BUY if we are not aggressively selling
		if (action != AgentAction.AGGRESSIVE_SELL) {
		    submitOrder(OrderSide.BUY, bidPrice);
		}

        // Only submit SELL if we are not aggressively buying
        if (action != AgentAction.AGGRESSIVE_BUY) {
            submitOrder(OrderSide.SELL, askPrice);
        }
	}
	
	private void submitOrder(OrderSide side, long price) {
		Order order = new Order(orderIdCounter++, symbol, 7777, side, OrderType.LIMIT, price, 10, 10, OrderStatus.NEW, new Date());
		engineManager.submitOrder(order, null);
	}
	
}
