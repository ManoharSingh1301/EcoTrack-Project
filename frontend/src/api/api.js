import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000, // 10 second timeout
});

// Attach the JWT (stored inside the `user` blob at login) to every request
api.interceptors.request.use((config) => {
  const stored = localStorage.getItem('user');
  const token = stored ? JSON.parse(stored).token : null;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Add response interceptor for better error handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // Session expired / invalid token → drop the session and bounce to login
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

// Items API
export const itemsApi = {
  getAllItems: () => api.get('/api/items'),
  getItemById: (id) => api.get(`/api/items/${id}`),
  getAvailableItems: () => api.get('/api/items/available'),
  getItemsByOwner: (ownerId) => api.get(`/api/items/owner/${ownerId}`),
  getItemsByCategory: (category) => api.get(`/api/items/category/${category}`),
  searchItems: (name) => api.get(`/api/items/search?name=${encodeURIComponent(name)}`),
  createItem: (item) => api.post('/api/items', item),
  updateItem: (id, item) => api.put(`/api/items/${id}`, item),
  toggleAvailability: (id) => api.patch(`/api/items/${id}/toggle-availability`),
  deleteItem: (id) => api.delete(`/api/items/${id}`),
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

export default api;
