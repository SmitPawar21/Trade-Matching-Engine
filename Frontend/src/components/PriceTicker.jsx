import React from 'react';

const PriceTicker = ({ prices, isConnected }) => {
  return (
    <div className="flex justify-between items-center bg-gray-900 border-b border-gray-800 p-4 sticky top-0 z-50 shadow-md">
      <div className="flex items-center gap-6">
        <h1 className="text-xl font-bold bg-gradient-to-r from-blue-400 to-emerald-400 bg-clip-text text-transparent">
          TradeEngine
        </h1>
        
        <div className="flex gap-4">
          <div className="px-4 py-1.5 bg-gray-800/50 rounded-lg border border-gray-700/50 flex gap-3 items-center">
            <span className="text-gray-400 font-medium text-sm">BTC/USD</span>
            <span className="text-emerald-400 font-mono font-bold">
              {prices.BTC > 0 ? `$${prices.BTC.toLocaleString()}` : '--'}
            </span>
          </div>
          
          <div className="px-4 py-1.5 bg-gray-800/50 rounded-lg border border-gray-700/50 flex gap-3 items-center">
            <span className="text-gray-400 font-medium text-sm">ETH/USD</span>
            <span className="text-emerald-400 font-mono font-bold">
              {prices.ETH > 0 ? `$${prices.ETH.toLocaleString()}` : '--'}
            </span>
          </div>
        </div>
      </div>

      <div className="flex items-center gap-2 text-sm font-medium">
        {isConnected ? (
          <>
            <div className="w-2.5 h-2.5 rounded-full bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.6)] animate-pulse"></div>
            <span className="text-emerald-500">Live</span>
          </>
        ) : (
          <>
            <div className="w-2.5 h-2.5 rounded-full bg-red-500 shadow-[0_0_8px_rgba(239,68,68,0.6)]"></div>
            <span className="text-red-500">Disconnected</span>
          </>
        )}
      </div>
    </div>
  );
};

export default PriceTicker;
