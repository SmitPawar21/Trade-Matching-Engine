import { useState, useEffect, useCallback, useRef } from 'react';

const WS_URL = 'ws://localhost:8080';

export const useWebSocket = () => {
  const [events, setEvents] = useState([]);
  const [trades, setTrades] = useState([]);
  const [prices, setPrices] = useState({ BTC: 0, ETH: 0 });
  const [prevPrices, setPrevPrices] = useState({ BTC: 0, ETH: 0 });
  const [isConnected, setIsConnected] = useState(false);
  const [stats, setStats] = useState({ totalTrades: 0, totalVolume: 0 });
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
        const timestamp = new Date().toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' });
        const enrichedData = { ...data, _timestamp: timestamp };
        
        // Add to event feed (keep last 100)
        setEvents((prev) => [enrichedData, ...prev].slice(0, 100));

        // Handle trade events — Java sends eventType: "TRADE_EXECUTED"
        if (data.eventType === 'TRADE_EXECUTED') {
          // Update price with direction tracking
          setPrices(prev => {
            setPrevPrices({ ...prev });
            return {
              ...prev,
              [data.symbol]: data.tradePrice
            };
          });

          // Add to trade history (keep last 50)
          setTrades(prev => [enrichedData, ...prev].slice(0, 50));

          // Update stats
          setStats(prev => ({
            totalTrades: prev.totalTrades + 1,
            totalVolume: prev.totalVolume + (data.tradeQty || 0)
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

  const sendRequest = (payload) => {
    if (ws.current && ws.current.readyState === WebSocket.OPEN) {
      ws.current.send(JSON.stringify(payload));
    }
  };

  return { events, trades, prices, prevPrices, isConnected, stats, sendRequest, setPrices };
};
