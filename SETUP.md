# Smart Desk Booking - Local Setup Guide

Follow these instructions to run the Smart Desk Booking system on your local machine.

## Prerequisites

Before you begin, ensure you have the following installed on your system:
1. **Java 17** (or higher)
2. **Docker Desktop** (Required for the Monitoring Stack: Prometheus & Grafana)
3. **Postman** (For testing the API endpoints)

## 1. Clone the Repository

To get started, clone the repository to your local machine and navigate into the project directory:

```bash
git clone https://github.com/AartRai/Smart-Desk-Booking.git
cd Smart-Desk-Booking
```

---

## 2. Start the Monitoring Stack (Optional but Recommended)

The project includes an observability stack to monitor application health, metrics, and API latency.

1. Open a terminal in the project root directory.
2. Run the following command to start Prometheus and Grafana in the background:
   ```bash
   docker-compose up -d
   ```
3. **Verify:**
   - Prometheus is running at: [http://localhost:9090](http://localhost:9090)
   - Grafana is running at: [http://localhost:3000](http://localhost:3000) *(Default login: `admin` / `admin`)*

---

## 3. Run the Spring Boot Application

The application uses an in-memory H2 database by default, meaning no external database setup is required. The database is automatically seeded with initial data (Desks, Floors, Zones, Employees) on startup.

1. Open a new terminal window in the project root.
2. Run the application using the Maven wrapper:
   ```bash
   ./mvnw spring-boot:run
   ```
3. The server will start on port **8081**.
   - You should see `Tomcat started on port 8081` in the console logs.

---

## 4. Test the APIs via Postman

To test the application, a complete Postman collection is included in the project.

1. Open **Postman**.
2. Click **Import** in the top-left corner.
3. Select and upload the `SmartDeskBooking_Final_Evaluation.postman_collection.json` file located in the root of this project.
4. **Important:** Run the requests in the `1. Authentication` folder first. The collection has an automated script that extracts the JWT token from the login response and saves it as a variable for all subsequent requests. You do not need to manually copy-paste tokens!

---

## 5. Access the Database Console

If you want to view the raw database tables, you can use the built-in H2 Console.

1. Open your browser and navigate to: [http://localhost:8081/h2-console](http://localhost:8081/h2-console)
2. **Login Credentials:**
   - **JDBC URL:** `jdbc:h2:mem:bookingdb`
   - **Username:** `sa`
   - **Password:** *(leave blank)*
3. Click **Connect** to view the `DESKS`, `BOOKINGS`, `EMPLOYEES`, etc.

---

## Shutting Down

To stop the Spring Boot server, press `Ctrl + C` in the terminal.
To stop the monitoring stack, run:
```bash
docker-compose down
```
