# CI/CD Pipeline Design — Backend

## Context

The backend has no CI/CD, no coverage tooling, and no versioning automation. This spec defines a GitHub Actions pipeline that enforces build quality, 80% test coverage, and manual semver versioning on every push.

## Goals

- Build and test on every PR and every push to master
- Enforce ≥80% instruction coverage (hard fail)
- Require a manually bumped version in `pom.xml` on every push to master
- Publish a GitHub Release with the JAR on every new version

## Workflow Triggers

| Event | Jobs |
|---|---|
| PR to master | `build` (compile + test + coverage gate) |
| Push to master | `build` then `release` (version check + tag + GitHub Release) |

## Job: `build`

Steps: checkout → Java 21 setup → `./mvnw verify`

- JaCoCo runs as part of `verify`, fails if instruction coverage < 80%
- On failure: upload JaCoCo HTML report as artifact

## Job: `release` (master push only, needs `build`)

1. Checkout with full git history (`fetch-depth: 0`)
2. Extract version: `mvn help:evaluate -Dexpression=project.version -q -DforceStdout`
3. Get latest tag: `git describe --tags --abbrev=0 2>/dev/null || echo ""`
4. Logic:
   - No tags → first release, proceed
   - `v{version}` == latest tag → fail with "Version not bumped in pom.xml"
   - `v{version}` != latest tag → proceed
5. Create tag `v{version}` and push
6. Build JAR: `./mvnw package -DskipTests`
7. Create GitHub Release via `gh release create` with JAR attached

## Coverage Changes (pom.xml)

Add JaCoCo plugin to `pom.xml`:
- `prepare-agent` goal on `initialize` phase
- `report` goal on `verify` phase  
- `check` goal on `verify` phase with `INSTRUCTION` minimum ratio `0.80`

## Tests to Add

To reach 80% coverage, add:
- `AuthControllerTest` — login (200/401), register (201/409), refresh (200/401)
- `AdminControllerTest` — create tenant (ADMIN=201, non-ADMIN=403), list tenants (200)

Existing tests already cover: `CarController`, `ReservationController`, `ReservationService`, `Reservation.overlaps()`.
