import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import API from '../api/axios';
import './Auth.css';

export default function ForgotPassword() {
  const [step, setStep] = useState(1); // 1 = enter email, 2 = enter OTP + new password
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleRequestOtp = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await API.post('/api/auth/forgot-password', { email });
      setSuccess('OTP sent to your email.');
      setStep(2);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to send OTP.');
    } finally {
      setLoading(false);
    }
  };

  const handleResetPassword = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);
    try {
      await API.post('/api/auth/reset-password', { email, otp, newPassword });
      setSuccess('Password reset successful! Redirecting to login...');
      setTimeout(() => navigate('/login'), 1500);
    } catch (err) {
      setError(err.response?.data?.error || 'Password reset failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-bg">
        <div className="auth-orb auth-orb-1"></div>
        <div className="auth-orb auth-orb-2"></div>
      </div>

      <div className="auth-card animate-fade-in-up" id="forgot-password-card">
        <div className="auth-header">
          <h1 className="auth-title">
            {step === 1 ? 'Forgot password?' : 'Reset password'}
          </h1>
          <p className="auth-subtitle">
            {step === 1
              ? "Enter your email and we'll send you a reset code"
              : `Enter the code sent to ${email}`}
          </p>
        </div>

        {error && <div className="alert alert-error">{error}</div>}
        {success && <div className="alert alert-success">{success}</div>}

        {step === 1 ? (
          <form onSubmit={handleRequestOtp} className="auth-form">
            <div className="form-group">
              <label className="form-label" htmlFor="forgot-email">Email</label>
              <input
                id="forgot-email"
                type="email"
                className="form-input"
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>

            <button
              type="submit"
              className="btn btn-primary btn-lg auth-submit"
              disabled={loading}
              id="btn-forgot-submit"
            >
              {loading ? 'Sending...' : 'Send Reset Code'}
            </button>
          </form>
        ) : (
          <form onSubmit={handleResetPassword} className="auth-form">
            <div className="form-group">
              <label className="form-label" htmlFor="reset-otp">Verification Code</label>
              <input
                id="reset-otp"
                type="text"
                className="form-input"
                placeholder="000000"
                value={otp}
                onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                maxLength={6}
                required
                style={{ textAlign: 'center', letterSpacing: '0.3em', fontSize: '1.25rem', fontWeight: 600 }}
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="reset-new-password">New Password</label>
              <input
                id="reset-new-password"
                type="password"
                className="form-input"
                placeholder="Min. 6 characters"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                minLength={6}
                required
              />
            </div>

            <button
              type="submit"
              className="btn btn-primary btn-lg auth-submit"
              disabled={loading}
              id="btn-reset-submit"
            >
              {loading ? 'Resetting...' : 'Reset Password'}
            </button>
          </form>
        )}

        <p className="auth-footer-text">
          <Link to="/login" className="auth-link">← Back to login</Link>
        </p>
      </div>
    </div>
  );
}
