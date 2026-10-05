# 🟢 ms-vehicles — Vehicle Service

[← Back to README](../README.md)

## Overview

**Port:** `8082`  
**Database:** PostgreSQL (`vehicles_db`)

The `ms-vehicles` service keeps the fleet inventory with each vehicle's operational status and odometer. It is the source of truth queried by `ms-routes`, `ms-maintenance`, `ms-fuel`, `ms-alerts` and `ms-dashboard`, and it also receives status changes from them.

---

## Data Model

### Entity: `Vehicle`

| Field           | Type    | Description             |
|-----------------|---------|-------------------------|
| `id`            | UUID    | Unique identifier       |
| `plate`         | String  | License plate (unique)  |
| `make`          | String  | Manufacturer            |
| `model`         | String  | Model                   |
| `year`          | Integer | Year of manufacture     |
| `type`          | Enum    | Vehicle type            |
| `fuelType`      | Enum    | Fuel type               |
| `tankCapacityL` | Integer | Tank capacity in liters |
| `odometerKm`    | Integer | Accumulated mileage     |
| `status`        | Enum    | Operational status      |
| `createdAt`     | Instant | Creation timestamp      |
| `updatedAt`     | Instant | Last update timestamp   |

### Enums

```
VehicleType:   CAR, VAN, TRUCK, MOTORCYCLE
FuelType:      DIESEL, GASOLINE, LPG, HYBRID
VehicleStatus: AVAILABLE, IN_USE, IN_MAINTENANCE, OUT_OF_SERVICE
```

---

## Endpoints

### 1. `GET /api/vehicles`

**Access:** `MANAGER`, `ADMIN` and internal calls

**Description:** Paginated list of vehicles, filterable by status, type or plate.

**Query Parameters:**

| Parameter | Type    | Required | Description                 |
|-----------|---------|----------|-----------------------------|
| `status`  | Enum    | No       | e.g. `AVAILABLE`            |
| `type`    | Enum    | No       | e.g. `VAN`                  |
| `plate`   | String  | No       | Partial match               |
| `page`    | Integer | No       | Default `0`                 |
| `size`    | Integer | No       | Default `20`, maximum `100` |

**Response `200 OK`:**
```json
{
  "content": [
    {
      "id": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
      "plate": "4821-KDF",
      "make": "Ford",
      "model": "Transit",
      "year": 2022,
      "type": "VAN",
      "fuelType": "DIESEL",
      "tankCapacityL": 80,
      "odometerKm": 45210,
      "status": "AVAILABLE"
    },
    {
      "id": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a02",
      "plate": "7305-LMN",
      "make": "Renault",
      "model": "Clio",
      "year": 2021,
      "type": "CAR",
      "fuelType": "GASOLINE",
      "tankCapacityL": 45,
      "odometerKm": 61880,
      "status": "IN_USE"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 25,
  "totalPages": 2
}
```

---

### 2. `GET /api/vehicles/summary`

**Access:** `MANAGER`, `ADMIN` and internal calls

**Description:** Count of vehicles by status and by type. Consumed by `ms-dashboard`.

**Response `200 OK`:**
```json
{
  "total": 25,
  "byStatus": {
    "AVAILABLE": 14,
    "IN_USE": 8,
    "IN_MAINTENANCE": 2,
    "OUT_OF_SERVICE": 1
  },
  "byType": {
    "CAR": 9,
    "VAN": 10,
    "TRUCK": 4,
    "MOTORCYCLE": 2
  }
}
```

---

### 3. `GET /api/vehicles/{vehicleId}`

**Access:** `MANAGER`, `ADMIN` and internal calls

**Description:** Returns the detail of a vehicle by its ID.

**Response `200 OK`:**
```json
{
  "id": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
  "plate": "4821-KDF",
  "make": "Ford",
  "model": "Transit",
  "year": 2022,
  "type": "VAN",
  "fuelType": "DIESEL",
  "tankCapacityL": 80,
  "odometerKm": 45210,
  "status": "AVAILABLE",
  "createdAt": "2026-07-01T08:00:00Z",
  "updatedAt": "2026-10-05T07:45:00Z"
}
```

