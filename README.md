# Acapela Webservice

> **Status: non-functional prototype (school project).** This project was built by a student team as a learning exercise, not as a working product. The routes, data model and security layer are designed and implemented, but the application is not meant to run end-to-end: the speech synthesis is not wired up, the database configuration is incomplete, and several features remain unfinished. It is kept in its original state, as the team wrote it.

## Learning Goals

The main purpose of this project was to practice and improve skills in:

- **Java backend development** with Java 21 and Maven (including a fat-jar build and locally bundled jars)
- **Asynchronous web servers** with Vert.x 5 (routers, sub-routers, `Future`, `executeBlocking`, verticles)
- **REST API design**, organized by resource (users, API keys, caches, requests) with the appropriate HTTP verbs
- **Data modeling and persistence** with JPA / EclipseLink and PostgreSQL (entity relationships, single-table inheritance, transactions)
- **Authentication and authorization**: JWT, role-based and owner-based access checks, BCrypt password hashing, input validation
- **Layered architecture**: handlers / services / models, with a business exception hierarchy mapped to HTTP status codes
- **Third-party SDK integration**: the proprietary Acapela JSAPI text-to-speech libraries
- **Basic front-end tooling**: a vanilla HTML/JS API tester and admin dashboard

## Overview

Acapela Webservice is a Java/Vert.x backend that exposes a REST API around the Acapela text-to-speech engine (via the bundled Acapela jsapi/shared client jars in `lib/`). It handles user accounts, API keys with credit balances, speech-synthesis requests, and a file cache for previously synthesized audio, all backed by a PostgreSQL database through Jakarta Persistence (EclipseLink).

## Tech Stack

- **Java 21**, built with **Maven**
- **Vert.x 5** (`vertx-web`, `vertx-launcher-application`) for the HTTP server and routing
- **Jakarta Persistence API** with **EclipseLink** as the JPA provider
- **PostgreSQL** as the database
- **JWT** authentication (`vertx-auth-jwt`)
- **BCrypt** for password hashing
- Acapela JSAPI jars (bundled locally under `lib/`, added as system-scoped Maven dependencies) for the actual speech synthesis calls

## Project Structure

```
src/main/java/fr/imta/acapela_webservice/
├── server/    VertxServer.java – entry point, router setup, JWT config
├── handler/   HTTP handlers (one per resource: users, API keys, cache, synthesis)
├── service/   business logic, talks to the database via EntityManager
└── model/     JPA entities and enums (User, ApiKey, Cache, Request, Role, Status, ...)
src/main/front_end/
├── admin-dashboard.html   basic admin UI
└── api-tester.html        manual API testing page
lib/           bundled Acapela JSAPI jars (system-scoped dependencies)
```

## Domain Model (high level)

- **User**: has a `Role` (`ADMIN` or `CLIENT`), authenticates via email/password, can hold API keys.
- **ApiKey**: belongs to a user, holds a credit balance, an active/unlimited flag, and a status.
- **Cache**: stores previously synthesized audio `File`s so repeated synthesis requests don't need to hit Acapela again.
- **Request**: a generic request record with a `RequestType` (`CACHESTORAGE`, `CREDITS`, `SYNTHESISREQUEST`) and a `Status` (`PENDING`, `PROCESSING`, `DONE`, `ERROR`, `REFUSED`, `CANCELLED`), used to track synthesis jobs, cache-storage requests, and credit top-up requests.

## API

All business routes are mounted under `/api`. Two routes are public (`POST /users` to register, `POST /login` to authenticate); everything else requires a valid JWT (`Authorization` header), and some routes are further restricted to the resource owner or to admins.

| Resource | Examples |
|---|---|
| Users | `GET/PUT/DELETE /users/:userId`, `PATCH /users/:userId/password`, `PATCH /users/:userId/role` (admin) |
| API keys | `POST /apikeys`, `GET /apikeys/:apiKeyId`, `PATCH /apikeys/:apiKeyId/credits` (admin) |
| Cache | `GET /caches/:cacheId/files`, `DELETE /caches/:cacheId`, `PATCH /caches/:cacheId/max-size` (admin) |
| Requests | `POST /requests/cache`, `POST /requests/credits`, `GET /requests/:requestId`, `PATCH /requests/:requestId` (admin) |

The full API reference (access rules, request bodies, responses) is in [`docs/API.md`](docs/API.md), and every route is declared in `VertxServer.java`. A simple `api-tester.html` page is included under `src/main/front_end/` for manual testing without a full client.

## Running Locally

Requirements: JDK 21, Maven (or the bundled `./mvnw`), and a running PostgreSQL instance.

1. Configure the database connection (currently set in `src/main/resources/META-INF/persistence.xml`; JDBC URL/credentials should be added there or externalized to environment variables — not yet done).
2. Build and run:
   ```bash
   ./mvnw compile exec:java
   ```
   or build a fat jar and run it directly:
   ```bash
   ./mvnw package
   java -jar target/starter-1.0.0-SNAPSHOT-fat.jar
   ```
3. The server listens on `http://localhost:8080`, with the API mounted under `/api`.

## Known Limitations / TODO

- The synthesis endpoint (`SynthesisHandler`) is not mounted on the router, and the Acapela engine registration / audio output are commented out, so no audio is actually produced.
- `persistence.xml` lists non-entity classes and has no JDBC URL or credentials, so the server cannot connect to a database as-is.
- A single shared `EntityManager` is used across threads, which is not safe under concurrent requests.
- The front-end pages (`admin-dashboard.html`, `api-tester.html`) target routes and a port that do not match the backend.

- The JWT signing secret is hardcoded in `VertxServer.java` — needs to move to configuration/environment variables before any real deployment.
- Some handler methods are marked in code comments as needing a rename for clarity (e.g. `deleteApiKey`, `getFileById`, `createCacheRequest`).
- No automated test coverage yet beyond the default `TestMainVerticle` scaffold.
- Database credentials and connection details are not externalized.