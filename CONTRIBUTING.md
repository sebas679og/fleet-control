# Contributing to FleetControl

Thank you for being part of this project. This document defines the workflows, conventions, and standards every contributor must follow to keep the codebase clean, consistent, and easy to collaborate on. Please read it carefully before opening your first pull request.

---

## Table of Contents

- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Clone the Repository](#clone-the-repository)
    - [Environment Configuration](#environment-configuration)
    - [Running the Full Stack](#running-the-full-stack)
- [Development Workflow](#development-workflow)
    - [Branch Strategy](#branch-strategy)
    - [Branch Naming Conventions](#branch-naming-conventions)
    - [Keeping Your Branch Up to Date](#keeping-your-branch-up-to-date)
- [Commit Conventions](#commit-conventions)
- [Linters and Tests](#linters-and-tests)
- [Pull Requests](#pull-requests)
- [Versioning](#versioning)

---

## Getting Started

### Prerequisites

Make sure the following tools are installed on your machine before proceeding:

| Tool           | Minimum Version |
|----------------|-----------------|
| Git            | 2.x             |
| Docker         | 24.x            |
| Docker Compose | 2.x             |
| Java (JDK)     | 25              |
| Maven          | 3.9.x           |

---

### Clone the Repository

```bash
git clone https://github.com/sebas679og/fleet-control
cd fleet-control
```

---

### Environment Configuration

The project uses a `.env` file at the repository root to supply secrets and environment-specific values to Docker Compose. A template is provided — **never commit your actual `.env` file**.

1. Copy the template:

```bash
cp .env.example .env
```

2. Open `.env` and edit the values as needed.

> [!IMPORTANT]
> `.env` is already listed in `.gitignore`. If it ever shows up in `git status`, do not stage or commit it.

---

### Running the Full Stack

Once your `.env` is ready, spin up the entire platform — all 9 microservices and their associated PostgreSQL databases — with a single command:

```bash
docker compose up --build
```

To run in detached mode (background):

```bash
docker compose up --build -d
```

To stop and remove all containers:

```bash
docker compose down
```

To also remove volumes (wipes database data):

```bash
docker compose down -v
```

---

## Development Workflow

### Branch Strategy

The project follows a three-tier branching model:

```
feat/* ──► dev ──► main
```

| Branch                  | Purpose                                                         |
|-------------------------|-----------------------------------------------------------------|
| `main`                  | Stable, production-ready code. Only receives merges from `dev`. |
| `dev`                   | Integration branch. All feature branches merge here first.      |
| `feat/*`, `fix/*`, etc. | Short-lived branches for individual changes.                    |

**Key rules:**
- **Never push directly to `main`**
- All work happens on a feature/fix branch and enters the codebase through a reviewed pull request into `dev`.
- `main` is only updated from `dev` when the team decides to release.

---

### Branch Naming Conventions

Every branch must be created from `main` and follow this naming format:

```
<prefix>/<short-description-in-kebab-case>
```

| Prefix      | When to use                                               |
|-------------|-----------------------------------------------------------|
| `feat/`     | A new feature or endpoint                                 |
| `fix/`      | A bug fix                                                 |
| `refactor/` | Code restructuring with no behaviour change               |
| `docs/`     | Documentation-only changes                                |
| `chore/`    | Maintenance tasks: dependencies, CI config, build scripts |

**Examples:**

```bash
feat/add-vehicle-status-endpoint
fix/jwt-expiration-not-returned-on-login
refactor/extract-jwt-filter-to-dedicated-service
docs/update-env-variables-in-readme
chore/upgrade-spring-boot-to-4-0
```

To create a new branch:

```bash
git checkout main
git pull origin main
git checkout -b feat/your-feature-name
```

---

### Keeping Your Branch Up to Date

All feature branches must stay in sync with `dev` using **rebase** — not merge — to maintain a clean, linear history.

```bash
git fetch origin
git rebase origin/dev
```

If conflicts arise during rebase, resolve them file by file, then continue:

```bash
# After resolving each conflict:
git add <resolved-file>
git rebase --continue
```

To abort a rebase and return to your previous state:

```bash
git rebase --abort
```

> Rebase rewrites history. If you have already pushed your branch, you will need to force-push after rebasing:
> ```bash
> git push --force-with-lease origin feat/your-feature-name
> ```
> Use `--force-with-lease` instead of `--force` — it is safer and will refuse to overwrite if someone else has pushed to your branch.

---

## Commit Conventions

Commits must be **atomic** (one logical change per commit) and follow this format:

```
<type>: <short description in imperative mood>

<optional body — explain the why, not the what, if the change
requires additional context.>
```

| Type       | When to use                                   |
|------------|-----------------------------------------------|
| `feat`     | Introducing new functionality                 |
| `fix`      | Correcting a bug                              |
| `refactor` | Restructuring code without changing behaviour |
| `docs`     | Documentation changes only                    |
| `chore`    | Build system, dependencies, CI/CD, config     |
| `test`     | Adding or updating tests                      |
| `style`    | Formatting, whitespace — no logic changes     |

**Rules:**
- The description must be in **imperative mood**: _"add endpoint"_, not _"added endpoint"_ or _"adds endpoint"_.
- Keep the first line under **72 characters**.
- Use English or Spanish consistently **within the same branch**. Do not mix languages.
- Do not end the first line with a period.

**Examples:**

```
feat: add vehicle status transition endpoint to ms-vehicles

Implements PATCH /api/vehicles/{vehicleId}/status. The endpoint
validates the allowed state transitions and updates the odometer
only when the new value is greater than the current one.
```

```
fix: correct fuel consumption calculation between full tanks
```

```
refactor: extract JWT logic into dedicated JwtService class
```

```
docs: update environment variables table in README
```

```
chore: bump Spring Boot to 4.0.0
```

```
test: add unit tests for MaintenanceService
```

---

## Linters and Tests

Before opening any PR, it is **mandatory** to run Spotless locally to ensure that the code follows the project's defined style.
Remember that to apply validations, you must navigate to the affected microservice directory and execute the commands from there.

Applies the formatter automatically — **mandatory step before any PR**:

```bash
./mvnw spotless:apply
```

Checks that the code already complies with the format without modifying it:

```bash
./mvnw spotless:check
```
> [!IMPORTANT]
> If `spotless:check` fails in the pipeline, the PR will be rejected. Always run `spotless:apply` before pushing to avoid this.

Verifies that the code complies with predefined style rules and best practices:

```bash
./mvnw checkstyle:check
```

> [!IMPORTANT]
> If `checkstyle:check` fails, the PR will also be rejected. Make sure to fix the reported issues before pushing.

Performs static code analysis to detect quality issues, common errors, and bad practices:

```bash
./mvnw pmd:check
```

> [!IMPORTANT]
> If `pmd:check` fails, the PR will also be rejected. Make sure to fix the reported issues before pushing.

### Run Tests Only

Runs tests while skipping Spotless, Checkstyle, and PMD validations:

```bash
./mvnw -B clean verify "-Dspotless.check.skip=true" "-Dcheckstyle.skip=true" "-Dpmd.skip=true"
```

### Linters + Tests Together

Runs the formatter and tests in a single step:

```bash
./mvnw -B clean verify
```

> [!TIP]
> It is recommended to run this command before every PR to ensure both style and tests pass successfully.

---

## Pull Requests

### From `feat/` to `dev`

1. Make sure your branch is up to date with `dev`:

```bash
git fetch origin
git rebase origin/dev
```

2. Run linters and tests locally (see previous section) before opening the PR.
3. Open the PR on GitHub targeting the `dev` branch.
4. The PR title must follow the same format as commits.
5. Briefly describe what changes were made and why.
6. Assign at least one reviewer from the team.

### From `dev` to `main`

Only project maintainers should open PRs from `dev` to `main`. Once merged, the GitHub Actions workflow will automatically build and publish the Docker images.

---

### PR Checklist

Before requesting a review, verify the following:

- [ ] Branch is rebased on top of `origin/dev`.
- [ ] All existing tests pass locally (`./mvnw verify`).
- [ ] New functionality includes at least the minimum required tests.
- [ ] Swagger/OpenAPI annotations are updated if endpoints changed.
- [ ] The `pom.xml` version has been bumped appropriately (see [Versioning](#versioning)).
- [ ] No secrets, `.env` files, or credentials are included in the diff.
- [ ] PR title follows the commit convention format.
- [ ] At least one reviewer has been assigned.

---

## Versioning

FleetControl follows **[Semantic Versioning](https://semver.org/)** (`MAJOR.MINOR.PATCH`). Each microservice is versioned independently via its own `pom.xml`.

| Change type                        | Version bump | Example         |
|------------------------------------|--------------|-----------------|
| Bug fix                            | Patch        | `0.1.0 → 0.1.1` |
| New feature (backwards-compatible) | Minor        | `0.1.0 → 0.2.0` |
| Breaking change                    | Major        | `0.1.0 → 1.0.0` |

**Always bump the version before opening a PR** for the affected microservice. Use the Maven Versions plugin to avoid manual edits:

```bash
cd ms-<service-name>
./mvnw versions:set "-DnewVersion=0.2.0" "-DgenerateBackupPoms=false"
```

Then commit the version bump as a separate, dedicated commit:

```bash
git add pom.xml
git commit -m "chore: bump version to 0.2.0"
```

> Keeping the version bump in its own commit makes it easy to identify in the history and avoids cluttering the meaningful change commits.

This ensures that Docker images published to the registry always reflect the exact version of the code they contain, making rollbacks and deployments traceable and predictable.
