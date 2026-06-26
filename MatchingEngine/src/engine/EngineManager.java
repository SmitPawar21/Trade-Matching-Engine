package engine;

import java.util.*;

import event.CancelOrderEvent;
import event.EngineResponsePublisher;
import event.ErrorEvent;
import event.NewOrderEvent;
import event.NoOpPublisher;
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
    
    private void printOrder(Order order) {
    	System.out.println("New Order: \nOrder id: "+order.getOrderId()+"\nSymbol: "+order.getSymbol()+"\nPrice: "+order.getPrice()+"\nQuantity: "+order.getQuantity()+"\n");
    }
    
    /**
     * Synchronously seed an order into the book.
     * Bypasses async queue — call only during startup.
     */
    public void seedOrder(Order order) {
        SymbolEngine engine = engines.get(order.getSymbol());
        if (engine == null) {
            System.err.println("No engine for symbol: " + order.getSymbol());
            return;
        }
        engine.seedOrder(order);
        printOrder(order);
    }
    
    public void submitOrder(Order order, EngineResponsePublisher publisher) {

        if (publisher == null) {
            publisher = NoOpPublisher.INSTANCE;
        }

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
        printOrder(order);
    }

    public void cancelOrder(String symbol, long orderId, EngineResponsePublisher publisher) {

        if (publisher == null) {
            publisher = NoOpPublisher.INSTANCE;
        }

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

