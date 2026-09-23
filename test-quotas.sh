#!/bin/bash

echo "Waiting for application to start..."
while ! curl -s http://localhost:8080/api/floors > /dev/null 2>&1; do
    sleep 1
done
echo "Application started!"

echo -e "\n--- 1. Set Team Quota (Max: 1 desk on Floor 1 for Team 1) ---"
curl -s -X POST http://localhost:8080/api/admin/quotas \
  -H "Content-Type: application/json" \
  -d '{"teamId":1, "floorId":1, "maxDesks":1}' | jq

echo -e "\n--- 2. Create another HOT desk for Bob (Desk 3) ---"
curl -s -X POST http://localhost:8080/api/desks \
  -H "Content-Type: application/json" \
  -d '{"zoneId":1, "deskType":"HOT", "xCoordinate":30.0, "yCoordinate":30.0}' | jq

echo -e "\n--- 3. First Booking (Employee 1, Desk 1) - Should Succeed ---"
curl -s -X POST http://localhost:8080/api/bookings \
  -H "X-Employee-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{
    "deskId": 1,
    "bookingDate": "2026-10-01",
    "timeWindow": "FULL_DAY"
  }' | jq

echo -e "\n--- 4. Second Booking (Employee 2 in same team, Desk 3) - Should Fail due to Quota ---"
curl -s -X POST http://localhost:8080/api/bookings \
  -H "X-Employee-Id: 2" \
  -H "Content-Type: application/json" \
  -d '{
    "deskId": 3,
    "bookingDate": "2026-10-01",
    "timeWindow": "FULL_DAY"
  }' | jq

echo -e "\n--- 5. Suggest Desk for Employee 2 - Should Fail due to Quota ---"
curl -s -X GET "http://localhost:8080/api/placements/suggest?floorId=1&date=2026-10-01&timeWindow=FULL_DAY" \
  -H "X-Employee-Id: 2" | jq
