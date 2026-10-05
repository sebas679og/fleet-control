# ⚫ ms-dashboard — Dashboard Service

[← Back to README](../README.md)

## Overview

**Port:** `8089`  
**Database:** None (in-memory Caffeine cache)

The `ms-dashboard` service aggregates data from the other microservices into an executive fleet summary and into time series ready to be plotted. It has no database: it stores responses in a Caffeine cache with a validity of **60 seconds**, using the endpoint and all its parameters as the cache key.

---

## Aggregation Rules

- Calls to the source services are launched **in parallel**.
- If a source service does not respond, the dashboard returns the available data with `partial` set to `true` and the `unavailable` list with the affected services. Values that depended on that service are returned as `null`.
- With demo data, the summary responds in less than 2 seconds without cache.

| Block                 | Source                                                    |
|-----------------------|-----------------------------------------------------------|
| `fleet`               | `ms-vehicles` (`GET /api/vehicles/summary`)               |
| `kpis`                | Statistics of `ms-routes`, `ms-fuel` and `ms-maintenance` |
| `alerts`              | `ms-alerts`                                               |
| `upcomingMaintenance` | The next 5 pending orders from `ms-maintenance`           |

---

## Data Model

This service has no persistence. It only keeps the in-memory cache described above.

---

## Endpoints

### 1. `GET /api/dashboard`

**Access:** `MANAGER`, `ADMIN`

**Description:** Main fleet summary for the last days: vehicle status, key indicators, open alerts and upcoming maintenance.

**Query Parameters:**

| Parameter | Type    | Required | Description                         |
|-----------|---------|----------|-------------------------------------|
| `days`    | Integer | No       | Default `30`, between `1` and `365` |

**Response `200 OK`:**
```json
{
  "generatedAt": "2026-10-05T10:30:00Z",
  "period": { "from": "2026-09-05", "to": "2026-10-05" },
  "partial": false,
  "unavailable": [],
  "fleet": {
    "total": 25,
    "available": 14,
    "inUse": 8,
    "inMaintenance": 2,
    "outOfService": 1
  },
  "kpis": {
    "routesCompleted": 412,
    "distanceKm": 38420.5,
    "fuelLiters": 4226.3,
    "fuelCost": 6846.61,
    "avgConsumptionL100km": 11.0,
    "maintenanceCost": 2140.25
  },
  "alerts": { "open": 11, "critical": 2, "warning": 9, "info": 0 },
  "upcomingMaintenance": [
    {
      "orderId": "e5b8c3f7-91a2-4d60-b7c4-0a1f6d2e8b01",
      "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
      "plate": "4821-KDF",
      "type": "OIL_CHANGE",
      "scheduledFor": "2026-10-08"
    }
  ]
}
```

**Partial response example** (when `ms-fuel` is down): `partial` is `true`, `unavailable` is `["ms-fuel"]` and `fuelLiters`, `fuelCost` and `avgConsumptionL100km` are `null`.

**Error responses:**

| Status | `name`             | Description                       |
|--------|--------------------|-----------------------------------|
| 400    | `VALIDATION_ERROR` | `days` is outside the 1–365 range |

---

### 2. `GET /api/dashboard/timeseries`

**Access:** `MANAGER`, `ADMIN`

**Description:** Time series of one or more metrics with the same granularity, ready for a line or bar chart.

**Query Parameters:**

| Parameter     | Type                   | Required | Description                                                                                           |
|---------------|------------------------|----------|-------------------------------------------------------------------------------------------------------|
| `metrics`     | List (comma-separated) | Yes      | `DISTANCE_KM`, `FUEL_LITERS`, `FUEL_COST`, `CONSUMPTION_L100KM`, `MAINTENANCE_COST`, `ALERTS_CREATED` |
| `from`        | Date (`YYYY-MM-DD`)    | Yes      | Range start                                                                                           |
| `to`          | Date (`YYYY-MM-DD`)    | Yes      | Range end. The maximum range is 366 days                                                              |
| `granularity` | Enum                   | No       | `DAY` (default), `WEEK` or `MONTH`                                                                    |

**Metric sources:**

| Metric               | Source service   | Unit     |
|----------------------|------------------|----------|
| `DISTANCE_KM`        | `ms-routes`      | km       |
| `FUEL_LITERS`        | `ms-fuel`        | L        |
| `FUEL_COST`          | `ms-fuel`        | EUR      |
| `CONSUMPTION_L100KM` | `ms-fuel`        | L/100 km |
| `MAINTENANCE_COST`   | `ms-maintenance` | EUR      |
| `ALERTS_CREATED`     | `ms-alerts`      | alerts   |

**Series rules:** `period` is the start date of the period (the day for `DAY`, the Monday for `WEEK`, day 1 for `MONTH`); periods without data are returned with value `0` so the series has no gaps; dates are UTC.

**Response `200 OK`:**
```json
{
  "from": "2026-09-07",
  "to": "2026-09-27",
  "granularity": "WEEK",
  "partial": false,
  "unavailable": [],
  "series": [
    {
      "metric": "DISTANCE_KM",
      "unit": "km",
      "points": [
        { "period": "2026-09-07", "value": 9034.1 },
        { "period": "2026-09-14", "value": 9710.6 },
        { "period": "2026-09-21", "value": 9340.2 }
      ]
    },
    {
      "metric": "FUEL_COST",
      "unit": "EUR",
      "points": [
        { "period": "2026-09-07", "value": 1595.21 },
        { "period": "2026-09-14", "value": 1746.20 },
        { "period": "2026-09-21", "value": 1664.39 }
      ]
    }
  ]
}
```

