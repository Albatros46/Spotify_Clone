# Spotify Clone · Spring Boot REST API

![Java](https://img.shields.io/badge/Java-21-orange) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F) ![Spring Security](https://img.shields.io/badge/Spring%20Security-7%20%2B%20JWT-6DB33F) ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-336791) ![Tests](https://img.shields.io/badge/tests-55%20passing-brightgreen)

Backend of a music streaming app: users upload songs, build playlists and share them. Stateless JWT authentication with refresh-token rotation, file uploads, pagination and search. The Angular frontend and the Gemini AI song insights are the next steps.

Part of my 52-week QA Automation roadmap — the first **Spring Boot** project. It is also the system under test for the API-testing weeks (REST Assured, auth, contract tests).

## API

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | – | create an account (always role `USER`) → tokens |
| POST | `/api/auth/login` | – | e-mail + password → access + refresh token |
| POST | `/api/auth/refresh` | – | refresh token → **new** pair (the old refresh token stops working) |
| POST | `/api/auth/logout` | ✓ | revoke the refresh token |
| GET / PUT | `/api/users/me` | ✓ | own profile |
| PUT | `/api/users/me/password` | ✓ | change password (old one required, ends all sessions) |
| POST | `/api/songs` | ✓ | `multipart/form-data`: `title`, `artist`, `album`, `file` (audio), `image` (optional) |
| GET | `/api/songs?search=&page=&size=&sort=` | ✓ | paginated list, search in title / artist / album |
| GET | `/api/songs/me`, `/api/songs/{id}` | ✓ | own songs, one song |
| DELETE | `/api/songs/{id}` | owner or admin | also removes it from every playlist |
| POST | `/api/playlists` | ✓ | create (private by default) |
| GET | `/api/playlists/me`, `/api/playlists/public` | ✓ | own / public playlists, paginated |
| GET | `/api/playlists/{id}` | owner, or anyone if public | playlist with songs in order |
| PUT / DELETE | `/api/playlists/{id}` | owner | update / delete |
| POST / DELETE | `/api/playlists/{id}/songs/{songId}` | owner | add (at the end) / remove (positions close up) |
| GET | `/uploads/songs/*`, `/uploads/images/*` | – | the uploaded files (an `<audio>` tag sends no token) |

Every error has the same JSON shape:

```json
{ "timestamp": "…", "status": 400, "error": "Bad Request", "message": "Validation failed",
  "path": "/api/auth/register", "errors": ["email: Email should be valid."] }
```

## Run

1. PostgreSQL with a database `spotify_music`.
2. Secrets: copy `config/application-secrets.properties.example` to `config/application-secrets.properties` and fill it in (that file is gitignored), **or** set the same names as environment variables: `DB_PASSWORD`, `JWT_SECRET` (≥ 32 characters), `MAIL_USERNAME`, `MAIL_PASSWORD`, `GEMINI_API_KEY`.
3. `./mvnw spring-boot:run` → `http://localhost:8042`

```bash
./mvnw test     # 55 tests, in-memory H2 — no PostgreSQL and no secrets needed
```

## Security decisions

| Decision | Why |
|---|---|
| Registration ignores any `role` in the request | the original DTO had a `role` field — anyone could have registered as `ADMIN` |
| Refresh tokens stored only as SHA-256, rotated on every use | a database leak does not hand out sessions; a stolen token works at most once |
| Access and refresh tokens carry a `type` claim | one cannot be used in place of the other |
| Same answer for wrong password and unknown e-mail | does not reveal which e-mails are registered |
| Someone else's private playlist answers **404**, not 403 | does not reveal which playlist ids exist |
| Uploads: content-type + extension allow-list, random file name | no `../../` paths, no `.exe` disguised as `audio/mpeg` |
| Secrets only from environment / a gitignored file | they were in `application.properties` before |
| CORS only for the configured frontend URL | tested: another origin gets 403 |

## Review of the original code — what was wrong

| # | Problem | Fix |
|---|---|---|
| 1 | Mail password, Gemini key, DB password and JWT secret in `application.properties` | moved to env / gitignored file, `*.example` template |
| 2 | `RegisterUserRequest` had `role`, no `password` | `password` added (8–72 chars, BCrypt), `role` removed |
| 3 | `boolean isPublic` + Lombok → JSON field `"public"` | `Boolean isPublic` in the DTOs → `"isPublic"` (regression test) |
| 4 | `@Data` on JPA entities: `equals`/`hashCode`/`toString` touch lazy relations | `@Getter`/`@Setter` |
| 5 | `PlaylistWithSongResponse`: `album` never set, URLs without base URL, `position` as String | fixed, same URLs as `SongResponse`, `Integer` |
| 6 | The same song could be added to a playlist twice | unique constraint + 409 |
| 7 | `application.properties` was not UTF-8 → **`mvn` could not build the project** | re-saved as UTF-8 |
| 8 | Messages: "must **exceed** 100 characters", "between 2 characters" | corrected |
| 9 | `uploads/songs` vs. `upload/images` | one `uploads/` folder |
| 10 | `AppUserRequest` mixed profile, password, role and refresh token | split into `UpdateProfileRequest` and `ChangePasswordRequest` |

## Bugs the tests found in my own code

- **Unknown URL → 500.** The catch-all exception handler also caught Spring's own 404/405/415 errors. Found by the test that checks a deleted song's file is gone. Now Spring's HTTP errors keep their status (regression test `springHttpErrorsKeepTheirStatus`).
- **Tests could not start on this machine:** Surefire's manifest JAR in the temp folder (drive `C:`) and the project on `E:` → JDK 26 dropped `target/classes` from the classpath (`'other' has different root`). Fixed with `useManifestOnlyJar=false`.

## Tests (55)

| Class | Tests | What |
|---|---|---|
| `AuthApiTest` | 18 | register, role cannot be chosen, BCrypt + SHA-256 storage, duplicates, validation messages, login, 401 JSON, refresh rotation, token types, logout, change password, CORS, status codes, no stack traces |
| `SongApiTest` | 10 | multipart upload, file served and deleted, wrong type, missing file / title, search, pagination, 404, owner / admin delete |
| `PlaylistApiTest` | 10 | `isPublic` JSON name, private by default, validation, 404 for others' private playlists, read-only for others, positions and gap closing, duplicates, song deletion, update / delete |
| `JwtServiceTest` | 8 | token types, tampering, other secret, expiry, short secret |
| `FileStorageServiceTest` | 6 | random names, path traversal, content type, extension allow-list, delete stays inside the folder |
| `TokenHasherTest` · context | 3 | |

Beyond the tests, the app was run against the real PostgreSQL 18 database and the main flow checked with `curl` (register → upload → playlist → refresh rotation → delete).

## Next steps
- Forgot / reset password by e-mail (`ForgotPasswordRequest` is prepared)
- Gemini AI song insights (`SongAiInsightsResponse` is prepared; feature off without `GEMINI_API_KEY`)
- Playlist cover upload · Angular frontend · Flyway migrations instead of `ddl-auto=update`

## What I learned
- _TODO: 2–3 sentences in your own words_
