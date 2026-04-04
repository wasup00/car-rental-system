# Car Rental System — Backend

A REST API for managing car rentals built with Spring Boot and SQLite. Supports multi-tenant operation with JWT-based authentication and role-based access control.

## Tech Stack

- **Java 21** with **Spring Boot 4.0.2**
- **Spring Security 7** with stateless JWT authentication
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

On first startup, the system seeds a default tenant and user accounts:

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@carrental.com | admin123 |
| Client | client@demo.com | client123 |
| Customer | customer@demo.com | customer123 |

The default tenant is **Demo Fleet** (slug: `demo`) and comes with a fleet of 10 cars:

| Type | Count | License Plates |
|------|-------|----------------|
| SEDAN | 5 | SEDAN-001 to SEDAN-005 |
| SUV | 3 | SUV-001 to SUV-003 |
| VAN | 2 | VAN-001 to VAN-002 |

## API Endpoints

All endpoints except `/auth/**` require a valid `Authorization: Bearer <token>` header.

### Authentication

#### Login
```
POST /auth/login
```
```json
{ "email": "customer@demo.com", "password": "customer123" }
```
Returns `{ "accessToken", "refreshToken", "user" }`.

#### Register (customer self-registration)
```
POST /auth/register
```
```json
{ "email": "jane@example.com", "password": "secret", "fullName": "Jane Smith", "tenantSlug": "demo" }
```

#### Refresh token
```
POST /auth/refresh
```
```json
{ "refreshToken": "<token>" }
```

#### List available tenants
```
GET /auth/tenants
```

---

### Cars

#### Get all cars
```
GET /cars
```
Returns cars scoped to the authenticated user's tenant. Admins see all cars.

#### Check availability
```
GET /cars/availability?type={carType}&startDate={dateTime}&days={number}
```

| Parameter | Type | Description |
|-----------|------|-------------|
| `type` | `SEDAN \| SUV \| VAN` | Vehicle type |
| `startDate` | `LocalDateTime` | e.g. `2025-07-01T10:00:00` |
| `days` | `int` | Number of rental days |

```json
{ "available": 3, "total": 5 }
```

#### Add a car *(Client, Admin)*
```
POST /cars
```
```json
{ "type": "SEDAN", "licensePlate": "SEDAN-006" }
```

#### Remove a car *(Client, Admin)*
```
DELETE /cars/{id}
```

---

### Reservations

#### Create a reservation
```
POST /reservations
```
```json
{ "carType": "SEDAN", "startDateTime": "2025-07-01T10:00:00", "numberOfDays": 3 }
```
The customer name is derived from the authenticated user. Returns `409 Conflict` if no car of the requested type is available.

#### Get reservations
```
GET /reservations
```
- **Admin** — all reservations
- **Client** — all reservations within their tenant
- **Customer** — their own reservations only

#### Cancel a reservation
```
DELETE /reservations/{id}
```
Returns `204 No Content`. Customers can only cancel their own reservations; clients can cancel any within their tenant.

---

### Admin

All `/admin/**` endpoints require the `ADMIN` role.

#### Create a tenant
```
POST /admin/tenants
```
```json
{
  "tenantName": "Acme Rentals",
  "tenantSlug": "acme",
  "clientEmail": "admin@acme.com",
  "clientPassword": "secret",
  "clientFullName": "Acme Admin"
}
```

#### List all tenants
```
GET /admin/tenants
```

#### List all users
```
GET /admin/users
```

---

## Roles

| Role | Description |
|------|-------------|
| `ADMIN` | Full system access — manages tenants, users, and all data |
| `CLIENT` | Manages their own tenant's fleet and reservations |
| `CUSTOMER` | Books cars and manages their own reservations |

## Project Structure

```
src/main/java/com/wasup/car_rental_system/
├── config/          # Data initialization, exception handling
├── controller/      # REST controllers (Auth, Admin, Car, Reservation)
├── dto/             # Request/response objects
├── exception/       # Custom exceptions
├── model/           # JPA entities (Car, Reservation, Tenant, User, Role)
├── repository/      # Spring Data JPA repositories
├── security/        # JWT provider, filter, SecurityConfig, UserPrincipal
└── service/         # Business logic (Auth, Admin, Car, Availability, Reservation)
```
