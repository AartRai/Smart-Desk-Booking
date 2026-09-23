# Smart Desk Booking System

A robust, enterprise-grade hot-desking and office management REST API built with Java and Spring Boot. 

This system manages office floors, zones, and desks, allowing employees to book hot desks dynamically. It goes beyond simple CRUD by incorporating advanced logic like spatial hashing for colleague proximity, database-level concurrency control for double-booking prevention, global timezone awareness, and automated no-show cancellations.

## Tech Stack
- **Java 17**
- **Spring Boot 3.x**
- **Spring Data JPA & Hibernate**
- **H2 Database** (In-memory, easily swappable for PostgreSQL/MySQL)
- **Maven**
- **Lombok**

## Core Features

### 1. Concurrency-Safe Booking
Prevents double-booking race conditions utilizing a database-level unique constraint (`UK558P4I6KSN6MJ3PKSO848YHGG`) on the `bookings` table (`desk_id`, `booking_date`, `time_window`). This ensures that even under heavy concurrent load, a single desk cannot be booked by multiple people at the same time.

### 2. Smart Proximity Placement (Spatial Hashing)
When an employee requests an automatic desk assignment, the `PlacementService` finds an available desk near their teammates. It achieves this efficiently by building an in-memory Spatial Index using a 2D grid overlay (Spatial Hashing). It scans the grid cells for teammates and spirals outward if necessary, ensuring fast, O(1) cell lookups without expensive bounding box SQL queries.

### 3. Team Quota Management
The `QuotaService` strictly enforces maximum capacity limits for specific teams on specific floors, preventing large teams from monopolizing premium floor space.

### 4. Global Timezone Support
The system operates seamlessly across multiple global regions. The `Floor` entity defines a specific localized timezone (e.g., `Asia/Tokyo`, `America/New_York`). All check-ins and cron jobs evaluate dates and cut-off times relative to the specific floor's local time, completely independently of the centralized server's system time.

### 5. Automated No-Show Release
A Spring `@Scheduled` cron job routinely sweeps the database for unchecked-in bookings that have surpassed their local 10:00 AM cutoff time, automatically cancelling them and freeing up inventory for other employees.

### 6. Caching & Monitoring
- **Real-Time Dashboards (Prometheus & Grafana)**: The `docker-compose.yml` provides a full observability stack. Prometheus scrapes JVM, HTTP, and DB metrics exposed by Micrometer on `/actuator/prometheus`. Grafana (`http://localhost:3000`, admin/admin) provides real-time dashboards to track system health and active bookings.
- **Advanced Logging Mechanisms (Logback)**: Configured via `logback-spring.xml` to use a `RollingFileAppender`. In addition to the console, logs are written to `logs/booking-app.log` with daily rotation, providing comprehensive error recovery and forensic logging mechanisms to promptly identify issues.
- **Spring Caching**: Read-heavy operations (like retrieving floor maps) are cached in-memory (`@Cacheable`), with cache eviction (`@CacheEvict`) upon modifications, drastically reducing database load.
- **Global Error Handling**: `@ControllerAdvice` provides clean, structured JSON error responses for data integrity violations, validation failures, and malformed requests.

## Getting Started

### Prerequisites
- JDK 17 installed
- Maven installed

### Running the Application
The application is configured to run against a real PostgreSQL database for production realism.

1. Start the PostgreSQL database using Docker Compose:
```bash
docker compose up -d
```

2. Run the application with the `postgres` profile active:
```bash
SPRING_PROFILES_ACTIVE=postgres ./mvnw spring-boot:run
```
*(The application will start on `http://localhost:8081`)*

An H2 database is automatically spun up and seeded with initial data (employees, floors, zones, desks) from `src/main/resources/data.sql`.

### Running Tests
The project contains 22 comprehensive unit and integration tests covering concurrency limits, spatial indexing, quota validation, and timezone-aware scheduling.

```bash
./mvnw test
```