**Response `404 Not Found`:**
```json
{
  "code": 404,
  "name": "VEHICLE_NOT_FOUND",
  "description": "No vehicle exists with the provided ID",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

---

### 4. `POST /api/vehicles`

**Access:** `MANAGER`, `ADMIN`

**Description:** Registers a vehicle. It is always created in `AVAILABLE` status.

**Request Body:**
```json
{
  "plate": "9012-PQR",
  "make": "Mercedes-Benz",
  "model": "Actros",
  "year": 2023,
  "type": "TRUCK",
  "fuelType": "DIESEL",
  "tankCapacityL": 400,
  "odometerKm": 12000
}
```

**Validation Rules:**
- Plate is required and unique.
- Year must be between 1990 and the year after the current one.
- `tankCapacityL` must be greater than 0 and `odometerKm` greater than or equal to 0.

**Response `201 Created`:** the created vehicle, with the same structure as the detail response.

**Error responses:**

| Status | `name`                   | Description                              |
|--------|--------------------------|------------------------------------------|
| 409    | `VEHICLE_ALREADY_EXISTS` | A vehicle with that plate already exists |
| 400    | `VALIDATION_ERROR`       | One or more fields are invalid           |

---

### 5. `PUT /api/vehicles/{vehicleId}`

**Access:** `MANAGER`, `ADMIN`

**Description:** Updates the descriptive data of the vehicle: `make`, `model`, `year`, `type`, `fuelType` and `tankCapacityL`. It does **not** modify the plate, the status or the odometer.

**Request Body:**
```json
{
  "make": "Ford",
  "model": "Transit Custom",
  "year": 2022,
  "type": "VAN",
  "fuelType": "DIESEL",
  "tankCapacityL": 80
}
```

**Response `200 OK`:** the updated vehicle.

**Error responses:**

| Status | `name`              | Description                            |
|--------|---------------------|----------------------------------------|
| 404    | `VEHICLE_NOT_FOUND` | No vehicle exists with the provided ID |
| 400    | `VALIDATION_ERROR`  | One or more fields are invalid         |

---

### 6. `PATCH /api/vehicles/{vehicleId}/status`

**Access:** `MANAGER`, `ADMIN` and internal calls (`ms-routes` and `ms-maintenance`)

**Description:** Changes the vehicle status and, optionally, updates its odometer. The odometer can only increase.

**Request Body:**
```json
{
  "status": "AVAILABLE",
  "odometerKm": 45310
}
```

**Allowed transitions:**

| From             | To               | Restriction  |
|------------------|------------------|--------------|
| `AVAILABLE`      | `IN_USE`         | —            |
| `IN_USE`         | `AVAILABLE`      | —            |
| `AVAILABLE`      | `IN_MAINTENANCE` | —            |
| `IN_MAINTENANCE` | `AVAILABLE`      | —            |
| Any status       | `OUT_OF_SERVICE` | —            |
| `OUT_OF_SERVICE` | `AVAILABLE`      | `ADMIN` only |

**Response `200 OK`:** the updated vehicle.

**Error responses:**

| Status | `name`                      | Description                                                      |
|--------|-----------------------------|------------------------------------------------------------------|
| 409    | `INVALID_STATUS_TRANSITION` | The transition is not allowed (e.g. `IN_USE` → `IN_MAINTENANCE`) |
| 409    | `INVALID_ODOMETER`          | The value is lower than the current odometer                     |
| 403    | `FORBIDDEN`                 | A `MANAGER` tried to reactivate an out-of-service vehicle        |
| 404    | `VEHICLE_NOT_FOUND`         | No vehicle exists with the provided ID                           |

**Response `409 Conflict`:**
```json
{
  "code": 409,
  "name": "INVALID_STATUS_TRANSITION",
  "description": "Cannot change the vehicle status from IN_USE to IN_MAINTENANCE",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

---

## Common Errors

| Status | `name`          | Description                                                    |
|--------|-----------------|----------------------------------------------------------------|
| 401    | `UNAUTHORIZED`  | Missing, malformed or invalid token, or wrong `X-Internal-Key` |
| 401    | `TOKEN_EXPIRED` | The token has expired                                          |
| 403    | `FORBIDDEN`     | The user's role does not have permission                       |

---

## Error Contract

All controlled errors use the same JSON structure:

```json
{
  "code": 404,
  "name": "VEHICLE_NOT_FOUND",
  "description": "No vehicle exists with the provided ID",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

`VALIDATION_ERROR` adds a `details` array with `field` and `reason`:

```json
{
  "code": 400,
  "name": "VALIDATION_ERROR",
  "description": "The request contains invalid fields",
  "details": [
    { "field": "year", "reason": "must be between 1990 and 2027" }
  ],
  "timestamp": "2026-10-05T10:30:00Z"
}
```

---

## Demo Data

With the `demo` profile the seeder generates **25 vehicles** of the four types and several statuses, with deterministic UUIDs derived from `DEMO_SEED` (e.g. `UUID.nameUUIDFromBytes("vehicle-7")`). It only inserts data when the table is empty.

---

## Environment Variables

| Variable                     | Description                                         | Example                                       |
|------------------------------|-----------------------------------------------------|-----------------------------------------------|
| `SPRING_DATASOURCE_URL`      | PostgreSQL connection URL                           | `jdbc:postgresql://postgres:5432/vehicles_db` |
| `SPRING_DATASOURCE_USERNAME` | Database username                                   | `fleet`                                       |
| `SPRING_DATASOURCE_PASSWORD` | Database password                                   | `change_this_password`                        |
| `JWT_SECRET`                 | Secret to verify JWT signatures (HS256, ≥ 32 chars) | `change_this_secret_of_at_least_32_chars`     |
| `INTERNAL_API_KEY`           | Shared key for internal calls (`X-Internal-Key`)    | `change_this_internal_key`                    |
| `SPRING_PROFILES_ACTIVE`     | Active Spring profile                               | `demo`                                        |
| `DEMO_SEED`                  | Seed for deterministic demo data                    | `42`                                          |
| `DEMO_DAYS`                  | Days of history to generate                         | `90`                                          |

---

## Notes

- This service does not call any other microservice.
- A valid `X-Internal-Key` is treated as a `MANAGER`, so `OUT_OF_SERVICE` → `AVAILABLE` is never possible through an internal call.
- The plate and the odometer are never changed through `PUT`; the odometer only moves forward through `PATCH .../status`.
