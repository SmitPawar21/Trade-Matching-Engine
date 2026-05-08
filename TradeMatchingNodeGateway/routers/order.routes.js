import express from "express";
import { cancelOrder, placeOrder } from "../controllers/order.controller.js";
const router = express.Router();

router.post('/place', placeOrder);
router.post('/cancel', cancelOrder);

export default router;