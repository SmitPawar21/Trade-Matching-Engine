import WebSocket, { WebSocketServer } from "ws";

const wss = new WebSocketServer({ port: 8080 });

let clients = [];

wss.on('connection', (ws) => {
    clients.push(ws);

    ws.on('close', () => {
        clients = clients.filter(c => c !== ws); 
    });
});

export const broadcast = (message) => {
    clients.forEach((ws) => {
        if (ws.readyState === WebSocket.OPEN) { 
            ws.send(message); 
        }
    });
}