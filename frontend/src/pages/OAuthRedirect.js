import React, { useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function OAuthRedirect() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();

  useEffect(() => {
    // Parse the token from the URL (e.g. ?token=xxxxx)
    const params = new URLSearchParams(location.search);
    const token = params.get('token');

    if (token) {
      // Store JWT in localStorage as fallback for cross-origin cookie issues
      localStorage.setItem('jwt_token', token);

      // Decode JWT payload to get user info (basic decode without verification)
      try {
        const payloadBase64 = token.split('.')[1];
        const decodedJson = atob(payloadBase64);
        const payload = JSON.parse(decodedJson);
        
        // Pass to our AuthContext login function
        // AuthContext expects an object with username, email, role
        login({
          id: payload.userId,
          email: payload.sub,
          username: payload.username,
          role: payload.role
        });
        
        navigate('/problems', { replace: true });
      } catch (err) {
        console.error("Failed to parse OAuth JWT", err);
        navigate('/login?error=Invalid+OAuth+Token');
      }
    } else {
      navigate('/login?error=Authentication+Failed');
    }
  }, [location, navigate, login]);

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', color: 'white' }}>
      <h2>Authenticating...</h2>
    </div>
  );
}
