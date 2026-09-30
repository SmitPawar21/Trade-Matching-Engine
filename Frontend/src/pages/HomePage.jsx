import React from 'react';
import { useWebSocket } from '../hooks/useWebSocket';
import PriceTicker from '../components/PriceTicker';
import OrderEntryForm from '../components/OrderEntryForm';
import EventFeed from '../components/EventFeed';
import TradeHistory from '../components/TradeHistory';

const HomePage = () => {
  const { events, trades, prices, prevPrices, isConnected, stats } = useWebSocket();

  return (
    <div className="min-h-screen bg-[#0a0a0a] text-gray-200 font-sans selection:bg-blue-500/30">
      <PriceTicker prices={prices} prevPrices={prevPrices} isConnected={isConnected} stats={stats} />
      
      <main className="max-w-7xl mx-auto p-4 lg:p-6 space-y-6 mt-4">
        {/* Top Section: Order Entry + Event Feed */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6" style={{ minHeight: '420px' }}>
          <div className="lg:col-span-1 flex flex-col">
            <OrderEntryForm prices={prices} />
          </div>
          <div className="lg:col-span-2 flex flex-col">
            <EventFeed events={events} />
          </div>
        </div>

        {/* Trade History Table */}
        <TradeHistory trades={trades} />

        {/* Architecture Info Footer */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-6">
          <div className="bg-gray-800/40 border border-gray-700/50 rounded-xl p-4">
            <h4 className="text-xs uppercase tracking-wider text-gray-500 mb-1">Engine</h4>
            <p className="text-sm text-gray-300">Java · Lock-free · O(log N) Matching</p>
          </div>
          <div className="bg-gray-800/40 border border-gray-700/50 rounded-xl p-4">
            <h4 className="text-xs uppercase tracking-wider text-gray-500 mb-1">Gateway</h4>
            <p className="text-sm text-gray-300">Node.js · TCP Socket · WebSocket</p>
          </div>
          <div className="bg-gray-800/40 border border-gray-700/50 rounded-xl p-4">
            <h4 className="text-xs uppercase tracking-wider text-gray-500 mb-1">Intelligence</h4>
            <p className="text-sm text-gray-300">PPO Agent · Sentiment Simulation</p>
          </div>
        </div>
      </main>
    </div>
  );
};

export default HomePage;