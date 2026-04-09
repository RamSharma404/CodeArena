import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import API from '../api/axios';
import './Problems.css';

export default function Problems() {
  const [problems, setProblems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [difficulty, setDifficulty] = useState('');
  const { isAuthenticated, user } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    fetchProblems();
    // eslint-disable-next-line
  }, [difficulty, isAuthenticated]);

  const fetchProblems = async () => {
    setLoading(true);
    try {
      const params = {};
      if (difficulty) params.difficulty = difficulty;
      if (search.trim()) params.search = search.trim();
      const res = await API.get('/api/problems', { params });
      setProblems(res.data);
    } catch (err) {
      console.error('Failed to fetch problems', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    fetchProblems();
  };

  const getDifficultyClass = (diff) => {
    switch (diff) {
      case 'EASY': return 'badge-easy';
      case 'MEDIUM': return 'badge-medium';
      case 'HARD': return 'badge-hard';
      default: return '';
    }
  };

  const isAdmin = user?.role === 'ADMIN';

  const handleAddQuestion = () => {
    navigate('/add-question');
  };

  const getStatusIcon = (status) => {
    if (status === 'SOLVED') {
      return (
        <span className="status-solved" title="Solved">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#22c55e" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <path d="M20 6L9 17l-5-5"/>
          </svg>
        </span>
      );
    }
    if (status === 'ATTEMPTED') {
      return (
        <span className="status-attempted" title="Attempted">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#f59e0b" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="8" x2="12" y2="12"/>
            <line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
        </span>
      );
    }
    return <span className="status-none"></span>;
  };

  return (
    <div className="problems-page">
      <div className="problems-header container">
        <div className="problems-header-text">
          <h1 className="problems-title" id="problems-page-title">Problems</h1>
          <p className="problems-subtitle">Practice makes perfect. Choose a challenge and start coding.</p>
        </div>

        <div className="problems-filters">
          {isAdmin && (
            <button 
              className="btn btn-primary add-question-btn"
              onClick={handleAddQuestion}
              id="add-question-btn"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <line x1="12" y1="5" x2="12" y2="19"/>
                <line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
              Add Question
            </button>
          )}
          <form onSubmit={handleSearch} className="problems-search-form">
            <div className="search-input-wrapper">
              <svg className="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="11" cy="11" r="8"/>
                <line x1="21" y1="21" x2="16.65" y2="16.65"/>
              </svg>
              <input
                type="text"
                className="form-input search-input"
                placeholder="Search problems..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                id="search-problems"
              />
            </div>
          </form>

          <select
            className="form-select"
            value={difficulty}
            onChange={(e) => setDifficulty(e.target.value)}
            id="filter-difficulty"
          >
            <option value="">All Difficulty</option>
            <option value="EASY">Easy</option>
            <option value="MEDIUM">Medium</option>
            <option value="HARD">Hard</option>
          </select>
        </div>
      </div>

      <div className="container">
        {loading ? (
          <div className="spinner-overlay">
            <div className="spinner"></div>
          </div>
        ) : problems.length === 0 ? (
          <div className="problems-empty">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" strokeWidth="1.5">
              <circle cx="12" cy="12" r="10"/>
              <path d="M8 15h8M9 9h.01M15 9h.01"/>
            </svg>
            <p>No problems found.</p>
          </div>
        ) : (
          <div className="table-container animate-fade-in">
            <table className="table" id="problems-table">
              <thead>
                <tr>
                  <th style={{width: 50}}>Status</th>
                  <th style={{width: 50}}>#</th>
                  <th>Title</th>
                  <th style={{width: 110}}>Difficulty</th>
                  <th>Tags</th>
                </tr>
              </thead>
              <tbody>
                {problems.map((p, i) => (
                  <tr
                    key={p.id}
                    className="table-row-link"
                    onClick={() => navigate(`/problems/${p.slug}`)}
                    style={{ animationDelay: `${i * 40}ms` }}
                    id={`problem-row-${p.id}`}
                  >
                    <td>{getStatusIcon(p.solvedStatus)}</td>
                    <td className="problem-number">{p.id}</td>
                    <td className="problem-title-cell">{p.title}</td>
                    <td>
                      <span className={`badge ${getDifficultyClass(p.difficulty)}`}>
                        {p.difficulty}
                      </span>
                    </td>
                    <td className="problem-tags">
                      {p.topicTags && p.topicTags.split(',').map((tag, j) => (
                        <span key={j} className="tag-chip">{tag.trim()}</span>
                      ))}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
