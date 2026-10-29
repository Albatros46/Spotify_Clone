# Musify · React frontend

React 19 + TypeScript + Vite frontend for the Spotify Clone API (`../`).

```bash
npm install
npm run dev        # http://localhost:5173 — /api and /uploads are proxied to the API on :8042
npm test           # Vitest + Testing Library
npm run build      # type check + production build into dist/
```

| Folder | Content |
|---|---|
| `src/api` | `fetch` client: Bearer token, one shared refresh on 401, `ApiError` with the backend's field errors |
| `src/auth` | session (`AuthContext`), route guard |
| `src/player` | queue state and the player bar (`<audio>`) |
| `src/pages` | login / register, songs + search, upload, library, playlist, profile |
