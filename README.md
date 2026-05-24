# Billiard Club — System Documentation

Spring Boot web application for managing a billiard club: clients, table bookings, game sessions, tournament records, and payments.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 17, Spring Boot 3.2.3 |
| Security | Spring Security (form login, CSRF cookie) |
| Persistence | PostgreSQL 15, Spring Data JPA, Hibernate |
| Search | OpenSearch 2.17.0 (optional, graceful degradation) |
| Templates | Thymeleaf + REST API (hybrid) |
| Build | Gradle 8.8 |
| Container | Docker, docker-compose |
| Kubernetes | Rancher Desktop (k3s), kubectl |
| Tests | JUnit 5, Mockito, Spring WebMvcTest |
| Monitoring | Spring Boot Actuator (`/actuator/health`) |

---

## Functional Modules

### Clients (`/clients`)
- List, add, delete clients (rank: Amateur / Professional)
- Indexed in OpenSearch for full-text search

### Tables (`/tables`)
- Billiard tables: Russian / Pool / Snooker
- Hourly pricing per table type
- CRUD (add/edit/delete — ADMIN only)

### Bookings (`/bookings` + REST `/api/bookings`)
- Create booking (client + table + time range)
- Status flow: `PENDING → ACTIVE → COMPLETED / CANCELLED`
- Payment recording: `CASH / CARD`
- Price calculator: `GET /api/bookings/price?tableId=&startTime=&endTime=`

### Games (`/games`)
- Game session started from booking (ACTIVE status)
- Tracks opponent

### Tournament (`/tournament` + REST `/api/tournament`)
- Records of matches: winner, loser, table, date
- Edit / delete — ADMIN only

### Search (`/api/search/clients`)
- Full-text search by client name, rank, phone
- Powered by OpenSearch; falls back to empty result when disabled

---

## REST API

### Base URL: `/api`

#### Bookings

| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| `GET` | `/api/bookings` | Any | List all bookings |
| `POST` | `/api/bookings` | Any | Create booking → 201 + Location |
| `PATCH` | `/api/bookings/{id}` | Any | Change status (CANCELLED / ACTIVE / COMPLETED) |
| `POST` | `/api/bookings/{id}/payments` | Any | Record payment → 201 |
| `GET` | `/api/bookings/price` | Any | Calculate price |

**PATCH body examples:**
```json
{ "status": "CANCELLED" }
{ "status": "ACTIVE", "opponentId": 2 }
{ "status": "COMPLETED", "winnerId": 1 }
```

#### Tournament

| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| `GET` | `/api/tournament` | Any | List all records |
| `GET` | `/api/tournament/{id}` | Any | Get record by id |
| `PUT` | `/api/tournament/{id}` | ADMIN | Update winner/loser names → 200 |
| `DELETE` | `/api/tournament/{id}` | ADMIN | Delete record → 204 |

#### Search (public)

| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| `GET` | `/api/search/clients` | None | Full-text client search |

Query params: `q`, `rank`, `page` (default 0), `size` (default 20)

Response when OpenSearch is disabled:
```json
{ "opensearchEnabled": false, "results": [] }
```

---

## Security

- Form-based login at `/login` (session cookie)
- CSRF: cookie-based (`XSRF-TOKEN`) — JS reads cookie, sends `X-XSRF-TOKEN` header
- `/api/search/**` — CSRF disabled (public read-only)
- `/actuator/health` — `permitAll()` (required for K8s health probes)
- `/api/**` unauthenticated → **401** (not 302 redirect — custom entry point configured)

### Roles

| Role | Permissions |
|------|------------|
| `RECEPTION` | All read + bookings + payments |
| `ADMIN` | Everything + table/client management + tournament edit/delete |

---

## Running Locally

### Prerequisites
- JDK 17
- Docker Desktop or Rancher Desktop

### Option 1: Docker Compose

```bash
# Start PostgreSQL + OpenSearch + App
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop
docker-compose down
```

App available at: **http://localhost:7070**

Default users (seeded by DataInitializer):
- `admin` / `admin` (role ADMIN)
- `reception` / `reception` (role RECEPTION)

### Option 2: IDE (local dev)

```bash
# Start only PostgreSQL (OpenSearch optional)
docker-compose up -d postgres

# Run Spring Boot
./gradlew bootRun
```

Set `opensearch.enabled=false` in `application.properties` to run without OpenSearch.

### Option 3: Rancher Desktop (Kubernetes)

```powershell
# First deploy — build JAR + Docker image + load into VM + apply manifests
.\rancher\build-and-load.ps1
kubectl apply -f rancher/k8s/

# Subsequent updates
.\rancher\build-and-load.ps1 -Restart

# Check status
kubectl get all -n billiard-club

# View logs
kubectl logs -n billiard-club deployment/billiard-app -f
```

App available at: **http://localhost:30707**

---

## Build & Test

```bash
# Run all tests (18 tests)
./gradlew test

# Full build (compile + test + JAR)
./gradlew build

# JAR only (skip tests)
./gradlew bootJar -x test
```

Test configuration: `src/test/resources/application-test.properties`
- H2 in-memory database
- `opensearch.enabled=false` — no OpenSearch needed

---