## API Endpoints Overview

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/admin/floors` | Create a new Floor |
| `POST` | `/api/admin/zones` | Create a new Zone |
| `POST` | `/api/admin/desks` | Create a new Desk |
| `POST` | `/api/bookings` | [Book a specific desk](#1-successful-booking) |
| `POST` | `/api/bookings/auto` | Auto-book a desk near teammates |
| `POST` | `/api/bookings/{id}/checkin` | Check into a booking |
| `GET` | `/actuator/health` | Check application health (Port 8081) |

## Assumptions
- **Cut-off Times:** The system universally assumes a localized `10:00 AM` cut-off for both check-ins and cancellations.
- **Auto-Release Polling:** The no-show release cron job runs every 15 minutes, meaning a desk abandoned at `10:00` might theoretically not become available until `10:15` depending on the cron cycle.
- **Team Structure:** An employee belongs to at most one team (`employee.getTeam()`), simplifying the quota and neighborhood calculations.
- **Spatial Grid Size:** The internal spatial index assumes a fixed grid cell size of `10x10` coordinate units for clustering teammate desks.

## Trade-offs
- **Concurrency Strategy (DB Constraint vs Locking):** We use a database-level unique constraint (`UK558P4I6KSN6MJ3PKSO848YHGG`) instead of application-level pessimistic locking (`SELECT ... FOR UPDATE`). This avoids long-lived database locks and deadlocks during high-traffic 9:00 AM booking rushes, efficiently delegating the race condition resolution to the RDBMS index which simply rejects the loser.
- **Placement Algorithm (Grid-Hashing vs PostGIS/K-D Tree):** We use an in-memory grid-hashing (Spatial Index) instead of a k-d tree or complex PostGIS spatial queries. Given that floor plans rarely change mid-day and have a bounded size (e.g. 500 desks), an in-memory `ConcurrentHashMap` of pre-computed cell grid buckets provides instantaneous neighbor lookups without paying the heavy I/O overhead of executing bounding-box SQL queries on every auto-book attempt.
- **Caching Choice (In-Process vs Redis):** We utilize in-process Spring caching (`ConcurrentMapCacheManager`) instead of a distributed cache like Redis. Since the cached data consists solely of static layout entities (Floors, Zones) and this is designed as a single-node deployment, in-memory caching is dramatically simpler to operate, requires fewer moving parts, and eliminates network latency entirely.
- **Cut-off Boundary Decision:** The cut-off time boundary is strictly exclusive (i.e. exact equality to 10:00:00 counts as "too late"). This guarantees that edge-case user cancellations exactly at the boundary moment are predictably denied, ensuring the background release job can safely free up the desk without race conditions against last-millisecond user actions.
- **Database Split (H2 vs. Postgres):** We keep an embedded H2 database as the default for fast, zero-dependency unit testing, but use a real Dockerized PostgreSQL instance for the runtime application and concurrency integration tests to accurately reflect how unique constraints and locking behave in a real production RDBMS.

## Cost Estimation (Time & Space Complexity)
- **Booking Write Path:** **Time: O(1) / Space: O(1).** The actual booking insertion is an O(1) B-Tree index lookup/insertion for the unique constraint. Space is strictly O(1) per request to store the entity.
- **Neighbour-Placement Lookup:** **Time: O(1) / Space: O(D).** The spatial hashing algorithm computes the mathematical cell key directly from X/Y coordinates in constant time, and performs a hash map lookup for the center cell and its 8 immediate neighbors. It completely avoids an O(N²) pairwise comparison. Space scales linearly O(D) with the total number of desks D on the floor.
- **Quota Check:** **Time: O(1) / Space: O(1).** The quota enforcement relies on a highly optimized database `COUNT` query utilizing composite indexes on `(team_id, floor_id, date)`. This provides near constant-time validation with O(1) memory overhead in the application server.

## Demo
Please refer to the `demo/README.md` file for exact cURL requests to replicate these scenarios.
*Screenshots below demonstrate the core requirements being met:*

### 1. Successful Booking
![Successful Booking](demo/1_successful_booking.png)

### 4. 409 Conflict (Double Booking Race)
![Double Booking Conflict](demo/4_double_booking_conflict.png)

### 5. No-Show Auto Release
![No-Show Release Log](demo/5_no_show_release.png)
