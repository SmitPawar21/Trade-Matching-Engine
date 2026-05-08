import express from "express";
import orderRouter from "./routers/order.routes.js"
import { NODE_SERVER_PORT } from "./ports.js";

const app = express();
const PORT = NODE_SERVER_PORT;

app.use(express.json());

app.use("/api/orders", orderRouter);

app.get("/", (req, res) => {
    return res.status(200).json({message: "Hello Smit"});
});

app.listen(PORT, () => {
    console.log("Server is running on PORT: ", PORT);
});