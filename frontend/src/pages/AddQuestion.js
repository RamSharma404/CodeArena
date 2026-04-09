import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import API from '../api/axios';
import './AddQuestion.css';

const DIFFICULTIES = ['EASY', 'MEDIUM', 'HARD'];
const LANGUAGES = ['JAVA', 'PYTHON', 'CPP', 'JAVASCRIPT'];

export default function AddQuestion() {
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Form fields
  const [title, setTitle] = useState('');
  const [slug, setSlug] = useState('');
  const [description, setDescription] = useState('');
  const [difficulty, setDifficulty] = useState('EASY');
  const [topicTags, setTopicTags] = useState('');
  const [companyTags, setCompanyTags] = useState('');
  
  // Test cases
  const [testCases, setTestCases] = useState([
    { input: '', output: '', hidden: false },
  ]);

  // Templates
  const [templates, setTemplates] = useState(
    LANGUAGES.map(lang => ({
      language: lang,
      userTemplate: '',
      driverCode: '',
    }))
  );

  // Redirect if not admin
  if (isAuthenticated && user?.role !== 'ADMIN') {
    navigate('/problems');
    return null;
  }

  const handleTestCaseChange = (index, field, value) => {
    const updated = [...testCases];
    updated[index][field] = value;
    setTestCases(updated);
  };

  const addTestCase = () => {
    setTestCases([...testCases, { input: '', output: '', hidden: false }]);
  };

  const removeTestCase = (index) => {
    setTestCases(testCases.filter((_, i) => i !== index));
  };

  const handleTemplateChange = (index, field, value) => {
    const updated = [...templates];
    updated[index][field] = value;
    setTemplates(updated);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (!title.trim() || !slug.trim() || !description.trim()) {
      setError('Please fill in title, slug, and description');
      return;
    }

    if (testCases.some(tc => !tc.input.trim() || !tc.output.trim())) {
      setError('Please fill in all test case inputs and outputs');
      return;
    }

    setLoading(true);
    try {
      const payload = {
        title,
        slug,
        description,
        difficulty,
        topicTags: topicTags.split(',').map(t => t.trim()).filter(t => t).join(','),
        companyTags: companyTags.split(',').map(t => t.trim()).filter(t => t).join(','),
        testCases,
        templates: templates.filter(t => t.userTemplate.trim()),
      };

      await API.post('/api/problems', payload);
      setSuccess('Question added successfully!');
      setTimeout(() => navigate('/problems'), 1500);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create question');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="add-question-page">
      <div className="container">
        <div className="add-question-header">
          <h1>Add New Question</h1>
          <p>Create a new coding problem for the platform</p>
        </div>

        {error && <div className="alert alert-error">{error}</div>}
        {success && <div className="alert alert-success">{success}</div>}

        <form onSubmit={handleSubmit} className="add-question-form">
          {/* Basic Info */}
          <div className="form-section">
            <h2 className="form-section-title">Basic Information</h2>
            
            <div className="form-group">
              <label className="form-label">Title *</label>
              <input
                type="text"
                className="form-input"
                placeholder="e.g., Two Sum"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                id="question-title"
              />
            </div>

            <div className="form-group">
              <label className="form-label">Slug *</label>
              <input
                type="text"
                className="form-input"
                placeholder="e.g., two-sum"
                value={slug}
                onChange={(e) => setSlug(e.target.value.toLowerCase().replace(/\s+/g, '-'))}
                id="question-slug"
              />
            </div>

            <div className="form-grid-2">
              <div className="form-group">
                <label className="form-label">Difficulty *</label>
                <select
                  className="form-select"
                  value={difficulty}
                  onChange={(e) => setDifficulty(e.target.value)}
                  id="question-difficulty"
                >
                  {DIFFICULTIES.map(d => (
                    <option key={d} value={d}>{d}</option>
                  ))}
                </select>
              </div>
            </div>
          </div>

          {/* Description */}
          <div className="form-section">
            <h2 className="form-section-title">Description</h2>
            
            <div className="form-group">
              <label className="form-label">Problem Description *</label>
              <textarea
                className="form-textarea"
                rows="10"
                placeholder="Describe the problem in detail. You can use HTML tags."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                id="question-description"
              />
              <small className="form-hint">HTML tags are supported (p, strong, code, pre, etc.)</small>
            </div>

            <div className="form-grid-2">
              <div className="form-group">
                <label className="form-label">Topic Tags (comma-separated)</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g., arrays, hashing, two-pointers"
                  value={topicTags}
                  onChange={(e) => setTopicTags(e.target.value)}
                  id="question-topic-tags"
                />
              </div>
              <div className="form-group">
                <label className="form-label">Company Tags (comma-separated)</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g., Google, Amazon, Meta"
                  value={companyTags}
                  onChange={(e) => setCompanyTags(e.target.value)}
                  id="question-company-tags"
                />
              </div>
            </div>
          </div>

          {/* Test Cases */}
          <div className="form-section">
            <div className="form-section-header">
              <h2 className="form-section-title">Test Cases</h2>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={addTestCase}
                id="btn-add-testcase"
              >
                + Add Test Case
              </button>
            </div>

            <div className="testcases-container">
              {testCases.map((tc, idx) => (
                <div key={idx} className="testcase-form-card">
                  <div className="testcase-form-header">
                    <span>Test Case {idx + 1}</span>
                    {testCases.length > 1 && (
                      <button
                        type="button"
                        className="btn btn-ghost btn-sm"
                        onClick={() => removeTestCase(idx)}
                        id={`btn-remove-testcase-${idx}`}
                      >
                        ✕
                      </button>
                    )}
                  </div>

                  <div className="form-grid-2">
                    <div className="form-group">
                      <label className="form-label">Input</label>
                      <textarea
                        className="form-textarea"
                        rows="4"
                        placeholder="e.g., 2\n7\n11\n15\n9"
                        value={tc.input}
                        onChange={(e) => handleTestCaseChange(idx, 'input', e.target.value)}
                        id={`testcase-input-${idx}`}
                      />
                    </div>
                    <div className="form-group">
                      <label className="form-label">Output</label>
                      <textarea
                        className="form-textarea"
                        rows="4"
                        placeholder="e.g., 0 1"
                        value={tc.output}
                        onChange={(e) => handleTestCaseChange(idx, 'output', e.target.value)}
                        id={`testcase-output-${idx}`}
                      />
                    </div>
                  </div>

                  <div className="form-group checkbox-group">
                    <label className="checkbox-label">
                      <input
                        type="checkbox"
                        checked={tc.hidden}
                        onChange={(e) => handleTestCaseChange(idx, 'hidden', e.target.checked)}
                        id={`testcase-hidden-${idx}`}
                      />
                      Hidden Test Case
                    </label>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Templates */}
          <div className="form-section">
            <h2 className="form-section-title">Code Templates (Optional)</h2>
            <p className="form-hint">Add starter code templates for each language</p>

            <div className="templates-container">
              {templates.map((tpl, idx) => (
                <div key={tpl.language} className="template-form-card">
                  <h3 className="template-language">{tpl.language}</h3>

                  <div className="form-group">
                    <label className="form-label">User Template</label>
                    <textarea
                      className="form-textarea template-textarea"
                      rows="6"
                      placeholder={`// ${tpl.language} template code`}
                      value={tpl.userTemplate}
                      onChange={(e) => handleTemplateChange(idx, 'userTemplate', e.target.value)}
                      id={`template-user-${tpl.language}`}
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">Driver Code (for testing)</label>
                    <textarea
                      className="form-textarea template-textarea"
                      rows="6"
                      placeholder={`// ${tpl.language} driver code to run tests`}
                      value={tpl.driverCode}
                      onChange={(e) => handleTemplateChange(idx, 'driverCode', e.target.value)}
                      id={`template-driver-${tpl.language}`}
                    />
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Submit */}
          <div className="form-actions">
            <button
              type="button"
              className="btn btn-ghost"
              onClick={() => navigate('/problems')}
              id="btn-cancel-add"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn btn-success"
              disabled={loading}
              id="btn-submit-question"
            >
              {loading ? (
                <><div className="spinner" style={{width:14,height:14,borderWidth:2}}></div> Creating...</>
              ) : (
                'Create Question'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
