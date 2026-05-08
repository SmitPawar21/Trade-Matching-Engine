package event;

public class CancelOrderEvent implements EngineEvent {
	private final long orderId;
	private final String symbol;
	private final EngineResponsePublisher publisher;
	
	public CancelOrderEvent(String symbol, long orderId, EngineResponsePublisher publisher) {
		this.symbol = symbol;
        this.orderId = orderId;
        this.publisher = publisher;
    }

    public long getOrderId() {
        return orderId;
    }
    
    public String getSymbol() {
    	return symbol;
    }
    
    public EngineResponsePublisher getPublisher() {
    	return publisher;
    }
}
