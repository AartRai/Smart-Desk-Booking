#!/bin/bash

echo "Waiting for application to start..."
while ! curl -s http://localhost:8080/api/desks > /dev/null 2>&1; do
    sleep 1
done
echo "Application started!"

TODAY=$(date +%Y-%m-%d)
TOMORROW=$(date -v+1d +%Y-%m-%d)

echo -e "\n--- 1. Book a Desk for Today (Employee 1, Desk 1) ---"
BOOKING_TODAY_JSON=$(curl -s -X POST http://localhost:8080/api/bookings \
  -H "X-Employee-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{
    "deskId": 1,
    "bookingDate": "'$TODAY'",
    "timeWindow": "FULL_DAY"
  }')

echo $BOOKING_TODAY_JSON | jq
BOOKING_ID=$(echo $BOOKING_TODAY_JSON | jq '.id')

echo -e "\n--- 2. Check-In to Today's Booking (Booking ID: $BOOKING_ID) ---"
curl -s -X POST http://localhost:8080/api/bookings/$BOOKING_ID/checkin \
  -H "X-Employee-Id: 1" | jq

echo -e "\n--- 3. Book a Desk for Tomorrow (Employee 2, Desk 1) ---"
BOOKING_TOMORROW_JSON=$(curl -s -X POST http://localhost:8080/api/bookings \
  -H "X-Employee-Id: 2" \
  -H "Content-Type: application/json" \
  -d '{
    "deskId": 1,
    "bookingDate": "'$TOMORROW'",
    "timeWindow": "FULL_DAY"
  }')

echo $BOOKING_TOMORROW_JSON | jq
BOOKING_TOMORROW_ID=$(echo $BOOKING_TOMORROW_JSON | jq '.id')

echo -e "\n--- 4. Try to Check-In to Tomorrow's Booking (Should Fail) ---"
curl -s -X POST http://localhost:8080/api/bookings/$BOOKING_TOMORROW_ID/checkin \
  -H "X-Employee-Id: 2" | jq
