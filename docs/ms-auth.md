# 🟣 ms-auth — Authentication Service

[← Back to README](../README.md)

## Overview

**Port:** `8081`  
**Database:** PostgreSQL (`auth_db`)

The `ms-auth` service manages user registration and authentication. It issues JWT tokens (HS256, valid for 1 hour) that all domain microservices validate on every protected request. The token carries the claims `sub` (user id), `username` and `role`.

---

## Data Model

### Entity: `User`

| Field       | Type    | Description                  |
|-------------|---------|------------------------------|
| `id`        | UUID    | Unique identifier            |
| `username`  | String  | Username (unique, no spaces) |
| `email`     | String  | Email address (unique)       |
| `password`  | String  | Bcrypt hash of the password  |
| `role`      | Enum    | User role                    |
| `createdAt` | Instant | Registration timestamp       |
| `updatedAt` | Instant | Last update timestamp        |

### Enums

```
UserRole: MANAGER, ADMIN
```

### Roles

| Role      | Permissions                                                                                                                    |
|-----------|--------------------------------------------------------------------------------------------------------------------------------|
| `MANAGER` | Manages vehicles, drivers, routes, refuels and maintenance; reads the dashboard; acknowledges alerts. Assigned on registration |
| `ADMIN`   | Everything a `MANAGER` can do, plus editing alert rules and triggering manual evaluations. Created by the demo seeder          |

---

## Endpoints

### 1. `POST /api/auth/register`

**Access:** Public

**Description:** Registers a new user with the `MANAGER` role.

**Request Body:**
```json
{
  "username": "carlos_ruiz",
  "email": "carlos@fleetcontrol.com",
  "password": "FleetPass123!"
}
```

**Validation Rules:**
- Email must be unique and in a valid format.
- Password must be at least 8 characters, contain at least one uppercase letter and one number.
- Username must be unique and contain no spaces.

**Response `201 Created`:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "carlos_ruiz",
  "email": "carlos@fleetcontrol.com",
  "role": "MANAGER",
  "createdAt": "2026-10-05T10:30:00Z"
}
```

**Response `409 Conflict`** — when the email or username is already taken:
```json
{
  "code": 409,
  "name": "USER_ALREADY_EXISTS",
  "description": "A user with that email already exists",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

**Response `400 Bad Request`** — when a field does not meet the validation rules:
```json
{
  "code": 400,
  "name": "VALIDATION_ERROR",
  "description": "The request contains invalid fields",
  "details": [
    { "field": "password", "reason": "must contain at least one uppercase letter" }
  ],
  "timestamp": "2026-10-05T10:30:00Z"
}
```

---

### 2. `POST /api/auth/login`

**Access:** Public

**Description:** Authenticates a user and returns a JWT token.

**Request Body:**
```json
{
  "email": "carlos@fleetcontrol.com",
  "password": "FleetPass123!"
}
```

**Response `200 OK`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "role": "MANAGER"
}
```

**Response `401 Unauthorized`:**
```json
{
  "code": 401,
  "name": "INVALID_CREDENTIALS",
  "description": "Incorrect email or password",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

---

### 3. `POST /api/auth/validate`

**Access:** Internal (consumed by other services or diagnostic tools)

**Description:** Validates a JWT token and returns the associated user information. Domain services validate tokens locally with `JWT_SECRET`; this endpoint is available for remote checks.

**Required Header:**
```
Authorization: Bearer <token>
```

**Response `200 OK`:**
```json
{
  "valid": true,
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "carlos_ruiz",
  "role": "MANAGER"
}
```

**Response `401 Unauthorized`:**
```json
{
  "code": 401,
  "name": "TOKEN_EXPIRED",
  "description": "The token has expired",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

| Case                     | Status | `name`          |
|--------------------------|--------|-----------------|
| Missing `Authorization`  | 401    | `UNAUTHORIZED`  |
| Malformed or invalid JWT | 401    | `UNAUTHORIZED`  |
| Expired JWT              | 401    | `TOKEN_EXPIRED` |

---

## JWT Validation Strategy

Other microservices can validate JWT tokens in two ways:

- **Locally (default):** verify the HS256 signature with the shared `JWT_SECRET` and read the claims `sub`, `username` and `role`. The role decides access.
- **Remotely:** call `POST /api/auth/validate` on `ms-auth`.

Internal service-to-service calls and scheduled tasks use the `X-Internal-Key` header instead of a user token; the receiving service then treats the call with `MANAGER` permissions.

---

## Demo Data

With `SPRING_PROFILES_ACTIVE=demo`, the seeder creates the user `admin@fleetcontrol.com` with the `ADMIN` role. Its password is read from `DEMO_ADMIN_PASSWORD`. The seeder is idempotent: it only inserts data when the tables are empty.

---

## Error Contract

All controlled errors use the same JSON structure:

```json
{
  "code": 409,
  "name": "USER_ALREADY_EXISTS",
  "description": "A user with that email already exists",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

| Field         | Description                |
|---------------|----------------------------|
| `code`        | HTTP status code           |
| `name`        | Business error identifier  |
| `description` | Human-readable explanation |
| `timestamp`   | ISO-8601 instant in UTC    |

`VALIDATION_ERROR` adds a `details` array with `field` and `reason`.

---

## Environment Variables

| Variable                     | Description                                                | Example                                   |
|------------------------------|------------------------------------------------------------|-------------------------------------------|
| `SPRING_DATASOURCE_URL`      | PostgreSQL connection URL for the auth service             | `jdbc:postgresql://postgres:5432/auth_db` |
| `SPRING_DATASOURCE_USERNAME` | Database username                                          | `fleet`                                   |
| `SPRING_DATASOURCE_PASSWORD` | Database password                                          | `change_this_password`                    |
| `JWT_SECRET`                 | Secret used to sign tokens (HS256); at least 32 characters | `change_this_secret_of_at_least_32_chars` |
| `JWT_EXPIRATION`             | Token expiry in milliseconds                               | `3600000`                                 |
| `INTERNAL_API_KEY`           | Shared key for internal service-to-service calls           | `change_this_internal_key`                |
| `DEMO_ADMIN_PASSWORD`        | Password of the demo `ADMIN` user                          | `Change_Admin123`                         |
| `SPRING_PROFILES_ACTIVE`     | Active Spring profile (`demo` enables the seeder)          | `demo`                                    |
| `DEMO_SEED`                  | Seed for deterministic demo data                           | `42`                                      |
| `DEMO_DAYS`                  | Days of history to generate                                | `90`                                      |

---

## Notes

- Passwords are never stored in plain text; bcrypt hashing is mandatory.
- The token lifetime is **3600 seconds (1 hour)**; `expiresIn` in the login response is expressed in seconds.
- Registration always assigns `MANAGER`; only the seeder creates `ADMIN` users.
- `POST /api/auth/validate` should be treated as internal.
