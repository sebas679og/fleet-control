# 🔴 ms-alerts — Alerts Service

[← Back to README](../README.md)

## Overview

**Port:** `8088`  
**Database:** PostgreSQL (`alerts_db`)

The `ms-alerts` service detects situations that need attention and turns them into alerts: upcoming or overdue maintenance, driver licenses about to expire or already expired, and abnormal fuel consumption. A scheduled evaluator queries `ms-maintenance`, `ms-drivers` and `ms-fuel`, and uses `ms-vehicles` to complete messages with the plate.

---

## Data Model

### Entity: `Alert`

| Field            | Type    | Description                                    |
|------------------|---------|------------------------------------------------|
| `id`             | UUID    | Unique identifier                              |
| `type`           | Enum    | Alert type                                     |
| `severity`       | Enum    | Severity                                       |
| `status`         | Enum    | Alert status                                   |
| `vehicleId`      | UUID    | Affected vehicle (optional)                    |
| `driverId`       | UUID    | Affected driver (optional)                     |
| `message`        | String  | Human-readable message                         |
| `dedupKey`       | String  | Unique key of the condition that originated it |
| `createdAt`      | Instant | Creation timestamp                             |
| `acknowledgedAt` | Instant | Acknowledgement timestamp                      |
| `acknowledgedBy` | UUID    | User who acknowledged it                       |
| `comment`        | String  | Acknowledgement comment                        |
| `resolvedAt`     | Instant | Resolution timestamp                           |

### Entity: `AlertRule`

| Field       | Type    | Description                                  |
|-------------|---------|----------------------------------------------|
| `type`      | Enum    | Alert type the rule applies to (primary key) |
| `enabled`   | Boolean | Whether the rule is active                   |
| `severity`  | Enum    | Severity assigned to the alerts              |
| `params`    | JSON    | Rule parameters                              |
| `updatedAt` | Instant | Last modification timestamp                  |

### Enums

```
AlertType:     MAINTENANCE_DUE, MAINTENANCE_OVERDUE, LICENSE_EXPIRING, LICENSE_EXPIRED, HIGH_CONSUMPTION
AlertSeverity: INFO, WARNING, CRITICAL
AlertStatus:   OPEN, ACKNOWLEDGED, RESOLVED
```

---

## Evaluation Rules

| Alert type            | Condition                                                                                            | Default severity | Parameters             |
|-----------------------|------------------------------------------------------------------------------------------------------|------------------|------------------------|
| `MAINTENANCE_DUE`     | `PENDING` order with `scheduledFor` between today and the next `daysBefore` days                     | `WARNING`        | `daysBefore: 7`        |
| `MAINTENANCE_OVERDUE` | `PENDING` order with `scheduledFor` before today                                                     | `CRITICAL`       | —                      |
| `LICENSE_EXPIRING`    | License expiring in `daysBefore` days or less                                                        | `WARNING`        | `daysBefore: 30`       |
| `LICENSE_EXPIRED`     | License with an expiration date before today                                                         | `CRITICAL`       | —                      |
| `HIGH_CONSUMPTION`    | Average consumption of the last 30 days exceeds by `thresholdPercent` % that of the previous 60 days | `WARNING`        | `thresholdPercent: 25` |

**Evaluator behavior:**

- Runs every 15 minutes (`ALERTS_EVALUATION_INTERVAL_MS`, default `900000`), with an initial delay of 60 seconds to wait for the other services to start.
- Each alert has a unique `dedupKey` (e.g. `MAINTENANCE_DUE:<orderId>`), so the same condition never creates duplicate alerts.
- When the condition stops being met, the alert becomes `RESOLVED` on the next evaluation.
- If a dependency does not respond, the evaluator skips that rule in that round and continues with the others.
- `ms-alerts` does not generate demo data: the evaluator creates alerts from the other services.

---

## Endpoints

### 1. `GET /api/alerts`

**Access:** `MANAGER`, `ADMIN` and internal calls

**Description:** Paginated list of alerts, ordered by severity descending and creation date descending.

**Query Parameters:**

| Parameter  | Type    | Required | Description                 |
|------------|---------|----------|-----------------------------|
| `status`   | Enum    | No       | e.g. `OPEN`                 |
| `severity` | Enum    | No       | Filter by severity          |
| `type`     | Enum    | No       | Filter by alert type        |
| `vehicle`  | UUID    | No       | Filter by vehicle           |
| `page`     | Integer | No       | Default `0`                 |
| `size`     | Integer | No       | Default `20`, maximum `100` |

