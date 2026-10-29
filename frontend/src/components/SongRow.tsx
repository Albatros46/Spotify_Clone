import { useState } from 'react';

import type { Playlist, Song } from '../api/types';
import { usePlayer } from '../player/PlayerContext';

interface Props {
  song: Song;
  position: number;
  onPlay: () => void;
  playlists: Playlist[];
  onAddToPlaylist: (playlistId: number) => Promise<void>;
  canDelete: boolean;
  onDelete: () => void;
}

export function SongRow({ song, position, onPlay, playlists, onAddToPlaylist, canDelete, onDelete }: Props) {
  const { current, playing } = usePlayer();
  const [feedback, setFeedback] = useState('');
  const isCurrent = current?.id === song.id;

  async function add(value: string) {
    if (!value) {
      return;
    }
    try {
      await onAddToPlaylist(Number(value));
      setFeedback('Added');
    } catch (e) {
      setFeedback(e instanceof Error ? e.message : 'Failed');
    }
  }

  return (
    <li className={`song-row${isCurrent ? ' current' : ''}`}>
      <button type="button" className="row-play" aria-label={`Play ${song.title}`} onClick={onPlay}>
        {isCurrent && playing ? '♫' : position}
      </button>
      {song.imageUrl ? <img src={song.imageUrl} alt="" className="cover" /> : <div className="cover cover-placeholder" />}
      <div className="song-main">
        <div className="title">{song.title}</div>
        <div className="muted small">{song.artist}</div>
      </div>
      <div className="muted small album">{song.album}</div>
      <div className="row-actions">
        {playlists.length > 0 && (
          <select aria-label={`Add ${song.title} to playlist`} value=""
            onChange={(e) => add(e.target.value)}>
            <option value="">+ Playlist</option>
            {playlists.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
          </select>
        )}
        {canDelete && (
          <button type="button" className="link danger" aria-label={`Delete ${song.title}`} onClick={onDelete}>
            Delete
          </button>
        )}
        {feedback && <span className="small muted" role="status">{feedback}</span>}
      </div>
    </li>
  );
}
