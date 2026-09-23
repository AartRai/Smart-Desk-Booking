#!/bin/bash

echo "Starting application..."
./mvnw spring-boot:run > app.log 2>&1 &
APP_PID=$!

echo "Waiting for application to start on 8081..."
while ! curl -s http://localhost:8081/api/admin/desks > /dev/null 2>&1; do
    sleep 1
done
echo "Application started!"

echo -e "\n--- 1. Testing Actuator Health ---"
curl -s http://localhost:8081/actuator/health | jq

echo -e "\n--- 2. Testing Actuator Caches ---"
curl -s http://localhost:8081/actuator/caches | jq

echo -e "\n--- 3. Testing Validation Error (MethodArgumentNotValid) ---"
curl -s -X POST http://localhost:8081/api/admin/floors \
  -H "Content-Type: application/json" \
  -d '{"name":"", "maxCapacity":10}' | jq

echo -e "\n--- 4. Testing Malformed JSON (HttpMessageNotReadable) ---"
curl -s -X POST http://localhost:8081/api/admin/floors \
  -H "Content-Type: application/json" \
  -d '{"name":"Bad JSON", "maxCapacity":10' | jq

echo -e "\n--- 5. Testing Data Integrity Violation (Duplicate Name) ---"
curl -s -X POST http://localhost:8081/api/admin/floors \
  -H "Content-Type: application/json" \
  -d '{"name":"Level 1", "maxCapacity":10}' | jq

echo -e "\nCleaning up..."
kill $APP_PID
wait $APP_PID 2>/dev/null
echo "Done."
