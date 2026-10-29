import { NavLink, Outlet } from 'react-router';

import { useAuth } from '../auth/AuthContext';
import { PlayerBar } from '../player/PlayerBar';

export function Layout() {
  const { user } = useAuth();
  return (
    <div className="app">
      <aside className="sidebar">
        <div className="logo">🎵 Musify</div>
        <nav aria-label="Main">
          <NavLink to="/" end>Home</NavLink>
          <NavLink to="/library">Your Library</NavLink>
          <NavLink to="/upload">Upload</NavLink>
          <NavLink to="/profile">{user?.name ?? 'Profile'}</NavLink>
        </nav>
      </aside>
      <main className="content">
        <Outlet />
      </main>
      <PlayerBar />
    </div>
  );
}
