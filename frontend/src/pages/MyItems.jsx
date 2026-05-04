import { useState, useEffect } from 'react';
import { itemsApi } from '../api/api';

function MyItems({ user }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    category: '',
    available: true,
    ownerId: user.userId,
  });

  useEffect(() => {
    fetchMyItems();
  }, [user]);

  const fetchMyItems = async () => {
    setLoading(true);
    try {
      const response = await itemsApi.getItemsByOwner(user.userId);
      setItems(response.data);
    } catch (error) {
      console.error('Error fetching my items:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingItem) {
        await itemsApi.updateItem(editingItem.id, formData);
      } else {
        await itemsApi.createItem(formData);
      }
      setShowForm(false);
      setEditingItem(null);
      resetForm();
      fetchMyItems();
    } catch (error) {
      console.error('Error saving item:', error);
      alert('Failed to save item. Please try again.');
    }
  };

  const handleEdit = (item) => {
    setEditingItem(item);
    setFormData({
      name: item.name,
      description: item.description,
      category: item.category,
      available: item.available,
      ownerId: user.userId,
    });
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this item?')) {
      try {
        await itemsApi.deleteItem(id);
        fetchMyItems();
      } catch (error) {
        console.error('Error deleting item:', error);
      }
    }
  };

  const handleToggleAvailability = async (id) => {
    try {
      await itemsApi.toggleAvailability(id);
      fetchMyItems();
    } catch (error) {
      console.error('Error toggling availability:', error);
    }
  };

  const resetForm = () => {
    setFormData({
      name: '',
      description: '',
      category: '',
      available: true,
      ownerId: user.userId,
    });
  };

  const handleChange = (e) => {
    const value =
      e.target.type === 'checkbox' ? e.target.checked : e.target.value;
    setFormData({
      ...formData,
      [e.target.name]: value,
    });
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="flex flex-col items-center gap-4">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-600"></div>
          <p className="text-xl text-gray-600 dark:text-gray-400">Loading your items...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-4 sm:space-y-6">
      <div className="flex flex-col xs:flex-row justify-between items-start xs:items-center gap-3 mb-2">
        <h1 className="text-2xl sm:text-3xl md:text-4xl font-bold bg-gradient-to-r from-green-600 to-emerald-600 bg-clip-text text-transparent">My Items</h1>
        <button
          onClick={() => {
            setShowForm(!showForm);
            if (showForm) {
              setEditingItem(null);
              resetForm();
            }
          }}
          className="flex-shrink-0 bg-gradient-to-r from-green-600 to-emerald-600 text-white px-4 sm:px-6 py-2.5 sm:py-3 rounded-lg hover:from-green-700 hover:to-emerald-700 shadow-lg transition text-sm sm:text-base"
        >
          {showForm ? 'Cancel' : 'Add New Item'}
        </button>
      </div>

      {showForm && (
        <div className="backdrop-blur-xl bg-white/80 dark:bg-gray-900/80 border border-white/20 dark:border-gray-700/50 rounded-2xl shadow-2xl p-4 sm:p-6">
          <h2 className="text-xl sm:text-2xl font-semibold mb-4 text-gray-800 dark:text-white">
            {editingItem ? 'Edit Item' : 'Add New Item'}
          </h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-gray-700 dark:text-gray-300 font-semibold mb-2">
                Item Name *
              </label>
              <input
                type="text"
                name="name"
                value={formData.name}
                onChange={handleChange}
                className="w-full px-4 py-2.5 border-2 border-gray-200 dark:border-gray-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-transparent bg-white dark:bg-gray-800 text-gray-900 dark:text-white transition"
                required
              />
            </div>

            <div>
              <label className="block text-gray-700 dark:text-gray-300 font-semibold mb-2">
                Description
              </label>
              <textarea
                name="description"
                value={formData.description}
                onChange={handleChange}
                rows="3"
                className="w-full px-4 py-2.5 border-2 border-gray-200 dark:border-gray-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-transparent bg-white dark:bg-gray-800 text-gray-900 dark:text-white transition resize-none"
              />
            </div>

            <div>
              <label className="block text-gray-700 dark:text-gray-300 font-semibold mb-2">
                Category *
              </label>
              <input
                type="text"
                name="category"
                value={formData.category}
                onChange={handleChange}
                className="w-full px-4 py-2.5 border-2 border-gray-200 dark:border-gray-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-transparent bg-white dark:bg-gray-800 text-gray-900 dark:text-white transition"
                placeholder="e.g., Tools, Garden, Electronics"
                required
              />
            </div>

            <div className="flex items-center">
              <input
                type="checkbox"
                name="available"
                checked={formData.available}
                onChange={handleChange}
                className="mr-2 w-4 h-4 accent-green-600"
              />
              <label className="text-gray-700 dark:text-gray-300 font-semibold">
                Available for lending
              </label>
            </div>

            <button
              type="submit"
              className="w-full bg-gradient-to-r from-green-600 to-emerald-600 text-white py-3 rounded-lg hover:from-green-700 hover:to-emerald-700 transition shadow-lg font-semibold"
            >
              {editingItem ? 'Update Item' : 'Create Item'}
            </button>
          </form>
        </div>
      )}

      {items.length === 0 ? (
        <div className="backdrop-blur-xl bg-white/70 dark:bg-gray-900/70 border border-white/20 dark:border-gray-700/50 rounded-2xl shadow-2xl p-8 sm:p-12 text-center">
          <p className="text-lg sm:text-xl text-gray-600 dark:text-gray-400">
            You haven't added any items yet.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-2 xl:grid-cols-3 gap-4 sm:gap-6">
          {items.map((item) => (
            <div
              key={item.id}
              className="backdrop-blur-xl bg-white/70 dark:bg-gray-900/70 border border-white/20 dark:border-gray-700/50 rounded-2xl shadow-xl p-4 sm:p-6 hover:shadow-2xl transition-all duration-300"
            >
              <div className="flex justify-between items-start mb-3 sm:mb-4 gap-2">
                <h3 className="text-lg sm:text-xl font-semibold text-gray-800 dark:text-white flex-1 min-w-0 break-words">
                  {item.name}
                </h3>
                <span
                  className={`flex-shrink-0 px-2 sm:px-3 py-1 rounded-full text-xs sm:text-sm font-medium ${
                    item.available
                      ? 'bg-green-100 dark:bg-green-900/50 text-green-800 dark:text-green-300'
                      : 'bg-red-100 dark:bg-red-900/50 text-red-800 dark:text-red-300'
                  }`}
                >
                  {item.available ? 'Available' : 'In Use'}
                </span>
              </div>

              <p className="text-sm sm:text-base text-gray-600 dark:text-gray-400 mb-2">{item.description}</p>

              <div className="mt-3 sm:mt-4 text-xs sm:text-sm text-gray-500 dark:text-gray-500">
                <p>
                  <span className="font-semibold">Category:</span>{' '}
                  {item.category}
                </p>
              </div>

              <div className="mt-4 flex flex-wrap gap-2">
                <button
                  onClick={() => handleEdit(item)}
                  className="flex-1 min-w-[4rem] bg-blue-500 text-white px-3 py-2 rounded-lg hover:bg-blue-600 text-sm transition"
                >
                  Edit
                </button>
                <button
                  onClick={() => handleToggleAvailability(item.id)}
                  className="flex-1 min-w-[4rem] bg-yellow-500 text-white px-3 py-2 rounded-lg hover:bg-yellow-600 text-sm transition"
                >
                  Toggle
                </button>
                <button
                  onClick={() => handleDelete(item.id)}
                  className="flex-1 min-w-[4rem] bg-red-500 text-white px-3 py-2 rounded-lg hover:bg-red-600 text-sm transition"
                >
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default MyItems;
