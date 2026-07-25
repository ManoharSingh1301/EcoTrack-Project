import { useState, useEffect } from 'react';
import { itemsApi, CATEGORIES, CONDITIONS, parseApiError } from '../api/api';
import { Plus, Pencil, Trash2, RefreshCw, ImagePlus, Package, X } from 'lucide-react';
import ItemCard from '../components/ItemCard';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import ConfirmDialog from '../components/ConfirmDialog';
import { useToast } from '../contexts/ToastContext';

const EMPTY_FORM = {
  name: '', description: '', category: '', available: true,
  condition: 'GOOD', maxBorrowDays: 7, lateFeePerDay: 0, securityDeposit: 0,
};

function MyItems({ user }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [formData, setFormData] = useState(EMPTY_FORM);
  const [imageFile, setImageFile] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [saving, setSaving] = useState(false);
  const [confirmId, setConfirmId] = useState(null);
  const toast = useToast();

  useEffect(() => {
    fetchMyItems();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user.userId]);

  const fetchMyItems = async () => {
    setLoading(true);
    try {
      const res = await itemsApi.getItemsByOwner(user.userId);
      setItems(res.data);
    } catch (err) {
      toast.error(parseApiError(err, 'Could not load your items.'));
    } finally {
      setLoading(false);
    }
  };

  const resetForm = () => {
    setFormData(EMPTY_FORM);
    setEditingItem(null);
    setImageFile(null);
    if (imagePreview) URL.revokeObjectURL(imagePreview);
    setImagePreview(null);
  };

  const openCreate = () => {
    resetForm();
    setShowForm(true);
  };

  const openEdit = (item) => {
    resetForm();
    setEditingItem(item);
    setFormData({
      name: item.name,
      description: item.description || '',
      category: item.category,
      available: item.available,
      condition: item.condition || 'GOOD',
      maxBorrowDays: item.maxBorrowDays ?? 7,
      lateFeePerDay: item.lateFeePerDay ?? 0,
      securityDeposit: item.securityDeposit ?? 0,
    });
    setShowForm(true);
  };

  const handleChange = (e) => {
    const value = e.target.type === 'checkbox' ? e.target.checked : e.target.value;
    setFormData((f) => ({ ...f, [e.target.name]: value }));
  };

  const handleFile = (e) => {
    const file = e.target.files?.[0];
    if (imagePreview) URL.revokeObjectURL(imagePreview);
    if (file) {
      setImageFile(file);
      setImagePreview(URL.createObjectURL(file));
    } else {
      setImageFile(null);
      setImagePreview(null);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    // ownerId is required by the backend DTO; the server overrides it with the
    // authenticated user, but we send it to satisfy validation.
    const payload = { ...formData, ownerId: user.userId };
    try {
      if (editingItem) {
        await itemsApi.updateItem(editingItem.id, payload, imageFile);
        toast.success('Item updated');
      } else {
        await itemsApi.createItem(payload, imageFile);
        toast.success('Item listed');
      }
      setShowForm(false);
      resetForm();
      fetchMyItems();
    } catch (err) {
      toast.error(parseApiError(err, 'Failed to save the item.'));
    } finally {
      setSaving(false);
    }
  };

  const handleToggle = async (id) => {
    try {
      await itemsApi.toggleAvailability(id);
      fetchMyItems();
    } catch (err) {
      toast.error(parseApiError(err, 'Could not update availability.'));
    }
  };

  const doDelete = async () => {
    const id = confirmId;
    setConfirmId(null);
    try {
      await itemsApi.deleteItem(id);
      toast.success('Item deleted');
      fetchMyItems();
    } catch (err) {
      toast.error(parseApiError(err, 'Could not delete the item.'));
    }
  };

  if (loading) return <Spinner label="Loading your items…" />;

  return (
    <div className="space-y-6">
      <header className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 dark:text-white">My listings</h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">Manage what you share with the community.</p>
        </div>
        {showForm ? (
          <button onClick={() => { setShowForm(false); resetForm(); }} className="btn-ghost self-start">
            <X className="w-4 h-4" /> Close
          </button>
        ) : (
          <button onClick={openCreate} className="btn-primary self-start">
            <Plus className="w-4 h-4" /> Add new item
          </button>
        )}
      </header>

      {showForm && (
        <div className="surface rounded-2xl p-5 sm:p-6">
          <h2 className="text-xl font-semibold mb-4 text-gray-900 dark:text-white">
            {editingItem ? 'Edit item' : 'List a new item'}
          </h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1.5">Name *</label>
                <input name="name" value={formData.name} onChange={handleChange} required
                       minLength={2} maxLength={100} className="input" placeholder="e.g. Cordless drill" />
              </div>
              <div>
                <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1.5">Category *</label>
                <select name="category" value={formData.category} onChange={handleChange} required className="input">
                  <option value="" disabled>Choose a category</option>
                  {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
                </select>
              </div>
            </div>

            <div>
              <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1.5">Description</label>
              <textarea name="description" value={formData.description} onChange={handleChange}
                        rows="3" maxLength={1000} className="input resize-none"
                        placeholder="Condition, how to borrow, anything useful…" />
            </div>

            <div className="grid sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1.5">Condition</label>
                <select name="condition" value={formData.condition} onChange={handleChange} className="input">
                  {CONDITIONS.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1.5">Max borrow days</label>
                <input type="number" name="maxBorrowDays" value={formData.maxBorrowDays} onChange={handleChange}
                       min={1} max={365} className="input" />
              </div>
              <div>
                <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1.5">Late fee (₹ / day)</label>
                <input type="number" name="lateFeePerDay" value={formData.lateFeePerDay} onChange={handleChange}
                       min={0} step="1" className="input" />
              </div>
              <div>
                <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1.5">Security deposit (₹)</label>
                <input type="number" name="securityDeposit" value={formData.securityDeposit} onChange={handleChange}
                       min={0} step="1" className="input" />
              </div>
            </div>

            <div>
              <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1.5">Photo</label>
              <div className="flex items-center gap-4">
                <label className="btn-ghost cursor-pointer">
                  <ImagePlus className="w-4 h-4" /> {imageFile ? 'Change photo' : 'Upload photo'}
                  <input type="file" accept="image/png,image/jpeg,image/webp" onChange={handleFile} className="hidden" />
                </label>
                {imagePreview && (
                  <img src={imagePreview} alt="Preview" className="w-16 h-16 rounded-lg object-cover border border-gray-200 dark:border-gray-700" />
                )}
                {editingItem?.hasImage && !imagePreview && (
                  <span className="text-xs text-gray-500 dark:text-gray-400">Current photo kept unless you upload a new one.</span>
                )}
              </div>
            </div>

            <label className="flex items-center gap-2 cursor-pointer select-none">
              <input type="checkbox" name="available" checked={formData.available} onChange={handleChange}
                     className="w-4 h-4 accent-emerald-600" />
              <span className="text-sm font-medium text-gray-700 dark:text-gray-300">Available for lending</span>
            </label>

            <button type="submit" disabled={saving} className="btn-primary w-full">
              {saving ? <RefreshCw className="w-4 h-4 animate-spin" /> : editingItem ? <Pencil className="w-4 h-4" /> : <Plus className="w-4 h-4" />}
              {editingItem ? 'Save changes' : 'Create listing'}
            </button>
          </form>
        </div>
      )}

      {items.length === 0 ? (
        <EmptyState
          icon={Package}
          title="No listings yet"
          message="Add your first item to start sharing."
          action={!showForm && <button onClick={openCreate} className="btn-primary"><Plus className="w-4 h-4" /> Add item</button>}
        />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4 sm:gap-6">
          {items.map((item, i) => (
            <ItemCard
              key={item.id}
              item={item}
              index={i}
              footer={
                <div className="flex gap-2">
                  <button onClick={() => openEdit(item)} className="btn-ghost flex-1 py-2 text-sm">
                    <Pencil className="w-4 h-4" /> Edit
                  </button>
                  <button onClick={() => handleToggle(item.id)} className="btn-ghost flex-1 py-2 text-sm"
                          title={item.available ? 'Mark as in use' : 'Mark as available'}>
                    <RefreshCw className="w-4 h-4" />
                    {item.available ? 'In use' : 'Free'}
                  </button>
                  <button onClick={() => setConfirmId(item.id)} className="btn-danger py-2 px-3 text-sm" aria-label="Delete item">
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              }
            />
          ))}
        </div>
      )}

      <ConfirmDialog
        open={confirmId !== null}
        title="Delete this item?"
        message="This permanently removes the listing. This can't be undone."
        onCancel={() => setConfirmId(null)}
        onConfirm={doDelete}
      />
    </div>
  );
}

export default MyItems;
