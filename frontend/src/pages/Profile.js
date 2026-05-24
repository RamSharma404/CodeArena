import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { Link } from 'react-router-dom';
import API from '../api/axios';
import './Profile.css';

export default function Profile() {
  const { user } = useAuth();
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const res = await API.get('/api/users/profile');
        setStats(res.data);
      } catch (err) {
        setError('Failed to load profile statistics');
      } finally {
        setLoading(false);
      }
    };
    fetchStats();
  }, []);

  if (loading) {
    return (
      <div className="profile-loading" style={{ minHeight: 'calc(100vh - 60px)' }}>
        <div className="spinner"></div>
      </div>
    );
  }

  if (error || !stats) {
    return (
      <div className="container profile-error">
        <h2>Oops!</h2>
        <p>{error}</p>
      </div>
    );
  }

  // Calculate completion percentage
  const completionPercent = stats.totalProblems > 0 
    ? Math.round((stats.totalSolved / stats.totalProblems) * 100) 
    : 0;

  // Language Breakdown mapping
  const langs = stats.languageStats ? Object.entries(stats.languageStats).sort((a, b) => b[1] - a[1]) : [];

  return (
    <div className="profile-page">
      <div className="container profile-container">
        
        {/* Left Column: User Info & Overall Stats */}
        <div className="profile-sidebar">
          
          <div className="profile-card user-info-card">
            <div className="profile-avatar-large">
              {stats.username.charAt(0).toUpperCase()}
            </div>
            <h1 className="profile-username">{stats.username}</h1>
            <p className="profile-email">{stats.email}</p>
            <div className="profile-rank-badge">Rank: Novice</div>
          </div>

          <div className="profile-card stat-highlight-card">
            <h3 className="card-title">Completion</h3>
            <div className="circular-progress-container group">
              {/* Circular Progress Ring */}
              <div 
                className="circular-progress"
                style={{ '--progress': `${completionPercent}%` }}
              >
                <div className="circular-progress-inner">
                  <div className="progress-value">
                    <span className="progress-number">{completionPercent}</span>
                    <span className="progress-percent">%</span>
                  </div>
                </div>
              </div>
              <div className="progress-label">
                Solved {stats.totalSolved} / {stats.totalProblems}
              </div>
            </div>
          </div>

        </div>

        {/* Right Column: Detailed Stats */}
        <div className="profile-content">
          
          <div className="stats-grid">
            <div className="profile-card stat-box">
              <div className="stat-icon">📈</div>
              <div className="stat-info">
                <span className="stat-label">Total Submissions</span>
                <span className="stat-value">{stats.totalSubmissions}</span>
              </div>
            </div>
            <div className="profile-card stat-box">
              <div className="stat-icon">🎯</div>
              <div className="stat-info">
                <span className="stat-label">Acceptance Rate</span>
                <span className="stat-value">{stats.acceptanceRate}%</span>
              </div>
            </div>
          </div>

          <div className="profile-card difficulty-card">
            <h3 className="card-title">Solved by Difficulty</h3>
            <div className="difficulty-bars">
              
              <div className="diff-bar-item easy">
                <div className="diff-header">
                  <span className="diff-label">Easy</span>
                  <span className="diff-count">{stats.easySolved} <span className="diff-total">/ {stats.totalEasy}</span></span>
                </div>
                <div className="progress-track">
                  <div className="progress-fill" style={{ width: `${stats.totalEasy > 0 ? (stats.easySolved / stats.totalEasy) * 100 : 0}%` }}></div>
                </div>
              </div>

              <div className="diff-bar-item medium">
                <div className="diff-header">
                  <span className="diff-label">Medium</span>
                  <span className="diff-count">{stats.mediumSolved} <span className="diff-total">/ {stats.totalMedium}</span></span>
                </div>
                <div className="progress-track">
                  <div className="progress-fill" style={{ width: `${stats.totalMedium > 0 ? (stats.mediumSolved / stats.totalMedium) * 100 : 0}%` }}></div>
                </div>
              </div>

              <div className="diff-bar-item hard">
                <div className="diff-header">
                  <span className="diff-label">Hard</span>
                  <span className="diff-count">{stats.hardSolved} <span className="diff-total">/ {stats.totalHard}</span></span>
                </div>
                <div className="progress-track">
                  <div className="progress-fill" style={{ width: `${stats.totalHard > 0 ? (stats.hardSolved / stats.totalHard) * 100 : 0}%` }}></div>
                </div>
              </div>

            </div>
          </div>

          <div className="profile-bottom-grid">
            <div className="profile-card languages-card">
              <h3 className="card-title">Languages Used</h3>
              {langs.length > 0 ? (
                <ul className="lang-list">
                  {langs.map(([lang, count]) => (
                    <li key={lang} className="lang-item">
                      <span className="lang-name">{lang}</span>
                      <span className="lang-count">{count} subs</span>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="no-data">No submissions yet.</p>
              )}
            </div>

            <div className="profile-card recent-subs-card">
              <h3 className="card-title">Recent Submissions</h3>
              {stats.recentSubmissions && stats.recentSubmissions.length > 0 ? (
                <div className="recent-subs-list">
                  {stats.recentSubmissions.slice(0, 5).map(sub => (
                    <Link to={`/submission/${sub.id}`} key={sub.id} className="recent-sub-item">
                      <div className="recent-sub-left">
                        <span className="recent-sub-title">{sub.problemTitle}</span>
                        <span className="recent-sub-time">{new Date(sub.submittedAt).toLocaleDateString()}</span>
                      </div>
                      <div className="recent-sub-right">
                        <span className={`badge ${sub.status === 'ACCEPTED' ? 'badge-accepted' : 'badge-wrong'}`}>
                          {sub.status === 'ACCEPTED' ? 'Accepted' : 'Failed'}
                        </span>
                      </div>
                    </Link>
                  ))}
                </div>
              ) : (
                <p className="no-data">No recent submissions.</p>
              )}
            </div>
          </div>

        </div>
      </div>
    </div>
  );
}
