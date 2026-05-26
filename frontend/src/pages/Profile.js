import React, { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import API from '../api/axios';
import './Profile.css';

export default function Profile() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  // Edit Profile States
  const [isEditing, setIsEditing] = useState(false);
  const [editForm, setEditForm] = useState({ name: '', bio: '', photoUrl: '' });
  const [isSaving, setIsSaving] = useState(false);
  const [photoPreview, setPhotoPreview] = useState(null);
  const fileInputRef = useRef(null);

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onloadend = () => {
      setPhotoPreview(reader.result);
      setEditForm(prev => ({ ...prev, photoUrl: reader.result }));
    };
    reader.readAsDataURL(file);
  };

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const res = await API.get('/api/users/profile');
        setStats(res.data);
        setEditForm({
          name: res.data.name || '',
          bio: res.data.bio || '',
          photoUrl: res.data.photoUrl || ''
        });
      } catch (err) {
        setError('Failed to load profile statistics');
      } finally {
        setLoading(false);
      }
    };
    fetchStats();
  }, []);

  const handleEditSubmit = async (e) => {
    e.preventDefault();
    setIsSaving(true);
    try {
      await API.put('/api/users/profile', editForm);
      // Optimistic update
      setStats({ ...stats, ...editForm });
      setIsEditing(false);
    } catch (err) {
      alert('Failed to save profile updates.');
    } finally {
      setIsSaving(false);
    }
  };

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
            {stats.photoUrl || stats.avatarUrl ? (
              <img 
                src={stats.photoUrl || stats.avatarUrl} 
                alt={stats.username} 
                className="profile-avatar-large" 
                style={{ objectFit: 'cover' }}
              />
            ) : (
              <div className="profile-avatar-large">
                {stats.username.charAt(0).toUpperCase()}
              </div>
            )}
            <h1 className="profile-username">{stats.name || stats.username}</h1>
            {stats.name && <p className="profile-username-tag">@{stats.username}</p>}
            <p className="profile-email">{stats.email}</p>
            {stats.bio && <p className="profile-bio">{stats.bio}</p>}
            <div className="profile-rank-badge">Rank: Novice</div>
            <button className="btn btn-secondary btn-edit-profile" onClick={() => { setIsEditing(!isEditing); setPhotoPreview(null); }}>
              {isEditing ? 'Cancel' : 'Edit Profile'}
            </button>

            {isEditing && (
              <form onSubmit={handleEditSubmit} className="inline-edit-form">
                <div className="form-group">
                  <label>Display Name</label>
                  <input 
                    type="text" 
                    className="form-input" 
                    placeholder="e.g. Jane Doe"
                    value={editForm.name}
                    onChange={(e) => setEditForm({...editForm, name: e.target.value})}
                  />
                </div>
                <div className="form-group">
                  <label>Profile Photo</label>
                  <div className="photo-upload-area" onClick={() => fileInputRef.current.click()}>
                    {photoPreview ? (
                      <img src={photoPreview} alt="Preview" className="photo-preview" />
                    ) : (
                      <div className="photo-upload-placeholder">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                          <polyline points="17 8 12 3 7 8"/>
                          <line x1="12" y1="3" x2="12" y2="15"/>
                        </svg>
                        <span>Click to upload photo</span>
                      </div>
                    )}
                    <input 
                      type="file" 
                      ref={fileInputRef}
                      accept="image/*"
                      onChange={handleFileChange}
                      style={{ display: 'none' }}
                    />
                  </div>
                </div>
                <div className="form-group">
                  <label>Bio</label>
                  <textarea 
                    className="form-input" 
                    rows="3" 
                    placeholder="Tell us about yourself..."
                    value={editForm.bio}
                    onChange={(e) => setEditForm({...editForm, bio: e.target.value})}
                  ></textarea>
                </div>
                <button type="submit" className="btn btn-primary btn-save-profile" disabled={isSaving}>
                  {isSaving ? 'Saving...' : 'Save Changes'}
                </button>
              </form>
            )}
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
              <div className="stat-icon" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{color: 'var(--accent)'}}>
                  <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline>
                </svg>
              </div>
              <div className="stat-info">
                <span className="stat-label">Total Submissions</span>
                <span className="stat-value">{stats.totalSubmissions}</span>
              </div>
            </div>
            <div className="profile-card stat-box">
              <div className="stat-icon" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{color: 'var(--success)'}}>
                  <circle cx="12" cy="12" r="10"></circle>
                  <circle cx="12" cy="12" r="6"></circle>
                  <circle cx="12" cy="12" r="2"></circle>
                </svg>
              </div>
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
