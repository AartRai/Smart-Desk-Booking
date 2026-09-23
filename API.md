# Smart Desk Booking - API Demo Guide

Use these `curl` commands to easily demonstrate the API functionality for your video. The scripts are designed to dynamically extract and save the JWT authentication tokens, so you don't need to manually copy and paste them.

---

### 1. Login & Save Token (Authentication)
*Demonstrates: Secure JWT Authentication (Requirement 2.b)*

```bash
# Log in as Alice (Employee 1) and save the token dynamically
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
-H "Content-Type: application/json" \
-d '{"employeeId": 1}' | grep -o '"token":"[^"]*' | grep -o '[^"]*$')

echo "Logged in successfully! Token: $TOKEN"
```

---

### 2. View All Desks (Search)
*Demonstrates: Admin fetching available desks*

```bash
# Log in as Charlie (Admin, Employee ID 3) to get an Admin Token
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
-H "Content-Type: application/json" \
-d '{"employeeId": 3}' | grep -o '"token":"[^"]*' | grep -o '[^"]*$')

# View all 10 desks on the floor
curl -s -X GET http://localhost:8081/api/admin/desks \
-H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

---

### 3. Book a Hot Desk (Core Booking Flow)
*Demonstrates: Creating a booking*

```bash
# Alice books Desk 4 for tomorrow morning
curl -i -X POST http://localhost:8081/api/bookings \
-H "Content-Type: application/json" \
-H "Authorization: Bearer $TOKEN" \
-d '{"deskId": 4, "bookingDate": "2026-10-15", "timeWindow": "MORNING", "employeeId": 1}'
```

---

### 4. Advanced Placement (Suggest Desk Near Colleague)
*Demonstrates: Spatial Hashing / Grid Neighborhood Search (Requirement V)*

```bash
# Bob wants to sit near Alice (Employee 1). The system should suggest a desk physically close to Desk 1!
curl -s -X GET "http://localhost:8081/api/placement/suggest?colleagueId=1&date=2026-10-15&timeWindow=MORNING" \
-H "Authorization: Bearer $TOKEN" | jq .
```

---

### 5. Concurrency (The Double Booking Race Condition)
*Demonstrates: Handling system failures/race conditions mathematically (Requirement IV)*

```bash
# 1. Get Bob's token
TOKEN2=$(curl -s -X POST http://localhost:8081/api/auth/login \
-H "Content-Type: application/json" \
-d '{"employeeId": 2}' | grep -o '"token":"[^"]*' | grep -o '[^"]*$')

# 2. Alice and Bob try to book Desk 4 at the EXACT same millisecond
curl -s -i -X POST http://localhost:8081/api/bookings \
-H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
-d '{"deskId": 4, "bookingDate": "2026-11-20", "timeWindow": "AFTERNOON", "employeeId": 1}' & \
curl -s -i -X POST http://localhost:8081/api/bookings \
-H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN2" \
-d '{"deskId": 4, "bookingDate": "2026-11-20", "timeWindow": "AFTERNOON", "employeeId": 2}' & \
wait
```
*(One will output `201 Created`, the other will output `400 Bad Request` with a conflict error).*

---

### 6. Cancel Booking (Illegal Cancellation / Cut-off test)
*Demonstrates: 24-hour Cut-off logic (Requirement 1)*

```bash
# Alice books a desk for TOMORROW (less than 24 hours away)
NEW_BOOKING_ID=$(curl -s -X POST http://localhost:8081/api/bookings \
-H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
-d '{"deskId": 5, "bookingDate": "2026-09-24", "timeWindow": "MORNING", "employeeId": 1}' | jq -r .id)

# Alice immediately tries to cancel it, but it violates the 24-hour rule!
curl -i -X DELETE http://localhost:8081/api/bookings/$NEW_BOOKING_ID \
-H "Authorization: Bearer $TOKEN"
```

---

### 7. Trigger No-Show Release (Fault Tolerance & Cron)
*Demonstrates: Cron job execution & freeing up team quotas (Requirement III)*

```bash
# Admin manually triggers the No-Show cron job (simulating end of day)
curl -i -X POST http://localhost:8081/api/admin/bookings/release-no-shows \
-H "Authorization: Bearer $ADMIN_TOKEN"
```

---

### 8. System Monitoring (Prometheus Metrics)
*Demonstrates: Monitoring integration (Requirement 3)*

```bash
# Fetch live server metrics
curl -s -X GET http://localhost:8081/actuator/prometheus | head -n 20
```
