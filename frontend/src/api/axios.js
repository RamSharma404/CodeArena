import axios from 'axios';

const API = axios.create({
  baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // send cookies with every request
});

// Request interceptor: attach Bearer token if available (fallback for cross-origin cookie issues)
API.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('jwt_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Handle 401 responses globally
API.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // Only clear & redirect if not already on auth pages
      const path = window.location.pathname;
      if (!path.startsWith('/login') && !path.startsWith('/signup') &&
          !path.startsWith('/verify') && !path.startsWith('/oauth2')) {
        localStorage.removeItem('user');
        localStorage.removeItem('jwt_token');
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default API;
