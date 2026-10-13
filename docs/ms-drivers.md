# 🟡 ms-drivers — Driver Service

[← Back to README](../README.md)

## Overview

**Port:** `8083`  
**Database:** PostgreSQL (`drivers_db`)

The `ms-drivers` service manages fleet drivers, their driving license and their status. `ms-routes` queries it to check that a driver can be assigned to a route, and `ms-alerts` queries it to detect licenses that are about to expire.

---

## Authentication

Every endpoint requires authentication, either with a `Bearer` JWT issued by `ms-auth`
(`MANAGER` or `ADMIN` role) or with the shared `X-Internal-Key` header for
service-to-service calls (grants `MANAGER` permissions). Without credentials the service
returns `401 UNAUTHORIZED`; an expired token returns `401 TOKEN_EXPIRED`.

---

## Data Model

### Entity: `Driver`

| Field              | Type      | Description             |
|--------------------|-----------|-------------------------|
| `id`               | UUID      | Unique identifier       |
| `fullName`         | String    | Full name               |
| `email`            | String    | Email address           |
| `phone`            | String    | Contact phone           |
| `licenseNumber`    | String    | License number (unique) |
| `licenseCategory`  | Enum      | License category        |
| `licenseExpiresAt` | LocalDate | License expiration date |
| `status`           | Enum      | Driver status           |
| `createdAt`        | Instant   | Creation timestamp      |
| `updatedAt`        | Instant   | Last update timestamp   |

### Enums

```
LicenseCategory: A, B, C
DriverStatus:    ACTIVE, ON_LEAVE, SUSPENDED
```

---

## Endpoints

### 1. `GET /api/drivers`

**Access:** `MANAGER`, `ADMIN` and internal calls

**Description:** Paginated list of drivers, filterable by status or by license expiration.

**Query Parameters:**

| Parameter               | Type    | Required | Description                                                                                          |
|-------------------------|---------|----------|------------------------------------------------------------------------------------------------------|
| `status`                | Enum    | No       | e.g. `ACTIVE`                                                                                        |
| `licenseExpiringInDays` | Integer | No       | Drivers whose license expires within that number of days or less, **including already expired ones** |
| `page`                  | Integer | No       | Default `0`                                                                                          |
| `size`                  | Integer | No       | Default `20`, maximum `100`                                                                          |

**Response `200 OK`:**
```json
{
  "content": [
    {
      "id": "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01",
      "fullName": "Laura Fernández",
      "email": "laura.fernandez@fleetcontrol.com",
      "phone": "+34 600 123 456",
      "licenseNumber": "B-4471923",
      "licenseCategory": "B",
      "licenseExpiresAt": "2027-03-14",
      "status": "ACTIVE"
    },
    {
      "id": "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a02",
      "fullName": "Miguel Torres",
      "email": "miguel.torres@fleetcontrol.com",
      "phone": "+34 611 987 654",
      "licenseNumber": "C-2038841",
      "licenseCategory": "C",
      "licenseExpiresAt": "2026-10-20",
      "status": "ACTIVE"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 20,
  "totalPages": 1
}
```

---

### 2. `GET /api/drivers/{driverId}`

**Access:** `MANAGER`, `ADMIN` and internal calls

**Description:** Returns the detail of a driver by ID. It returns the same fields as the list plus `createdAt` and `updatedAt`.

**Response `200 OK`:**
```json
{
  "id": "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01",
  "fullName": "Laura Fernández",
  "email": "laura.fernandez@fleetcontrol.com",
  "phone": "+34 600 123 456",
  "licenseNumber": "B-4471923",
  "licenseCategory": "B",
  "licenseExpiresAt": "2027-03-14",
  "status": "ACTIVE",
  "createdAt": "2026-07-01T08:00:00Z",
  "updatedAt": "2026-10-05T07:45:00Z"
}
```

