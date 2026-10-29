import type { ErrorBody } from './types';

/** An error answer of the API, with the message and the field errors the backend sent. */
export class ApiError extends Error {
  readonly status: number;
  readonly errors: string[];

  constructor(status: number, message: string, errors: string[] = []) {
    super(message);
    this.status = status;
    this.errors = errors;
  }
}

const REFRESH_KEY = 'musify.refreshToken';

/*
 * The access token lives only in memory (gone on reload), the refresh token in localStorage so a
 * reload keeps the session. Trade-off: localStorage is readable by any script on the page (XSS);
 * an httpOnly cookie would be safer but needs CSRF protection on the backend.
 */
let accessToken: string | null = null;
let refreshInFlight: Promise<boolean> | null = null;
let onSessionExpired: () => void = () => {};

export const tokens = {
  setSession(access: string, refresh: string) {
    accessToken = access;
    localStorage.setItem(REFRESH_KEY, refresh);
  },
  clear() {
    accessToken = null;
    localStorage.removeItem(REFRESH_KEY);
  },
  refreshToken: () => localStorage.getItem(REFRESH_KEY),
  hasAccessToken: () => accessToken !== null,
  onSessionExpired(callback: () => void) {
    onSessionExpired = callback;
  },
};

interface Options {
  method?: string;
  json?: unknown;
  form?: FormData;
  auth?: boolean;
}

export async function api<T>(path: string, options: Options = {}): Promise<T> {
  const response = await send(path, options);

  // Access token expired: refresh once, then repeat the original request once.
  if (response.status === 401 && options.auth !== false && tokens.refreshToken()) {
    if (await refreshSession()) {
      return parse<T>(await send(path, options));
    }
    tokens.clear();
    onSessionExpired();
  }
  return parse<T>(response);
}

/** Several requests may fail with 401 at the same time; they share ONE refresh call. */
export function refreshSession(): Promise<boolean> {
  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      const refreshToken = tokens.refreshToken();
      if (!refreshToken) {
        return false;
      }
      const response = await send('/api/auth/refresh', { method: 'POST', json: { refreshToken }, auth: false });
      if (!response.ok) {
        return false;
      }
      const body = await response.json();
      tokens.setSession(body.accessToken, body.refreshToken); // rotation: the old refresh token is now invalid
      return true;
    })().finally(() => {
      refreshInFlight = null;
    });
  }
  return refreshInFlight;
}

function send(path: string, { method = 'GET', json, form, auth = true }: Options): Promise<Response> {
  const headers: Record<string, string> = {};
  if (json !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  if (auth && accessToken) {
    headers.Authorization = `Bearer ${accessToken}`;
  }
  return fetch(path, {
    method,
    headers,
    // FormData: the browser sets the multipart boundary itself
    body: form ?? (json !== undefined ? JSON.stringify(json) : undefined),
  });
}

async function parse<T>(response: Response): Promise<T> {
  if (response.status === 204) {
    return undefined as T;
  }
  const text = await response.text();
  const body = parseJson(text);
  if (!response.ok) {
    // Not every error is JSON (e.g. "Invalid CORS request" comes as plain text, found in a UI test)
    const error = body as ErrorBody | undefined;
    const message = error?.message ?? (text || response.statusText || `HTTP ${response.status}`);
    throw new ApiError(response.status, message, error?.errors ?? []);
  }
  return body as T;
}

function parseJson(text: string): unknown {
  if (!text) {
    return undefined;
  }
  try {
    return JSON.parse(text);
  } catch {
    return undefined;
  }
}
