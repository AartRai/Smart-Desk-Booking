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
- **Spring Caching**: Read-heavy operations (like retrieving floor maps) are cached in-memory (`@Cacheable`), with cache eviction (`@CacheEvict`) upon modifications, drastically reducing database load.
- **Actuator**: Health, metrics, and cache statuses are exposed via Spring Boot Actuator endpoints on port `8081`.
- **Global Error Handling**: `@ControllerAdvice` provides clean, structured JSON error responses for data integrity violations, validation failures, and malformed requests.

## Getting Started

### Prerequisites
- JDK 17 installed
- Maven installed

### Running the Application
The application will start on `http://localhost:8081`. 

```bash
./mvnw clean install
./mvnw spring-boot:run
```

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
| `POST` | `/api/bookings` | Book a specific desk |
| `POST` | `/api/bookings/auto` | Auto-book a desk near teammates |
| `POST` | `/api/bookings/{id}/checkin` | Check into a booking |
| `GET` | `/actuator/health` | Check application health (Port 8081) |