**Response `404 Not Found`:**
```json
{
  "error": "DRIVER_NOT_FOUND",
  "message": "Driver not found: 3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a99",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

---

### 3. `POST /api/drivers`

**Access:** `MANAGER`, `ADMIN`

**Description:** Registers a driver. It is always created in `ACTIVE` status.

**Request Body:**
```json
{
  "fullName": "Sara Ibáñez",
  "email": "sara.ibanez@fleetcontrol.com",
  "phone": "+34 622 456 789",
  "licenseNumber": "B-5120377",
  "licenseCategory": "B",
  "licenseExpiresAt": "2029-06-30"
}
```

**Validation Rules:**
- License number is required and unique.
- Email must have a valid format.
- `licenseExpiresAt` must be later than the current date.

**Response `201 Created`:** the created driver, including `id`, `status`, `createdAt` and `updatedAt`.

**Error responses:**

| Status | `error`                 | Description                                      |
|--------|-------------------------|--------------------------------------------------|
| 409    | `DRIVER_ALREADY_EXIST`  | A driver with that license number already exists |
| 400    | `VALIDATION_ERROR`      | One or more fields are invalid                   |

**Response `409 Conflict`:**
```json
{
  "error": "DRIVER_ALREADY_EXIST",
  "message": "Driver already exists with license number: B-5120377",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

---

### 4. `PUT /api/drivers/{driverId}`

**Access:** `MANAGER`, `ADMIN`

**Description:** Updates all the driver data, including the status (`ACTIVE`, `ON_LEAVE`, `SUSPENDED`). It allows renewing the license with a new expiration date.

**Request Body:**
```json
{
  "fullName": "Sara Ibáñez",
  "email": "sara.ibanez@fleetcontrol.com",
  "phone": "+34 622 456 789",
  "licenseNumber": "B-5120377",
  "licenseCategory": "B",
  "licenseExpiresAt": "2031-06-30",
  "status": "ACTIVE"
}
```

**Response `200 OK`:** the updated driver.

**Error responses:**

| Status | `error`                 | Description                                  |
|--------|-------------------------|----------------------------------------------|
| 404    | `DRIVER_NOT_FOUND`      | No driver exists with the provided ID        |
| 409    | `DRIVER_ALREADY_EXIST`  | The license number belongs to another driver |
| 400    | `VALIDATION_ERROR`      | One or more fields are invalid               |

---

## Common Errors

| Status | `error`         | Description                                                    |
|--------|-----------------|----------------------------------------------------------------|
| 401    | `UNAUTHORIZED`  | Missing, malformed or invalid token, or wrong `X-Internal-Key` |
| 401    | `TOKEN_EXPIRED` | The token has expired                                          |
| 403    | `FORBIDDEN`     | The user's role does not have permission                       |
| 400    | `VALIDATION_ERROR` | Invalid pagination (`page < 0` or `size` outside `1-100`)   |

---

## Error Contract

All controlled errors use the same JSON structure:

```json
{
  "error": "DRIVER_NOT_FOUND",
  "message": "Driver not found: 3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a99",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

`VALIDATION_ERROR` adds a `details` array with `field` and `reason`.

---

## Demo Data

With the `demo` profile Flyway applies `db/demo/V2__demo_drivers.sql` with **20 drivers**
(deterministic ids `driver-1..20`): 17 `ACTIVE`, 2 `ON_LEAVE`, 1 `SUSPENDED`; 3 licences
expiring within 30 days and 1 expired (relative `CURRENT_DATE` offsets, so the distribution
never rots). Other profiles only run `V1` and get an empty table.

---

## Environment Variables

| Variable                              | Description                                         | Example                                      |
|---------------------------------------|-----------------------------------------------------|----------------------------------------------|
| `FLEET_CONTROL_DATASOURCE_URL_DRIVERS`  | PostgreSQL connection URL                           | `jdbc:postgresql://postgres:5432/drivers_db` |
| `FLEET_CONTROL_DATASOURCE_USERNAME`     | Database username                                   | `fleet_app`                                  |
| `FLEET_CONTROL_DATASOURCE_PASSWORD`     | Database password                                   | `Fleet_password`                             |
| `JWT_SECRET`                          | Secret to verify JWT signatures (HS256, ≥ 32 chars) | `change_this_secret_of_at_least_32_chars`    |
| `JWT_EXPIRATION`                      | Unused by this service (tokens validated by signature) | `3600000`                                 |
| `INTERNAL_API_KEY`                    | Shared key for internal calls (`X-Internal-Key`)    | `change_this_internal_key`                   |
| `SPRING_PROFILES_ACTIVE`              | Active Spring profile                               | `demo`                                       |
| `DEMO_SEED`                           | Reserved for other services (this seed is static SQL) | `42`                                       |
| `DEMO_DAYS`                           | Days of history to generate (used by other services)| `90`                                         |

---

## Notes

- This service does not call any other microservice.
- License categories map to vehicle types: `A` → `MOTORCYCLE`; `B` → `CAR`, `VAN`; `C` → `CAR`, `VAN`, `TRUCK`. The check itself is done by `ms-routes`.
- There is no delete endpoint: drivers are deactivated through `status`.