## Project Structure

```
src/main/java/com/billiardclub/
├── config/
│   ├── DataInitializer.java       # Seed data (@Order(1))
│   ├── OpenSearchConfig.java      # OpenSearch client bean (optional)
│   ├── SearchInitializer.java     # Reindex on startup (@Order(2))
│   └── SecurityConfig.java        # Spring Security rules
├── controller/
│   ├── BookingApiController.java  # REST /api/bookings
│   ├── BookingController.java     # UI /bookings
│   ├── ClientController.java      # UI /clients
│   ├── DashboardController.java   # UI /dashboard
│   ├── GameController.java        # UI /games
│   ├── LoginController.java       # /login
│   ├── SearchController.java      # REST /api/search/clients
│   ├── TableController.java       # UI /tables
│   ├── TournamentApiController.java # REST /api/tournament
│   └── TournamentController.java  # UI /tournament
├── dto/
│   ├── BookingPatchDto.java        # PATCH /api/bookings/{id}
│   ├── BookingRequestDto.java      # POST /api/bookings
│   ├── BookingResponseDto.java     # Response body
│   ├── TournamentResponseDto.java  # GET /api/tournament
│   └── TournamentUpdateDto.java    # PUT /api/tournament/{id}
├── exception/
│   ├── BusinessException.java
│   └── GlobalExceptionHandler.java
├── model/
│   ├── BilliardTable.java
│   ├── Booking.java
│   ├── BookingStatus.java
│   ├── Client.java
│   ├── Game.java
│   ├── TableType.java
│   ├── TournamentRecord.java
│   └── User.java
├── repository/
├── search/
│   ├── ClientDocument.java         # OpenSearch document
│   ├── ClientSearchService.java    # Interface
│   └── ClientSearchServiceImpl.java # Null-guard pattern B
└── service/
    ├── BookingService.java
    ├── ClientService.java          # Calls indexClient() on save/delete
    ├── GameService.java
    ├── TableService.java
    ├── TournamentService.java
    └── UserService.java

src/main/resources/
├── application.properties
├── static/js/api.js                # CSRF-aware fetch utility
└── templates/
    ├── bookings/
    ├── clients/
    ├── dashboard.html
    ├── error.html
    ├── login.html
    ├── tables/
    └── tournament/

src/test/java/com/billiardclub/controller/
├── BookingApiControllerTest.java   # 10 tests
├── SearchControllerTest.java       # 2 tests
└── TournamentApiControllerTest.java # 6 tests

rancher/
├── build-and-load.ps1              # Build JAR → Docker → load to VM
└── k8s/
    ├── 00-namespace.yaml
    ├── 01-secrets.yaml
    ├── 02-postgres.yaml
    ├── 06-app.yaml                 # ConfigMap + Deployment + NodePort 30707
    └── 08-opensearch.yaml          # sysctl initContainer + Deployment
```

---

## OpenSearch Integration

Optional component — app works without it (`opensearch.enabled=false`).

**Index:** `clients`  
**Fields:** `fullName` (boosted ×2), `rank`, `phone`  
**Graceful degradation:** `ClientSearchServiceImpl` checks `client == null` at the start of every method — no exception when disabled.

### Enable/Disable

`application.properties`:
```properties
opensearch.enabled=true
opensearch.url=${OPENSEARCH_URL:http://localhost:9200}
```

`application-test.properties`:
```properties
opensearch.enabled=false
```

---

## Kubernetes Architecture

```
billiard-club namespace
├── postgres (ClusterIP :5432, PVC 1Gi)
├── opensearch (ClusterIP :9200, PVC 2Gi)
│     └── initContainer: sysctl vm.max_map_count=262144 (privileged)
└── billiard-app (NodePort 30707)
      ├── initContainer: wait-for-postgres (nc -z postgres 5432)
      └── initContainer: wait-for-opensearch (nc -z opensearch 9200)
```

**Image loading:** `build-and-load.ps1` builds the JAR locally with Gradle, packages it into a Docker image, saves to `.tar`, and loads into the Rancher Desktop VM via `rdctl shell`.

`imagePullPolicy: Never` — billiard-app (loaded manually)  
`imagePullPolicy: IfNotPresent` — postgres, opensearch (public images)

---

## Configuration Reference

| Property | Default | Override (env) |
|----------|---------|----------------|
| `server.port` | `7070` | `SERVER_PORT` |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/billiard_club` | `SPRING_DATASOURCE_URL` |
| `opensearch.enabled` | `true` | `OPENSEARCH_ENABLED` |
| `opensearch.url` | `http://localhost:9200` | `OPENSEARCH_URL` |
| `management.endpoint.health.show-details` | `when-authorized` | — |

---

## Frontend (JavaScript)

`static/js/api.js` — CSRF-aware fetch utility used by all Thymeleaf pages.

```javascript
// Reads XSRF-TOKEN cookie, sends X-XSRF-TOKEN header on mutating requests
apiRequest(url, 'PATCH', { status: 'CANCELLED' })
```

Thymeleaf pages render data via GET (server-side rendering).  
All mutations (cancel booking, pay, start game, delete record, update tournament) use `fetch()` against the REST API — no form submissions for write operations.
