// Mirrors the DTOs of the Spring Boot API (com.muzikApp.dto.response)

export interface User {
  id: number;
  name: string;
  email: string;
  role: 'USER' | 'ADMIN';
  accessToken?: string;
  refreshToken?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Song {
  id: number;
  title: string;
  artist: string;
  album: string;
  songUrl: string;
  imageUrl: string | null;
  createdAt: string;
  appUserId: number;
  appUserName: string;
}

export interface Playlist {
  id: number;
  name: string;
  description: string | null;
  isPublic: boolean;
  imageUrl: string | null;
  createdAt: string;
  updatedAt: string;
  appUserId: number;
  appUserName: string;
}

export interface PlaylistSong {
  songId: number;
  title: string;
  artist: string;
  album: string;
  songUrl: string;
  imageUrl: string | null;
  position: number;
  addedAt: string;
}

export interface PlaylistWithSongs extends Playlist {
  songCount: number;
  songs: PlaylistSong[];
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ErrorBody {
  status: number;
  error: string;
  message: string;
  path: string;
  errors?: string[];
}
