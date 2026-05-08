import { cancel_order, sendOrder } from "../services/engineClient.service.js";

export const placeOrder = async (req, res) => {
    try {
        const order = req.body;
        // {
        //     "orderId": 101,
        //     "symbol": "AAPL",
        //     "userId": 5001,
        //     "side": "BUY",
        //     "type": "LIMIT",
        //     "price": 15000,
        //     "quantity": 10
        // }
    
        if(order.price <= 0 || order.quantity <= 0) {
            const err = "Kindly enter valid price and quantity";
            res.status(400).json({message: `Order failed to placed. ${err}`});
        }
    
        await sendOrder(order);
    
        res.status(201).json({message: "Order Placed Successfully"});
    } catch (err) {
        res.status(500).json({message: `Order failed to placed. ${err}`});
    }
}

export const cancelOrder = async (req, res) => {
    try {
        const {symbol, orderId} = req.body;
        // {
        //     "symbol": "BTC",
        //     "orderId": 1001
        // }
    
        await cancel_order(symbol, orderId);
    
        res.status(200).json({message: "Order Cancelled Successfully"});
    } catch (err) {
        res.status(500).json({message: `Order Cancellation Failed. ${err}`});
    }
}