package model;

import java.util.Date;

public class Order {
	private long orderId;
    private String symbol;
    private long userId;
    private OrderSide side;
    private OrderType type;
    private long price;
    private long quantity;
    private long remainingQuantity;
    private OrderStatus status;
    private Date timestamp;
    
	public Order(long orderId, String symbol, long userId, OrderSide side, OrderType type, long price, long quantity,
			long remainingQuantity, OrderStatus status, Date timestamp) {
		super();
		this.orderId = orderId;
		this.symbol = symbol;
		this.userId = userId;
		this.side = side;
		this.type = type;
		this.price = price;
		this.quantity = quantity;
		this.remainingQuantity = remainingQuantity;
		this.status = status;
		this.timestamp = timestamp;
	}
	public long getOrderId() {
		return orderId;
	}
	public void setOrderId(long orderId) {
		this.orderId = orderId;
	}
	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	public long getUserId() {
		return userId;
	}
	public void setUserId(long userId) {
		this.userId = userId;
	}
	public OrderSide getSide() {
		return side;
	}
	public void setSide(OrderSide side) {
		this.side = side;
	}
	public OrderType getType() {
		return type;
	}
	public void setType(OrderType type) {
		this.type = type;
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
	public long getRemainingQuantity() {
		return remainingQuantity;
	}
	public void setRemainingQuantity(long remainingQuantity) {
		this.remainingQuantity = remainingQuantity;
	}
	public OrderStatus getStatus() {
		return status;
	}
	public void setStatus(OrderStatus status) {
		this.status = status;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
    
    
}
