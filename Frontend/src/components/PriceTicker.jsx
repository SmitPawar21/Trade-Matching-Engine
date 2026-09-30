import React from 'react';

const PriceTicker = ({ prices, prevPrices, isConnected, stats }) => {
  const getPriceDirection = (symbol) => {
    if (!prevPrices || prevPrices[symbol] === 0) return 'neutral';
    if (prices[symbol] > prevPrices[symbol]) return 'up';
    if (prices[symbol] < prevPrices[symbol]) return 'down';
    return 'neutral';
  };

  const renderPrice = (symbol, label) => {
    const dir = getPriceDirection(symbol);
    const colorClass = dir === 'up' ? 'text-emerald-400' : dir === 'down' ? 'text-red-400' : 'text-gray-300';
    const arrow = dir === 'up' ? '▲' : dir === 'down' ? '▼' : '';
    const bgPulse = dir === 'up' ? 'bg-emerald-500/10' : dir === 'down' ? 'bg-red-500/10' : 'bg-gray-800/50';

    return (
      <div className={`px-5 py-2 rounded-lg border border-gray-700/50 flex gap-3 items-center transition-colors duration-300 ${bgPulse}`}>
        <span className="text-gray-400 font-medium text-sm">{label}</span>
        <span className={`font-mono font-bold text-lg tabular-nums transition-colors duration-300 ${colorClass}`}>
          {prices[symbol] > 0 ? `$${prices[symbol].toLocaleString()}` : '—'}
        </span>
        {arrow && <span className={`text-xs ${colorClass}`}>{arrow}</span>}
      </div>
    );
  };

  return (
    <div className="flex justify-between items-center bg-gray-900/95 backdrop-blur-md border-b border-gray-800 px-6 py-3 sticky top-0 z-50 shadow-lg">
      <div className="flex items-center gap-6">
        <h1 className="text-xl font-bold bg-gradient-to-r from-blue-400 to-emerald-400 bg-clip-text text-transparent tracking-tight">
          ⚡ TradeEngine
        </h1>
        
        <div className="flex gap-3">
          {renderPrice('BTC', 'BTC/USD')}
          {renderPrice('ETH', 'ETH/USD')}
        </div>

        {/* Live Stats */}
        <div className="flex gap-4 ml-4 text-xs text-gray-500 font-mono">
          <span>Trades: <span className="text-gray-300">{stats.totalTrades}</span></span>
          <span>Vol: <span className="text-gray-300">{stats.totalVolume.toLocaleString()}</span></span>
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
