# 🟠 ms-fuel — Fuel Service

[← Back to README](../README.md)

## Overview

**Port:** `8087`  
**Database:** PostgreSQL (`fuel_db`)

The `ms-fuel` service records fleet refuels and computes the consumption of each vehicle. It consumes `ms-vehicles` to validate the vehicle, get its fuel type and tank capacity, and enrich responses with the plate. It is the origin of the **liters**, **cost** and **consumption** metrics.

---

## Data Model

### Entity: `FuelRecord`

| Field               | Type       | Description                                                   |
|---------------------|------------|---------------------------------------------------------------|
| `id`                | UUID       | Unique identifier                                             |
| `vehicleId`         | UUID       | Vehicle (reference to `ms-vehicles`)                          |
| `refueledAt`        | Instant    | Refuel date and time                                          |
| `fuelType`          | Enum       | Fuel type, taken from the vehicle                             |
| `liters`            | BigDecimal | Liters refueled                                               |
| `pricePerLiter`     | BigDecimal | Price per liter in EUR                                        |
| `totalCost`         | BigDecimal | Total cost in EUR                                             |
| `odometerKm`        | Integer    | Odometer at refuel time                                       |
| `fullTank`          | Boolean    | Whether the tank was filled                                   |
| `station`           | String     | Gas station                                                   |
| `consumptionL100km` | BigDecimal | Computed consumption (null if there is no previous full tank) |
| `createdAt`         | Instant    | Registration timestamp                                        |

### Enums

```
FuelType: DIESEL, GASOLINE, LPG, HYBRID   // same values as ms-vehicles
```

---

## Business Rules

- `fuelType` is taken from the vehicle; it is not sent in the body.
- `liters` cannot exceed the vehicle tank capacity.
- `odometerKm` cannot be lower than the vehicle's last refuel odometer.
- `totalCost` is `liters × pricePerLiter`, rounded to 2 decimals.
- Consumption is computed between two **consecutive full-tank** refuels (`fullTank = true`), adding the liters of the partial refuels in between:

```
consumptionL100km = (liters since previous full tank / km between full tanks) × 100
```

  Example: full tank at 45,000 km, a partial refuel of 20 L, and a new full tank of 38 L at 45,500 km → `(20 + 38) / 500 × 100 = 11.6 L/100 km`. If there is no previous full tank, `consumptionL100km` is `null`.

---

## Endpoints

### 1. `POST /api/fuel/refuels`

**Access:** `MANAGER`, `ADMIN`

**Description:** Registers a refuel.

**Request Body:**
```json
{
  "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
  "refueledAt": "2026-10-06T12:15:00Z",
  "liters": 58.4,
  "pricePerLiter": 1.62,
  "odometerKm": 45572,
  "fullTank": true,
  "station": "Repsol A-3 km 112"
}
```

**Response `201 Created`:**
```json
{
  "id": "b7a3f9e2-4c18-4d5a-9e60-3f1d8c2a7b01",
  "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
  "refueledAt": "2026-10-06T12:15:00Z",
  "fuelType": "DIESEL",
  "liters": 58.4,
  "pricePerLiter": 1.62,
  "totalCost": 94.61,
  "odometerKm": 45572,
  "fullTank": true,
  "station": "Repsol A-3 km 112",
  "consumptionL100km": 11.3
}
```

**Error responses:**

| Status | `name`                | Description                                             |
|--------|-----------------------|---------------------------------------------------------|
| 404    | `VEHICLE_NOT_FOUND`   | The vehicle does not exist                              |
| 400    | `VALIDATION_ERROR`    | `liters` is 0 or negative, or exceeds the tank capacity |
| 409    | `INVALID_ODOMETER`    | The odometer is lower than the previous refuel's        |
| 503    | `SERVICE_UNAVAILABLE` | `ms-vehicles` did not respond                           |

