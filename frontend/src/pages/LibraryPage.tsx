import { useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router';

import { playlistApi } from '../api/endpoints';
import type { Playlist } from '../api/types';
import { ErrorMessage } from '../components/ErrorMessage';

function PlaylistGrid({ playlists, empty }: { playlists: Playlist[]; empty: string }) {
  if (playlists.length === 0) {
    return <p className="muted">{empty}</p>;
  }
  return (
    <div className="grid">
      {playlists.map((p) => (
        <Link key={p.id} to={`/playlists/${p.id}`} className="tile">
          <div className="tile-cover">♪</div>
          <div className="title">{p.name}</div>
          <div className="muted small">{p.isPublic ? 'Public' : 'Private'} · {p.appUserName}</div>
        </Link>
      ))}
    </div>
  );
}

export function LibraryPage() {
  const [mine, setMine] = useState<Playlist[]>([]);
  const [publicOnes, setPublicOnes] = useState<Playlist[]>([]);
  const [name, setName] = useState('');
  const [isPublic, setIsPublic] = useState(false);
  const [error, setError] = useState<unknown>(null);

  async function load() {
    try {
      const [m, p] = await Promise.all([playlistApi.mine(), playlistApi.publicOnes()]);
      setMine(m.content);
      setPublicOnes(p.content);
    } catch (e) {
      setError(e);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function create(e: FormEvent) {
    e.preventDefault();
    try {
      await playlistApi.create(name, '', isPublic);
      setName('');
      setIsPublic(false);
      setError(null);
      await load();
    } catch (err) {
      setError(err);
    }
  }

  return (
    <section>
      <h1>Your Library</h1>
      <form className="inline-form" onSubmit={create}>
        <input aria-label="New playlist name" placeholder="New playlist name" value={name}
          onChange={(e) => setName(e.target.value)} required maxLength={100} />
        <label className="checkbox">
          <input type="checkbox" checked={isPublic} onChange={(e) => setIsPublic(e.target.checked)} /> Public
        </label>
        <button type="submit" className="primary">Create playlist</button>
      </form>
      <ErrorMessage error={error} />
      <h2>Your playlists</h2>
      <PlaylistGrid playlists={mine} empty="You have no playlists yet." />
      <h2>Public playlists</h2>
      <PlaylistGrid playlists={publicOnes} empty="No public playlists yet." />
    </section>
  );
}
