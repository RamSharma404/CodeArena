import React, { useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import API from '../api/axios';
import './Auth.css';

export default function VerifyOtp() {
  const [searchParams] = useSearchParams();
  const emailFromUrl = searchParams.get('email') || '';
  const [email] = useState(emailFromUrl);
  const [otp, setOtp] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const navigate = useNavigate();

  const handleVerify = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);
    try {
      await API.post('/api/auth/verify-otp', { email, otp });
      setSuccess('Email verified successfully! Redirecting to login...');
      setTimeout(() => navigate('/login'), 1500);
    } catch (err) {
      setError(err.response?.data?.error || err.response?.data?.message || 'Verification failed.');
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    setError('');
    setSuccess('');
    setResending(true);
    try {
      await API.post(`/api/auth/resend-otp?email=${encodeURIComponent(email)}`);
      setSuccess('OTP resent successfully. Check your email.');
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to resend OTP.');
    } finally {
      setResending(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-bg">
        <div className="auth-orb auth-orb-1"></div>
        <div className="auth-orb auth-orb-2"></div>
      </div>

      <div className="auth-card animate-fade-in-up" id="verify-otp-card">
        <div className="auth-header">
          <div className="auth-icon-circle">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <rect x="3" y="5" width="18" height="14" rx="2"/>
              <polyline points="3,7 12,13 21,7"/>
            </svg>
          </div>
          <h1 className="auth-title">Check your email</h1>
          <p className="auth-subtitle">
            We sent a 6-digit code to <strong>{email}</strong>
          </p>
        </div>

        {error && <div className="alert alert-error">{error}</div>}
        {success && <div className="alert alert-success">{success}</div>}

        <form onSubmit={handleVerify} className="auth-form">
          <div className="form-group">
            <label className="form-label" htmlFor="otp-input">Verification Code</label>
            <input
              id="otp-input"
              type="text"
              className="form-input otp-input"
              placeholder="000000"
              value={otp}
              onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
              maxLength={6}
              required
              style={{ textAlign: 'center', letterSpacing: '0.5em', fontSize: '1.5rem', fontWeight: 600 }}
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary btn-lg auth-submit"
            disabled={loading || otp.length !== 6}
            id="btn-verify-submit"
          >
            {loading ? (
              <><div className="spinner" style={{width:18,height:18,borderWidth:2}}></div> Verifying...</>
            ) : 'Verify Email'}
          </button>
        </form>

        <div className="auth-footer-text">
          Didn't receive the code?{' '}
          <button
            className="auth-link"
            onClick={handleResend}
            disabled={resending}
            id="btn-resend-otp"
            style={{ background: 'none', border: 'none', cursor: 'pointer' }}
          >
            {resending ? 'Resending...' : 'Resend OTP'}
          </button>
        </div>

        <p className="auth-footer-text" style={{ marginTop: 8 }}>
          <Link to="/login" className="auth-link">← Back to login</Link>
        </p>
      </div>
    </div>
  );
}
