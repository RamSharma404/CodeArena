import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import API from '../api/axios';
import './Submissions.css';

export default function Submissions() {
  const [submissions, setSubmissions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [expandedId, setExpandedId] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchSubmissions = async () => {
      try {
        const res = await API.get('/api/submissions');
        setSubmissions(res.data);
      } catch (err) {
        console.error('Failed to fetch submissions', err);
      } finally {
        setLoading(false);
      }
    };
    fetchSubmissions();
  }, []);

  const getStatusColor = (status) => {
    if (!status) return '';
    const s = status.toUpperCase();
    if (s === 'ACCEPTED') return 'var(--success)';
    return 'var(--danger)';
  };

  const getStatusBadgeClass = (status) => {
    if (!status) return '';
    return status.toUpperCase() === 'ACCEPTED' ? 'badge-accepted' : 'badge-wrong';
  };

  const toggleExpand = (id) => {
    setExpandedId(expandedId === id ? null : id);
  };

  return (
    <div className="submissions-page container">
      <div className="submissions-header">
        <h1 className="submissions-title" id="submissions-page-title">Submissions</h1>
        <p className="submissions-subtitle">Your complete submission history across all problems</p>
      </div>

      {loading ? (
        <div className="spinner-overlay">
          <div className="spinner"></div>
        </div>
      ) : submissions.length === 0 ? (
        <div className="submissions-empty">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" strokeWidth="1.5">
            <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
            <polyline points="14,2 14,8 20,8"/>
          </svg>
          <p>No submissions yet</p>
          <button className="btn btn-primary" onClick={() => navigate('/problems')}>
            Start Solving
          </button>
        </div>
      ) : (
        <div className="submissions-list-full animate-fade-in">
          {submissions.map((sub, i) => (
            <div
              key={sub.id}
              className={`submission-card ${expandedId === sub.id ? 'expanded' : ''}`}
              style={{ animationDelay: `${i * 40}ms` }}
              id={`submission-${sub.id}`}
            >
              <div
                className="submission-card-header"
                onClick={() => toggleExpand(sub.id)}
              >
                <div className="submission-card-left">
                  <span className={`badge ${getStatusBadgeClass(sub.status)}`}>
                    {sub.status}
                  </span>
                  <span className="submission-problem-id">Problem #{sub.problemId}</span>
                </div>

                <div className="submission-card-right">
                  <span className="submission-card-lang">{sub.language}</span>
                  {sub.runtimeMs != null && (
                    <span className="submission-card-metric">⏱ {sub.runtimeMs}ms</span>
                  )}
                  {sub.memoryKb != null && (
                    <span className="submission-card-metric">💾 {sub.memoryKb}KB</span>
                  )}
                  <span className="submission-card-date">
                    {sub.submittedAt && new Date(sub.submittedAt).toLocaleString()}
                  </span>
                  <svg
                    className={`expand-icon ${expandedId === sub.id ? 'rotated' : ''}`}
                    width="16" height="16" viewBox="0 0 24 24" fill="none"
                    stroke="var(--text-muted)" strokeWidth="2"
                  >
                    <polyline points="6,9 12,15 18,9"/>
                  </svg>
                </div>
              </div>

              {expandedId === sub.id && sub.code && (
                <div className="submission-card-code animate-fade-in">
                  <pre><code>{sub.code}</code></pre>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