**Response `200 OK`:**
```json
{
  "content": [
    {
      "id": "d1a5c7e9-3f42-4b8a-a6d0-9e2b4c8f1a01",
      "type": "MAINTENANCE_OVERDUE",
      "severity": "CRITICAL",
      "status": "OPEN",
      "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a05",
      "driverId": null,
      "message": "OIL_CHANGE maintenance overdue since 2026-09-28 on vehicle 3342-HJK",
      "createdAt": "2026-10-05T06:15:00Z",
      "acknowledgedAt": null
    },
    {
      "id": "d1a5c7e9-3f42-4b8a-a6d0-9e2b4c8f1a02",
      "type": "LICENSE_EXPIRING",
      "severity": "WARNING",
      "status": "ACKNOWLEDGED",
      "vehicleId": null,
      "driverId": "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a02",
      "message": "The license of Miguel Torres expires on 2026-10-20",
      "createdAt": "2026-10-04T06:15:00Z",
      "acknowledgedAt": "2026-10-04T09:12:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 12,
  "totalPages": 1
}
```

---

### 2. `POST /api/alerts/{alertId}/acknowledge`

**Access:** `MANAGER`, `ADMIN`

**Description:** Marks an open alert as acknowledged. The body is optional.

**Request Body:**
```json
{
  "comment": "Appointment booked with the workshop for Thursday"
}
```

**Response `200 OK`:** the alert with `status` set to `ACKNOWLEDGED`, and `acknowledgedAt` and `acknowledgedBy` filled in.

**Error responses:**

| Status | `name`                | Description                          |
|--------|-----------------------|--------------------------------------|
| 404    | `ALERT_NOT_FOUND`     | No alert exists with the provided ID |
| 409    | `INVALID_ALERT_STATE` | The alert is not `OPEN`              |

