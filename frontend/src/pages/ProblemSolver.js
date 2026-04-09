import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
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

  // Execution / Submission
  const [outputTab, setOutputTab] = useState('result');
  const [running, setRunning] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [runResult, setRunResult] = useState(null);
  const [submitResult, setSubmitResult] = useState(null);

  // Submissions history
  const [submissions, setSubmissions] = useState([]);
  const [loadingSubs, setLoadingSubs] = useState(false);

  // AI Review
  const [reviewing, setReviewing] = useState(false);
  const [aiReview, setAiReview] = useState(null);

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

  // Fetch submissions when tab is opened
  useEffect(() => {
    if (outputTab === 'submissions' && problem && isAuthenticated) {
      fetchSubmissions();
    }
    // eslint-disable-next-line
  }, [outputTab]);

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
        const rect = page.getBoundingClientRect();
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
    setOutputTab('result');
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

  // Submit code
  const handleSubmit = async () => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    if (!code.trim()) return;
    setSubmitting(true);
    setSubmitResult(null);
    setOutputTab('result');
    setError('');
    try {
      const res = await API.post('/api/submit', {
        problemId: problem.id,
        code,
        language,
      });
      setSubmitResult(res.data);
    } catch (err) {
      setSubmitResult({
        status: 'ERROR',
        error: err.response?.data || 'Submission failed',
      });
    } finally {
      setSubmitting(false);
    }
  };

  // AI Review
  const handleAIReview = async () => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    setReviewing(true);
    setAiReview(null);
    setOutputTab('ai');
    try {
      const res = await API.post(`/api/problems/${problem.id}/review`, {
        code,
        language,
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

  if (loadingProblem) {
    return (
      <div className="spinner-overlay" style={{ minHeight: 'calc(100vh - 60px)' }}>
        <div className="spinner"></div>
      </div>
    );
  }

  if (error || !problem) {
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

  return (
    <div className="solver-page" id="solver-page" ref={pageRef} style={{ userSelect: isResizing ? 'none' : 'auto' }}>
      {/* Left Panel — Problem Description */}
      <div className="solver-left" id="problem-description-panel" style={{ width: `${leftWidth}%` }}>
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
                <><div className="spinner" style={{width:14,height:14,borderWidth:2}}></div> Running</>
              ) : (
                <>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><polygon points="5,3 19,12 5,21"/></svg>
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
                <><div className="spinner" style={{width:14,height:14,borderWidth:2}}></div> Submitting</>
              ) : (
                <>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"><polyline points="20,6 9,17 4,12"/></svg>
                  Submit
                </>
              )}
            </button>
            <button
              className="btn btn-ghost btn-sm"
              onClick={handleAIReview}
              disabled={reviewing}
              id="btn-ai-review"
              title="Get AI Review (requires accepted submission)"
            >
              {reviewing ? (
                <><div className="spinner" style={{width:14,height:14,borderWidth:2}}></div> Reviewing</>
              ) : (
                <>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M12 2a10 10 0 110 20 10 10 0 010-20z"/><path d="M9.09 9a3 3 0 015.83 1c0 2-3 3-3 3"/><circle cx="12" cy="17" r=".5"/></svg>
                  AI Review
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

        {/* Output Panel */}
        <div className="output-panel" id="output-panel" style={{ height: `${100 - editorHeight}%` }}>
          <div className="tabs">
            <button
              className={`tab ${outputTab === 'result' ? 'active' : ''}`}
              onClick={() => setOutputTab('result')}
              id="tab-result"
            >
              Result
            </button>
            <button
              className={`tab ${outputTab === 'submissions' ? 'active' : ''}`}
              onClick={() => setOutputTab('submissions')}
              id="tab-submissions"
            >
              Submissions
            </button>
            <button
              className={`tab ${outputTab === 'ai' ? 'active' : ''}`}
              onClick={() => setOutputTab('ai')}
              id="tab-ai-review"
            >
              AI Review
            </button>
          </div>

          <div className="output-content">
            {/* === Result Tab === */}
            {outputTab === 'result' && (
              <div className="output-result" id="output-result">
                {(running || submitting) && (
                  <div className="output-running">
                    <div className="spinner" style={{width:20,height:20,borderWidth:2}}></div>
                    <span>{running ? 'Running your code...' : 'Submitting...'}</span>
                  </div>
                )}

                {!running && !submitting && !runResult && !submitResult && (
                  <div className="output-placeholder">
                    <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" strokeWidth="1.5">
                      <polygon points="5,3 19,12 5,21"/>
                    </svg>
                    <p>Run or submit your code to see results</p>
                  </div>
                )}

                {!running && runResult && (
                  <div className="result-card animate-fade-in">
                    <div className="result-status" style={{ color: getStatusColor(runResult.status) }}>
                      {runResult.status}
                    </div>
                    {runResult.output && (
                      <div className="result-row">
                        <span className="result-label">Output</span>
                        <pre className="result-pre">{runResult.output}</pre>
                      </div>
                    )}
                    {runResult.expectedOutput && (
                      <div className="result-row">
                        <span className="result-label">Expected</span>
                        <pre className="result-pre">{runResult.expectedOutput}</pre>
                      </div>
                    )}
                    {runResult.error && (
                      <div className="result-row">
                        <span className="result-label">Error</span>
                        <pre className="result-pre result-error-text">{runResult.error}</pre>
                      </div>
                    )}
                    <div className="result-metrics">
                      {runResult.runtimeMs != null && (
                        <span className="result-metric">⏱ {runResult.runtimeMs}ms</span>
                      )}
                      {runResult.memoryKb != null && (
                        <span className="result-metric">💾 {runResult.memoryKb}KB</span>
                      )}
                    </div>
                  </div>
                )}

                {!submitting && submitResult && (
                  <div className="result-card animate-fade-in">
                    <div className="result-status" style={{ color: getStatusColor(submitResult.status) }}>
                      {submitResult.status}
                    </div>
                    {submitResult.totalTestCases != null && (
                      <div className="result-row">
                        <span className="result-label">Test Cases</span>
                        <span className="result-value">
                          {submitResult.passedTestCases} / {submitResult.totalTestCases} passed
                        </span>
                      </div>
                    )}
                    {submitResult.failedInput && (
                      <div className="result-row">
                        <span className="result-label">Failed Input</span>
                        <pre className="result-pre">{submitResult.failedInput}</pre>
                      </div>
                    )}
                    {submitResult.failedExpectedOutput && (
                      <div className="result-row">
                        <span className="result-label">Expected</span>
                        <pre className="result-pre">{submitResult.failedExpectedOutput}</pre>
                      </div>
                    )}
                    {submitResult.failedActualOutput && (
                      <div className="result-row">
                        <span className="result-label">Your Output</span>
                        <pre className="result-pre">{submitResult.failedActualOutput}</pre>
                      </div>
                    )}
                    {submitResult.error && (
                      <div className="result-row">
                        <span className="result-label">Error</span>
                        <pre className="result-pre result-error-text">{submitResult.error}</pre>
                      </div>
                    )}
                    <div className="result-metrics">
                      {submitResult.runtimeMs != null && (
                        <span className="result-metric">⏱ {submitResult.runtimeMs}ms</span>
                      )}
                      {submitResult.memoryKb != null && (
                        <span className="result-metric">💾 {submitResult.memoryKb}KB</span>
                      )}
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* === Submissions Tab === */}
            {outputTab === 'submissions' && (
              <div className="output-submissions" id="output-submissions">
                {!isAuthenticated ? (
                  <div className="output-placeholder">
                    <p>Please <button className="auth-link" onClick={() => navigate('/login')} style={{background:'none',border:'none',cursor:'pointer'}}>log in</button> to view submissions</p>
                  </div>
                ) : loadingSubs ? (
                  <div className="output-running">
                    <div className="spinner" style={{width:20,height:20,borderWidth:2}}></div>
                    <span>Loading submissions...</span>
                  </div>
                ) : submissions.length === 0 ? (
                  <div className="output-placeholder">
                    <p>No submissions yet for this problem</p>
                  </div>
                ) : (
                  <div className="submissions-list">
                    {submissions.map((sub) => (
                      <div key={sub.id} className="submission-item">
                        <div className="submission-item-header">
                          <span className="submission-status" style={{ color: getStatusColor(sub.status) }}>
                            {sub.status}
                          </span>
                          <span className="submission-lang">{sub.language}</span>
                          <span className="submission-time">
                            {sub.runtimeMs != null && `${sub.runtimeMs}ms`}
                          </span>
                          <span className="submission-date">
                            {sub.submittedAt && new Date(sub.submittedAt).toLocaleDateString()}
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* === AI Review Tab === */}
            {outputTab === 'ai' && (
              <div className="output-ai" id="output-ai-review">
                {reviewing && (
                  <div className="output-running">
                    <div className="spinner" style={{width:20,height:20,borderWidth:2}}></div>
                    <span>AI is reviewing your code...</span>
                  </div>
                )}

                {!reviewing && !aiReview && (
                  <div className="output-placeholder">
                    <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" strokeWidth="1.5">
                      <circle cx="12" cy="12" r="10"/>
                      <path d="M9.09 9a3 3 0 015.83 1c0 2-3 3-3 3"/>
                      <circle cx="12" cy="17" r=".5"/>
                    </svg>
                    <p>Click "AI Review" after solving the problem to get feedback</p>
                  </div>
                )}

                {!reviewing && aiReview && (
                  <div className="ai-review-content animate-fade-in">
                    {aiReview.error ? (
                      <div className="alert alert-error">{typeof aiReview.error === 'string' ? aiReview.error : 'AI Review unavailable. Solve the problem first.'}</div>
                    ) : (
                      <>
                        <div className="ai-review-rating">
                          <span className="ai-rating-label">Overall Rating</span>
                          <span className="ai-rating-value">{aiReview.overallRating}</span>
                        </div>

                        <div className="ai-review-grid">
                          <div className="ai-card">
                            <h4 className="ai-card-title">⏱ Time Complexity</h4>
                            <p>{aiReview.timeComplexity}</p>
                          </div>
                          <div className="ai-card">
                            <h4 className="ai-card-title">💾 Space Complexity</h4>
                            <p>{aiReview.spaceComplexity}</p>
                          </div>
                        </div>

                        <div className="ai-card ai-card-full">
                          <h4 className="ai-card-title">✅ What You Did Well</h4>
                          <p>{aiReview.whatYouDidWell}</p>
                        </div>

                        <div className="ai-card ai-card-full">
                          <h4 className="ai-card-title">🔧 Improvements</h4>
                          <p>{aiReview.improvements}</p>
                        </div>

                        <div className="ai-card ai-card-full">
                          <h4 className="ai-card-title">💡 Alternative Approach</h4>
                          <p>{aiReview.alternativeApproach}</p>
                        </div>
                      </>
                    )}
                  </div>
                )}
              </div>
            )}
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
