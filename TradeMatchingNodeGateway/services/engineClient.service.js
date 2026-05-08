import net from "net";
import { JAVA_ENGINE_PORT } from "../ports.js";
import { broadcast } from "../websocket/wss.server.js";

const client = new net.Socket();

client.connect(JAVA_ENGINE_PORT, '127.0.0.1', () => {
    console.log("Connected to Java Engine");
});

client.on('data', (data) => {
    const message = data.toString();
    console.log("Received message from Java engine:", message);

    broadcast(message);
});

export const sendOrder = (order) => {
    const payload = JSON.stringify({
        type: "NEW_ORDER",
        data: order
    }) + "\n";
    client.write(payload);
}

export const cancel_order = (symbol, orderId) => {
    const payload = JSON.stringify({
        type: "CANCEL_ORDER",
        data: {symbol, orderId}
    }) + "\n";
    client.write(payload);
}