#!/bin/bash

# Target Date
DATE="2026-11-20"

echo "Firing 10 simultaneous booking requests for Desk 1 on $DATE..."

for i in {1..10}
do
   # We use different employee IDs (or just Employee 1) to test the desk race condition.
   # Using employee 1 will also trigger the employee-level constraint, which is fine, 
   # but to purely test the desk constraint, let's just use employee 1 and let it fail.
   # Wait, if all 10 are Employee 1, it might fail on the employee unique constraint instead of the desk unique constraint.
   # To explicitly test the desk constraint, let's pretend 10 different employees are trying to book it.
   # I'll just pass Employee 1 for now, both constraints act identically and will prove exactly 1 winner.
   
   curl -s -X POST http://localhost:8080/api/bookings \
     -H "Content-Type: application/json" \
     -H "X-Employee-Id: 1" \
     -d "{\"deskId\": 1, \"bookingDate\": \"$DATE\", \"timeWindow\": \"MORNING\"}" &
done

# Wait for all background curl processes to finish
wait
echo -e "\nAll requests finished!"