**Response `409 Conflict`:**
```json
{
  "code": 409,
  "name": "INVALID_ODOMETER",
  "description": "The odometer cannot be lower than the previous refuel odometer",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

---

### 2. `GET /api/fuel/refuels`

**Access:** `MANAGER`, `ADMIN` and internal calls

**Description:** Paginated list of refuels, ordered by `refueledAt` descending.

**Query Parameters:**

| Parameter | Type                | Required | Description                 |
|-----------|---------------------|----------|-----------------------------|
| `vehicle` | UUID                | No       | Filter by vehicle           |
| `from`    | Date (`YYYY-MM-DD`) | No       | Range start                 |
| `to`      | Date (`YYYY-MM-DD`) | No       | Range end                   |
| `page`    | Integer             | No       | Default `0`                 |
| `size`    | Integer             | No       | Default `20`, maximum `100` |

**Response `200 OK`:** a page with the usual structure (`content`, `page`, `size`, `totalElements`, `totalPages`) where each element has the same fields as the creation response.

---

### 3. `GET /api/fuel/consumption`

**Access:** `MANAGER`, `ADMIN` and internal calls

**Description:** Aggregated consumption per vehicle over a period, ordered by `cost` descending. Distance is computed from the odometer difference between the first and last refuel of the period. Plate and type come from `ms-vehicles`. Consumed by `ms-dashboard` and `ms-alerts`.

**Query Parameters:**

| Parameter | Type                | Required | Description |
|-----------|---------------------|----------|-------------|
| `from`    | Date (`YYYY-MM-DD`) | Yes      | Range start |
| `to`      | Date (`YYYY-MM-DD`) | Yes      | Range end   |

**Response `200 OK`:**
```json
{
  "from": "2026-09-01",
  "to": "2026-09-30",
  "vehicles": [
    {
      "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a03",
      "plate": "9012-PQR",
      "type": "TRUCK",
      "liters": 1845.2,
      "cost": 2951.96,
      "distanceKm": 6720,
      "avgL100km": 27.5
    },
    {
      "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
      "plate": "4821-KDF",
      "type": "VAN",
      "liters": 690.3,
      "cost": 1118.29,
      "distanceKm": 6310,
      "avgL100km": 10.9
    }
  ]
}
```

---

### 4. `GET /api/fuel/stats`

**Access:** `MANAGER`, `ADMIN` and internal calls

**Description:** Time series of liters, cost and average consumption for the fleet or a single vehicle. Consumed by `ms-dashboard`.

**Query Parameters:**

| Parameter     | Type                | Required | Description                        |
|---------------|---------------------|----------|------------------------------------|
| `from`        | Date (`YYYY-MM-DD`) | Yes      | Range start                        |
| `to`          | Date (`YYYY-MM-DD`) | Yes      | Range end                          |
| `granularity` | Enum                | No       | `DAY` (default), `WEEK` or `MONTH` |
| `vehicle`     | UUID                | No       | Filter by vehicle                  |

**Series rules:** `period` is the start date of the period (the day for `DAY`, the Monday for `WEEK`, day 1 for `MONTH`); empty periods return `0`; dates are UTC.

**Response `200 OK`:**
```json
{
  "from": "2026-09-07",
  "to": "2026-09-27",
  "granularity": "WEEK",
  "totals": { "liters": 3090.0, "cost": 5005.80, "avgPricePerLiter": 1.62, "avgL100km": 11.0 },
  "series": [
    { "period": "2026-09-07", "liters": 984.7, "cost": 1595.21, "avgPricePerLiter": 1.62, "avgL100km": 10.9 },
    { "period": "2026-09-14", "liters": 1077.9, "cost": 1746.20, "avgPricePerLiter": 1.62, "avgL100km": 11.1 },
    { "period": "2026-09-21", "liters": 1027.4, "cost": 1664.39, "avgPricePerLiter": 1.62, "avgL100km": 11.0 }
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
| 503    | `SERVICE_UNAVAILABLE` | `ms-vehicles` does not respond                                 |

---

## Error Contract

All controlled errors use the same JSON structure:

```json
{
  "code": 400,
  "name": "VALIDATION_ERROR",
  "description": "The request contains invalid fields",
  "details": [
    { "field": "liters", "reason": "must not exceed the tank capacity (80 L)" }
  ],
  "timestamp": "2026-10-05T10:30:00Z"
}
```

`details` appears only on `VALIDATION_ERROR`. `SERVICE_UNAVAILABLE` adds a `service` field with the name of the unresponsive service.

---

## Inter-Service Communication

| Action                              | Calls                                          |
|-------------------------------------|------------------------------------------------|
| Register a refuel                   | `ms-vehicles`: get fuel type and tank capacity |
| Consumption / list (plate and type) | `ms-vehicles`: get plate and type              |

Feign clients use a 2 s connection / 5 s read timeout, one retry on `GET` only and a circuit breaker per client (50 % of the last 10 calls, 30 s wait).

---

## Demo Data

With the `demo` profile the seeder generates about **300 refuels** over `DEMO_DAYS` days:

| Vehicle type | Base consumption (L/100 km) |
|--------------|-----------------------------|
| `CAR`        | 6.5                         |
| `VAN`        | 10.5                        |
| `TRUCK`      | 27                          |
| `MOTORCYCLE` | 4.2                         |

- Each refuel varies ±8 % around the base consumption; price per liter fluctuates between 1.45 and 1.75 EUR with a weekly trend.
- Two vehicles have, over the last 30 days, a consumption 30 % higher than their previous average, so `ms-alerts` raises `HIGH_CONSUMPTION` alerts on the first start.

---

## Environment Variables

| Variable                     | Description                                         | Example                                   |
|------------------------------|-----------------------------------------------------|-------------------------------------------|
| `SPRING_DATASOURCE_URL`      | PostgreSQL connection URL                           | `jdbc:postgresql://postgres:5432/fuel_db` |
| `SPRING_DATASOURCE_USERNAME` | Database username                                   | `fleet`                                   |
| `SPRING_DATASOURCE_PASSWORD` | Database password                                   | `change_this_password`                    |
| `JWT_SECRET`                 | Secret to verify JWT signatures (HS256, ≥ 32 chars) | `change_this_secret_of_at_least_32_chars` |
| `INTERNAL_API_KEY`           | Shared key for internal calls (`X-Internal-Key`)    | `change_this_internal_key`                |
| `VEHICLES_SERVICE_URL`       | Base URL of `ms-vehicles`                           | `http://ms-vehicles:8082`                 |
| `SPRING_PROFILES_ACTIVE`     | Active Spring profile                               | `demo`                                    |
| `DEMO_SEED`                  | Seed for deterministic demo data                    | `42`                                      |
| `DEMO_DAYS`                  | Days of history to generate                         | `90`                                      |

---

## Notes

- The consumption calculation must be covered by unit tests, including the `(20 + 38) / 500 × 100 = 11.6 L/100 km` example.
- Monetary amounts are in EUR with 2 decimals; volumes in liters; consumption in L/100 km.
