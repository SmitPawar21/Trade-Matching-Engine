package event;

public class TradeExecutedEvent implements EngineResponse {
	private final String eventType = "TRADE_EXECUTED";
	
	private final String symbol;
	
	private final long buyOrderId;
	private final long sellOrderId;
	
	private final long tradeQty;
	private final long tradePrice;

	public TradeExecutedEvent(String symbol, long buyOrderId, long sellOrderId, long tradeQty, long tradePrice) {
		super();
		this.symbol = symbol;
		this.buyOrderId = buyOrderId;
		this.sellOrderId = sellOrderId;
		this.tradeQty = tradeQty;
		this.tradePrice = tradePrice;
	}
	public String getEventType() {
		return eventType;
	}
	public String getSymbol() {
		return symbol;
	}
	public long getBuyOrderId() {
		return buyOrderId;
	}
	public long getSellOrderId() {
		return sellOrderId;
	}
	public long getTradeQty() {
		return tradeQty;
	}
	public long getTradePrice() {
		return tradePrice;
	}
	
	
}
