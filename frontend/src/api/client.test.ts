import { beforeEach, describe, expect, it, vi } from 'vitest';

import { api, ApiError, tokens } from './client';

function json(status: number, body?: unknown): Response {
  return new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

describe('api client', () => {
  beforeEach(() => tokens.clear());

  it('sends the access token as Bearer header', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(json(200, { ok: true }));
    tokens.setSession('access-1', 'refresh-1');

    await api('/api/users/me');

    const headers = fetchMock.mock.calls[0][1]?.headers as Record<string, string>;
    expect(headers.Authorization).toBe('Bearer access-1');
  });

  it('turns an error answer into ApiError with message and field errors', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(json(400, {
      status: 400, message: 'Validation failed', errors: ['email: Email should be valid.'],
    }));

    const error = (await api('/api/auth/register', { auth: false }).catch((e: unknown) => e)) as ApiError;

    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(400);
    expect(error.message).toBe('Validation failed');
    expect(error.errors).toEqual(['email: Email should be valid.']);
  });

  it('on 401 refreshes once, stores the rotated tokens and repeats the request', async () => {
    tokens.setSession('expired', 'refresh-old');
    const fetchMock = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(json(401, { status: 401, message: 'Authentication required' }))
      .mockResolvedValueOnce(json(200, { accessToken: 'access-new', refreshToken: 'refresh-new' }))
      .mockResolvedValueOnce(json(200, { name: 'Anna' }));

    const me = await api<{ name: string }>('/api/users/me');

    expect(me.name).toBe('Anna');
    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(fetchMock.mock.calls[1][0]).toBe('/api/auth/refresh');
    expect(JSON.parse(fetchMock.mock.calls[1][1]?.body as string)).toEqual({ refreshToken: 'refresh-old' });
    expect((fetchMock.mock.calls[2][1]?.headers as Record<string, string>).Authorization).toBe('Bearer access-new');
    expect(tokens.refreshToken()).toBe('refresh-new');
  });

  it('two parallel 401s share a single refresh call', async () => {
    tokens.setSession('expired', 'refresh-old');
    let refreshCalls = 0;
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      if (input === '/api/auth/refresh') {
        refreshCalls++;
        return json(200, { accessToken: 'access-new', refreshToken: 'refresh-new' });
      }
      const auth = (init?.headers as Record<string, string>).Authorization;
      return auth === 'Bearer access-new' ? json(200, { ok: true }) : json(401, { message: 'expired' });
    });

    await Promise.all([api('/api/songs'), api('/api/playlists/me')]);

    // a second refresh would send the already rotated (invalid) refresh token and log the user out
    expect(refreshCalls).toBe(1);
  });

  it('when the refresh fails too, clears the session and reports it', async () => {
    tokens.setSession('expired', 'refresh-revoked');
    const expired = vi.fn();
    tokens.onSessionExpired(expired);
    vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(json(401, { message: 'Authentication required' }))
      .mockResolvedValueOnce(json(401, { message: 'Refresh token was already used or revoked' }));

    await expect(api('/api/users/me')).rejects.toBeInstanceOf(ApiError);

    expect(expired).toHaveBeenCalledOnce();
    expect(tokens.refreshToken()).toBeNull();
  });

  it('does not try to refresh for public calls like login', async () => {
    tokens.setSession('x', 'refresh');
    const fetchMock = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValue(json(401, { message: 'Invalid email or password' }));

    await expect(api('/api/auth/login', { method: 'POST', json: {}, auth: false }))
      .rejects.toThrow('Invalid email or password');
    expect(fetchMock).toHaveBeenCalledOnce();
  });

  it('a plain-text error body becomes the message instead of a JSON parse error', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('Invalid CORS request', { status: 403 }));

    const error = (await api('/api/auth/register', { auth: false }).catch((e: unknown) => e)) as ApiError;

    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(403);
    expect(error.message).toBe('Invalid CORS request');
  });

  it('204 No Content resolves without parsing a body', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 204 }));

    await expect(api('/api/songs/1', { method: 'DELETE' })).resolves.toBeUndefined();
  });
});
