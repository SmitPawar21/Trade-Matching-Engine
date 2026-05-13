import model.Order;
import model.OrderType;

public class Snippet {
	private String validateOrder(Order order) {
	
	    if (order == null) {
	        return "NULL_ORDER";
	    }
	
	    if (order.getOrderId() <= 0) {
	        return "INVALID_ORDER_ID";
	    }
	
	    if (order.getUserId() <= 0) {
	        return "INVALID_USER_ID";
	    }
	
	    if (order.getSide() == null) {
	        return "INVALID_SIDE";
	    }
	
	    if (order.getType() == null) {
	        return "INVALID_ORDER_TYPE";
	    }
	
	    if (order.getQuantity() <= 0) {
	        return "INVALID_QUANTITY";
	    }
	
	    if (order.getRemainingQuantity()
	            != order.getQuantity()) {
	
	        return "INVALID_REMAINING_QUANTITY";
	    }
	
	    if (order.getType() == OrderType.LIMIT
	            &&
	            order.getPrice() <= 0) {
	
	        return "INVALID_PRICE";
	    }
	
	    return null;
	}
}