import React, { useState } from 'react';

const OrderEntryForm = () => {
  const [formData, setFormData] = useState({
    symbol: 'BTC',
    side: 'BUY',
    orderType: 'LIMIT',
    price: '',
    quantity: '',
    userId: '1',
    orderId: ''
  });
  const [status, setStatus] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setStatus({ type: 'loading', msg: 'Submitting order...' });

    // Ensure numeric types
    const orderPayload = {
      ...formData,
      price: Number(formData.price),
      quantity: Number(formData.quantity),
      userId: Number(formData.userId),
      // Randomize orderId for frontend if not provided
      orderId: formData.orderId ? Number(formData.orderId) : Math.floor(Math.random() * 1000000)
    };

    try {
      const response = await fetch('http://localhost:5000/api/orders/place', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(orderPayload)
      });
      
      const data = await response.json();
      if (response.ok) {
        setStatus({ type: 'success', msg: 'Order placed successfully!' });
        // Reset numeric fields
        setFormData(prev => ({ ...prev, price: '', quantity: '' }));
      } else {
        setStatus({ type: 'error', msg: data.message || 'Failed to place order' });
      }
    } catch (err) {
      setStatus({ type: 'error', msg: 'Network error connecting to Node API' });
    }
  };

  const handleChange = (e) => {
    setFormData(prev => ({ ...prev, [e.target.name]: e.target.value }));
    setStatus(null); // Clear status on typing
  };

  return (
    <div className="bg-gray-800/80 backdrop-blur-sm rounded-xl border border-gray-700 p-6 flex-1 shadow-lg">
      <h2 className="text-lg font-semibold text-gray-100 mb-6 flex items-center gap-2">
        <svg className="w-5 h-5 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
        </svg>
        Place Order
      </h2>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-medium text-gray-400 uppercase tracking-wider mb-1.5">Symbol</label>
            <select 
              name="symbol" 
              value={formData.symbol} 
              onChange={handleChange}
              className="w-full bg-gray-900 border border-gray-700 text-gray-200 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block p-2.5 transition-colors"
            >
              <option value="BTC">BTC</option>
              <option value="ETH">ETH</option>
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-400 uppercase tracking-wider mb-1.5">User ID</label>
            <input 
              type="number" 
              name="userId" 
              value={formData.userId} 
              onChange={handleChange}
              required
              className="w-full bg-gray-900 border border-gray-700 text-gray-200 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block p-2.5 transition-colors"
            />
          </div>
        </div>

        <div>
          <label className="block text-xs font-medium text-gray-400 uppercase tracking-wider mb-1.5">Side</label>
          <div className="grid grid-cols-2 gap-2">
            <button
              type="button"
              onClick={() => setFormData(prev => ({...prev, side: 'BUY'}))}
              className={`py-2 rounded-lg text-sm font-semibold transition-all ${
                formData.side === 'BUY' 
                  ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/50' 
                  : 'bg-gray-900 text-gray-400 border border-gray-700 hover:bg-gray-800'
              }`}
            >
              BUY
            </button>
            <button
              type="button"
              onClick={() => setFormData(prev => ({...prev, side: 'SELL'}))}
              className={`py-2 rounded-lg text-sm font-semibold transition-all ${
                formData.side === 'SELL' 
                  ? 'bg-red-500/20 text-red-400 border border-red-500/50' 
                  : 'bg-gray-900 text-gray-400 border border-gray-700 hover:bg-gray-800'
              }`}
            >
              SELL
            </button>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-medium text-gray-400 uppercase tracking-wider mb-1.5">Price</label>
            <div className="relative">
              <span className="absolute inset-y-0 left-0 flex items-center pl-3 text-gray-500">$</span>
              <input 
                type="number" 
                name="price" 
                value={formData.price} 
                onChange={handleChange}
                required
                min="0"
                className="w-full bg-gray-900 border border-gray-700 text-gray-200 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block pl-8 p-2.5 transition-colors"
                placeholder="0.00"
              />
            </div>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-400 uppercase tracking-wider mb-1.5">Quantity</label>
            <input 
              type="number" 
              name="quantity" 
              value={formData.quantity} 
              onChange={handleChange}
              required
              min="1"
              className="w-full bg-gray-900 border border-gray-700 text-gray-200 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block p-2.5 transition-colors"
              placeholder="0"
            />
          </div>
        </div>

        <button 
          type="submit" 
          disabled={status?.type === 'loading'}
          className={`w-full py-3 rounded-lg text-sm font-bold uppercase tracking-wider text-white transition-all transform active:scale-[0.98] ${
            formData.side === 'BUY' 
              ? 'bg-emerald-600 hover:bg-emerald-500 shadow-[0_0_15px_rgba(5,150,105,0.3)]' 
              : 'bg-red-600 hover:bg-red-500 shadow-[0_0_15px_rgba(220,38,38,0.3)]'
          } ${status?.type === 'loading' ? 'opacity-70 cursor-not-allowed' : ''}`}
        >
          {status?.type === 'loading' ? 'Processing...' : `Submit ${formData.side} Order`}
        </button>

        {status && (
          <div className={`text-sm p-3 rounded-lg ${
            status.type === 'success' ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 
            status.type === 'error' ? 'bg-red-500/10 text-red-400 border border-red-500/20' : 
            'text-gray-400'
          }`}>
            {status.msg}
          </div>
        )}
      </form>
    </div>
  );
};

export default OrderEntryForm;
