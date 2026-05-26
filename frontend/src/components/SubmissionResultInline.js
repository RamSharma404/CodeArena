import React, { useState, useEffect } from 'react';
import API from '../api/axios';
import './SubmissionResultInline.css';

export default function SubmissionResultInline({ submissionId, problem }) {
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [polling, setPolling] = useState(false);

  // AI Review
  const [reviewing, setReviewing] = useState(false);
  const [aiReview, setAiReview] = useState(null);

  // Runtime distribution chart
  const [distribution, setDistribution] = useState(null);

  useEffect(() => {
    if (submissionId) {
      setResult(null);
      setAiReview(null);
      setDistribution(null);
      setError('');
      fetchResult();
    }
    // eslint-disable-next-line
  }, [submissionId]);

  const fetchResult = async () => {
    try {
      setLoading(true);
      const res = await API.get(`/api/submission/${submissionId}`);
      setResult(res.data);

      if (res.data.status === 'PENDING' || res.data.status === 'RUNNING') {
        setPolling(true);
        pollResult();
      } else if (res.data.status === 'ACCEPTED') {
        fetchDistribution();
      }
    } catch (err) {
      setError('Submission not found');
    } finally {
      setLoading(false);
    }
  };

  const fetchDistribution = async () => {
    try {
      const res = await API.get(`/api/submission/${submissionId}/distribution`);
      setDistribution(res.data);
    } catch {}
  };

  const pollResult = async () => {
    const poll = async () => {
      try {
        const res = await API.get(`/api/submission/${submissionId}`);
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
      const res = await API.post(`/api/problems/${problem.id}/review`, {
        code: result.code,
        language: result.language,
      });
      setAiReview(res.data);
    } catch (err) {
      setAiReview({
        error: err.response?.data || 'AI Review unavailable.',
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
    if (s === 'PENDING' || s === 'RUNNING') return '#38bdf8'; // Blue for running
    return 'var(--text-muted)';
  };

  const getStatusBg = (status) => {
    if (!status) return '';
    const s = status.toUpperCase();
    if (s === 'ACCEPTED') return 'var(--success-soft)';
    if (s === 'WRONG_ANSWER') return 'var(--danger-soft)';
    if (s.includes('ERROR') || s === 'TIME_LIMIT') return 'var(--danger-soft)';
    if (s === 'PENDING' || s === 'RUNNING') return 'rgba(56, 189, 248, 0.1)';
    return 'var(--bg-elevated)';
  };

  const isPending = !result || result.status === 'PENDING' || result.status === 'RUNNING';

  const formatOutput = (val) => {
    if (val === null || val === undefined) return val;
    if (typeof val === 'string' && val.trim() === '') return `"${val}"`;
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

  if (loading && !result) {
    return (
      <div className="cool-loader-container">
        <div className="terminal-loader pulse-glow">
          <div className="terminal-header">
            <div className="terminal-dot red"></div>
            <div className="terminal-dot yellow"></div>
            <div className="terminal-dot green"></div>
          </div>
          <div className="terminal-body">
            <div className="terminal-line">> Uploading code...</div>
            <div className="blinking-cursor"></div>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="alert alert-error" style={{ margin: 16 }}>
        {error}
      </div>
    );
  }

  if (isPending) {
    return (
      <div className="cool-loader-container">
        <div className="terminal-loader pulse-glow">
          <div className="terminal-header">
            <div className="terminal-dot red"></div>
            <div className="terminal-dot yellow"></div>
            <div className="terminal-dot green"></div>
          </div>
          <div className="terminal-body">
            <div className="terminal-line">> Compiling {result.language} code...</div>
            <div className="terminal-line">> Running against hidden test cases...</div>
            <div className="terminal-line">> Judging submission...</div>
            <div className="blinking-cursor"></div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="inline-submission-result animate-fade-in">
      <div className="inline-result-header">
        <div className="inline-result-top">
          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            <span
              className="inline-status-badge"
              style={{ color: getStatusColor(result.status), background: getStatusBg(result.status) }}
            >
              {result.status}
            </span>
            {result.totalTestCases != null && (
              <span className="inline-test-count">
                {result.passedTestCases} / {result.totalTestCases} test cases passed
              </span>
            )}
          </div>
          
          <div className="inline-metrics">
            {result.runtimeMs != null && (
              <div className="inline-metric">
                <span className="inline-metric-label">Runtime</span>
                <span className="inline-metric-value">⏱ {result.runtimeMs}ms</span>
              </div>
            )}
            {result.language && (
              <div className="inline-metric">
                <span className="inline-metric-label">Language</span>
                <span className="inline-metric-value">{result.language}</span>
              </div>
            )}
          </div>
        </div>

        {result.status === 'ACCEPTED' && !aiReview && (
          <button
            className="btn btn-primary"
            onClick={handleAIReview}
            disabled={reviewing}
            style={{ alignSelf: 'flex-start', display: 'flex', alignItems: 'center', gap: '8px', padding: '8px 16px' }}
          >
            {reviewing ? (
              <><div className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }}></div> Analyzing...</>
            ) : (
              <>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" stroke="none">
                  <path d="M12 1.5l1.9 5.8a2 2 0 0 0 1.3 1.3l5.8 1.9-5.8 1.9a2 2 0 0 0-1.3 1.3L12 18.2l-1.9-5.8a2 2 0 0 0-1.3-1.3L3 9.2l5.8-1.9a2 2 0 0 0 1.3-1.3L12 1.5z"></path>
                </svg>
                Get AI Code Review
              </>
            )}
          </button>
        )}
      </div>

      {reviewing && (
        <div className="sd-ai-loading" style={{marginTop: '10px'}}>
          <div className="spinner" style={{ width: 24, height: 24, borderWidth: 2 }}></div>
          <span>AI is reviewing your code...</span>
        </div>
      )}

      {aiReview && !aiReview.error && (
        <div className="inline-card animate-fade-in" style={{ padding: '24px' }}>
          <h3 className="inline-section-title">✨ AI Code Review</h3>
          <div className="sd-ai-rating-bar" style={{ marginBottom: '16px' }}>
            <span className="sd-ai-rating-label">Overall Rating</span>
            <span className="sd-ai-rating-value">{aiReview.overallRating}</span>
          </div>
          <div className="inline-ai-grid">
            <div className="inline-ai-item">
              <h4 style={{ display: 'flex', alignItems: 'center' }}>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{marginRight: '8px', color: 'var(--accent)'}}>
                  <circle cx="12" cy="12" r="10"></circle>
                  <polyline points="12 6 12 12 16 14"></polyline>
                </svg>
                Time Complexity
              </h4>
              <p>{renderComplexity(aiReview.timeComplexity)}</p>
            </div>
            <div className="inline-ai-item">
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
          <div className="inline-ai-full">
            <h4 style={{ display: 'flex', alignItems: 'center' }}>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--success)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{marginRight: '6px'}}>
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                <polyline points="22 4 12 14.01 9 11.01"></polyline>
              </svg>
              What You Did Well
            </h4>
            <p>{aiReview.whatYouDidWell}</p>
          </div>
          <div className="inline-ai-full">
            <h4 style={{ display: 'flex', alignItems: 'center' }}>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--warning)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{marginRight: '6px'}}>
                <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline>
                <polyline points="17 6 23 6 23 12"></polyline>
              </svg>
              Improvements
            </h4>
            <p>{aiReview.improvements}</p>
          </div>
          <div className="inline-ai-full">
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
        <div className="alert alert-error">
          {typeof aiReview.error === 'string' ? aiReview.error : 'AI Review unavailable.'}
        </div>
      )}

      {result.failedInput && (
        <div className="inline-card animate-fade-in">
          <h3 className="inline-section-title" style={{ color: 'var(--danger)' }}>❌ Failed Test Case</h3>
          <div className="inline-failed-grid">
            <div className="inline-failed-item">
              <span className="inline-failed-label">Input</span>
              <pre className="inline-failed-pre">{formatOutput(result.failedInput)}</pre>
            </div>
            {result.failedExpectedOutput && (
              <div className="inline-failed-item">
                <span className="inline-failed-label">Expected Output</span>
                <pre className="inline-failed-pre inline-expected">{formatOutput(result.failedExpectedOutput)}</pre>
              </div>
            )}
            {result.failedActualOutput && (
              <div className="inline-failed-item">
                <span className="inline-failed-label">Your Output</span>
                <pre className="inline-failed-pre inline-actual">{formatOutput(result.failedActualOutput)}</pre>
              </div>
            )}
          </div>
        </div>
      )}

      {result.error && (
        <div className="inline-card animate-fade-in">
          <h3 className="inline-section-title" style={{ color: 'var(--danger)' }}>⚠️ Error</h3>
          <pre className="inline-failed-pre" style={{ background: 'var(--bg-elevated)', borderRadius: 6, border: '1px solid var(--border)' }}>{result.error}</pre>
        </div>
      )}

      {/* Simplified Distribution stats */}
      {result.status === 'ACCEPTED' && distribution && (
        <div className="inline-card animate-fade-in">
          <h3 className="inline-section-title" style={{ display: 'flex', alignItems: 'center' }}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{marginRight: '8px', color: 'var(--accent)'}}>
              <path d="M18 20V10M12 20V4M6 20v-6"></path>
            </svg>
            Runtime Distribution
          </h3>
          {result.ranking?.percentile != null ? (
            <div style={{ padding: '12px', background: 'var(--success-soft)', borderRadius: '8px', border: '1px solid rgba(16, 185, 129, 0.2)', marginBottom: '16px' }}>
              <span style={{ fontSize: '1.1rem', color: 'var(--success)', fontWeight: 600 }}>
                Faster than {result.ranking.percentile}% of {result.language} submissions
              </span>
            </div>
          ) : (
            <div style={{ padding: '12px', background: 'var(--bg-elevated)', borderRadius: '8px', border: '1px solid var(--border)', marginBottom: '16px' }}>
              Your Runtime: <strong>{result.runtimeMs}ms</strong>
            </div>
          )}
          {distribution.buckets && distribution.buckets.length > 0 && (
             <div className="sd-lc-chart" style={{ height: '150px', marginTop: '16px' }}>
             <div className="sd-lc-chart-inner">
               <div className="sd-lc-bars-area">
                 {(() => {
                   const totalCount = distribution.buckets.reduce((sum, b) => sum + b.count, 0) || 1;
                   const maxCount = Math.max(...distribution.buckets.map(b => b.count), 1);
                   const maxPercent = (maxCount / totalCount) * 100;
                   const topTick = Math.ceil(maxPercent);
                   
                   return distribution.buckets.map((bucket, i) => {
                     const percent = (bucket.count / totalCount) * 100;
                     const heightPercentage = Math.max((percent / topTick) * 100, 2);
                     return (
                       <div key={i} className="sd-lc-bar-col">
                         <div
                           className={`sd-lc-bar ${bucket.isUser ? 'sd-lc-bar-user' : ''}`}
                           style={{ height: `${heightPercentage}%` }}
                         >
                            {bucket.isUser && <div className="sd-lc-bar-marker">▼</div>}
                         </div>
                       </div>
                     );
                   });
                 })()}
               </div>
             </div>
           </div>
          )}
        </div>
      )}
    </div>
  );
}
