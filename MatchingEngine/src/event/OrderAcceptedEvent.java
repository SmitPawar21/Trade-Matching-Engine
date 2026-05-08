package event;

public class OrderAcceptedEvent implements EngineResponse {
	private final String eventType = "ORDER_ACCEPTED";

    private final String symbol;
    private final long orderId;
    
	public OrderAcceptedEvent(String symbol, long orderId) {
		super();
		this.symbol = symbol;
		this.orderId = orderId;
	}
	
	public String getEventType() {
		return eventType;
	}
	public String getSymbol() {
		return symbol;
	}
	public long getOrderId() {
		return orderId;
	}
}
