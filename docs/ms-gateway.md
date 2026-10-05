# ⚪ ms-gateway — API Gateway

[← Back to README](../README.md)

## Overview

**Port:** `8080`  
**Database:** None (stateless)

The `ms-gateway` service is the single entry point to FleetControl. It routes each request to the right microservice, applies global rate limiting, generates the `X-Request-Id` header when it is missing and strips `X-Internal-Key` from every external request so that no client can impersonate an internal service. It **does not validate JWT tokens**: each downstream service validates them on its own.

---

## Routing Table

| Path prefix           | Destination           |
|-----------------------|-----------------------|
| `/api/auth/**`        | `ms-auth:8081`        |
| `/api/vehicles/**`    | `ms-vehicles:8082`    |
| `/api/drivers/**`     | `ms-drivers:8083`     |
| `/api/routes/**`      | `ms-routes:8085`      |
| `/api/maintenance/**` | `ms-maintenance:8086` |
| `/api/fuel/**`        | `ms-fuel:8087`        |
| `/api/alerts/**`      | `ms-alerts:8088`      |
| `/api/dashboard/**`   | `ms-dashboard:8089`   |

---

## Endpoints

### 1. `ANY /api/**`

**Access:** Public (the destination service validates the JWT)

**Description:** Reverse proxy. Forwards the request to the matching service and returns the downstream response unmodified. If the destination does not answer within 5 seconds, the gateway returns `503`.

**Header handling:**

| Header           | Behavior                                             |
|------------------|------------------------------------------------------|
| `X-Request-Id`   | Generated (UUID) when the request does not carry one |
| `X-Internal-Key` | Always removed from external requests                |
| `Authorization`  | Forwarded untouched                                  |

**Error responses:**

| Status | `name`                | Description                                      |
|--------|-----------------------|--------------------------------------------------|
| 429    | `RATE_LIMIT_EXCEEDED` | The IP exceeded the gateway limit                |
| 503    | `SERVICE_UNAVAILABLE` | The destination did not respond within 5 seconds |

**Response `503 Service Unavailable`:**
```json
{
  "code": 503,
  "name": "SERVICE_UNAVAILABLE",
  "description": "The service did not respond in time",
  "service": "ms-vehicles",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

---

### 2. `GET /health`

**Access:** Public

**Description:** Reports the state of the gateway and of every downstream service. It queries each service's `/actuator/health` with a 2-second timeout and marks as `DOWN` those that do not answer. It **always returns `200`**.

**Response `200 OK`:**
```json
{
  "gateway": "UP",
  "timestamp": "2026-10-05T10:30:00Z",
  "services": {
    "ms-auth": "UP",
    "ms-vehicles": "UP",
    "ms-drivers": "UP",
    "ms-routes": "UP",
    "ms-maintenance": "UP",
    "ms-fuel": "UP",
    "ms-alerts": "UP",
    "ms-dashboard": "DOWN"
  }
}
```

---

## Rate Limiting

- **Limit:** 60 requests per minute per IP (configurable).
- The counter resets once the minute has elapsed.

**Response `429 Too Many Requests`** (includes the `Retry-After` header):
```json
{
  "code": 429,
  "name": "RATE_LIMIT_EXCEEDED",
  "description": "Too many requests. Limit: 60 req/min",
  "retryAfter": 30,
  "timestamp": "2026-10-05T10:30:00Z"
}
```

---

## Error Contract

All controlled errors use the same JSON structure:

```json
{
  "code": 503,
  "name": "SERVICE_UNAVAILABLE",
  "description": "The service did not respond in time",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

| Field         | Description                |
|---------------|----------------------------|
| `code`        | HTTP status code           |
| `name`        | Business error identifier  |
| `description` | Human-readable explanation |
| `timestamp`   | ISO-8601 instant in UTC    |

Additional fields: `service` on `SERVICE_UNAVAILABLE` and `retryAfter` on `RATE_LIMIT_EXCEEDED`.

---

## Environment Variables

| Variable                  | Description                        | Example                      |
|---------------------------|------------------------------------|------------------------------|
| `AUTH_SERVICE_URL`        | Base URL of `ms-auth`              | `http://ms-auth:8081`        |
| `VEHICLES_SERVICE_URL`    | Base URL of `ms-vehicles`          | `http://ms-vehicles:8082`    |
| `DRIVERS_SERVICE_URL`     | Base URL of `ms-drivers`           | `http://ms-drivers:8083`     |
| `ROUTES_SERVICE_URL`      | Base URL of `ms-routes`            | `http://ms-routes:8085`      |
| `MAINTENANCE_SERVICE_URL` | Base URL of `ms-maintenance`       | `http://ms-maintenance:8086` |
| `FUEL_SERVICE_URL`        | Base URL of `ms-fuel`              | `http://ms-fuel:8087`        |
| `ALERTS_SERVICE_URL`      | Base URL of `ms-alerts`            | `http://ms-alerts:8088`      |
| `DASHBOARD_SERVICE_URL`   | Base URL of `ms-dashboard`         | `http://ms-dashboard:8089`   |
| `RATE_LIMIT_PER_MINUTE`   | Maximum requests per minute per IP | `60`                         |

---

## Notes

- The gateway never trusts `X-Internal-Key` coming from outside: it is removed before forwarding.
- `/actuator/health` and Swagger UI of each service are reachable directly on their own ports during development.
- `POST /api/auth/validate` is routed by the prefix `/api/auth/**`; it is meant for internal use and diagnostics.