If a source service does not respond, only its metrics are omitted and the service is listed in `unavailable`.

**Response `400 Bad Request`:**
```json
{
  "code": 400,
  "name": "VALIDATION_ERROR",
  "description": "The request contains invalid fields",
  "details": [
    { "field": "metrics", "reason": "unknown metric: FUEL_PRICE" }
  ],
  "timestamp": "2026-10-05T10:30:00Z"
}
```

**Error responses:**

| Status | `name`             | Description                                                                            |
|--------|--------------------|----------------------------------------------------------------------------------------|
| 400    | `VALIDATION_ERROR` | A metric does not exist, a required parameter is missing or the range exceeds 366 days |

---

### 3. `GET /api/dashboard/vehicles/ranking`

**Access:** `MANAGER`, `ADMIN`

**Description:** Ranking of vehicles by fuel cost or by consumption, built from `GET /api/fuel/consumption`. The result is ordered from highest to lowest value.

**Query Parameters:**

| Parameter | Type    | Required | Description                                                                    |
|-----------|---------|----------|--------------------------------------------------------------------------------|
| `metric`  | Enum    | Yes      | `FUEL_COST` or `CONSUMPTION`                                                   |
| `days`    | Integer | No       | Default `30`                                                                   |
| `limit`   | Integer | No       | Default `5`, maximum `20`                                                      |
| `type`    | Enum    | No       | Restricts the ranking to a vehicle type, useful to compare equivalent vehicles |

**Response `200 OK`:**
```json
{
  "metric": "CONSUMPTION",
  "unit": "L/100 km",
  "period": { "from": "2026-09-05", "to": "2026-10-05" },
  "ranking": [
    { "rank": 1, "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a03", "plate": "9012-PQR", "type": "TRUCK", "value": 27.5 },
    { "rank": 2, "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a04", "plate": "1188-BCD", "type": "TRUCK", "value": 27.1 },
    { "rank": 3, "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01", "plate": "4821-KDF", "type": "VAN", "value": 10.9 }
  ]
}
```

**Error responses:**

| Status | `name`                | Description                                                          |
|--------|-----------------------|----------------------------------------------------------------------|
| 400    | `VALIDATION_ERROR`    | `metric` is missing or invalid, or `limit` exceeds 20                |
| 503    | `SERVICE_UNAVAILABLE` | `ms-fuel` did not respond (the ranking depends only on that service) |

---

## Common Errors

| Status | `name`          | Description                              |
|--------|-----------------|------------------------------------------|
| 401    | `UNAUTHORIZED`  | Missing, malformed or invalid token      |
| 401    | `TOKEN_EXPIRED` | The token has expired                    |
| 403    | `FORBIDDEN`     | The user's role does not have permission |

---

## Error Contract

All controlled errors use the same JSON structure:

```json
{
  "code": 401,
  "name": "UNAUTHORIZED",
  "description": "Missing or invalid authentication token",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

`VALIDATION_ERROR` adds a `details` array with `field` and `reason`. `SERVICE_UNAVAILABLE` adds a `service` field with the name of the unresponsive service.

---

## Inter-Service Communication

| Endpoint                              | Calls                                                                                             |
|---------------------------------------|---------------------------------------------------------------------------------------------------|
| `GET /api/dashboard`                  | `ms-vehicles`, `ms-routes`, `ms-fuel`, `ms-maintenance` and `ms-alerts`: summaries and statistics |
| `GET /api/dashboard/timeseries`       | The `/stats` endpoint of the services that own each requested metric                              |
| `GET /api/dashboard/vehicles/ranking` | `ms-fuel`: `GET /api/fuel/consumption`                                                            |

Calls run in parallel and carry `X-Internal-Key`. Feign clients use a 2 s connection / 5 s read timeout, one retry on `GET` only and a circuit breaker per client (50 % of the last 10 calls, 30 s wait). Unlike the other services, `ms-dashboard` does not answer `503` when a dependency is down: it degrades to a partial response.

---

## Environment Variables

| Variable                  | Description                                         | Example                                   |
|---------------------------|-----------------------------------------------------|-------------------------------------------|
| `JWT_SECRET`              | Secret to verify JWT signatures (HS256, ≥ 32 chars) | `change_this_secret_of_at_least_32_chars` |
| `INTERNAL_API_KEY`        | Shared key for internal calls (`X-Internal-Key`)    | `change_this_internal_key`                |
| `VEHICLES_SERVICE_URL`    | Base URL of `ms-vehicles`                           | `http://ms-vehicles:8082`                 |
| `ROUTES_SERVICE_URL`      | Base URL of `ms-routes`                             | `http://ms-routes:8085`                   |
| `FUEL_SERVICE_URL`        | Base URL of `ms-fuel`                               | `http://ms-fuel:8087`                     |
| `MAINTENANCE_SERVICE_URL` | Base URL of `ms-maintenance`                        | `http://ms-maintenance:8086`              |
| `ALERTS_SERVICE_URL`      | Base URL of `ms-alerts`                             | `http://ms-alerts:8088`                   |
| `SPRING_PROFILES_ACTIVE`  | Active Spring profile                               | `demo`                                    |

---

## Notes

- The cache key includes the endpoint and every query parameter, so different `days`, `metrics` or `granularity` values are cached separately.
- Because of the 60-second cache, a change in another service can take up to a minute to show in the dashboard.
- The dashboard is read-only: it never modifies data in other services.
