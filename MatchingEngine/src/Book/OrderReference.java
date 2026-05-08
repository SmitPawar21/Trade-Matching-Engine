package Book;

import model.*;

public class OrderReference {
	private OrderSide side;
    private long price;
    private Order order;
    
    public OrderReference(Order order) {
        this.order = order;
    }

    public OrderReference(OrderSide side, long price, Order order) {
        this.side = side;
        this.price = price;
        this.order = order;
    }

    public OrderSide getSide() {
        return side;
    }

    public long getPrice() {
        return price;
    }

    public Order getOrder() {
        return order;
    }

    public void setSide(OrderSide side) {
        this.side = side;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public void setOrder(Order order) {
        this.order = order;
    }
    
}
