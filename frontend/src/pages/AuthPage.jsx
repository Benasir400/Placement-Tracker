import { useEffect, useState } from 'react';
import {
  Eye,
  EyeOff,
  KeyRound,
  LogIn,
  MailCheck,
} from 'lucide-react';
import { acceptInvitation, getInvitationPreview, login } from '../services/api.js';

export default function AuthPage({ mode, onAuth }) {
  const isInvite = mode === 'invite';
  const [form, setForm] = useState({
    fullName: '',
    email: '',
    password: '',
    branch: 'CSE',
    code: ''
  });
  const [inviteStatus, setInviteStatus] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  useEffect(() => {
    if (!isInvite) return;
    const params = new URLSearchParams(window.location.search);
    const code = params.get('code') || params.get('token');
    if (code) {
      setForm((current) => ({ ...current, code, codeFromUrl: true }));
      loadInvitation(code);
    }
  }, [isInvite]);

  function update(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function loadInvitation(code) {
    if (!code?.trim()) return;
    setError('');
    setInviteStatus('Checking invitation...');
    try {
      const invitation = await getInvitationPreview(code.trim());
      setForm((current) => ({
        ...current,
        fullName: invitation.studentName || '',
        email: invitation.collegeEmail || current.email,
        branch: invitation.branch || current.branch
      }));
      setInviteStatus(invitation.status === 'ACCEPTED' ? 'This invitation has already been used.' : 'Invitation verified');
    } catch (err) {
      setInviteStatus('');
      setError(err.response?.data?.message || 'Invalid invitation code');
    }
  }

  async function submit(event) {
    event.preventDefault();
    setError('');
    setLoading(true);
    try {
      const auth = isInvite
        ? await acceptInvitation({ code: form.code, collegeEmail: form.email, password: form.password })
        : await login({ email: form.email, password: form.password });
      onAuth(auth);
    } catch (err) {
      const serverMsg = err.response?.data?.message;
      const details = err.response?.data?.details;
      const detailStr = Array.isArray(details) && details.length ? details.join(', ') : '';
      setError(serverMsg ? `${serverMsg}${detailStr ? `: ${detailStr}` : ''}` : err.message || 'Authentication failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="auth-screen">
      <form className="auth-panel" onSubmit={submit}>
        <div className="auth-title">
          <span className="auth-logo">PT</span>
          <h1>PlaceTrack</h1>
        </div>
        <div className="panel-icon">{isInvite ? <MailCheck size={24} /> : <LogIn size={24} />}</div>
        <h2>{isInvite ? 'Complete registration' : 'Login'}</h2>

        {isInvite ? (
          <>
            <label>
              Invitation code
              <input
                value={form.code}
                onChange={(event) => update('code', event.target.value)}
                onBlur={() => loadInvitation(form.code)}
                autoComplete="one-time-code"
                disabled={form.codeFromUrl}
              />
            </label>
            <label>
              College email
              <input
                type="email"
                value={form.email}
                onChange={(event) => update('email', event.target.value)}
                autoComplete="email"
                disabled={Boolean(form.fullName)}
              />
            </label>
            {form.fullName && (
              <label>
                Student
                <input value={`${form.fullName} | ${form.branch}`} disabled />
              </label>
            )}
            {inviteStatus && <p className="muted">{inviteStatus}</p>}
          </>
        ) : (
          <label>
            Email
            <input type="email" value={form.email} onChange={(event) => update('email', event.target.value)} autoComplete="email" />
          </label>
        )}

        <label>
          {isInvite ? 'Create password' : 'Password'}
          <span className="password-field">
            <input
              type={showPassword ? 'text' : 'password'}
              value={form.password}
              onChange={(event) => update('password', event.target.value)}
              autoComplete={isInvite ? 'new-password' : 'current-password'}
              minLength={isInvite ? 8 : undefined}
            />
            <button type="button" onClick={() => setShowPassword((current) => !current)} aria-label={showPassword ? 'Hide password' : 'Show password'}>
              {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
            </button>
          </span>
        </label>

        {error && <p className="inline-error">{error}</p>}

        <button type="submit" className="primary-button" disabled={loading}>
          <KeyRound size={18} />
          {loading ? 'Working...' : isInvite ? 'Create account' : 'Sign in'}
        </button>

        <a className="text-link" href={isInvite ? '/login' : '/register'}>
          {isInvite ? 'Login' : 'Use invitation code'}
        </a>
      </form>
    </main>
  );
}
