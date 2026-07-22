import { useState, useEffect } from 'react';
import { usersApi, parseApiError } from '../api/api';
import { Mail, MapPin, Phone, FileText, Pencil, Save, X } from 'lucide-react';
import { useToast } from '../contexts/ToastContext';

function Profile({ user, setUser }) {
  const [isEditing, setIsEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [formData, setFormData] = useState({
    username: user.username,
    email: user.email || '',
    fullName: user.fullName || '',
    address: '',
    phone: '',
    bio: '',
    password: '',
  });
  const toast = useToast();

  // Hydrate the full profile (address/phone/bio aren't in the login payload)
  // so editing never overwrites them with blanks.
  useEffect(() => {
    usersApi.getUserById(user.userId)
      .then(({ data }) => setFormData((prev) => ({
        ...prev,
        email: data.email ?? prev.email,
        fullName: data.fullName ?? prev.fullName,
        address: data.address ?? '',
        phone: data.phone ?? '',
        bio: data.bio ?? '',
      })))
      .catch((err) => console.error('Error loading profile:', err));
  }, [user.userId]);

  const handleChange = (e) => setFormData((f) => ({ ...f, [e.target.name]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const updateData = { ...formData };
      if (!updateData.password) delete updateData.password;

      await usersApi.updateUser(user.userId, updateData);

      const updatedUser = { ...user, email: formData.email, fullName: formData.fullName };
      setUser(updatedUser);
      localStorage.setItem('user', JSON.stringify(updatedUser));

      setIsEditing(false);
      toast.success('Profile updated');
    } catch (err) {
      toast.error(parseApiError(err, 'Failed to update profile.'));
    } finally {
      setSaving(false);
    }
  };

  const initials = (formData.fullName || user.username || '?')
    .split(' ').map((s) => s[0]).slice(0, 2).join('').toUpperCase();

  const Field = ({ icon: Icon, label, value }) => (
    <div className="flex items-start gap-3">
      <Icon className="w-4 h-4 mt-1 text-emerald-600 dark:text-emerald-400 flex-shrink-0" />
      <div>
        <p className="text-xs uppercase tracking-wide text-gray-400 dark:text-gray-500">{label}</p>
        <p className="text-gray-800 dark:text-gray-200">{value || <span className="text-gray-400 italic">Not set</span>}</p>
      </div>
    </div>
  );

  return (
    <div className="max-w-2xl mx-auto">
      <div className="surface rounded-2xl overflow-hidden">
        {/* Banner */}
        <div className="h-24 bg-gradient-to-r from-emerald-600 to-teal-500" />
        <div className="px-6 sm:px-8 pb-8">
          <div className="flex items-end gap-4 -mt-10">
            <div className="w-20 h-20 rounded-2xl bg-white dark:bg-gray-900 shadow-lg flex items-center justify-center ring-4 ring-white dark:ring-gray-900">
              <span className="text-2xl font-bold brand-text font-display">{initials}</span>
            </div>
            <div className="pb-1">
              <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">{formData.fullName}</h1>
              <p className="text-gray-500 dark:text-gray-400">@{user.username}</p>
            </div>
          </div>

          {!isEditing ? (
            <div className="mt-6 space-y-4">
              <Field icon={Mail} label="Email" value={formData.email} />
              <Field icon={MapPin} label="Address" value={formData.address} />
              <Field icon={Phone} label="Phone" value={formData.phone} />
              <Field icon={FileText} label="Bio" value={formData.bio} />
              <button onClick={() => setIsEditing(true)} className="btn-primary mt-2">
                <Pencil className="w-4 h-4" /> Edit profile
              </button>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="mt-6 space-y-4">
              <div>
                <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1.5">Username</label>
                <input value={formData.username} disabled className="input opacity-60 cursor-not-allowed" />
              </div>
              <div className="grid sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1.5">Email *</label>
                  <input type="email" name="email" value={formData.email} onChange={handleChange} required className="input" />
                </div>
                <div>
                  <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1.5">Full name *</label>
                  <input name="fullName" value={formData.fullName} onChange={handleChange} required className="input" />
                </div>
              </div>
              <div>
                <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1.5">Address</label>
                <input name="address" value={formData.address} onChange={handleChange} className="input" />
              </div>
              <div>
                <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1.5">Phone</label>
                <input type="tel" name="phone" value={formData.phone} onChange={handleChange} className="input" placeholder="+15551234567" />
              </div>
              <div>
                <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1.5">Bio</label>
                <textarea name="bio" value={formData.bio} onChange={handleChange} rows="3" maxLength={500} className="input resize-none" />
              </div>
              <div>
                <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1.5">New password <span className="font-normal text-gray-400">(leave blank to keep current)</span></label>
                <input type="password" name="password" value={formData.password} onChange={handleChange} className="input" />
              </div>
              <div className="flex gap-3">
                <button type="submit" disabled={saving} className="btn-primary flex-1">
                  <Save className="w-4 h-4" /> {saving ? 'Saving…' : 'Save changes'}
                </button>
                <button type="button" onClick={() => setIsEditing(false)} className="btn-ghost flex-1">
                  <X className="w-4 h-4" /> Cancel
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}

export default Profile;
