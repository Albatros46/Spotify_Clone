import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router';

import { userApi } from '../api/endpoints';
import { useAuth } from '../auth/AuthContext';
import { ErrorMessage } from '../components/ErrorMessage';

export function ProfilePage() {
  const { user, setUser, logout } = useAuth();
  const navigate = useNavigate();
  const [name, setName] = useState(user?.name ?? '');
  const [email, setEmail] = useState(user?.email ?? '');
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState<unknown>(null);

  async function saveProfile(e: FormEvent) {
    e.preventDefault();
    try {
      const emailChanged = email.trim().toLowerCase() !== user?.email;
      setUser(await userApi.update(name, email));
      if (emailChanged) {
        // the backend ends the session when the e-mail changes
        await logout();
        navigate('/login');
        return;
      }
      setMessage('Profile saved');
      setError(null);
    } catch (err) {
      setError(err);
    }
  }

  async function changePassword(e: FormEvent) {
    e.preventDefault();
    try {
      await userApi.changePassword(oldPassword, newPassword);
      await logout(); // all sessions end after a password change
      navigate('/login');
    } catch (err) {
      setError(err);
    }
  }

  return (
    <section>
      <h1>Profile</h1>
      {message && <p role="status" className="success">{message}</p>}
      <ErrorMessage error={error} />
      <form className="card form" onSubmit={saveProfile}>
        <h2>Account</h2>
        <label>Name <input value={name} onChange={(e) => setName(e.target.value)} required minLength={3} /></label>
        <label>Email <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required /></label>
        <button type="submit" className="primary">Save</button>
      </form>
      <form className="card form" onSubmit={changePassword}>
        <h2>Change password</h2>
        <label>Current password
          <input type="password" value={oldPassword} onChange={(e) => setOldPassword(e.target.value)} required
            autoComplete="current-password" />
        </label>
        <label>New password
          <input type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required
            minLength={8} autoComplete="new-password" />
        </label>
        <button type="submit" className="primary">Change password</button>
      </form>
      <button type="button" className="secondary" onClick={async () => { await logout(); navigate('/login'); }}>
        Log out
      </button>
    </section>
  );
}
