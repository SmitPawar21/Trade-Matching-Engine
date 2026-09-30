import { cancel_order, sendOrder } from "../services/engineClient.service.js";

export const placeOrder = async (req, res) => {
    try {
        const reqOrder = req.body;
        
        // Map REST payload to Java Engine expected format
        const orderId = reqOrder.orderId || Math.floor(Math.random() * 1000000);
        const engineOrder = {
            orderId: orderId,
            symbol: reqOrder.symbol,
            userId: reqOrder.userId || 1,
            side: reqOrder.side,
            orderType: reqOrder.type || reqOrder.orderType,
            price: Math.round(reqOrder.price),
            quantity: Math.round(reqOrder.quantity)
        };
    
        if(!engineOrder.price || engineOrder.price <= 0 || !engineOrder.quantity || engineOrder.quantity <= 0) {
            const err = "Kindly enter valid price and quantity";
            return res.status(400).json({message: `Order failed to placed. ${err}`});
        }

        await sendOrder(engineOrder);
    
        res.status(201).json({message: "Order Sent", orderId: orderId});
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