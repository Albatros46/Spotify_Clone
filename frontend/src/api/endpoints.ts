import { api } from './client';
import type { Page, Playlist, PlaylistWithSongs, Song, User } from './types';

export const authApi = {
  register: (name: string, email: string, password: string) =>
    api<User>('/api/auth/register', { method: 'POST', json: { name, email, password }, auth: false }),
  login: (email: string, password: string) =>
    api<User>('/api/auth/login', { method: 'POST', json: { email, password }, auth: false }),
  logout: () => api<{ message: string }>('/api/auth/logout', { method: 'POST' }),
};

export const userApi = {
  me: () => api<User>('/api/users/me'),
  update: (name: string, email: string) => api<User>('/api/users/me', { method: 'PUT', json: { name, email } }),
  changePassword: (oldPassword: string, newPassword: string) =>
    api<{ message: string }>('/api/users/me/password', { method: 'PUT', json: { oldPassword, newPassword } }),
};

export const songApi = {
  list: (search: string, page: number, size = 20) =>
    api<Page<Song>>(`/api/songs?${new URLSearchParams({ search, page: String(page), size: String(size) })}`),
  mine: (page = 0) => api<Page<Song>>(`/api/songs/me?page=${page}&size=50`),
  upload: (form: FormData) => api<Song>('/api/songs', { method: 'POST', form }),
  remove: (id: number) => api<void>(`/api/songs/${id}`, { method: 'DELETE' }),
};

export const playlistApi = {
  mine: () => api<Page<Playlist>>('/api/playlists/me?size=100'),
  publicOnes: () => api<Page<Playlist>>('/api/playlists/public?size=100'),
  get: (id: number) => api<PlaylistWithSongs>(`/api/playlists/${id}`),
  create: (name: string, description: string, isPublic: boolean) =>
    api<Playlist>('/api/playlists', { method: 'POST', json: { name, description, isPublic } }),
  update: (id: number, name: string, description: string | null, isPublic: boolean) =>
    api<Playlist>(`/api/playlists/${id}`, { method: 'PUT', json: { name, description, isPublic } }),
  remove: (id: number) => api<void>(`/api/playlists/${id}`, { method: 'DELETE' }),
  addSong: (id: number, songId: number) =>
    api<PlaylistWithSongs>(`/api/playlists/${id}/songs/${songId}`, { method: 'POST' }),
  removeSong: (id: number, songId: number) =>
    api<PlaylistWithSongs>(`/api/playlists/${id}/songs/${songId}`, { method: 'DELETE' }),
};
