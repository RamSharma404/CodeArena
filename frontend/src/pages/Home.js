import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Home.css';

export default function Home() {
  const { isAuthenticated } = useAuth();

  return (
    <div className="home-page">
      {/* Hero Section */}
      <section className="hero" id="hero-section">
        <div className="hero-bg">
          <div className="hero-orb hero-orb-1"></div>
          <div className="hero-orb hero-orb-2"></div>
          <div className="hero-orb hero-orb-3"></div>
          <div className="hero-grid"></div>
        </div>

        <div className="hero-content container">
          <div className="hero-badge animate-fade-in">
            <span className="hero-badge-dot"></span>
            Practice. Compete. Excel.
          </div>

          <h1 className="hero-title animate-fade-in-up">
            Master Coding<br />
            <span className="hero-title-gradient">Challenges</span>
          </h1>

          <p className="hero-subtitle animate-fade-in-up" style={{ animationDelay: '100ms' }}>
            Sharpen your problem-solving skills with curated challenges,
            real-time code execution, and AI-powered feedback.
          </p>

          <div className="hero-actions animate-fade-in-up" style={{ animationDelay: '200ms' }}>
            <Link to="/problems" className="btn btn-primary btn-lg" id="hero-explore-btn">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polygon points="5,3 19,12 5,21"/>
              </svg>
              Explore Problems
            </Link>
            {!isAuthenticated && (
              <Link to="/signup" className="btn btn-secondary btn-lg" id="hero-signup-btn">
                Get Started Free
              </Link>
            )}
          </div>

          <div className="hero-stats animate-fade-in-up" style={{ animationDelay: '350ms' }}>
            <div className="hero-stat">
              <span className="hero-stat-value">4+</span>
              <span className="hero-stat-label">Problems</span>
            </div>
            <div className="hero-stat-divider"></div>
            <div className="hero-stat">
              <span className="hero-stat-value">4</span>
              <span className="hero-stat-label">Languages</span>
            </div>
            <div className="hero-stat-divider"></div>
            <div className="hero-stat">
              <span className="hero-stat-value">AI</span>
              <span className="hero-stat-label">Code Review</span>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="features container" id="features-section">
        <h2 className="features-title">Everything you need to level up</h2>
        <p className="features-subtitle">A complete platform for coding practice and growth</p>

        <div className="features-grid stagger-children">
          <div className="feature-card animate-fade-in-up">
            <div className="feature-icon feature-icon-purple">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="16,18 22,12 16,6"/>
                <polyline points="8,6 2,12 8,18"/>
              </svg>
            </div>
            <h3 className="feature-title">Interactive Code Editor</h3>
            <p className="feature-desc">
              Write code in Java, Python, C++, or JavaScript with full syntax highlighting and auto-completion powered by Monaco Editor.
            </p>
          </div>

          <div className="feature-card animate-fade-in-up">
            <div className="feature-icon feature-icon-green">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polygon points="13,2 3,14 12,14 11,22 21,10 12,10"/>
              </svg>
            </div>
            <h3 className="feature-title">Instant Execution</h3>
            <p className="feature-desc">
              Run your code against test cases instantly. See output, errors, runtime, and memory usage in real time.
            </p>
          </div>

          <div className="feature-card animate-fade-in-up">
            <div className="feature-icon feature-icon-amber">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <path d="M9.09 9a3 3 0 015.83 1c0 2-3 3-3 3"/>
                <line x1="12" y1="17" x2="12.01" y2="17"/>
              </svg>
            </div>
            <h3 className="feature-title">AI Code Review</h3>
            <p className="feature-desc">
              Get instant AI feedback on complexity, improvements, and alternative approaches after solving a problem.
            </p>
          </div>

          <div className="feature-card animate-fade-in-up">
            <div className="feature-icon feature-icon-red">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 20V10"/>
                <path d="M18 20V4"/>
                <path d="M6 20v-4"/>
              </svg>
            </div>
            <h3 className="feature-title">Track Progress</h3>
            <p className="feature-desc">
              View your submission history, compare solutions, and track which problems you've solved across difficulty levels.
            </p>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="home-cta" id="cta-section">
        <div className="container">
          <div className="cta-card">
            <h2 className="cta-title">Ready to start coding?</h2>
            <p className="cta-text">Join CodeArena and challenge yourself with real coding problems.</p>
            <Link to={isAuthenticated ? "/problems" : "/signup"} className="btn btn-primary btn-lg" id="cta-btn">
              {isAuthenticated ? "Go to Problems" : "Create Account"}
            </Link>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="home-footer">
        <div className="container">
          <p className="footer-text">© 2026 CodeArena. Built for developers, by developers.</p>
        </div>
      </footer>
    </div>
  );
}
