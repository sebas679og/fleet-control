# 🟤 ms-maintenance — Maintenance Service

[← Back to README](../README.md)

## Overview

**Port:** `8086`  
**Database:** PostgreSQL (`maintenance_db`)

The `ms-maintenance` service manages scheduled maintenance: recurring plans per vehicle and the work orders derived from them. It consumes `ms-vehicles` to read the odometer and to mark the vehicle as `IN_MAINTENANCE` while the work is being done.

---

## Data Model

### Entity: `MaintenancePlan`

| Field          | Type      | Description                          |
|----------------|-----------|--------------------------------------|
| `id`           | UUID      | Unique identifier                    |
| `vehicleId`    | UUID      | Vehicle (reference to `ms-vehicles`) |
| `type`         | Enum      | Maintenance type                     |
| `intervalKm`   | Integer   | Interval in kilometers (optional)    |
| `intervalDays` | Integer   | Interval in days (optional)          |
| `lastDoneAt`   | LocalDate | Date of the last intervention        |
| `lastDoneKm`   | Integer   | Odometer at the last intervention    |
| `nextDueAt`    | LocalDate | Next due date                        |
| `nextDueKm`    | Integer   | Next due odometer                    |
| `active`       | Boolean   | Whether the plan is in force         |
| `createdAt`    | Instant   | Creation timestamp                   |

### Entity: `MaintenanceOrder`

| Field          | Type       | Description                          |
|----------------|------------|--------------------------------------|
| `id`           | UUID       | Unique identifier                    |
| `planId`       | UUID       | Plan that originated it              |
| `vehicleId`    | UUID       | Vehicle (reference to `ms-vehicles`) |
| `type`         | Enum       | Maintenance type                     |
| `status`       | Enum       | Order status                         |
| `scheduledFor` | LocalDate  | Planned date                         |
| `startedAt`    | Instant    | Intervention start                   |
| `completedAt`  | Instant    | Intervention end                     |
| `odometerKm`   | Integer    | Odometer when completed              |
| `cost`         | BigDecimal | Cost in EUR                          |
| `workshop`     | String     | Workshop that performed the work     |
| `notes`        | String     | Remarks                              |
| `createdAt`    | Instant    | Creation timestamp                   |

### Enums

```
MaintenanceType:        OIL_CHANGE, TIRE_ROTATION, BRAKE_CHECK, GENERAL_INSPECTION, LEGAL_INSPECTION
MaintenanceOrderStatus: PENDING, IN_PROGRESS, COMPLETED
```

---

## Business Rules

- A plan defines an interval by kilometers (`intervalKm`), by days (`intervalDays`) or both. At least one is required, and the plan is due when **the first one** is reached.
- A vehicle can only have **one active plan per maintenance type**.
- A daily task (`@Scheduled`, 06:00 UTC) checks active plans with no open order and creates a `PENDING` order if `nextDueAt` is 7 days away or less, or if the vehicle odometer is 500 km or less from `nextDueKm`.
- When an order is completed, the plan recalculates its next due values from that day's date and odometer.
- `nextDueAt = lastDoneAt + intervalDays` and `nextDueKm = lastDoneKm + intervalKm`.

---

## Endpoints

### 1. `POST /api/maintenance/plans`

**Access:** `MANAGER`, `ADMIN`

**Description:** Creates a maintenance plan for a vehicle. If `lastDoneAt` and `lastDoneKm` are omitted, today's date and the current vehicle odometer are used.

**Request Body:**
```json
{
  "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
  "type": "OIL_CHANGE",
  "intervalKm": 15000,
  "intervalDays": 365,
  "lastDoneAt": "2026-03-10",
  "lastDoneKm": 32000
}
```

**Response `201 Created`:**
```json
{
  "id": "9d4a6e21-0b3c-4a77-8f15-2c6e7b1d3a01",
  "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
  "type": "OIL_CHANGE",
  "intervalKm": 15000,
  "intervalDays": 365,
  "lastDoneAt": "2026-03-10",
  "lastDoneKm": 32000,
  "nextDueAt": "2027-03-10",
  "nextDueKm": 47000,
  "active": true
}
```

**Error responses:**

