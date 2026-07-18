import React, { useEffect } from 'react';
import { useWebSocket } from '../hooks/useWebSocket';
import PriceTicker from '../components/PriceTicker';
import OrderEntryForm from '../components/OrderEntryForm';
import EventFeed from '../components/EventFeed';
import OrderBookView from '../components/OrderBookView';

const HomePage = () => {
  const { events, prices, isConnected, sendRequest } = useWebSocket();

  // Periodically request state if needed (though the backend might not support this polling natively yet without a specific client request)
  // We'll rely on the TradeExecutedEvent for now to get mid prices, but if the backend supports GET_STATE, we could poll:
  // useEffect(() => {
  //   const interval = setInterval(() => {
  //     sendRequest({ type: 'GET_STATE', data: { symbol: 'BTC' } });
  //     sendRequest({ type: 'GET_STATE', data: { symbol: 'ETH' } });
  //   }, 1000);
  //   return () => clearInterval(interval);
  // }, [sendRequest]);

  return (
    <div className="min-h-screen bg-[#0a0a0a] text-gray-200 font-sans selection:bg-blue-500/30">
      <PriceTicker prices={prices} isConnected={isConnected} />
      
      <main className="max-w-7xl mx-auto p-4 lg:p-6 space-y-6 mt-4">
        {/* Top Section: Order Entry and Event Feed */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 h-[400px]">
          <div className="lg:col-span-1 h-full flex flex-col">
            <OrderEntryForm />
          </div>
          <div className="lg:col-span-2 h-full">
            <EventFeed events={events} />
          </div>
        </div>

        {/* Bottom Section: Order Books */}
        <div>
          <h2 className="text-xl font-bold mb-4 flex items-center gap-2 text-gray-100">
            <svg className="w-6 h-6 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
            </svg>
            Market Order Books
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <OrderBookView symbol="BTC" midPrice={prices.BTC} />
            <OrderBookView symbol="ETH" midPrice={prices.ETH} />
          </div>
        </div>
      </main>
    </div>
  );
};

export default HomePage;