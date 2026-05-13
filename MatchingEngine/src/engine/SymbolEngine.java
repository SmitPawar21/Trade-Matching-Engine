package engine;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import Book.OrderBook;
import event.CancelOrderEvent;
import event.EngineEvent;
import event.EngineResponsePublisher;
import event.NewOrderEvent;

public class SymbolEngine {
	private final String symbol;
    private final OrderBook orderBook;
    private final BlockingQueue<EngineEvent> queue;
    
    private Thread matchingThread;
    private volatile boolean running = true;
    
	public SymbolEngine(String symbol) {
		super();
		this.symbol = symbol;
		this.orderBook = new OrderBook();
	    this.queue = new LinkedBlockingQueue<>();
	}
	
	public void start() {
		matchingThread = new Thread(this::runLoop, "MatchingEngine- " + symbol);
        matchingThread.setName("MatchingEngine- " + symbol);
        matchingThread.start();
	}
	
	private void runLoop() {
        while (running) {
            try {
                EngineEvent event = queue.take();
                process(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
	
	private void process(EngineEvent event) {

        if (event instanceof NewOrderEvent) {
        	NewOrderEvent newOrder = (NewOrderEvent) event;

            orderBook.processNewOrder(newOrder.getOrder(), newOrder.getPublisher());
        }

        else if (event instanceof CancelOrderEvent) {
            CancelOrderEvent cancel = (CancelOrderEvent) event;
            orderBook.cancelOrder(cancel.getSymbol(), cancel.getOrderId(), cancel.getPublisher());
        }
    }
	
	public void submitEvent(EngineEvent event) {
        queue.offer(event);
    }

    public String getSymbol() {
        return symbol;
    }

    public void stop() {
        running = false;
        matchingThread.interrupt();
    }
    
    public OrderBook getOrderBook() {

        return orderBook;
    }
    
    
}
