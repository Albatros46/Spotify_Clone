import { useState, type FormEvent } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router';

import { useAuth } from '../auth/AuthContext';
import { ErrorMessage } from '../components/ErrorMessage';

/** Login and registration share one form; "mode" decides which fields and which call. */
export function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const { user, login, register } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);

  if (user) {
    return <Navigate to="/" replace />;
  }

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      if (mode === 'login') {
        await login(email, password);
      } else {
        await register(name, email, password);
      }
      navigate((location.state as { from?: string } | null)?.from ?? '/', { replace: true });
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }

  const isLogin = mode === 'login';
  return (
    <div className="auth">
      <form className="card" onSubmit={submit}>
        <div className="logo">🎵 Musify</div>
        <h1>{isLogin ? 'Log in' : 'Create your account'}</h1>
        {!isLogin && (
          <label>Name
            <input value={name} onChange={(e) => setName(e.target.value)} required minLength={3} maxLength={50} />
          </label>
        )}
        <label>Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" />
        </label>
        <label>Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required
            minLength={isLogin ? undefined : 8} autoComplete={isLogin ? 'current-password' : 'new-password'} />
        </label>
        <ErrorMessage error={error} />
        <button type="submit" className="primary" disabled={busy}>{isLogin ? 'Log in' : 'Sign up'}</button>
        <p className="muted small">
          {isLogin ? <>No account? <Link to="/register">Sign up</Link></>
            : <>Already registered? <Link to="/login">Log in</Link></>}
        </p>
      </form>
    </div>
  );
}
