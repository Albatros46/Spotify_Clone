import { useEffect, useRef, useState } from 'react';

import { usePlayer } from './PlayerContext';

function formatTime(seconds: number): string {
  if (!Number.isFinite(seconds)) {
    return '0:00';
  }
  const s = Math.floor(seconds);
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`;
}

export function PlayerBar() {
  const { current, playing, toggle, next, previous, setPlaying, queue, index } = usePlayer();
  const audio = useRef<HTMLAudioElement>(null);
  const [time, setTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [volume, setVolume] = useState(0.8);

  // play / pause follows the state; a new track starts automatically
  useEffect(() => {
    const element = audio.current;
    if (!element || !current) {
      return;
    }
    if (playing) {
      element.play().catch(() => setPlaying(false)); // e.g. file missing or autoplay blocked
    } else {
      element.pause();
    }
  }, [playing, current, setPlaying]);

  useEffect(() => {
    if (audio.current) {
      audio.current.volume = volume;
    }
  }, [volume]);

  return (
    <footer className="player" aria-label="Player">
      <div className="player-track">
        {current?.imageUrl ? <img src={current.imageUrl} alt="" /> : <div className="cover-placeholder" />}
        <div>
          <div className="title">{current?.title ?? 'Nothing playing'}</div>
          <div className="muted small">{current?.artist ?? ''}</div>
        </div>
      </div>

      <div className="player-controls">
        <div className="buttons">
          <button type="button" aria-label="Previous" onClick={previous} disabled={!current || index === 0}>⏮</button>
          <button type="button" className="play" aria-label={playing ? 'Pause' : 'Play'} onClick={toggle}
            disabled={!current}>{playing ? '⏸' : '▶'}</button>
          <button type="button" aria-label="Next" onClick={next} disabled={!current || index + 1 >= queue.length}>⏭</button>
        </div>
        <div className="progress">
          <span className="small muted">{formatTime(time)}</span>
          <input type="range" aria-label="Position" min={0} max={duration || 0} step={1} value={time}
            disabled={!current}
            onChange={(e) => {
              if (audio.current) {
                audio.current.currentTime = Number(e.target.value);
              }
            }} />
          <span className="small muted">{formatTime(duration)}</span>
        </div>
      </div>

      <div className="player-volume">
        <span aria-hidden>🔊</span>
        <input type="range" aria-label="Volume" min={0} max={1} step={0.05} value={volume}
          onChange={(e) => setVolume(Number(e.target.value))} />
      </div>

      {current && (
        <audio ref={audio} src={current.songUrl}
          onTimeUpdate={(e) => setTime(e.currentTarget.currentTime)}
          onLoadedMetadata={(e) => setDuration(e.currentTarget.duration)}
          onEnded={next} />
      )}
    </footer>
  );
}
