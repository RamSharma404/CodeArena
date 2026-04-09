import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Navbar.css';

export default function Navbar() {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);

  const handleLogout = async () => {
    await logout();
    navigate('/');
  };

  const isActive = (path) => location.pathname === path;

  return (
    <nav className="navbar" id="main-navbar">
      <div className="navbar-inner container">
        {/* Logo */}
        <Link to="/" className="navbar-brand" id="navbar-logo">
          <div className="navbar-logo-icon">
            <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
              <rect x="2" y="2" width="24" height="24" rx="6" fill="url(#logo-grad)" />
              <path d="M9 14l3 3 7-7" stroke="#fff" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/>
              <defs>
                <linearGradient id="logo-grad" x1="2" y1="2" x2="26" y2="26">
                  <stop stopColor="#6366f1"/>
                  <stop offset="1" stopColor="#a855f7"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
          <span className="navbar-brand-text">CodeArena</span>
        </Link>

        {/* Nav Links */}
        <div className={`navbar-links ${menuOpen ? 'open' : ''}`}>
          <Link
            to="/problems"
            className={`navbar-link ${isActive('/problems') ? 'active' : ''}`}
            id="nav-problems"
            onClick={() => setMenuOpen(false)}
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
              <polyline points="14,2 14,8 20,8"/>
              <line x1="16" y1="13" x2="8" y2="13"/>
              <line x1="16" y1="17" x2="8" y2="17"/>
              <polyline points="10,9 9,9 8,9"/>
            </svg>
            Problems
          </Link>

          {isAuthenticated && (
            <Link
              to="/submissions"
              className={`navbar-link ${isActive('/submissions') ? 'active' : ''}`}
              id="nav-submissions"
              onClick={() => setMenuOpen(false)}
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="9,11 12,14 22,4"/>
                <path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"/>
              </svg>
              Submissions
            </Link>
          )}
        </div>

        {/* Auth Actions */}
        <div className="navbar-actions">
          {isAuthenticated ? (
            <div className="navbar-user">
              <div className="navbar-avatar" id="user-avatar">
                {user?.username?.charAt(0).toUpperCase()}
              </div>
              <span className="navbar-username">{user?.username}</span>
              <button
                className="btn btn-ghost btn-sm"
                onClick={handleLogout}
                id="btn-logout"
              >
                Logout
              </button>
            </div>
          ) : (
            <div className="navbar-auth-buttons">
              <Link to="/login" className="btn btn-ghost btn-sm" id="btn-login">
                Log In
              </Link>
              <Link to="/signup" className="btn btn-primary btn-sm" id="btn-signup">
                Sign Up
              </Link>
            </div>
          )}

          {/* Mobile hamburger */}
          <button
            className="navbar-hamburger"
            onClick={() => setMenuOpen(!menuOpen)}
            id="btn-menu-toggle"
            aria-label="Toggle menu"
          >
            <span className={`hamburger-line ${menuOpen ? 'open' : ''}`}></span>
            <span className={`hamburger-line ${menuOpen ? 'open' : ''}`}></span>
            <span className={`hamburger-line ${menuOpen ? 'open' : ''}`}></span>
          </button>
        </div>
      </div>
    </nav>
  );
}
