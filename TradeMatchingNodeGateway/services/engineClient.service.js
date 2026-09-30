import net from "net";
import { JAVA_ENGINE_PORT } from "../ports.js";
import { broadcast } from "../websocket/wss.server.js";

const client = new net.Socket();

client.connect(JAVA_ENGINE_PORT, '127.0.0.1', () => {
    console.log("Connected to Java Engine");
});

let buffer = '';

client.on('data', (data) => {
    buffer += data.toString();
    
    // Java sends newline-delimited JSON — split and broadcast each complete message
    const lines = buffer.split('\n');
    buffer = lines.pop(); // Keep the last (possibly incomplete) chunk in the buffer
    
    for (const line of lines) {
        const trimmed = line.trim();
        if (trimmed.length === 0) continue;
        
        console.log("Received from Java engine:", trimmed);
        broadcast(trimmed);
    }
});
client.on('error', (err) => {
    console.error("Socket error with Java Engine:", err.message);
});

client.on('close', () => {
    console.log("Connection to Java Engine closed");
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