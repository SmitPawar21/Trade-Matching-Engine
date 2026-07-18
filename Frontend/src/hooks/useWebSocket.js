import { useState, useEffect, useCallback, useRef } from 'react';

const WS_URL = 'ws://localhost:8080';

export const useWebSocket = () => {
  const [events, setEvents] = useState([]);
  const [prices, setPrices] = useState({ BTC: 0, ETH: 0 });
  const [isConnected, setIsConnected] = useState(false);
  const ws = useRef(null);

  const connect = useCallback(() => {
    ws.current = new WebSocket(WS_URL);

    ws.current.onopen = () => {
      console.log('WebSocket Connected');
      setIsConnected(true);
    };

    ws.current.onclose = () => {
      console.log('WebSocket Disconnected. Reconnecting...');
      setIsConnected(false);
      setTimeout(connect, 2000);
    };

    ws.current.onerror = (err) => {
      console.error('WebSocket Error:', err);
      ws.current.close();
    };

    ws.current.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        
        // Add to event feed (keep last 50)
        setEvents((prev) => {
          const newEvents = [data, ...prev].slice(0, 50);
          return newEvents;
        });

        // Update mid prices based on GET_STATE or regular event payload if it includes it
        // If it's a TRADE event, update the price
        if (data.type === 'TradeExecutedEvent') {
            const { symbol, tradePrice } = data;
            setPrices(prev => ({
                ...prev,
                [symbol]: tradePrice
            }));
        }

      } catch (err) {
        console.error('Error parsing WS message:', err);
      }
    };
  }, []);

  useEffect(() => {
    connect();
    return () => {
      if (ws.current) {
        ws.current.close();
      }
    };
  }, [connect]);

  // Function to request state from the server if needed
  const sendRequest = (payload) => {
      if (ws.current && ws.current.readyState === WebSocket.OPEN) {
          ws.current.send(JSON.stringify(payload));
      }
  };

  return { events, prices, isConnected, sendRequest, setPrices };
};
