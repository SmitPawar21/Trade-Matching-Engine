package event;

import model.Order;

public class NewOrderEvent implements EngineEvent {
	private final Order order;
	private final EngineResponsePublisher publisher;
	
	public NewOrderEvent(Order order, EngineResponsePublisher publisher) {
        this.order = order;
        this.publisher = publisher;
    }

    public Order getOrder() {
        return order;
    }
    
    public EngineResponsePublisher getPublisher() {
    	return publisher;
    }
}
