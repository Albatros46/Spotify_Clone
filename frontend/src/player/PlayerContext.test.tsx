import { act, renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';

import { PlayerProvider, usePlayer, type Track } from './PlayerContext';

const track = (id: number): Track => ({ id, title: `Song ${id}`, artist: 'A', songUrl: `/s/${id}.mp3`, imageUrl: null });
const wrapper = ({ children }: { children: ReactNode }) => <PlayerProvider>{children}</PlayerProvider>;

describe('player queue', () => {
  it('starts at the chosen song and moves forward and back', () => {
    const { result } = renderHook(() => usePlayer(), { wrapper });

    act(() => result.current.play([track(1), track(2), track(3)], 1));
    expect(result.current.current?.id).toBe(2);
    expect(result.current.playing).toBe(true);

    act(() => result.current.next());
    expect(result.current.current?.id).toBe(3);

    act(() => result.current.previous());
    act(() => result.current.previous());
    act(() => result.current.previous()); // cannot go before the first song
    expect(result.current.current?.id).toBe(1);
  });

  it('stops at the end of the queue instead of looping', () => {
    const { result } = renderHook(() => usePlayer(), { wrapper });

    act(() => result.current.play([track(1)]));
    act(() => result.current.next());

    expect(result.current.current?.id).toBe(1);
    expect(result.current.playing).toBe(false);
  });

  it('toggle does nothing while the queue is empty', () => {
    const { result } = renderHook(() => usePlayer(), { wrapper });

    act(() => result.current.toggle());

    expect(result.current.playing).toBe(false);
    expect(result.current.current).toBeNull();
  });
});
