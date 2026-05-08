package event;

public class OrderCancelledEvent implements EngineResponse {
	private final String eventType = "ORDER_CANCELLED";
	
	private final String symbol;
	private final long orderId;
	
	public OrderCancelledEvent(String symbol, long orderId) {
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
