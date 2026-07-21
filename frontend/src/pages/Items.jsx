import { useState, useEffect } from 'react';
import { itemsApi } from '../api/api';
import { Package, Search, Filter, Loader } from 'lucide-react';
import MagneticButton from '../components/MagneticButton';

function Items({ user }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('all');
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    fetchItems();
  }, [filter]);

  const fetchItems = async () => {
    setLoading(true);
    try {
      let response;
      if (filter === 'available') {
        response = await itemsApi.getAvailableItems();
      } else {
        response = await itemsApi.getAllItems();
      }
      setItems(response.data);
    } catch (error) {
      console.error('Error fetching items:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async () => {
    if (searchTerm.trim()) {
      try {
        const response = await itemsApi.searchItems(searchTerm);
        setItems(response.data);
      } catch (error) {
        console.error('Error searching items:', error);
      }
    } else {
      fetchItems();
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="flex flex-col items-center gap-4">
          <Loader className="w-12 h-12 text-green-600 animate-spin" />
          <p className="text-xl text-gray-600 dark:text-gray-400">Loading items...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-4 sm:space-y-6">
      <div className="flex items-center gap-2 sm:gap-3">
        <Package className="w-8 h-8 sm:w-10 sm:h-10 text-green-600 dark:text-green-400 flex-shrink-0" />
        <h1 className="text-2xl sm:text-3xl md:text-4xl font-bold bg-gradient-to-r from-green-600 to-emerald-600 bg-clip-text text-transparent">
          Browse Available Items
        </h1>
      </div>

      <div className="backdrop-blur-xl bg-white/70 dark:bg-gray-900/70 border border-white/20 dark:border-gray-700/50 rounded-2xl shadow-2xl p-4 sm:p-6">
        <div className="flex flex-col sm:flex-row gap-3 sm:gap-4 mb-4">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-3 sm:top-3.5 w-4 h-4 sm:w-5 sm:h-5 text-gray-400" />
            <input
              type="text"
              placeholder="Search items..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
              className="w-full pl-9 sm:pl-10 pr-4 py-2.5 sm:py-3 text-sm sm:text-base border-2 border-gray-200 dark:border-gray-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 dark:bg-gray-800 dark:text-white transition"
            />
          </div>
          <MagneticButton
            onClick={handleSearch}
            className="bg-gradient-to-r from-green-600 to-emerald-600 text-white px-4 sm:px-6 py-2.5 sm:py-3 rounded-lg hover:from-green-700 hover:to-emerald-700 shadow-lg hover:shadow-xl flex items-center gap-2 text-sm sm:text-base"
          >
            <Search className="w-4 h-4 sm:w-5 sm:h-5" />
            <span className="hidden sm:inline">Search</span>
          </MagneticButton>
        </div>

        <div className="flex flex-wrap gap-2">
          <button
            onClick={() => setFilter('all')}
            className={`px-3 sm:px-4 py-2 rounded-lg flex items-center gap-1.5 sm:gap-2 text-sm sm:text-base transition-all duration-300 ${
              filter === 'all'
                ? 'bg-gradient-to-r from-green-600 to-emerald-600 text-white shadow-lg'
                : 'bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-300 dark:hover:bg-gray-600'
            }`}
          >
            <Filter className="w-3.5 h-3.5 sm:w-4 sm:h-4" />
            <span>All Items</span>
          </button>
          <button
            onClick={() => setFilter('available')}
            className={`px-3 sm:px-4 py-2 rounded-lg flex items-center gap-1.5 sm:gap-2 text-sm sm:text-base transition-all duration-300 ${
              filter === 'available'
                ? 'bg-gradient-to-r from-green-600 to-emerald-600 text-white shadow-lg'
                : 'bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-300 dark:hover:bg-gray-600'
            }`}
          >
            <Filter className="w-3.5 h-3.5 sm:w-4 sm:h-4" />
            <span>Available Only</span>
          </button>
        </div>
      </div>

      {items.length === 0 ? (
        <div className="backdrop-blur-xl bg-white/70 dark:bg-gray-900/70 border border-white/20 dark:border-gray-700/50 rounded-2xl shadow-2xl p-8 sm:p-12 text-center">
          <Package className="w-12 h-12 sm:w-16 sm:h-16 text-gray-400 mx-auto mb-4" />
          <p className="text-lg sm:text-xl text-gray-600 dark:text-gray-400">No items found.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-2 xl:grid-cols-3 gap-4 sm:gap-6">
          {items.map((item, index) => (
            <div
              key={item.id}
              className="backdrop-blur-xl bg-white/70 dark:bg-gray-900/70 border border-white/20 dark:border-gray-700/50 rounded-2xl shadow-xl p-4 sm:p-6 hover:shadow-2xl transition-all duration-300 transform hover:-translate-y-1"
              style={{
                animation: `fadeInUp 0.5s ease-out ${index * 0.1}s both`,
              }}
            >
              <div className="flex justify-between items-start mb-3 sm:mb-4 gap-2">
                <h3 className="text-lg sm:text-xl font-semibold text-gray-800 dark:text-white flex-1">
                  {item.name}
                </h3>
                <span
                  className={`px-2 sm:px-3 py-1 rounded-full text-xs sm:text-sm font-medium flex-shrink-0 ${
                    item.available
                      ? 'bg-green-100 dark:bg-green-900/50 text-green-800 dark:text-green-300'
                      : 'bg-red-100 dark:bg-red-900/50 text-red-800 dark:text-red-300'
                  }`}
                >
                  {item.available ? 'Available' : 'In Use'}
                </span>
              </div>

              <p className="text-sm sm:text-base text-gray-600 dark:text-gray-400 mb-2">{item.description}</p>

              <div className="mt-3 sm:mt-4 text-xs sm:text-sm text-gray-500 dark:text-gray-500 space-y-1">
                <p>
                  <span className="font-semibold">Category:</span>{' '}
                  {item.category}
                </p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default Items;
