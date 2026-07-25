import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

// Exported so the chat page can open the SockJS/STOMP connection through the gateway.
export const API_URL = API_BASE_URL;

// Categories shared by the create form and the browse filter
export const CATEGORIES = [
  'Tools', 'Electronics', 'Garden', 'Sports',
  'Books', 'Kitchen', 'Furniture', 'Toys', 'Other',
];

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

// Attach the JWT (stored inside the `user` blob at login) to every request.
api.interceptors.request.use((config) => {
  const stored = localStorage.getItem('user');
  const token = stored ? JSON.parse(stored).token : null;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  // Let the browser set the multipart boundary for file uploads.
  if (config.data instanceof FormData) {
    delete config.headers['Content-Type'];
  }
  return config;
});

// Response interceptor: on 401 drop the session and bounce to login.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('user');
      if (window.location.pathname !== '/login') {
        window.location.assign('/login');
      }
    } else if (error.code === 'ECONNABORTED') {
      console.error('Request timeout - server may be down');
    } else if (error.code === 'ERR_NETWORK') {
      console.error('Network error - cannot connect to server');
    } else if (!error.response) {
      console.error('No response from server - check if services are running');
    }
    return Promise.reject(error);
  }
);

// Build a multipart body: JSON "item" part (+ optional image) — matches the
// backend's @RequestPart("item") ItemRequest + @RequestPart("image") signature.
const buildItemForm = (item, imageFile) => {
  const form = new FormData();
  form.append('item', new Blob([JSON.stringify(item)], { type: 'application/json' }));
  if (imageFile) form.append('image', imageFile);
  return form;
};

// Items API
export const itemsApi = {
  getAllItems: () => api.get('/api/items'),
  getItemById: (id) => api.get(`/api/items/${id}`),
  getAvailableItems: () => api.get('/api/items/available'),
  getItemsByOwner: (ownerId) => api.get(`/api/items/owner/${ownerId}`),
  getItemsByCategory: (category) => api.get(`/api/items/category/${encodeURIComponent(category)}`),
  searchItems: (name) => api.get(`/api/items/search?name=${encodeURIComponent(name)}`),
  // create/update go through multipart so images are supported (PUT is multipart-only server-side)
  createItem: (item, imageFile) => api.post('/api/items', buildItemForm(item, imageFile)),
  updateItem: (id, item, imageFile) => api.put(`/api/items/${id}`, buildItemForm(item, imageFile)),
  toggleAvailability: (id) => api.patch(`/api/items/${id}/toggle-availability`),
  deleteItem: (id) => api.delete(`/api/items/${id}`),
  // fetch protected image bytes as a blob (an <img src> can't send the JWT)
  getItemImage: (id) => api.get(`/api/items/${id}/image`, { responseType: 'blob' }),
};

// Users API
export const usersApi = {
  getAllUsers: () => api.get('/api/users'),
  getUserById: (id) => api.get(`/api/users/${id}`),
  getUserByUsername: (username) => api.get(`/api/users/username/${username}`),
  register: (user) => api.post('/api/users/register', user),
  login: (credentials) => api.post('/api/users/login', credentials),
  updateUser: (id, user) => api.put(`/api/users/${id}`, user),
  deleteUser: (id) => api.delete(`/api/users/${id}`),
};

// Item condition options shared by the create form and cards
export const CONDITIONS = [
  { value: 'NEW', label: 'New' },
  { value: 'LIKE_NEW', label: 'Like New' },
  { value: 'GOOD', label: 'Good' },
  { value: 'FAIR', label: 'Fair' },
  { value: 'WORN', label: 'Worn' },
];

// Allowed borrow durations (days)
export const BORROW_DURATIONS = [1, 3, 7, 15, 30];

// Borrow Request workflow API
export const borrowApi = {
  create: (payload) => api.post('/api/borrow-requests', payload),
  incoming: () => api.get('/api/borrow-requests/incoming'),
  outgoing: () => api.get('/api/borrow-requests/outgoing'),
  accept: (id) => api.patch(`/api/borrow-requests/${id}/accept`),
  reject: (id) => api.patch(`/api/borrow-requests/${id}/reject`),
  cancel: (id) => api.patch(`/api/borrow-requests/${id}/cancel`),
  markReturned: (id) => api.patch(`/api/borrow-requests/${id}/return`),
};

// Favorites / wishlist API
export const favoritesApi = {
  list: () => api.get('/api/favorites'),
  ids: () => api.get('/api/favorites/ids'),
  add: (itemId) => api.post(`/api/favorites/${itemId}`),
  remove: (itemId) => api.delete(`/api/favorites/${itemId}`),
};

// Chat API (REST history; live messages go over WebSocket/STOMP)
export const chatApi = {
  getHistory: (user1Id, user2Id, itemId) =>
    api.get(`/api/chat/history/${user1Id}/${user2Id}`, { params: itemId ? { itemId } : {} }),
};

// Parse a backend error into a human string (shared by pages/toasts).
export const parseApiError = (err, fallback = 'Something went wrong. Please try again.') => {
  const data = err?.response?.data;
  if (typeof data === 'string' && data) return data;
  if (data?.message) return data.message;
  if (data?.error) return data.error;
  if (err?.message) return err.message;
  return fallback;
};

export default api;
