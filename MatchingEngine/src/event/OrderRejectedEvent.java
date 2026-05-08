package event;

public class OrderRejectedEvent implements EngineResponse {
	private final String eventType = "ORDER_REJECTED";
	
	private final String symbol;
	private final long orderId;
	private final String message;
	
	public OrderRejectedEvent(String symbol, long orderId, String message) {
		super();
		this.symbol = symbol;
		this.orderId = orderId;
		this.message = message;
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
	public String getMessage() {
		return message;
	}
}
