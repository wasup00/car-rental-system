# Car Rental System

A REST API for managing car rentals built with Spring Boot and SQLite.

## Tech Stack

- **Java 21** with **Spring Boot 4.0.2**
- **SQLite** for persistence (via Hibernate community dialect)
- **Spring Data JPA** for data access
- **Lombok** for boilerplate reduction
- **Maven** for build management

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.6+ (or use the included Maven wrapper)

### Run the Application

```bash
./mvnw spring-boot:run
```

The server starts on **http://localhost:8081**.

### Run Tests

```bash
./mvnw test
```

## Initial Data

On first startup, the system creates a fleet of 10 cars:

| Type  | Count | License Plates        |
|-------|-------|-----------------------|
| SEDAN | 5     | SEDAN-001 to SEDAN-005 |
| SUV   | 3     | SUV-001 to SUV-003     |
| VAN   | 2     | VAN-001 to VAN-002     |

## API Endpoints

### Cars

#### Get all cars

```
GET /cars
```

Returns a list of all cars in the system.

#### Check availability

```
GET /cars/availability?type={carType}&startDate={dateTime}&days={number}
```

**Parameters:**

| Name        | Type            | Description                          |
|-------------|-----------------|--------------------------------------|
| `type`      | `CarType`       | `SEDAN`, `SUV`, or `VAN`            |
| `startDate` | `LocalDateTime` | Start date/time (e.g. `2025-07-01T10:00:00`) |
| `days`      | `int`           | Number of rental days               |

**Response:**

```json
{
  "available": 3,
  "total": 5
}
```

### Reservations

#### Create a reservation

```
POST /reservations
```

**Request body:**

```json
{
  "carType": "SEDAN",
  "customerName": "John Doe",
  "startDateTime": "2025-07-01T10:00:00",
  "numberOfDays": 3
}
```

**Response** (`201 Created`):

```json
{
  "id": "uuid",
  "carId": "uuid",
  "licensePlate": "SEDAN-001",
  "carType": "SEDAN",
  "customerName": "John Doe",
  "startDateTime": "2025-07-01T10:00:00",
  "numberOfDays": 3
}
```

Returns `409 Conflict` if no cars of the requested type are available for the given dates.

#### Get all reservations

```
GET /reservations
```

Returns a list of all reservations.

#### Cancel a reservation

```
DELETE /reservations/{id}
```

Returns `204 No Content` on success, `404 Not Found` if the reservation doesn't exist.

## Project Structure

```
src/main/java/com/wasup/car_rental_system/
├── config/          # CORS, exception handling, data initialization
├── controller/      # REST controllers
├── dto/             # Request/response objects
├── exception/       # Custom exceptions
├── model/           # JPA entities (Car, Reservation, CarType)
├── repository/      # Spring Data JPA repositories
└── service/         # Business logic
```
