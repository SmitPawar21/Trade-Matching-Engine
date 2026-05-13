package engine;

import java.util.*;

import event.CancelOrderEvent;
import event.EngineResponsePublisher;
import event.ErrorEvent;
import event.NewOrderEvent;
import model.Order;

public class EngineManager {
	private final Map<String, SymbolEngine> engines = new HashMap<>();
	
	public void addSymbol(String symbol) {
        SymbolEngine engine = new SymbolEngine(symbol);
        engines.put(symbol, engine);
    }

    public void startAll() {
        for (SymbolEngine engine : engines.values()) {
            engine.start();
        }
    }
    
    public void submitOrder(Order order, EngineResponsePublisher publisher) {

        SymbolEngine engine = engines.get(order.getSymbol());

        if (engine == null) {
        	publisher.publish(
        	        new ErrorEvent(
        	                "No engine for symbol: "
        	                + order.getSymbol()
        	        )
        	);

        	return;
        }

        engine.submitEvent(new NewOrderEvent(order, publisher));
    }

    public void cancelOrder(String symbol, long orderId, EngineResponsePublisher publisher) {

        SymbolEngine engine = engines.get(symbol);

        if (engine == null) {
        	publisher.publish(
        	        new ErrorEvent(
        	                "No engine for symbol: "
        	                + symbol
        	        )
        	);

        	return;
        }

        engine.submitEvent(new CancelOrderEvent(symbol, orderId, publisher));
    }
    
    public SymbolEngine getEngine(
            String symbol) {

        return engines.get(
                symbol
        );
    }

}
