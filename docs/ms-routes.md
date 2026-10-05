# 🔵 ms-routes — Route Service

[← Back to README](../README.md)

## Overview

**Port:** `8085`  
**Database:** PostgreSQL (`routes_db`)

The `ms-routes` service manages fleet routes: planning, execution and history. It consumes `ms-vehicles` and `ms-drivers` to validate assignments and to update the vehicle status and odometer when a route starts or ends. It is the origin of the **distance traveled** metric.

---

## Data Model

### Entity: `Route`

| Field                  | Type       | Description                                   |
|------------------------|------------|-----------------------------------------------|
| `id`                   | UUID       | Unique identifier                             |
| `vehicleId`            | UUID       | Assigned vehicle (reference to `ms-vehicles`) |
| `driverId`             | UUID       | Assigned driver (reference to `ms-drivers`)   |
| `origin`               | String     | Origin point                                  |
| `destination`          | String     | Destination point                             |
| `plannedStart`         | Instant    | Planned start                                 |
| `estimatedDurationMin` | Integer    | Estimated duration in minutes                 |
| `plannedDistanceKm`    | BigDecimal | Planned distance                              |
| `status`               | Enum       | Route status                                  |
| `startedAt`            | Instant    | Actual start                                  |
| `endedAt`              | Instant    | Actual end                                    |
| `startOdometerKm`      | Integer    | Odometer when the route started               |
| `endOdometerKm`        | Integer    | Odometer when the route ended                 |
| `actualDistanceKm`     | BigDecimal | Distance actually traveled                    |
| `notes`                | String     | Remarks                                       |
| `createdBy`            | UUID       | User who planned the route                    |
| `createdAt`            | Instant    | Creation timestamp                            |
| `updatedAt`            | Instant    | Last update timestamp                         |

### Enums

```
RouteStatus: PLANNED, IN_PROGRESS, COMPLETED
```

---

## Business Rules

- The vehicle must exist and must not be `OUT_OF_SERVICE`.
- The driver must exist, be `ACTIVE` and have a valid license on the route start date.
- The license category must be compatible with the vehicle type: `A` → `MOTORCYCLE`; `B` → `CAR`, `VAN`; `C` → `CAR`, `VAN`, `TRUCK`.
- There cannot be two `PLANNED` or `IN_PROGRESS` routes overlapping in time for the same vehicle or the same driver. A route interval goes from `plannedStart` to `plannedStart` plus `estimatedDurationMin`.

---

## Endpoints

### 1. `POST /api/routes`

**Access:** `MANAGER`, `ADMIN`

**Description:** Plans a route by assigning a vehicle and a driver. Business rules are validated by calling `ms-vehicles` and `ms-drivers`. Nothing is saved if a dependency does not respond.

**Request Body:**
```json
{
  "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
  "driverId": "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01",
  "origin": "Madrid - Centro Logístico",
  "destination": "Valencia - Puerto",
  "plannedStart": "2026-10-06T07:00:00Z",
  "estimatedDurationMin": 240,
  "plannedDistanceKm": 355.0
}
```

**Response `201 Created`:** the route with `status` set to `PLANNED` and the model fields that already have a value.

**Error responses:**

| Status | `name`                  | Description                                                                  |
|--------|-------------------------|------------------------------------------------------------------------------|
| 404    | `VEHICLE_NOT_FOUND`     | The vehicle does not exist                                                   |
| 404    | `DRIVER_NOT_FOUND`      | The driver does not exist                                                    |
| 409    | `VEHICLE_NOT_AVAILABLE` | The vehicle is out of service                                                |
| 409    | `DRIVER_NOT_ELIGIBLE`   | The driver is inactive, has an expired license or an incompatible category   |
| 409    | `ROUTE_OVERLAP`         | The vehicle or the driver overlaps with another planned or in-progress route |
| 503    | `SERVICE_UNAVAILABLE`   | `ms-vehicles` or `ms-drivers` did not respond                                |