**Response `409 Conflict`:**
```json
{
  "code": 409,
  "name": "INVALID_ALERT_STATE",
  "description": "Only OPEN alerts can be acknowledged",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

---

### 3. `GET /api/alerts/rules`

**Access:** `MANAGER`, `ADMIN`

**Description:** Lists the configuration of the evaluation rules.

**Response `200 OK`:**
```json
[
  { "type": "MAINTENANCE_DUE", "enabled": true, "severity": "WARNING", "params": { "daysBefore": 7 } },
  { "type": "MAINTENANCE_OVERDUE", "enabled": true, "severity": "CRITICAL", "params": {} },
  { "type": "LICENSE_EXPIRING", "enabled": true, "severity": "WARNING", "params": { "daysBefore": 30 } },
  { "type": "LICENSE_EXPIRED", "enabled": true, "severity": "CRITICAL", "params": {} },
  { "type": "HIGH_CONSUMPTION", "enabled": true, "severity": "WARNING", "params": { "thresholdPercent": 25 } }
]
```

---

### 4. `PUT /api/alerts/rules/{type}`

**Access:** `ADMIN`

**Description:** Modifies a rule: enable or disable it, change its severity or adjust its parameters. The change applies on the next evaluation.

**Request Body:**
```json
{
  "enabled": true,
  "severity": "CRITICAL",
  "params": { "daysBefore": 14 }
}
```

**Response `200 OK`:** the updated rule.

**Error responses:**

| Status | `name`             | Description                                 |
|--------|--------------------|---------------------------------------------|
| 404    | `RULE_NOT_FOUND`   | No rule exists for that type                |
| 400    | `VALIDATION_ERROR` | A parameter is not valid for that rule type |
| 403    | `FORBIDDEN`        | The user is a `MANAGER`                     |

---

### 5. `POST /api/alerts/evaluate`

**Access:** `ADMIN`

**Description:** Triggers an immediate evaluation without waiting for the scheduled evaluator. Useful to test the rules.

**Response `200 OK`:**
```json
{
  "created": 3,
  "resolved": 1,
  "skipped": ["HIGH_CONSUMPTION"],
  "durationMs": 842
}
```

`skipped` lists the rule types that were not evaluated because a dependency did not respond.

---

### 6. `GET /api/alerts/stats`

**Access:** `MANAGER`, `ADMIN` and internal calls

**Description:** Time series of created alerts with a breakdown by severity, and the count of open alerts. Consumed by `ms-dashboard`.

**Query Parameters:**

| Parameter     | Type                | Required | Description                        |
|---------------|---------------------|----------|------------------------------------|
| `from`        | Date (`YYYY-MM-DD`) | Yes      | Range start                        |
| `to`          | Date (`YYYY-MM-DD`) | Yes      | Range end                          |
| `granularity` | Enum                | No       | `DAY` (default), `WEEK` or `MONTH` |

**Series rules:** `period` is the start date of the period (the day for `DAY`, the Monday for `WEEK`, day 1 for `MONTH`); empty periods return `0`; dates are UTC.

**Response `200 OK`:**
```json
{
  "from": "2026-09-07",
  "to": "2026-09-27",
  "granularity": "WEEK",
  "totals": { "created": 11 },
  "series": [
    { "period": "2026-09-07", "created": 3, "bySeverity": { "INFO": 0, "WARNING": 2, "CRITICAL": 1 } },
    { "period": "2026-09-14", "created": 5, "bySeverity": { "INFO": 0, "WARNING": 3, "CRITICAL": 2 } },
    { "period": "2026-09-21", "created": 3, "bySeverity": { "INFO": 0, "WARNING": 2, "CRITICAL": 1 } }
  ],
  "open": { "INFO": 0, "WARNING": 9, "CRITICAL": 2 }
}
```

---

## Common Errors

| Status | `name`          | Description                                                    |
|--------|-----------------|----------------------------------------------------------------|
| 401    | `UNAUTHORIZED`  | Missing, malformed or invalid token, or wrong `X-Internal-Key` |
| 401    | `TOKEN_EXPIRED` | The token has expired                                          |
| 403    | `FORBIDDEN`     | The user's role does not have permission                       |

Unlike the other domain services, `ms-alerts` does not fail when a dependency is down: it skips the affected rule and reports it in `skipped`.

---

## Error Contract

All controlled errors use the same JSON structure:

```json
{
  "code": 404,
  "name": "ALERT_NOT_FOUND",
  "description": "No alert exists with the provided ID",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

`VALIDATION_ERROR` adds a `details` array with `field` and `reason`.

---

## Inter-Service Communication

| Action           | Calls                                                                                                   |
|------------------|---------------------------------------------------------------------------------------------------------|
| Alert evaluation | `ms-maintenance`: pending orders. `ms-drivers`: licenses. `ms-fuel`: consumption. `ms-vehicles`: plates |

Feign clients use a 2 s connection / 5 s read timeout, one retry on `GET` only and a circuit breaker per client (50 % of the last 10 calls, 30 s wait). The scheduled evaluator authenticates with `X-Internal-Key`.

---

## Environment Variables

| Variable                        | Description                                         | Example                                     |
|---------------------------------|-----------------------------------------------------|---------------------------------------------|
| `SPRING_DATASOURCE_URL`         | PostgreSQL connection URL                           | `jdbc:postgresql://postgres:5432/alerts_db` |
| `SPRING_DATASOURCE_USERNAME`    | Database username                                   | `fleet`                                     |
| `SPRING_DATASOURCE_PASSWORD`    | Database password                                   | `change_this_password`                      |
| `JWT_SECRET`                    | Secret to verify JWT signatures (HS256, ≥ 32 chars) | `change_this_secret_of_at_least_32_chars`   |
| `INTERNAL_API_KEY`              | Shared key for internal calls (`X-Internal-Key`)    | `change_this_internal_key`                  |
| `VEHICLES_SERVICE_URL`          | Base URL of `ms-vehicles`                           | `http://ms-vehicles:8082`                   |
| `DRIVERS_SERVICE_URL`           | Base URL of `ms-drivers`                            | `http://ms-drivers:8083`                    |
| `MAINTENANCE_SERVICE_URL`       | Base URL of `ms-maintenance`                        | `http://ms-maintenance:8086`                |
| `FUEL_SERVICE_URL`              | Base URL of `ms-fuel`                               | `http://ms-fuel:8087`                       |
| `ALERTS_EVALUATION_INTERVAL_MS` | Evaluator interval in milliseconds                  | `900000`                                    |
| `SPRING_PROFILES_ACTIVE`        | Active Spring profile                               | `demo`                                      |

---

## Notes

- Rule changes made through `PUT /api/alerts/rules/{type}` take effect on the next evaluation, not immediately.
- Use `POST /api/alerts/evaluate` to test the rules on demand instead of waiting for the scheduler.
- Alerts are never deleted; when the condition disappears they move to `RESOLVED`.
