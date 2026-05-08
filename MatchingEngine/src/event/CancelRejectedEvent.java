package event;

public class CancelRejectedEvent implements EngineResponse {
	private final String eventType = "CANCEL_REJECTED";
	
	private final String symbol;
	private final long OrderId;
	private final String message;
	
	public CancelRejectedEvent(String symbol, long orderId, String message) {
		super();
		this.symbol = symbol;
		OrderId = orderId;
		this.message = message;
	}
	
	public String getEventType() {
		return eventType;
	}
	public String getSymbol() {
		return symbol;
	}
	public long getOrderId() {
		return OrderId;
	}
	public String getMessage() {
		return message;
	}
}
