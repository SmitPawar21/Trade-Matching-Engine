package agent;

import Book.OrderBook;
import engine.EngineManager;
import engine.SymbolEngine;
import model.MarketState;

public class OrderBookStateProvider implements MarketStateProvider {
	private final EngineManager engineManager;


	public OrderBookStateProvider(EngineManager engineManager) {
		this.engineManager = engineManager;
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


        MarketState state =
                new MarketState();
        
        if (orderBook.getBestBid()
                != null) {

            state.setBestBid(

                    orderBook
                            .getBestBid()
                            .getPrice()
            );
        }
        
        if (orderBook.getBestAsk()
                != null) {

            state.setBestAsk(

                    orderBook
                            .getBestAsk()
                            .getPrice()
            );
        }
        
        double bestBid =
                state.getBestBid();

        double bestAsk =
                state.getBestAsk();
        
        if (bestBid > 0 && bestAsk > 0) {
            state.setSpread(bestAsk - bestBid);

            state.setMidPrice(
                    (bestBid + bestAsk)/ 2.0
            );
        }

        return state;
	}

}
