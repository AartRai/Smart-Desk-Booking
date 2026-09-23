# Demo Screenshot Instructions

Please use the following exact cURL commands (or import them into Postman) to run the application flows and take the screenshots required for submission. Save the screenshots in this `demo/` folder and name them exactly as listed below so they link correctly in the README.

*Note: Make sure your Spring Boot server is running on `http://localhost:8081` before executing these commands.*

### 0. Login & Get JWT
All API endpoints are secured via Spring Security and JWT. We will login as Employee 1 and dynamically save the token into a bash variable.
```bash
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
-H "Content-Type: application/json" \
-d '{"employeeId": 1}' | grep -o '"token":"[^"]*' | grep -o '[^"]*$')

echo "Logged in successfully! Token: $TOKEN"
```

### 1. Successful Booking (`demo/1_successful_booking.png`)
Run this command to simulate Employee 1 booking Desk 1 for tomorrow:
```bash
curl -i -X POST http://localhost:8081/api/bookings \
-H "Content-Type: application/json" \
-H "Authorization: Bearer $TOKEN" \
-d '{
    "deskId": 1,
    "bookingDate": "'$(date -v+1d +%Y-%m-%d 2>/dev/null || date -d "tomorrow" +%Y-%m-%d)'",
    "timeWindow": "FULL_DAY",
    "employeeId": 1
}'
```
*Screenshot the terminal output showing `201 Created` and the booking details.*

### 2. Successful Cancellation Before Cut-off (`demo/2_cancel_before_cutoff.png`)
Extract the booking ID you just created and cancel it:
```bash
# Assuming the booking ID was 1 (change if necessary based on the previous output)
curl -i -X DELETE http://localhost:8081/api/bookings/1 \
-H "Authorization: Bearer $TOKEN"
```
*Screenshot the terminal output showing the `204 No Content` HTTP status.*

### 3. Rejected Cancellation After Cut-off (`demo/3_cancel_after_cutoff.png`)
To trigger this, you need a booking for today.
```bash
# Book a desk for TODAY
NEW_BOOKING_ID=$(curl -s -X POST http://localhost:8081/api/bookings \
-H "Content-Type: application/json" \
-H "Authorization: Bearer $TOKEN" \
-d '{
    "deskId": 2,
    "bookingDate": "'$(date +%Y-%m-%d)'",
    "timeWindow": "FULL_DAY",
    "employeeId": 1
}' | jq -r .id)

# Try to cancel it (assuming your system clock is past 10:00 AM)
curl -i -X DELETE http://localhost:8081/api/bookings/$NEW_BOOKING_ID \
-H "Authorization: Bearer $TOKEN"
```
*Screenshot the `400 Bad Request` JSON error stating "Cannot cancel a booking after the 10:00 cutoff time".*

### 4. 409 Conflict (Double Booking Race) (`demo/4_double_booking_conflict.png`)
To simulate a real-time race condition, we will use `wait` to send two requests at the exact same millisecond.
```bash
# Get Bob's token (Employee 2)
TOKEN2=$(curl -s -X POST http://localhost:8081/api/auth/login -H "Content-Type: application/json" -d '{"employeeId": 2}' | grep -o '"token":"[^"]*' | grep -o '[^"]*$')

# Alice and Bob try to book Desk 4 at the EXACT same millisecond
curl -s -i -X POST http://localhost:8081/api/bookings \
-H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
-d '{"deskId": 4, "bookingDate": "2026-11-20", "timeWindow": "AFTERNOON", "employeeId": 1}' & \
curl -s -i -X POST http://localhost:8081/api/bookings \
-H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN2" \
-d '{"deskId": 4, "bookingDate": "2026-11-20", "timeWindow": "AFTERNOON", "employeeId": 2}' & \
wait
```
*Screenshot the terminal showing one `201 Created` and one `400 Bad Request` or `409 Conflict`.*

### 5. No-Show Auto Release (`demo/5_no_show_release.png`)
Instead of waiting 15 minutes for the cron job, we can manually trigger the newly added admin endpoint!
```bash
# Get Admin Token (Charlie - Employee 3)
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login -H "Content-Type: application/json" -d '{"employeeId": 3}' | grep -o '"token":"[^"]*' | grep -o '[^"]*$')

# Manually trigger the No-Show Release
curl -i -X POST http://localhost:8081/api/admin/bookings/release-no-shows \
-H "Authorization: Bearer $ADMIN_TOKEN"
```
*Screenshot the Spring Boot server log output or the `200 OK` response showing `No-show release job triggered successfully.`*
