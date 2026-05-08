package Book;
import java.util.*;

import model.Order;

public class PriceLevel {
	private long price;
	private Deque<Order> orders;
	
	public PriceLevel(long price) {
        this.price = price;
        this.orders = new ArrayDeque<>();
    }

    public long getPrice() {
        return price;
    }

    public Deque<Order> getOrders() {
        return orders;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public void addOrder(Order order) {
        orders.add(order);
    }
}
