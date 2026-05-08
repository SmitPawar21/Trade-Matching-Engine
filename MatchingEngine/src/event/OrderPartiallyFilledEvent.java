package event;

public class OrderPartiallyFilledEvent implements EngineResponse {
	private final String eventType = "ORDER_PARTIALLY_FILLED";
	
	private final String symbol;
	private final long orderId;
	
	private final long remainingQty;
	
	public OrderPartiallyFilledEvent(String symbol, long orderId, long remainingQty) {
		super();
		this.symbol = symbol;
		this.orderId = orderId;
		this.remainingQty = remainingQty;
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

	public long getRemainingQty() {
		return remainingQty;
	}
}