| Status | `name`                | Description                                         |
|--------|-----------------------|-----------------------------------------------------|
| 404    | `VEHICLE_NOT_FOUND`   | The vehicle does not exist                          |
| 409    | `PLAN_ALREADY_EXISTS` | The vehicle already has an active plan of that type |
| 400    | `VALIDATION_ERROR`    | No interval was provided or a field is invalid      |
| 503    | `SERVICE_UNAVAILABLE` | `ms-vehicles` did not respond                       |

**Response `409 Conflict`:**
```json
{
  "code": 409,
  "name": "PLAN_ALREADY_EXISTS",
  "description": "The vehicle already has an active OIL_CHANGE plan",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

---

### 2. `GET /api/maintenance/plans`

**Access:** `MANAGER`, `ADMIN` and internal calls

**Description:** Lists maintenance plans.

**Query Parameters:**

| Parameter | Type    | Required | Description                |
|-----------|---------|----------|----------------------------|
| `vehicle` | UUID    | No       | Filter by vehicle          |
| `type`    | Enum    | No       | Filter by maintenance type |
| `active`  | Boolean | No       | Filter by active flag      |

**Response `200 OK`:** a list of plans with the same structure as the creation response.

---

### 3. `GET /api/maintenance/orders`

**Access:** `MANAGER`, `ADMIN` and internal calls

**Description:** Paginated list of work orders, ordered by `scheduledFor` ascending.

**Query Parameters:**

| Parameter   | Type                | Required | Description                                       |
|-------------|---------------------|----------|---------------------------------------------------|
| `vehicle`   | UUID                | No       | Filter by vehicle                                 |
| `status`    | Enum                | No       | e.g. `PENDING`                                    |
| `type`      | Enum                | No       | Filter by maintenance type                        |
| `dueBefore` | Date (`YYYY-MM-DD`) | No       | Orders with `scheduledFor` on or before that date |
| `page`      | Integer             | No       | Default `0`                                       |
| `size`      | Integer             | No       | Default `20`, maximum `100`                       |

**Response `200 OK`:**
```json
{
  "content": [
    {
      "id": "e5b8c3f7-91a2-4d60-b7c4-0a1f6d2e8b01",
      "planId": "9d4a6e21-0b3c-4a77-8f15-2c6e7b1d3a02",
      "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
      "type": "OIL_CHANGE",
      "status": "PENDING",
      "scheduledFor": "2026-10-08",
      "cost": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 6,
  "totalPages": 1
}
```

---

### 4. `POST /api/maintenance/orders/{orderId}/start`

**Access:** `MANAGER`, `ADMIN`

**Description:** Starts a pending order. It has no body. It changes the order to `IN_PROGRESS` and the vehicle to `IN_MAINTENANCE` in `ms-vehicles`.

**Response `200 OK`:** the order with `status` set to `IN_PROGRESS` and `startedAt` filled in.

**Error responses:**

| Status | `name`                  | Description                          |
|--------|-------------------------|--------------------------------------|
| 404    | `ORDER_NOT_FOUND`       | No order exists with the provided ID |
| 409    | `INVALID_ORDER_STATE`   | The order is not pending             |
| 409    | `VEHICLE_NOT_AVAILABLE` | The vehicle is in use                |
| 503    | `SERVICE_UNAVAILABLE`   | `ms-vehicles` did not respond        |

---

### 5. `POST /api/maintenance/orders/{orderId}/complete`

**Access:** `MANAGER`, `ADMIN`

**Description:** Completes an order in progress. It records the cost, returns the vehicle to `AVAILABLE` (updating its odometer if the given value is higher) and recalculates the associated plan. `odometerKm` is optional; if missing, the current vehicle odometer is used.

**Request Body:**
```json
{
  "cost": 185.50,
  "odometerKm": 45210,
  "workshop": "Taller Central Madrid",
  "notes": "Oil and filter change"
}
```

**Response `200 OK`:**
```json
{
  "id": "e5b8c3f7-91a2-4d60-b7c4-0a1f6d2e8b01",
  "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
  "type": "OIL_CHANGE",
  "status": "COMPLETED",
  "completedAt": "2026-10-08T09:40:00Z",
  "cost": 185.50,
  "odometerKm": 45210,
  "workshop": "Taller Central Madrid",
  "plan": {
    "id": "9d4a6e21-0b3c-4a77-8f15-2c6e7b1d3a02",
    "nextDueAt": "2027-10-08",
    "nextDueKm": 60210
  }
}
```

**Error responses:**

| Status | `name`                | Description                              |
|--------|-----------------------|------------------------------------------|
| 404    | `ORDER_NOT_FOUND`     | No order exists with the provided ID     |
| 409    | `INVALID_ORDER_STATE` | The order is not in progress             |
| 409    | `INVALID_ODOMETER`    | The odometer is lower than the vehicle's |
| 400    | `VALIDATION_ERROR`    | The cost is negative                     |
| 503    | `SERVICE_UNAVAILABLE` | `ms-vehicles` did not respond            |

---

### 6. `GET /api/maintenance/stats`

**Access:** `MANAGER`, `ADMIN` and internal calls

**Description:** Time series of completed orders and their cost, with a breakdown by type and a count of overdue or soon-due orders. Consumed by `ms-dashboard`.

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
  "totals": { "orders": 9, "cost": 1480.75 },
  "series": [
    { "period": "2026-09-07", "orders": 3, "cost": 420.00 },
    { "period": "2026-09-14", "orders": 4, "cost": 715.25 },
    { "period": "2026-09-21", "orders": 2, "cost": 345.50 }
  ],
  "byType": [
    { "type": "OIL_CHANGE", "orders": 4, "cost": 742.00 },
    { "type": "BRAKE_CHECK", "orders": 3, "cost": 530.75 },
    { "type": "TIRE_ROTATION", "orders": 2, "cost": 208.00 }
  ],
  "upcoming": { "overdue": 1, "dueIn7Days": 5 }
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
  "code": 409,
  "name": "INVALID_ORDER_STATE",
  "description": "The order is not in a state that allows this operation",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

`VALIDATION_ERROR` adds a `details` array with `field` and `reason`. `SERVICE_UNAVAILABLE` adds a `service` field with the name of the unresponsive service.

---

## Inter-Service Communication

| Action            | Calls                                                              |
|-------------------|--------------------------------------------------------------------|
| Create a plan     | `ms-vehicles`: check that the vehicle exists and read its odometer |
| Start an order    | `ms-vehicles`: change the vehicle to `IN_MAINTENANCE`              |
| Complete an order | `ms-vehicles`: change it to `AVAILABLE` and update the odometer    |
| Daily task        | `ms-vehicles`: read the odometer of each vehicle with a plan       |

Feign clients use a 2 s connection / 5 s read timeout, one retry on `GET` only and a circuit breaker per client (50 % of the last 10 calls, 30 s wait). The scheduled task authenticates with `X-Internal-Key`.

---

## Demo Data

With the `demo` profile the seeder generates about **50 plans** and **40 completed orders**, plus **5 pending orders due within the next 7 days** and **1 overdue order**. It only inserts data when the tables are empty.

---

## Environment Variables

| Variable                     | Description                                         | Example                                          |
|------------------------------|-----------------------------------------------------|--------------------------------------------------|
| `SPRING_DATASOURCE_URL`      | PostgreSQL connection URL                           | `jdbc:postgresql://postgres:5432/maintenance_db` |
| `SPRING_DATASOURCE_USERNAME` | Database username                                   | `fleet`                                          |
| `SPRING_DATASOURCE_PASSWORD` | Database password                                   | `change_this_password`                           |
| `JWT_SECRET`                 | Secret to verify JWT signatures (HS256, ≥ 32 chars) | `change_this_secret_of_at_least_32_chars`        |
| `INTERNAL_API_KEY`           | Shared key for internal calls (`X-Internal-Key`)    | `change_this_internal_key`                       |
| `VEHICLES_SERVICE_URL`       | Base URL of `ms-vehicles`                           | `http://ms-vehicles:8082`                        |
| `SPRING_PROFILES_ACTIVE`     | Active Spring profile                               | `demo`                                           |
| `DEMO_SEED`                  | Seed for deterministic demo data                    | `42`                                             |
| `DEMO_DAYS`                  | Days of history to generate                         | `90`                                             |

---

## Notes

- Orders are never created through the API; only the daily task generates them.
- The daily task does not create a second order when the plan already has an open one (`PENDING` or `IN_PROGRESS`).
