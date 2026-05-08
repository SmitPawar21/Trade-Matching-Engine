package event;

public class OrderFilledEvent implements EngineResponse {
	private final String eventType = "ORDER_FILLED";
	
	private final String symbol;
	private final long orderId;
	
	public OrderFilledEvent(String symbol, long orderId) {
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
