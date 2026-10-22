# API Reference

> This documents the API as it was designed and implemented in this school prototype.
> The project is not functional end-to-end (see the README), so some routes cannot be called successfully as-is.

All routes are mounted under `/api` on `http://localhost:8080`.

## Authentication

1. Create an account with `POST /api/users`.
2. Log in with `POST /api/login` to receive a JWT.
3. Send it on every other request: `Authorization: Bearer <token>`.

The token is signed with HS256, expires after **1 hour**, and holds two claims:

```json
{ "id": 42, "role": "CLIENT" }
```

### Access levels

| Level | Meaning |
|---|---|
| **Public** | No token required. |
| **SELF** | The caller must own the resource (the user, or the user owning the API key / cache / request). |
| **SELF or ADMIN** | The owner, or any user with the `ADMIN` role. |
| **ADMIN** | Only users with the `ADMIN` role. |

These checks are done in the handlers through `AuthUtils` (`assertSelf`, `assertSelfOrAdmin`, `assertAdmin`).

## Errors

Business errors are subclasses of `AppException`. The handler answers with the exception's status code and a plain-text message, for example:

```
HTTP/1.1 404
User not found with id: 7
```

## Users

| Method | Route | Access | Body | Response |
|---|---|---|---|---|
| `POST` | `/users` | Public | `first_name`, `last_name`, `email`, `password`, `password_confirmation`, `cached_by_default` | Created user |
| `POST` | `/login` | Public | `email`, `password` | `{ "token": "..." }` |
| `GET` | `/users` | ADMIN | — | List of users |
| `GET` | `/users/:userId` | SELF or ADMIN | — | User |
| `PUT` | `/users/:userId` | SELF | `first_name`, `last_name`, `email`, `cached_by_default` | Updated user |
| `PATCH` | `/users/:userId/password` | SELF | `password`, `password_confirmation` | 204 |
| `PATCH` | `/users/:userId/cached-by-default` | SELF | `cached_by_default` | 204 |
| `PATCH` | `/users/:userId/role` | ADMIN | `role` (`ADMIN` \| `CLIENT`) | 204 |
| `DELETE` | `/users/:userId` | SELF or ADMIN | — | 204 |

**Password rules:** at least 12 characters, one uppercase letter, one lowercase letter, one digit, one special character among `` !()-.?[]_`~;:@#$%^&*+= ``, no whitespace, and not starting with `.` or `-`.

## API keys

A new key starts **inactive**, with **0 credits** and the `PROCESSING` status. A cache is created with it.

| Method | Route | Access | Body | Response |
|---|---|---|---|---|
| `POST` | `/apikeys` | SELF | `name` | Created key |
| `GET` | `/apikeys` | ADMIN | — | List of keys |
| `GET` | `/apikeys/users/:userId` | SELF or ADMIN | — | Keys of the user |
| `GET` | `/apikeys/:apiKeyId` | SELF or ADMIN | — | Key |
| `PATCH` | `/apikeys/:apiKeyId/active` | SELF or ADMIN | `active` (boolean) | 204 |
| `PATCH` | `/apikeys/:apiKeyId/credits` | ADMIN | `credits` (int ≥ 0) | 204 |
| `PATCH` | `/apikeys/:apiKeyId/status` | ADMIN | `status` | 204 |
| `PATCH` | `/apikeys/:apiKeyId/unlimited` | ADMIN | `unlimited` (boolean) | 204 |
| `DELETE` | `/apikeys/:apiKeyId` | SELF or ADMIN | — | 204 |

Example key:

```json
{
  "id": 3,
  "name": "my-app",
  "token": "0f8c2a1e-...",
  "credit": 10,
  "active": true,
  "status": "DONE",
  "expiration_Date": null,
  "user_id": 42,
  "user_first_name": "Ada",
  "user_last_name": "Lovelace"
}
```

## Caches

Each API key has one cache holding the audio files already synthesized with it.

| Method | Route | Access | Body | Response |
|---|---|---|---|---|
| `GET` | `/caches/:cacheId/files` | SELF or ADMIN | — | List of files |
| `GET` | `/caches/:cacheId/files/:fileId` | SELF or ADMIN | — | File, or 404 |
| `DELETE` | `/caches/:cacheId` | SELF or ADMIN | — | 200 (cache emptied) |
| `DELETE` | `/caches/:cacheId/files/:fileId` | SELF or ADMIN | — | 200 |
| `PATCH` | `/caches/:cacheId/max-size` | ADMIN | `maxSize` (int) | 200 |

## Requests

Users ask an admin for more credits or more cache storage. The admin processes a request by changing its status.

| Method | Route | Access | Body | Response |
|---|---|---|---|---|
| `POST` | `/requests/credits` | SELF | `api_key_id`, `amount`, `description` | Created request (`PENDING`) |
| `POST` | `/requests/cache` | SELF | `userId`, `apiKeyId`, `size`, `description` | Created request (`PROCESSING`) |
| `GET` | `/requests` | ADMIN | — | All requests |
| `GET` | `/requests/users/:userId` | SELF or ADMIN | — | Credits and cache requests of the user |
| `GET` | `/requests/users/:userId/credits` | SELF or ADMIN | — | Credits requests of the user |
| `GET` | `/requests/users/:userId/caches` | SELF or ADMIN | — | Cache requests of the user |
| `GET` | `/requests/:requestId` | SELF or ADMIN | — | Request |
| `PATCH` | `/requests/:requestId` | ADMIN | `status` | 204 |
| `DELETE` | `/requests/:requestId` | SELF or ADMIN | — | 204 |

**Statuses** (`Status` enum, shared by requests and API keys): `PENDING`, `PROCESSING`, `DONE`, `ERROR`, `REFUSED`, `CANCELLED`.

## Synthesis (planned)

`SynthesisHandler.createSynthesis` was written for `POST /api/synthesis`, but the route is **not declared** in `VertxServer` yet. It does not use the JWT: the client authenticates with the token of an API key.

Planned body:

```json
{
  "text": "Bonjour",
  "token": "<api key token>",
  "voice": "Margaux",
  "language": "fr",
  "format": "wav",
  "use_cache": true
}
```

Only `text` and `token` are required; the other values above are the defaults.

Planned flow (`SynthesisService.generateAudio`):

1. Find the API key by its token; it must be active and have credits (unless unlimited).
2. If caching is enabled and the same text is already in the key's cache, return that file.
3. Otherwise synthesize with Acapela, store the file in the cache, spend one credit and record a `SynthesisRequest`.

Planned response:

```json
{
  "status": "success",
  "audio_path": "cache_audio/audio_<uuid>.wav",
  "message": "Le fichier audio a bien été généré ou récupéré."
}
```
