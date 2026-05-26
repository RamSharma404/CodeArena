import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import Editor from '@monaco-editor/react';

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

  // Copy code state
  const [copied, setCopied] = useState(false);

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

  const handleCopy = () => {
    if (result && result.code) {
      navigator.clipboard.writeText(result.code);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
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

  const formatOutput = (val) => {
    if (val === null || val === undefined) return val;
    if (typeof val === 'string' && val.trim() === '') {
      return `"${val}"`;
    }
    return val;
  };

  const renderComplexity = (text) => {
    if (!text) return null;
    const match = text.match(/(O\([^)]+\))/i);
    if (match) {
      const parts = text.split(match[1]);
      return (
        <span className="sd-ai-complexity-desc">
          {parts[0]}
          <span className="sd-ai-complexity-value">{match[1]}</span>
          {parts[1]}
        </span>
      );
    }
    return <span className="sd-ai-complexity-desc">{text}</span>;
  };

  return (
    <div className="sd-page container split-layout" id="submission-detail-page">
      
      {/* LEFT PANE */}
      <div className="sd-left-pane">
        {/* Back link */}
        {problem && (
          <Link to={`/problems/${problem.slug}`} className="sd-back-link" id="back-to-problem">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <polyline points="15,18 9,12 15,6" />
            </svg>
            Back to {problem.title}
          </Link>
        )}

        {/* Status Header & AI Review */}
        <div className="sd-header animate-fade-in" id="sd-header">
          <div className="sd-header-top">
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

              {/* AI Sparkle Button in Testcases Tab */}
              {result.status === 'ACCEPTED' && !aiReview && (
                <button
                  className="sd-ai-sparkle-btn"
                  onClick={handleAIReview}
                  disabled={reviewing}
                  title="Get AI Code Review"
                  id="btn-get-ai-review"
                >
                  {reviewing ? (
                    <>
                      <div className="spinner" style={{ width: 14, height: 14, borderWidth: 2, marginRight: '6px' }}></div>
                      Analyzing...
                    </>
                  ) : (
                    <>
                      <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor" stroke="none" style={{marginRight: '6px'}}>
                        <path d="M12 1.5l1.9 5.8a2 2 0 0 0 1.3 1.3l5.8 1.9-5.8 1.9a2 2 0 0 0-1.3 1.3L12 18.2l-1.9-5.8a2 2 0 0 0-1.3-1.3L3 9.2l5.8-1.9a2 2 0 0 0 1.3-1.3L12 1.5z"></path>
                        <path d="M19 2.5l.8 2.2a1 1 0 0 0 .6.6l2.2.8-2.2.8a1 1 0 0 0-.6.6l-.8 2.2-.8-2.2a1 1 0 0 0-.6-.6l-2.2-.8 2.2-.8a1 1 0 0 0 .6-.6l.8-2.2z"></path>
                        <path d="M5 14.5l.8 2.2a1 1 0 0 0 .6.6l2.2.8-2.2.8a1 1 0 0 0-.6.6l-.8 2.2-.8-2.2a1 1 0 0 0-.6-.6l-2.2-.8 2.2-.8a1 1 0 0 0 .6-.6l.8-2.2z"></path>
                      </svg>
                      <span style={{ fontWeight: 600, fontSize: '0.8125rem', letterSpacing: '0.02em' }}>Analysis</span>
                    </>
                  )}
                </button>
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

          {/* AI Review Content Inline */}
          {reviewing && (
            <div className="sd-ai-loading" style={{marginTop: '20px', borderTop: '1px solid var(--border)', paddingTop: '20px'}}>
              <div className="spinner" style={{ width: 24, height: 24, borderWidth: 2 }}></div>
              <span>AI is reviewing your code...</span>
            </div>
          )}

          {aiReview && !aiReview.error && (
            <div className="sd-ai-inline-content">
              <div className="sd-ai-rating-bar">
                <span className="sd-ai-rating-label">Overall Rating</span>
                <span className="sd-ai-rating-value">{aiReview.overallRating}</span>
              </div>
              <div className="sd-ai-grid">
                <div className="sd-ai-item sd-ai-complexity-card">
                  <h4 style={{ display: 'flex', alignItems: 'center' }}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{marginRight: '8px', color: 'var(--accent)'}}>
                      <circle cx="12" cy="12" r="10"></circle>
                      <polyline points="12 6 12 12 16 14"></polyline>
                    </svg>
                    Time Complexity
                  </h4>
                  <p>{renderComplexity(aiReview.timeComplexity)}</p>
                </div>
                <div className="sd-ai-item sd-ai-complexity-card">
                  <h4 style={{ display: 'flex', alignItems: 'center' }}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{marginRight: '8px', color: 'var(--accent)'}}>
                      <rect x="2" y="2" width="20" height="8" rx="2" ry="2"></rect>
                      <rect x="2" y="14" width="20" height="8" rx="2" ry="2"></rect>
                      <line x1="6" y1="6" x2="6.01" y2="6"></line>
                      <line x1="6" y1="18" x2="6.01" y2="18"></line>
                    </svg>
                    Space Complexity
                  </h4>
                  <p>{renderComplexity(aiReview.spaceComplexity)}</p>
                </div>
              </div>
              <div className="sd-ai-item sd-ai-full">
                <h4 style={{ display: 'flex', alignItems: 'center' }}>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--success)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{marginRight: '6px'}}>
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                    <polyline points="22 4 12 14.01 9 11.01"></polyline>
                  </svg>
                  What You Did Well
                </h4>
                <p>{aiReview.whatYouDidWell}</p>
              </div>
              <div className="sd-ai-item sd-ai-full">
                <h4 style={{ display: 'flex', alignItems: 'center' }}>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--warning)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{marginRight: '6px'}}>
                    <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline>
                    <polyline points="17 6 23 6 23 12"></polyline>
                  </svg>
                  Improvements
                </h4>
                <p>{aiReview.improvements}</p>
              </div>
              <div className="sd-ai-item sd-ai-full">
                <h4 style={{ display: 'flex', alignItems: 'center' }}>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{marginRight: '6px'}}>
                    <path d="M9 18h6"></path>
                    <path d="M10 22h4"></path>
                    <path d="M15.09 14c.18-.98.65-1.74 1.41-2.5A4.65 4.65 0 0 0 18 8 6 6 0 0 0 6 8c0 1 .23 2.23 1.5 3.5A4.61 4.61 0 0 1 8.91 14"></path>
                  </svg>
                  Alternative Approach
                </h4>
                <p>{aiReview.alternativeApproach}</p>
              </div>
            </div>
          )}

          {aiReview && aiReview.error && (
            <div className="alert alert-error" style={{ marginTop: 20 }}>
              {typeof aiReview.error === 'string' ? aiReview.error : 'AI Review unavailable.'}
            </div>
          )}
        </div>

        {/* Failed Test Case */}
        {result.failedInput && (
          <div className="sd-failed-card animate-fade-in" id="sd-failed-test">
            <h3 className="sd-section-title">❌ Failed Test Case</h3>
            <div className="sd-failed-grid">
              <div className="sd-failed-item">
                <span className="sd-failed-label">Input</span>
                <pre className="sd-failed-pre">{formatOutput(result.failedInput)}</pre>
              </div>
              {result.failedExpectedOutput && (
                <div className="sd-failed-item">
                  <span className="sd-failed-label">Expected Output</span>
                  <pre className="sd-failed-pre sd-expected">{formatOutput(result.failedExpectedOutput)}</pre>
                </div>
              )}
              {result.failedActualOutput && (
                <div className="sd-failed-item">
                  <span className="sd-failed-label">Your Output</span>
                  <pre className="sd-failed-pre sd-actual">{formatOutput(result.failedActualOutput)}</pre>
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



        {/* Runtime Distribution Bar Chart (ACCEPTED only) */}
        {result.status === 'ACCEPTED' && (
          <div className="sd-ranking-card animate-fade-in" id="sd-ranking">
            <div className="sd-chart-header">
              <h3 className="sd-section-title" style={{ display: 'flex', alignItems: 'center' }}>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{marginRight: '8px', color: 'var(--accent)'}}>
                  <path d="M18 20V10M12 20V4M6 20v-6"></path>
                </svg>
                Runtime Distribution
              </h3>
              {result.ranking?.percentile != null && (
                <span className="sd-percentile-badge">
                  Faster than <strong>{result.ranking.percentile}%</strong> of {result.language}
                </span>
              )}
            </div>

            {/* Bar Chart */}
            {distribution && distribution.buckets && distribution.buckets.length > 0 ? (
              <div className="sd-chart-wrapper">
                {(() => {
                  const totalCount = distribution.buckets.reduce((sum, b) => sum + b.count, 0) || 1;
                  const maxCount = Math.max(...distribution.buckets.map(b => b.count), 1);
                  const maxPercent = (maxCount / totalCount) * 100;
                  
                  // Create y-axis ticks (e.g., max, half, 0)
                  const topTick = Math.ceil(maxPercent);
                  const midTick = Math.ceil(topTick / 2);

                  return (
                    <div className="sd-lc-chart">
                      <div className="sd-lc-chart-inner">
                        {/* Y-Axis */}
                        <div className="sd-lc-y-axis">
                          <span className="sd-y-tick">{topTick}%</span>
                          <span className="sd-y-tick">{midTick}%</span>
                          <span className="sd-y-tick">0%</span>
                        </div>

                        {/* Grid Lines */}
                        <div className="sd-lc-grid">
                          <div className="sd-lc-grid-line" style={{ top: '0%' }}></div>
                          <div className="sd-lc-grid-line" style={{ top: '50%' }}></div>
                          <div className="sd-lc-grid-line" style={{ top: '100%' }}></div>
                        </div>

                        {/* Bars Area */}
                        <div className="sd-lc-bars-area">
                          {distribution.buckets.map((bucket, i) => {
                            const percent = (bucket.count / totalCount) * 100;
                            const heightPercentage = Math.max((percent / topTick) * 100, 2); // min 2% height for visibility
                            
                            return (
                              <div key={i} className="sd-lc-bar-col">
                                <div
                                  className={`sd-lc-bar ${bucket.isUser ? 'sd-lc-bar-user' : ''}`}
                                  style={{ height: `${heightPercentage}%` }}
                                >
                                  {/* Tooltip on hover */}
                                  <div className="sd-lc-tooltip">
                                    <div className="sd-tooltip-row"><strong>{percent.toFixed(1)}%</strong> of submissions</div>
                                    <div className="sd-tooltip-row">Runtime: <strong>{bucket.lo}ms</strong></div>
                                  </div>
                                  
                                  {bucket.isUser && (
                                    <div className="sd-lc-bar-marker">▼</div>
                                  )}
                                </div>
                              </div>
                            );
                          })}
                        </div>
                      </div>

                      {/* X-Axis */}
                      <div className="sd-lc-x-axis">
                        <span>{distribution.buckets[0].lo}ms</span>
                        <span>{distribution.buckets[Math.floor(distribution.buckets.length / 2)].lo}ms</span>
                        <span>{distribution.buckets[distribution.buckets.length - 1].hi}ms</span>
                      </div>
                    </div>
                  );
                })()}

                {distribution.userRuntimeMs != null && (
                  <div className="sd-chart-user-info">
                    Your runtime: <strong>{distribution.userRuntimeMs}ms</strong>
                    {distribution.totalAccepted != null && (
                      <span> · {distribution.totalAccepted} accepted</span>
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
      </div>

      {/* RIGHT PANE (Code Editor) */}
      <div className="sd-right-pane">
        {result.code && !isPending && (
          <div className="sd-code-card animate-fade-in" id="sd-code">
            <div className="sd-code-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 className="sd-section-title" style={{ display: 'flex', alignItems: 'center', margin: 0 }}>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{marginRight: '8px', color: 'var(--text-secondary)'}}>
                  <polyline points="16 18 22 12 16 6"></polyline>
                  <polyline points="8 6 2 12 8 18"></polyline>
                </svg>
                Submitted Code
              </h3>
              <button 
                className="btn btn-secondary btn-sm" 
                onClick={handleCopy}
                style={{ display: 'flex', alignItems: 'center', gap: '6px', background: 'var(--bg-elevated)', border: '1px solid var(--border)', color: 'var(--text-secondary)' }}
                title="Copy code to clipboard"
              >
                {copied ? (
                  <>
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--success)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <polyline points="20 6 9 17 4 12"></polyline>
                    </svg>
                    Copied!
                  </>
                ) : (
                  <>
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                      <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                    </svg>
                    Copy
                  </>
                )}
              </button>
            </div>
            <div className="sd-code-editor-wrapper">
              <Editor
                height="100%"
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
      </div>

    </div>
  );
}
