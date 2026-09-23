# Demo Screenshot Instructions

Please use the following exact cURL commands (or import them into Postman) to run the application flows and take the screenshots required for submission. Save the screenshots in this `demo/` folder and name them exactly as listed below so they link correctly in the README.

*Note: Make sure your Spring Boot server is running on `http://localhost:8081` before executing these commands.*

### 0. Login & Get JWT
All API endpoints are now secured via Spring Security and JWT. First, login as Employee 1 to get your token:
```bash
curl -X POST http://localhost:8081/api/auth/login \
-H "Content-Type: application/json" \
-d '{
    "employeeId": 1
}'
```
*Copy the `token` from the JSON response and replace `<YOUR_TOKEN_HERE>` in the requests below.*

### 1. Successful Booking (`demo/1_successful_booking.png`)
Run this command to simulate Employee 1 booking Desk 1 for tomorrow:
```bash
curl -X POST http://localhost:8081/api/bookings \
-H "Content-Type: application/json" \
-H "Authorization: Bearer <YOUR_TOKEN_HERE>" \
-d '{
    "deskId": 1,
    "bookingDate": "'$(date -v+1d +%Y-%m-%d 2>/dev/null || date -d "tomorrow" +%Y-%m-%d)'",
    "timeWindow": "FULL_DAY"
}'
```
*Screenshot the JSON response showing a 201 Created and the booking details.*

### 2. Successful Cancellation Before Cut-off (`demo/2_cancel_before_cutoff.png`)
Using the ID returned from the first booking (e.g., `1`), cancel it:
```bash
curl -X DELETE http://localhost:8081/api/bookings/1 \
-H "Authorization: Bearer <YOUR_TOKEN_HERE>" -v
```
*Screenshot the terminal output or Postman window showing the `204 No Content` HTTP status.*

### 3. Rejected Cancellation After Cut-off (`demo/3_cancel_after_cutoff.png`)
To trigger this, you need a booking for today (assuming the time is past 10:00 AM). 
First, create it (we bypass some validations in the DB directly if needed, but for the demo, just pass today's date):
```bash
curl -X POST http://localhost:8081/api/bookings \
-H "Content-Type: application/json" \
-H "Authorization: Bearer <YOUR_TOKEN_HERE>" \
-d '{
    "deskId": 2,
    "bookingDate": "'$(date +%Y-%m-%d)'",
    "timeWindow": "FULL_DAY"
}'
```
Then try to cancel it (assuming your system clock is currently past 10:00 AM):
```bash
curl -X DELETE http://localhost:8081/api/bookings/2 \
-H "Authorization: Bearer <YOUR_TOKEN_HERE>"
```
*Screenshot the `400 Bad Request` JSON error stating "Cannot cancel a booking after the 10:00 cutoff time".*

### 4. 409 Conflict (Double Booking Race) (`demo/4_double_booking_conflict.png`)
To simulate this quickly without a multi-threading script, try to book the exact same desk/day/time with a DIFFERENT employee (Employee 2). First login as Employee 2 to get their token:
```bash
curl -X POST http://localhost:8081/api/auth/login \
-H "Content-Type: application/json" \
-d '{
    "employeeId": 2
}'
```
Then execute the booking (using the token):
```bash
curl -X POST http://localhost:8081/api/bookings \
-H "Content-Type: application/json" \
-H "Authorization: Bearer <EMP2_TOKEN_HERE>" \
-d '{
    "deskId": 3,
    "bookingDate": "'$(date -v+2d +%Y-%m-%d 2>/dev/null || date -d "+2 days" +%Y-%m-%d)'",
    "timeWindow": "FULL_DAY"
}'
```
(Run it once for Employee 2. Then login as Employee 3, get their token, and run it AGAIN for Employee 3 for the exact same desk/date).
```bash
curl -X POST http://localhost:8081/api/bookings \
-H "Content-Type: application/json" \
-H "Authorization: Bearer <EMP3_TOKEN_HERE>" \
-d '{
    "deskId": 3,
    "bookingDate": "'$(date -v+2d +%Y-%m-%d 2>/dev/null || date -d "+2 days" +%Y-%m-%d)'",
    "timeWindow": "FULL_DAY"
}'
```
*Screenshot the `400 Bad Request` or `409 Conflict` error stating "Booking failed due to a conflict."*

### 5. No-Show Auto Release (`demo/5_no_show_release.png`)
To test this, look at the server logs. Wait for the `NoShowReleaseService` cron job to run at the 15-minute mark (e.g., xx:00, xx:15), or trigger it via your IDE debugger.
*Screenshot the Spring Boot server log output showing `Running No-Show Auto-Release Job` and `Successfully released X no-show bookings globally.`*

