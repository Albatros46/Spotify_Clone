import { useEffect, useState } from 'react';

import { playlistApi, songApi } from '../api/endpoints';
import type { Playlist, Song } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { ErrorMessage } from '../components/ErrorMessage';
import { SongRow } from '../components/SongRow';
import { usePlayer } from '../player/PlayerContext';

export function HomePage() {
  const { user } = useAuth();
  const { play } = usePlayer();
  const [search, setSearch] = useState('');
  const [songs, setSongs] = useState<Song[]>([]);
  const [page, setPage] = useState(0);
  const [last, setLast] = useState(true);
  const [playlists, setPlaylists] = useState<Playlist[]>([]);
  const [error, setError] = useState<unknown>(null);

  // search as you type, but only 300 ms after the last key press
  useEffect(() => {
    const timer = setTimeout(() => load(search, 0), 300);
    return () => clearTimeout(timer);
  }, [search]);

  useEffect(() => {
    playlistApi.mine().then((p) => setPlaylists(p.content)).catch(setError);
  }, []);

  async function load(query: string, pageNumber: number) {
    try {
      const result = await songApi.list(query, pageNumber);
      setSongs((previous) => (pageNumber === 0 ? result.content : [...previous, ...result.content]));
      setPage(result.page);
      setLast(result.last);
      setError(null);
    } catch (e) {
      setError(e);
    }
  }

  async function remove(song: Song) {
    if (!confirm(`Delete "${song.title}"?`)) {
      return;
    }
    try {
      await songApi.remove(song.id);
      setSongs((s) => s.filter((x) => x.id !== song.id));
    } catch (e) {
      setError(e);
    }
  }

  return (
    <section>
      <div className="page-header">
        <h1>Songs</h1>
        <input type="search" className="search" placeholder="Search title, artist or album" aria-label="Search songs"
          value={search} onChange={(e) => setSearch(e.target.value)} />
      </div>
      <ErrorMessage error={error} />
      {songs.length === 0 ? (
        <p className="muted">{search ? 'No songs match your search.' : 'No songs yet — upload the first one.'}</p>
      ) : (
        <ol className="song-list">
          {songs.map((song, i) => (
            <SongRow key={song.id} song={song} position={i + 1}
              onPlay={() => play(songs, i)}
              playlists={playlists}
              onAddToPlaylist={async (playlistId) => { await playlistApi.addSong(playlistId, song.id); }}
              canDelete={user?.role === 'ADMIN' || user?.id === song.appUserId}
              onDelete={() => remove(song)} />
          ))}
        </ol>
      )}
      {!last && <button type="button" className="secondary" onClick={() => load(search, page + 1)}>Load more</button>}
    </section>
  );
}
