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

	public MarketMakerRunner(EngineManager engineManager, MarketAgent agent, MarketStateProvider stateProvider,
			String symbol) {
		super();
		this.engineManager = engineManager;
		this.agent = agent;
		this.stateProvider = stateProvider;
		this.symbol = symbol;
	}
	
	public void start() {
        scheduler.scheduleAtFixedRate(this::runAgent, 0, 100, TimeUnit.MILLISECONDS);
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
		long bidPrice = (long) state.getBestBid();
		long askPrice = (long) state.getBestAsk();
		
		switch(action) {
			case TIGHT_SPREAD:
				bidPrice += 1;
	            askPrice -= 1;
	            break;
            
			case AGGRESSIVE_BUY:
                bidPrice += 2;
                break;
                
			case AGGRESSIVE_SELL:
                askPrice -= 2;
                break;
                
			default:
                break;
		}
		
		submitOrder(OrderSide.BUY,bidPrice);

        submitOrder(OrderSide.SELL,askPrice);
	}
	
	private void submitOrder(OrderSide side, long price) {
		Order order = new Order(orderIdCounter++, symbol, -1, side, OrderType.LIMIT, price, 10, 10, OrderStatus.NEW, new Date());
		engineManager.submitOrder(order,null);
	}
	
}
