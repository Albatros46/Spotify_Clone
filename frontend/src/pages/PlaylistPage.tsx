import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router';

import { playlistApi } from '../api/endpoints';
import type { PlaylistWithSongs } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { ErrorMessage } from '../components/ErrorMessage';
import { usePlayer, type Track } from '../player/PlayerContext';

export function PlaylistPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const { play } = usePlayer();
  const navigate = useNavigate();
  const [playlist, setPlaylist] = useState<PlaylistWithSongs | null>(null);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    playlistApi.get(Number(id)).then(setPlaylist).catch(setError);
  }, [id]);

  if (error && !playlist) {
    return <ErrorMessage error={error} />;
  }
  if (!playlist) {
    return <p className="muted">Loading…</p>;
  }

  const isOwner = user?.id === playlist.appUserId;
  const tracks: Track[] = playlist.songs.map((s) => ({
    id: s.songId, title: s.title, artist: s.artist, songUrl: s.songUrl, imageUrl: s.imageUrl,
  }));

  async function run(action: () => Promise<PlaylistWithSongs | void>) {
    try {
      const result = await action();
      if (result) {
        setPlaylist(result);
      }
      setError(null);
    } catch (e) {
      setError(e);
    }
  }

  return (
    <section>
      <div className="playlist-header">
        <div className="tile-cover big">♪</div>
        <div>
          <div className="muted small">{playlist.isPublic ? 'Public playlist' : 'Private playlist'}</div>
          <h1>{playlist.name}</h1>
          <div className="muted">{playlist.appUserName} · {playlist.songCount} songs</div>
          <div className="actions">
            <button type="button" className="primary" disabled={tracks.length === 0} onClick={() => play(tracks)}>
              ▶ Play
            </button>
            {isOwner && (
              <>
                <button type="button" className="secondary" onClick={() => run(async () => {
                  await playlistApi.update(playlist.id, playlist.name, playlist.description, !playlist.isPublic);
                  return playlistApi.get(playlist.id);
                })}>
                  Make {playlist.isPublic ? 'private' : 'public'}
                </button>
                <button type="button" className="link danger" onClick={() => run(async () => {
                  if (confirm(`Delete playlist "${playlist.name}"?`)) {
                    await playlistApi.remove(playlist.id);
                    navigate('/library');
                  }
                })}>Delete playlist</button>
              </>
            )}
          </div>
        </div>
      </div>
      <ErrorMessage error={error} />
      {playlist.songs.length === 0 ? (
        <p className="muted">No songs yet. Add songs from the Home page.</p>
      ) : (
        <ol className="song-list">
          {playlist.songs.map((s, i) => (
            <li key={s.songId} className="song-row">
              <button type="button" className="row-play" aria-label={`Play ${s.title}`}
                onClick={() => play(tracks, i)}>{s.position}</button>
              {s.imageUrl ? <img src={s.imageUrl} alt="" className="cover" /> : <div className="cover cover-placeholder" />}
              <div className="song-main">
                <div className="title">{s.title}</div>
                <div className="muted small">{s.artist}</div>
              </div>
              <div className="muted small album">{s.album}</div>
              <div className="row-actions">
                {isOwner && (
                  <button type="button" className="link danger" aria-label={`Remove ${s.title}`}
                    onClick={() => run(() => playlistApi.removeSong(playlist.id, s.songId))}>Remove</button>
                )}
              </div>
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}
