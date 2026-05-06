import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import Editor from '@monaco-editor/react';
import { useAuth } from '../context/AuthContext';
import API from '../api/axios';
import './SubmissionDetail.css';

const LANG_MAP = {
  JAVA: 'java',
  PYTHON: 'python',
  CPP: 'cpp',
  JAVASCRIPT: 'javascript',
};

export default function SubmissionDetail() {
  const { id } = useParams();
  const navigate = useNavigate();


  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [, setPolling] = useState(false);

  // AI Review
  const [reviewing, setReviewing] = useState(false);
  const [aiReview, setAiReview] = useState(null);

  // Problem info (for AI review + back link)
  const [problem, setProblem] = useState(null);

  // Runtime distribution chart
  const [distribution, setDistribution] = useState(null);

  useEffect(() => {
    fetchResult();
    // eslint-disable-next-line
  }, [id]);

  const fetchResult = async () => {
    try {
      setLoading(true);
      const res = await API.get(`/api/submission/${id}`);
      setResult(res.data);

      // If still processing, start polling
      if (res.data.status === 'PENDING' || res.data.status === 'RUNNING') {
        setPolling(true);
        pollResult();
      } else if (res.data.status === 'ACCEPTED') {
        // Fetch runtime distribution for bar chart
        fetchDistribution();
      }

      // Fetch problem info
      if (res.data.problemId) {
        try {
          const pRes = await API.get(`/api/problems/${res.data.problemId}`);
          setProblem(pRes.data);
        } catch {}
      }
    } catch (err) {
      setError('Submission not found');
    } finally {
      setLoading(false);
    }
  };

  const fetchDistribution = async () => {
    try {
      const res = await API.get(`/api/submission/${id}/distribution`);
      setDistribution(res.data);
    } catch {}
  };

  const pollResult = async () => {
    const poll = async () => {
      try {
        const res = await API.get(`/api/submission/${id}`);
        setResult(res.data);
        if (res.data.status === 'PENDING' || res.data.status === 'RUNNING') {
          setTimeout(poll, 2000);
        } else {
          setPolling(false);
          if (res.data.status === 'ACCEPTED') {
            fetchDistribution();
          }
        }
      } catch {
        setPolling(false);
      }
    };
    setTimeout(poll, 2000);
  };

  const handleAIReview = async () => {
    if (!result || !problem) return;
    setReviewing(true);
    setAiReview(null);
    try {
      const res = await API.post(`/api/problems/${result.problemId}/review`, {
        code: result.code,
        language: result.language,
      });
      setAiReview(res.data);
    } catch (err) {
      setAiReview({
        error: err.response?.data || 'AI Review unavailable. You must have an accepted submission first.',
      });
    } finally {
      setReviewing(false);
    }
  };

  const getStatusColor = (status) => {
    if (!status) return '';
    const s = status.toUpperCase();
    if (s === 'ACCEPTED') return 'var(--success)';
    if (s === 'WRONG_ANSWER') return 'var(--danger)';
    if (s.includes('ERROR') || s === 'TIME_LIMIT') return 'var(--danger)';
    if (s === 'PENDING' || s === 'RUNNING') return 'var(--warning)';
    return 'var(--text-muted)';
  };

  const getStatusBg = (status) => {
    if (!status) return '';
    const s = status.toUpperCase();
    if (s === 'ACCEPTED') return 'var(--success-soft)';
    if (s === 'WRONG_ANSWER') return 'var(--danger-soft)';
    if (s.includes('ERROR') || s === 'TIME_LIMIT') return 'var(--danger-soft)';
    if (s === 'PENDING' || s === 'RUNNING') return 'var(--warning-soft)';
    return 'var(--bg-elevated)';
  };

  if (loading) {
    return (
      <div className="spinner-overlay" style={{ minHeight: 'calc(100vh - 60px)' }}>
        <div className="spinner"></div>
      </div>
    );
  }

  if (error || !result) {
    return (
      <div className="sd-page container">
        <h2>Submission not found</h2>
        <p>{error}</p>
        <button className="btn btn-primary" onClick={() => navigate('/problems')}>
          Back to Problems
        </button>
      </div>
    );
  }

  const isPending = result.status === 'PENDING' || result.status === 'RUNNING';

  return (
    <div className="sd-page container" id="submission-detail-page">
      {/* Back link */}
      {problem && (
        <Link to={`/problems/${problem.slug}`} className="sd-back-link" id="back-to-problem">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <polyline points="15,18 9,12 15,6" />
          </svg>
          Back to {problem.title}
        </Link>
      )}

      {/* Status Header */}
      <div className="sd-header animate-fade-in" id="sd-header">
        <div className="sd-status-section">
          <span
            className="sd-status-badge"
            style={{ color: getStatusColor(result.status), background: getStatusBg(result.status) }}
          >
            {isPending && <div className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }}></div>}
            {result.status}
          </span>
          {result.totalTestCases != null && !isPending && (
            <span className="sd-test-count">
              {result.passedTestCases} / {result.totalTestCases} test cases passed
            </span>
          )}
        </div>

        <div className="sd-metrics">
          {result.runtimeMs != null && (
            <div className="sd-metric">
              <span className="sd-metric-label">Runtime</span>
              <span className="sd-metric-value">⏱ {result.runtimeMs}ms</span>
            </div>
          )}
          {result.language && (
            <div className="sd-metric">
              <span className="sd-metric-label">Language</span>
              <span className="sd-metric-value">{result.language}</span>
            </div>
          )}
        </div>
      </div>

      {/* Runtime Distribution Bar Chart (ACCEPTED only) */}
      {result.status === 'ACCEPTED' && (
        <div className="sd-ranking-card animate-fade-in" id="sd-ranking">
          <div className="sd-chart-header">
            <h3 className="sd-section-title">📊 Runtime Distribution</h3>
            {result.ranking?.percentile != null && (
              <span className="sd-percentile-badge">
                Faster than <strong>{result.ranking.percentile}%</strong> of {result.language} submissions
              </span>
            )}
          </div>

          {/* Bar Chart */}
          {distribution && distribution.buckets && distribution.buckets.length > 0 ? (
            <div className="sd-chart">
              <div className="sd-chart-bars">
                {(() => {
                  const maxCount = Math.max(...distribution.buckets.map(b => b.count), 1);
                  return distribution.buckets.map((bucket, i) => (
                    <div key={i} className="sd-bar-col">
                      <span className="sd-bar-count">{bucket.count}</span>
                      <div
                        className={`sd-bar ${bucket.isUser ? 'sd-bar-user' : ''}`}
                        style={{ height: `${Math.max((bucket.count / maxCount) * 140, 4)}px` }}
                        title={`${bucket.rangeLabel}: ${bucket.count} submission${bucket.count !== 1 ? 's' : ''}`}
                      >
                        {bucket.isUser && (
                          <div className="sd-bar-marker">▼</div>
                        )}
                      </div>
                      <span className="sd-bar-label">{bucket.lo}ms</span>
                    </div>
                  ));
                })()}
              </div>
              {distribution.userRuntimeMs != null && (
                <div className="sd-chart-user-info">
                  Your runtime: <strong>{distribution.userRuntimeMs}ms</strong>
                  {distribution.totalAccepted != null && (
                    <span> · {distribution.totalAccepted} accepted submissions</span>
                  )}
                </div>
              )}
            </div>
          ) : (
            <div className="sd-ranking-grid">
              {result.ranking?.percentile != null && (
                <div className="sd-ranking-item">
                  <span className="sd-ranking-value" style={{ color: 'var(--success)' }}>
                    Top {result.ranking.percentile}%
                  </span>
                  <span className="sd-ranking-label">Faster than</span>
                </div>
              )}
              {result.runtimeMs != null && (
                <div className="sd-ranking-item">
                  <span className="sd-ranking-value">{result.runtimeMs}ms</span>
                  <span className="sd-ranking-label">Your Runtime</span>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* Failed Test Case */}
      {result.failedInput && (
        <div className="sd-failed-card animate-fade-in" id="sd-failed-test">
          <h3 className="sd-section-title">❌ Failed Test Case</h3>
          <div className="sd-failed-grid">
            <div className="sd-failed-item">
              <span className="sd-failed-label">Input</span>
              <pre className="sd-failed-pre">{result.failedInput}</pre>
            </div>
            {result.failedExpectedOutput && (
              <div className="sd-failed-item">
                <span className="sd-failed-label">Expected Output</span>
                <pre className="sd-failed-pre sd-expected">{result.failedExpectedOutput}</pre>
              </div>
            )}
            {result.failedActualOutput && (
              <div className="sd-failed-item">
                <span className="sd-failed-label">Your Output</span>
                <pre className="sd-failed-pre sd-actual">{result.failedActualOutput}</pre>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Error */}
      {result.error && (
        <div className="sd-error-card animate-fade-in">
          <h3 className="sd-section-title">⚠️ Error</h3>
          <pre className="sd-error-pre">{result.error}</pre>
        </div>
      )}

      {/* Code */}
      {result.code && !isPending && (
        <div className="sd-code-card animate-fade-in" id="sd-code">
          <h3 className="sd-section-title">📝 Submitted Code</h3>
          <div className="sd-code-editor">
            <Editor
              height="400px"
              language={LANG_MAP[result.language] || 'java'}
              value={result.code}
              theme="vs-dark"
              options={{
                readOnly: true,
                fontSize: 14,
                fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
                minimap: { enabled: false },
                scrollBeyondLastLine: false,
                padding: { top: 16 },
                lineNumbersMinChars: 3,
                renderLineHighlight: 'none',
              }}
            />
          </div>
        </div>
      )}

      {/* AI Review Section */}
      {result.status === 'ACCEPTED' && (
        <div className="sd-ai-card animate-fade-in" id="sd-ai-review">
          <div className="sd-ai-header">
            <h3 className="sd-section-title">🤖 AI Code Review</h3>
            {!aiReview && (
              <button
                className="btn btn-primary btn-sm"
                onClick={handleAIReview}
                disabled={reviewing}
                id="btn-get-ai-review"
              >
                {reviewing ? (
                  <><div className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }}></div> Analyzing...</>
                ) : (
                  'Get AI Review'
                )}
              </button>
            )}
          </div>

          {reviewing && (
            <div className="sd-ai-loading">
              <div className="spinner" style={{ width: 24, height: 24, borderWidth: 2 }}></div>
              <span>AI is reviewing your code...</span>
            </div>
          )}

          {aiReview && !aiReview.error && (
            <div className="sd-ai-content">
              <div className="sd-ai-rating-bar">
                <span className="sd-ai-rating-label">Overall Rating</span>
                <span className="sd-ai-rating-value">{aiReview.overallRating}</span>
              </div>

              <div className="sd-ai-grid">
                <div className="sd-ai-item">
                  <h4>⏱ Time Complexity</h4>
                  <p>{aiReview.timeComplexity}</p>
                </div>
                <div className="sd-ai-item">
                  <h4>💾 Space Complexity</h4>
                  <p>{aiReview.spaceComplexity}</p>
                </div>
              </div>

              <div className="sd-ai-item sd-ai-full">
                <h4>✅ What You Did Well</h4>
                <p>{aiReview.whatYouDidWell}</p>
              </div>

              <div className="sd-ai-item sd-ai-full">
                <h4>🔧 Improvements</h4>
                <p>{aiReview.improvements}</p>
              </div>

              <div className="sd-ai-item sd-ai-full">
                <h4>💡 Alternative Approach</h4>
                <p>{aiReview.alternativeApproach}</p>
              </div>
            </div>
          )}

          {aiReview && aiReview.error && (
            <div className="alert alert-error" style={{ marginTop: 12 }}>
              {typeof aiReview.error === 'string' ? aiReview.error : 'AI Review unavailable. Solve the problem first.'}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
