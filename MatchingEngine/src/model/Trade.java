package model;

import java.util.Date;

public class Trade {
	private long tradeId;
    private long buyOrderId;
    private long sellOrderId;
    private long price;
    private long quantity;
    private Date timestamp;
    
	public Trade(long tradeId, long buyOrderId, long sellOrderId, long price, long quantity, Date timestamp) {
		super();
		this.tradeId = tradeId;
		this.buyOrderId = buyOrderId;
		this.sellOrderId = sellOrderId;
		this.price = price;
		this.quantity = quantity;
		this.timestamp = timestamp;
	}
	public long getTradeId() {
		return tradeId;
	}
	public void setTradeId(long tradeId) {
		this.tradeId = tradeId;
	}
	public long getBuyOrderId() {
		return buyOrderId;
	}
	public void setBuyOrderId(long buyOrderId) {
		this.buyOrderId = buyOrderId;
	}
	public long getSellOrderId() {
		return sellOrderId;
	}
	public void setSellOrderId(long sellOrderId) {
		this.sellOrderId = sellOrderId;
	}
	public long getPrice() {
		return price;
	}
	public void setPrice(long price) {
		this.price = price;
	}
	public long getQuantity() {
		return quantity;
	}
	public void setQuantity(long quantity) {
		this.quantity = quantity;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
    
    
}