**Response `409 Conflict`:**
```json
{
  "code": 409,
  "name": "DRIVER_NOT_ELIGIBLE",
  "description": "The driver license category is not compatible with the vehicle type",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

---

### 2. `GET /api/routes`

**Access:** `MANAGER`, `ADMIN` and internal calls

**Description:** Paginated list of routes, ordered by `plannedStart` descending.

**Query Parameters:**

| Parameter | Type                | Required | Description                      |
|-----------|---------------------|----------|----------------------------------|
| `vehicle` | UUID                | No       | Filter by vehicle                |
| `driver`  | UUID                | No       | Filter by driver                 |
| `status`  | Enum                | No       | e.g. `COMPLETED`                 |
| `from`    | Date (`YYYY-MM-DD`) | No       | Filters by `plannedStart` (from) |
| `to`      | Date (`YYYY-MM-DD`) | No       | Filters by `plannedStart` (to)   |
| `page`    | Integer             | No       | Default `0`                      |
| `size`    | Integer             | No       | Default `20`, maximum `100`      |

**Response `200 OK`:** a page with the usual structure (`content`, `page`, `size`, `totalElements`, `totalPages`) where each element is a route.

---

### 3. `GET /api/routes/{routeId}`

**Access:** `MANAGER`, `ADMIN` and internal calls

**Description:** Returns the detail of a route by ID.

**Response `200 OK`:**
```json
{
  "id": "c8e1b7a4-52d9-4f06-8e3b-1a7d90c4f201",
  "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
  "driverId": "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01",
  "origin": "Madrid - Centro Logístico",
  "destination": "Valencia - Puerto",
  "plannedStart": "2026-10-06T07:00:00Z",
  "estimatedDurationMin": 240,
  "plannedDistanceKm": 355.0,
  "status": "COMPLETED",
  "startedAt": "2026-10-06T07:04:12Z",
  "endedAt": "2026-10-06T11:20:45Z",
  "startOdometerKm": 45210,
  "endOdometerKm": 45572,
  "actualDistanceKm": 362.4,
  "notes": "No incidents"
}
```

**Response `404 Not Found`:**
```json
{
  "code": 404,
  "name": "ROUTE_NOT_FOUND",
  "description": "No route exists with the provided ID",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

---

### 4. `POST /api/routes/{routeId}/start`

**Access:** `MANAGER`, `ADMIN`

**Description:** Starts a planned route. It has no body. Operations run in this order:

1. Check that the route is `PLANNED`.
2. Query the vehicle in `ms-vehicles` and check that it is `AVAILABLE`.
3. Change the vehicle to `IN_USE` in `ms-vehicles`.
4. Save the route as `IN_PROGRESS`, with `startedAt` set to the current time and `startOdometerKm` set to the vehicle odometer.

If step 4 fails, the vehicle is returned to `AVAILABLE` as a compensation.

**Response `200 OK`:** the route with `status` set to `IN_PROGRESS`.

**Error responses:**

| Status | `name`                  | Description                             |
|--------|-------------------------|-----------------------------------------|
| 404    | `ROUTE_NOT_FOUND`       | No route exists with the provided ID    |
| 409    | `INVALID_ROUTE_STATE`   | The route is not planned                |
| 409    | `VEHICLE_NOT_AVAILABLE` | The vehicle is in use or in maintenance |
| 503    | `SERVICE_UNAVAILABLE`   | `ms-vehicles` did not respond           |

---

### 5. `POST /api/routes/{routeId}/complete`

**Access:** `MANAGER`, `ADMIN`

**Description:** Finishes a route in progress. It computes `endOdometerKm` by adding the rounded actual distance to `startOdometerKm`, and leaves the vehicle `AVAILABLE` with the updated odometer. If `ms-vehicles` does not respond, the route stays `IN_PROGRESS` and the operation can be retried.

**Request Body:**
```json
{
  "actualDistanceKm": 362.4,
  "notes": "No incidents"
}
```

**Response `200 OK`:** the route with `status` set to `COMPLETED` (same structure as the detail response).

**Error responses:**

| Status | `name`                | Description                                                  |
|--------|-----------------------|--------------------------------------------------------------|
| 404    | `ROUTE_NOT_FOUND`     | No route exists with the provided ID                         |
| 409    | `INVALID_ROUTE_STATE` | The route is not in progress                                 |
| 400    | `VALIDATION_ERROR`    | `actualDistanceKm` is not greater than 0                     |
| 503    | `SERVICE_UNAVAILABLE` | `ms-vehicles` did not respond; the route stays `IN_PROGRESS` |

---

### 6. `GET /api/routes/stats`

**Access:** `MANAGER`, `ADMIN` and internal calls

**Description:** Time series of completed routes, grouped by completion date. Consumed by `ms-dashboard`.

**Query Parameters:**

| Parameter     | Type                | Required | Description                        |
|---------------|---------------------|----------|------------------------------------|
| `from`        | Date (`YYYY-MM-DD`) | Yes      | Range start                        |
| `to`          | Date (`YYYY-MM-DD`) | Yes      | Range end                          |
| `granularity` | Enum                | No       | `DAY` (default), `WEEK` or `MONTH` |
| `vehicle`     | UUID                | No       | Filter by vehicle                  |

**Series rules:** `period` is the start date of the period (the day itself for `DAY`, the Monday for `WEEK`, day 1 for `MONTH`); periods without data are returned with value `0`; all dates are computed in UTC.

**Response `200 OK`:**
```json
{
  "from": "2026-09-07",
  "to": "2026-09-27",
  "granularity": "WEEK",
  "totals": { "routes": 302, "distanceKm": 28084.9 },
  "series": [
    { "period": "2026-09-07", "routes": 97, "distanceKm": 9034.1, "avgDurationMin": 128 },
    { "period": "2026-09-14", "routes": 104, "distanceKm": 9710.6, "avgDurationMin": 131 },
    { "period": "2026-09-21", "routes": 101, "distanceKm": 9340.2, "avgDurationMin": 126 }
  ]
}
```

---

## Common Errors

| Status | `name`                | Description                                                    |
|--------|-----------------------|----------------------------------------------------------------|
| 401    | `UNAUTHORIZED`        | Missing, malformed or invalid token, or wrong `X-Internal-Key` |
| 401    | `TOKEN_EXPIRED`       | The token has expired                                          |
| 403    | `FORBIDDEN`           | The user's role does not have permission                       |
| 503    | `SERVICE_UNAVAILABLE` | A dependency does not respond                                  |

---

## Error Contract

All controlled errors use the same JSON structure:

```json
{
  "code": 409,
  "name": "ROUTE_OVERLAP",
  "description": "The vehicle or the driver already has a route in that time interval",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

`VALIDATION_ERROR` adds a `details` array with `field` and `reason`. `SERVICE_UNAVAILABLE` adds a `service` field with the name of the service that is not responding:

```json
{
  "code": 503,
  "name": "SERVICE_UNAVAILABLE",
  "description": "A required service is not responding",
  "service": "ms-vehicles",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

---

## Inter-Service Communication

| Action           | Calls                                                           |
|------------------|-----------------------------------------------------------------|
| Plan a route     | `ms-vehicles`: get the vehicle. `ms-drivers`: get the driver    |
| Start a route    | `ms-vehicles`: get the vehicle and change it to `IN_USE`        |
| Complete a route | `ms-vehicles`: change it to `AVAILABLE` and update the odometer |

Feign clients use a 2 s connection timeout and 5 s read timeout, one retry on `GET` requests only, and a Resilience4j circuit breaker per client (opens at 50 % failures over the last 10 calls and waits 30 s before probing again). Calls carry the `X-Internal-Key` header.

---

## Demo Data

With the `demo` profile the seeder generates about **1,300 completed routes** over working days and **3 planned routes**, referencing the deterministic UUIDs of vehicles and drivers without calling other services at startup. It only inserts data when the table is empty.

---

## Environment Variables

| Variable                     | Description                                         | Example                                     |
|------------------------------|-----------------------------------------------------|---------------------------------------------|
| `SPRING_DATASOURCE_URL`      | PostgreSQL connection URL                           | `jdbc:postgresql://postgres:5432/routes_db` |
| `SPRING_DATASOURCE_USERNAME` | Database username                                   | `fleet`                                     |
| `SPRING_DATASOURCE_PASSWORD` | Database password                                   | `change_this_password`                      |
| `JWT_SECRET`                 | Secret to verify JWT signatures (HS256, ≥ 32 chars) | `change_this_secret_of_at_least_32_chars`   |
| `INTERNAL_API_KEY`           | Shared key for internal calls (`X-Internal-Key`)    | `change_this_internal_key`                  |
| `VEHICLES_SERVICE_URL`       | Base URL of `ms-vehicles`                           | `http://ms-vehicles:8082`                   |
| `DRIVERS_SERVICE_URL`        | Base URL of `ms-drivers`                            | `http://ms-drivers:8083`                    |
| `SPRING_PROFILES_ACTIVE`     | Active Spring profile                               | `demo`                                      |
| `DEMO_SEED`                  | Seed for deterministic demo data                    | `42`                                        |
| `DEMO_DAYS`                  | Days of history to generate                         | `90`                                        |

---

## Notes

- Route `status` has only three values; there is no cancelled state.
- `GET /api/routes/stats` is declared before `GET /api/routes/{routeId}` in the controller (or constrained by UUID type) to avoid path ambiguity.
- Dates in filters and series are interpreted in UTC.
