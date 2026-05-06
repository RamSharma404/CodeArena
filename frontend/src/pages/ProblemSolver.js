import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import Editor from '@monaco-editor/react';
import { useAuth } from '../context/AuthContext';
import API from '../api/axios';
import './ProblemSolver.css';

const LANGUAGES = [
  { value: 'JAVA', label: 'Java', monacoLang: 'java' },
  { value: 'PYTHON', label: 'Python', monacoLang: 'python' },
  { value: 'CPP', label: 'C++', monacoLang: 'cpp' },
  { value: 'JAVASCRIPT', label: 'JavaScript', monacoLang: 'javascript' },
];

export default function ProblemSolver() {
  const { slug } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const editorRef = useRef(null);
  const pageRef = useRef(null);

  // Problem data
  const [problem, setProblem] = useState(null);
  const [loadingProblem, setLoadingProblem] = useState(true);

  // Editor
  const [language, setLanguage] = useState('JAVA');
  const [code, setCode] = useState('');
  const [loadingTemplate, setLoadingTemplate] = useState(false);

  // Execution
  const [running, setRunning] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [runResult, setRunResult] = useState(null);
  const [activeTestCase, setActiveTestCase] = useState(0);

  // Left panel tabs: "description" or "submissions"
  const [leftTab, setLeftTab] = useState('description');
  const [submissions, setSubmissions] = useState([]);
  const [loadingSubs, setLoadingSubs] = useState(false);
  const [expandedSubId, setExpandedSubId] = useState(null);

  // Error
  const [error, setError] = useState('');

  // Resizable panels
  const [leftWidth, setLeftWidth] = useState(45);
  const [editorHeight, setEditorHeight] = useState(55);
  const [isResizing, setIsResizing] = useState(null);

  // Fetch problem detail
  useEffect(() => {
    const fetchProblem = async () => {
      setLoadingProblem(true);
      try {
        const res = await API.get(`/api/problems/slug/${slug}`);
        setProblem(res.data);
      } catch (err) {
        setError('Problem not found');
      } finally {
        setLoadingProblem(false);
      }
    };
    fetchProblem();
  }, [slug]);

  // Fetch template when problem or language changes
  useEffect(() => {
    if (!problem) return;
    const fetchTemplate = async () => {
      setLoadingTemplate(true);
      try {
        const res = await API.get(`/api/problems/${problem.id}/template`, {
          params: { language },
        });
        setCode(res.data.userTemplate || '');
      } catch {
        setCode('// Template not available for this language');
      } finally {
        setLoadingTemplate(false);
      }
    };
    fetchTemplate();
  }, [problem, language]);

  // Fetch submissions when left tab changes to submissions
  useEffect(() => {
    if (leftTab === 'submissions' && problem && isAuthenticated) {
      fetchSubmissions();
    }
    // eslint-disable-next-line
  }, [leftTab]);

  const fetchSubmissions = async () => {
    setLoadingSubs(true);
    try {
      const res = await API.get('/api/submissions', {
        params: { problemId: problem.id },
      });
      setSubmissions(res.data);
    } catch {
      setSubmissions([]);
    } finally {
      setLoadingSubs(false);
    }
  };

  // ─── RESIZE HANDLERS ───────────────────────────────────
  useEffect(() => {
    const handleMouseMove = (e) => {
      if (!isResizing) return;

      const page = pageRef.current;
      if (!page) return;

      if (isResizing === 'vertical') {
        const rect = page.getBoundingClientRect();
        const newLeftWidth = ((e.clientX - rect.left) / rect.width) * 100;
        if (newLeftWidth >= 25 && newLeftWidth <= 75) {
          setLeftWidth(newLeftWidth);
        }
      } else if (isResizing === 'horizontal') {
        const rightPanel = page.querySelector('.solver-right');
        if (rightPanel) {
          const rightRect = rightPanel.getBoundingClientRect();
          const newEditorHeight = ((e.clientY - rightRect.top) / rightRect.height) * 100;
          if (newEditorHeight >= 20 && newEditorHeight <= 80) {
            setEditorHeight(newEditorHeight);
          }
        }
      }
    };

    const handleMouseUp = () => {
      setIsResizing(null);
    };

    if (isResizing) {
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);
      return () => {
        document.removeEventListener('mousemove', handleMouseMove);
        document.removeEventListener('mouseup', handleMouseUp);
      };
    }
  }, [isResizing]);

  // Run code
  const handleRun = async () => {
    if (!code.trim()) return;
    setRunning(true);
    setRunResult(null);
    setActiveTestCase(0);
    setError('');
    try {
      const res = await API.post('/api/execute', {
        problemId: problem.id,
        code,
        language,
      });
      setRunResult(res.data);
    } catch (err) {
      setRunResult({
        status: 'ERROR',
        error: err.response?.data || 'Execution failed',
      });
    } finally {
      setRunning(false);
    }
  };

  // Submit code — navigates to submission detail page
  const handleSubmit = async () => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    if (!code.trim()) return;
    setSubmitting(true);
    setError('');
    try {
      const res = await API.post('/api/submit', {
        problemId: problem.id,
        code,
        language,
      });

      const submissionId = res.data.submissionId;
      if (submissionId) {
        // Navigate to submission detail page
        navigate(`/submission/${submissionId}`);
      }
    } catch (err) {
      setError(err.response?.data || 'Submission failed');
      setSubmitting(false);
    }
  };

  const getMonacoLang = () => {
    return LANGUAGES.find(l => l.value === language)?.monacoLang || 'java';
  };

  const getStatusColor = (status) => {
    if (!status) return '';
    const s = status.toUpperCase();
    if (s === 'ACCEPTED' || s === 'SUCCESS') return 'var(--success)';
    if (s === 'WRONG_ANSWER') return 'var(--danger)';
    if (s.includes('ERROR') || s === 'TIME_LIMIT') return 'var(--danger)';
    return 'var(--warning)';
  };

  const getStatusBadgeClass = (status) => {
    if (!status) return '';
    return status.toUpperCase() === 'ACCEPTED' ? 'badge-accepted' : 'badge-wrong';
  };

  if (loadingProblem) {
    return (
      <div className="spinner-overlay" style={{ minHeight: 'calc(100vh - 60px)' }}>
        <div className="spinner"></div>
      </div>
    );
  }

  if (error && !problem) {
    return (
      <div className="solver-error container">
        <h2>Problem not found</h2>
        <p>{error}</p>
        <button className="btn btn-primary" onClick={() => navigate('/problems')}>
          Back to Problems
        </button>
      </div>
    );
  }

  const currentTC = runResult?.testCaseResults?.[activeTestCase];

  return (
    <div className="solver-page" id="solver-page" ref={pageRef} style={{ userSelect: isResizing ? 'none' : 'auto' }}>
      {/* Left Panel */}
      <div className="solver-left" id="problem-description-panel" style={{ width: `${leftWidth}%` }}>
        {/* Left Panel Tabs */}
        <div className="solver-left-tabs">
          <Link to="/problems" className="solver-back-btn" title="Back to Problems" id="btn-back-to-problems">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <polyline points="15,18 9,12 15,6" />
            </svg>
          </Link>
          <button
            className={`solver-left-tab ${leftTab === 'description' ? 'active' : ''}`}
            onClick={() => setLeftTab('description')}
            id="tab-description"
          >
            📄 Description
          </button>
          <button
            className={`solver-left-tab ${leftTab === 'submissions' ? 'active' : ''}`}
            onClick={() => setLeftTab('submissions')}
            id="tab-problem-submissions"
          >
            📋 Submissions
          </button>
        </div>

        {/* Description Tab */}
        {leftTab === 'description' && problem && (
          <>
            <div className="solver-left-header">
              <h1 className="solver-problem-title" id="problem-title">{problem.title}</h1>
              <div className="solver-meta">
                <span className={`badge badge-${problem.difficulty?.toLowerCase()}`}>
                  {problem.difficulty}
                </span>
                {problem.topicTags && problem.topicTags.split(',').map((tag, i) => (
                  <span key={i} className="tag-chip">{tag.trim()}</span>
                ))}
              </div>
            </div>

            <div className="solver-left-body">
              <div
                className="solver-description"
                id="problem-description"
                dangerouslySetInnerHTML={{ __html: formatDescription(problem.description) }}
              />

              {/* Visible Test Cases */}
              {problem.visibleTestCases && problem.visibleTestCases.length > 0 && (
                <div className="solver-testcases" id="visible-testcases">
                  <h3 className="solver-section-title">Examples</h3>
                  {problem.visibleTestCases.map((tc, i) => (
                    <div key={tc.id || i} className="testcase-card">
                      <div className="testcase-header">Example {i + 1}</div>
                      <div className="testcase-row">
                        <span className="testcase-label">Input:</span>
                        <code className="testcase-value">{tc.input}</code>
                      </div>
                      <div className="testcase-row">
                        <span className="testcase-label">Output:</span>
                        <code className="testcase-value">{tc.output}</code>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </>
        )}

        {/* Submissions Tab */}
        {leftTab === 'submissions' && (
          <div className="solver-submissions-tab" id="problem-submissions-list">
            {!isAuthenticated ? (
              <div className="output-placeholder">
                <p>Please <button className="auth-link" onClick={() => navigate('/login')} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--accent-glow)', textDecoration: 'underline' }}>log in</button> to view submissions</p>
              </div>
            ) : loadingSubs ? (
              <div className="output-running" style={{ padding: 32 }}>
                <div className="spinner" style={{ width: 20, height: 20, borderWidth: 2 }}></div>
                <span>Loading submissions...</span>
              </div>
            ) : submissions.length === 0 ? (
              <div className="output-placeholder" style={{ padding: 40 }}>
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" strokeWidth="1.5">
                  <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" />
                  <polyline points="14,2 14,8 20,8" />
                </svg>
                <p>No submissions yet for this problem</p>
              </div>
            ) : (
              <div className="solver-submissions-list">
                {submissions.map((sub) => (
                  <div key={sub.id} className={`solver-sub-item ${expandedSubId === sub.id ? 'expanded' : ''}`}>
                    <div
                      className="solver-sub-header"
                      onClick={() => setExpandedSubId(expandedSubId === sub.id ? null : sub.id)}
                    >
                      <div className="solver-sub-left">
                        <span className={`badge ${getStatusBadgeClass(sub.status)}`}>
                          {sub.status}
                        </span>
                        <span className="solver-sub-lang">{sub.language}</span>
                        {sub.runtimeMs != null && (
                          <span className="solver-sub-time">⏱ {sub.runtimeMs}ms</span>
                        )}
                      </div>
                      <div className="solver-sub-right">
                        <span className="solver-sub-date">
                          {sub.submittedAt && new Date(sub.submittedAt).toLocaleDateString()}
                        </span>
                        <Link
                          to={`/submission/${sub.id}`}
                          className="solver-sub-detail-link"
                          onClick={(e) => e.stopPropagation()}
                          title="View details"
                        >
                          →
                        </Link>
                        <svg
                          className={`expand-icon ${expandedSubId === sub.id ? 'rotated' : ''}`}
                          width="14" height="14" viewBox="0 0 24 24" fill="none"
                          stroke="var(--text-muted)" strokeWidth="2"
                        >
                          <polyline points="6,9 12,15 18,9" />
                        </svg>
                      </div>
                    </div>
                    {expandedSubId === sub.id && sub.code && (
                      <div className="solver-sub-code animate-fade-in">
                        <pre><code>{sub.code}</code></pre>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      {/* Vertical Resize Handle */}
      <div
        className="resize-handle resize-handle-vertical"
        onMouseDown={() => setIsResizing('vertical')}
        id="resize-vertical-handle"
      />

      {/* Right Panel — Editor + Output */}
      <div className="solver-right" id="editor-panel">
        {/* Editor Header */}
        <div className="editor-header">
          <select
            className="form-select editor-lang-select"
            value={language}
            onChange={(e) => setLanguage(e.target.value)}
            id="language-selector"
          >
            {LANGUAGES.map(l => (
              <option key={l.value} value={l.value}>{l.label}</option>
            ))}
          </select>

          <div className="editor-actions">
            <button
              className="btn btn-secondary btn-sm"
              onClick={handleRun}
              disabled={running || !code.trim()}
              id="btn-run"
            >
              {running ? (
                <><div className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }}></div> Running</>
              ) : (
                <>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><polygon points="5,3 19,12 5,21" /></svg>
                  Run
                </>
              )}
            </button>
            <button
              className="btn btn-success btn-sm"
              onClick={handleSubmit}
              disabled={submitting || !code.trim()}
              id="btn-submit"
            >
              {submitting ? (
                <><div className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }}></div> Submitting</>
              ) : (
                <>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"><polyline points="20,6 9,17 4,12" /></svg>
                  Submit
                </>
              )}
            </button>
          </div>
        </div>

        {/* Monaco Editor */}
        <div className="editor-wrapper" id="code-editor" style={{ height: `${editorHeight}%` }}>
          {loadingTemplate ? (
            <div className="spinner-overlay" style={{ minHeight: 200 }}>
              <div className="spinner"></div>
            </div>
          ) : (
            <Editor
              height="100%"
              language={getMonacoLang()}
              value={code}
              onChange={(val) => setCode(val || '')}
              onMount={(editor) => { editorRef.current = editor; }}
              theme="vs-dark"
              options={{
                fontSize: 14,
                fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
                minimap: { enabled: false },
                scrollBeyondLastLine: false,
                padding: { top: 16 },
                wordWrap: 'on',
                automaticLayout: true,
                tabSize: 4,
                renderLineHighlight: 'gutter',
                lineNumbersMinChars: 3,
              }}
            />
          )}
        </div>

        {/* Horizontal Resize Handle */}
        <div
          className="resize-handle resize-handle-horizontal"
          onMouseDown={() => setIsResizing('horizontal')}
          id="resize-horizontal-handle"
        />

        {/* Output Panel — Only "Result" tab now */}
        <div className="output-panel" id="output-panel" style={{ height: `${100 - editorHeight}%` }}>
          <div className="tabs">
            <button className="tab active" id="tab-result">Result</button>
          </div>

          <div className="output-content">
            <div className="output-result" id="output-result">
              {/* Loading state */}
              {running && (
                <div className="output-running">
                  <div className="spinner" style={{ width: 20, height: 20, borderWidth: 2 }}></div>
                  <span>Running your code against test cases...</span>
                </div>
              )}

              {/* Error display */}
              {error && !running && (
                <div className="alert alert-error" style={{ margin: 12 }}>{error}</div>
              )}

              {/* Placeholder */}
              {!running && !runResult && !error && (
                <div className="output-placeholder">
                  <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" strokeWidth="1.5">
                    <polygon points="5,3 19,12 5,21" />
                  </svg>
                  <p>Run your code to see results</p>
                </div>
              )}

              {/* Run Results — Per Test Case Tabs */}
              {!running && runResult && (
                <div className="run-results animate-fade-in">
                  {/* Overall status banner */}
                  <div className="run-status-banner" style={{ borderLeftColor: getStatusColor(runResult.status) }}>
                    <span className="run-status-text" style={{ color: getStatusColor(runResult.status) }}>
                      {runResult.status}
                    </span>
                    {runResult.totalCount != null && (
                      <span className="run-pass-count">
                        {runResult.passedCount} / {runResult.totalCount} passed
                      </span>
                    )}
                    {runResult.runtimeMs != null && (
                      <span className="run-runtime">⏱ {runResult.runtimeMs}ms</span>
                    )}
                  </div>

                  {/* General error (compile error etc.) */}
                  {runResult.error && !runResult.testCaseResults?.length && (
                    <div className="run-error-block">
                      <pre className="result-pre result-error-text">{runResult.error}</pre>
                    </div>
                  )}

                  {/* Test case tabs */}
                  {runResult.testCaseResults && runResult.testCaseResults.length > 0 && (
                    <>
                      <div className="tc-tabs">
                        {runResult.testCaseResults.map((tc, i) => (
                          <button
                            key={i}
                            className={`tc-tab ${activeTestCase === i ? 'active' : ''} ${tc.passed ? 'tc-passed' : 'tc-failed'}`}
                            onClick={() => setActiveTestCase(i)}
                          >
                            <span className={`tc-dot ${tc.passed ? 'dot-pass' : 'dot-fail'}`}></span>
                            Case {tc.testCaseIndex}
                          </button>
                        ))}
                      </div>

                      {/* Active test case detail */}
                      {currentTC && (
                        <div className="tc-detail animate-fade-in" key={activeTestCase}>
                          {currentTC.error && (
                            <div className="tc-row">
                              <span className="tc-label">Error</span>
                              <pre className="tc-pre tc-error">{currentTC.error}</pre>
                            </div>
                          )}
                          <div className="tc-row">
                            <span className="tc-label">Input</span>
                            <pre className="tc-pre">{currentTC.input}</pre>
                          </div>
                          <div className="tc-row">
                            <span className="tc-label">Expected Output</span>
                            <pre className="tc-pre tc-expected">{currentTC.expectedOutput}</pre>
                          </div>
                          <div className="tc-row">
                            <span className="tc-label">Your Output</span>
                            <pre className={`tc-pre ${currentTC.passed ? 'tc-output-pass' : 'tc-output-fail'}`}>
                              {currentTC.actualOutput || '(no output)'}
                            </pre>
                          </div>
                        </div>
                      )}
                    </>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

// Helper: basic formatting for description text
function formatDescription(text) {
  if (!text) return '';
  return text
    .replace(/\n/g, '<br/>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
}
