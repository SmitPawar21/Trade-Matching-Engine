package transport.dto;

public class EngineRequest {
    private String type;

    private String symbol;

    private long orderId;

    private long userId;

    private String side;

    private String orderType;

    private long price;

    private long quantity;
    
    private int actionIdx;

    public EngineRequest() {
    }

    public String getType() {
        return type;
    }

    public String getSymbol() {
        return symbol;
    }

    public long getOrderId() {
        return orderId;
    }

    public long getUserId() {
        return userId;
    }

    public String getSide() {
        return side;
    }

    public String getOrderType() {
        return orderType;
    }

    public long getPrice() {
        return price;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

	public int getActionIdx() {
		return actionIdx;
	}

	public void setActionIdx(int actionIdx) {
		this.actionIdx = actionIdx;
	}
}
