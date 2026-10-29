import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';

/** What the player needs from a track; works for both Song and PlaylistSong. */
export interface Track {
  id: number;
  title: string;
  artist: string;
  songUrl: string;
  imageUrl: string | null;
}

interface PlayerState {
  queue: Track[];
  index: number;
  current: Track | null;
  playing: boolean;
  play: (queue: Track[], startIndex?: number) => void;
  toggle: () => void;
  next: () => void;
  previous: () => void;
  setPlaying: (playing: boolean) => void;
}

const PlayerContext = createContext<PlayerState | null>(null);

export function PlayerProvider({ children }: { children: ReactNode }) {
  const [queue, setQueue] = useState<Track[]>([]);
  const [index, setIndex] = useState(0);
  const [playing, setPlaying] = useState(false);

  const value = useMemo<PlayerState>(() => ({
    queue,
    index,
    current: queue[index] ?? null,
    playing,
    play: (tracks, startIndex = 0) => {
      setQueue(tracks);
      setIndex(startIndex);
      setPlaying(tracks.length > 0);
    },
    toggle: () => setPlaying((p) => (queue.length > 0 ? !p : false)),
    next: () => {
      if (index + 1 < queue.length) {
        setIndex(index + 1);
      } else {
        setPlaying(false); // end of the queue: stop instead of looping
      }
    },
    previous: () => setIndex(Math.max(0, index - 1)),
    setPlaying,
  }), [queue, index, playing]);

  return <PlayerContext.Provider value={value}>{children}</PlayerContext.Provider>;
}

export function usePlayer(): PlayerState {
  const context = useContext(PlayerContext);
  if (!context) {
    throw new Error('usePlayer must be used inside <PlayerProvider>');
  }
  return context;
}
