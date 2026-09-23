#!/bin/bash

echo "Waiting for application to start..."
while ! curl -s http://localhost:8080/api/admin/desks > /dev/null 2>&1; do
    sleep 1
done
echo "Application started!"

SERVER_TODAY=$(date +%Y-%m-%d)
echo "Server Today is: $SERVER_TODAY"

echo -e "\n--- 1. Create a Floor in UTC-11 (Niue) ---"
curl -s -X POST http://localhost:8080/api/admin/floors \
  -H "Content-Type: application/json" \
  -d '{"name":"Niue Floor", "maxCapacity":10, "timezone":"Pacific/Niue"}' | jq

echo -e "\n--- 2. Create a Zone for Niue Floor ---"
curl -s -X POST http://localhost:8080/api/admin/zones \
  -H "Content-Type: application/json" \
  -d '{"name":"Niue Zone", "floorId":2}' | jq

echo -e "\n--- 3. Create a Desk on Niue Floor ---"
curl -s -X POST http://localhost:8080/api/admin/desks \
  -H "Content-Type: application/json" \
  -d '{"zoneId":2, "deskType":"HOT", "xCoordinate":1.0, "yCoordinate":1.0}' | jq

echo -e "\n--- 4. Create a Floor in UTC+14 (Kiritimati) ---"
curl -s -X POST http://localhost:8080/api/admin/floors \
  -H "Content-Type: application/json" \
  -d '{"name":"Kiritimati Floor", "maxCapacity":10, "timezone":"Pacific/Kiritimati"}' | jq

echo -e "\n--- 5. Create a Zone for Kiritimati Floor ---"
curl -s -X POST http://localhost:8080/api/admin/zones \
  -H "Content-Type: application/json" \
  -d '{"name":"Kiri Zone", "floorId":3}' | jq

echo -e "\n--- 6. Create a Desk on Kiritimati Floor ---"
curl -s -X POST http://localhost:8080/api/admin/desks \
  -H "Content-Type: application/json" \
  -d '{"zoneId":3, "deskType":"HOT", "xCoordinate":1.0, "yCoordinate":1.0}' | jq

echo -e "\n--- 7. Book Niue Desk for Server Today ---"
NIUE_BOOKING=$(curl -s -X POST http://localhost:8080/api/bookings \
  -H "X-Employee-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{
    "deskId": 3,
    "bookingDate": "'$SERVER_TODAY'",
    "timeWindow": "FULL_DAY"
  }')
echo $NIUE_BOOKING | jq
NIUE_BOOKING_ID=$(echo $NIUE_BOOKING | jq '.id')

echo -e "\n--- 8. Try to Check-In to Niue Booking ---"
curl -s -X POST http://localhost:8080/api/bookings/$NIUE_BOOKING_ID/checkin \
  -H "X-Employee-Id: 1" | jq

echo -e "\n--- 9. Book Kiritimati Desk for Server Today ---"
KIRI_BOOKING=$(curl -s -X POST http://localhost:8080/api/bookings \
  -H "X-Employee-Id: 2" \
  -H "Content-Type: application/json" \
  -d '{
    "deskId": 4,
    "bookingDate": "'$SERVER_TODAY'",
    "timeWindow": "FULL_DAY"
  }')
echo $KIRI_BOOKING | jq
KIRI_BOOKING_ID=$(echo $KIRI_BOOKING | jq '.id')

echo -e "\n--- 10. Try to Check-In to Kiritimati Booking ---"
curl -s -X POST http://localhost:8080/api/bookings/$KIRI_BOOKING_ID/checkin \
  -H "X-Employee-Id: 2" | jq

