<<<<<<< HEAD
# Agentic Engineering Workbench + URL Shortener

A local starter project built with Java 21, Spring Boot 3.5, Maven, Spring Data JPA, H2, and a single-page HTML/CSS/JavaScript dashboard.

## Features

### URL shortener
- Create short links for valid HTTP/HTTPS URLs.
- Optional expiration timestamp.
- Redirect through `GET /{7-character-code}`.
- Click count and last-clicked timestamp are updated on redirect requests.
- List URLs and aggregate metrics for the analytics dashboard.
- Individual URL analytics endpoint displayed in an on-page details panel (not just a browser alert).
- Choose an optional local expiry date/time when creating a URL; expired links return HTTP 410 and are counted as expired in aggregate metrics.
- The dashboard refreshes URL analytics when you return to the browser tab.

### Engineering workbench
- Create workflow plans from a requirement.
- Greenfield, brownfield, and ambiguous scenario classification.
- Deterministic planner fallback that does not require an AI API key.
- Dependency-aware task execution; independent ready tasks are dispatched concurrently.
- Retry tracking, simulated failure testing, and safe-stop behavior.
- Optional approval gate; reject, clarify, replan, resume, and rollback-request actions.
- Workflow/task persistence and audit events.

## Requirements
- JDK 21
- Maven 3.9+

## Run in Eclipse or from a terminal

1. Extract the ZIP file.
2. In Eclipse: **File → Import → Maven → Existing Maven Projects**, select the extracted `agentic-workbench` folder, and finish.
3. Ensure Eclipse uses a JDK 21 installation.
4. Run `WorkbenchApplication.java` as **Java Application**, or execute:

   ```bash
   mvn spring-boot:run
   ```

5. Open `http://localhost:8080`.
6. H2 console (optional): `http://localhost:8080/h2-console`; JDBC URL `jdbc:h2:file:./data/workbenchdb;DB_CLOSE_ON_EXIT=FALSE`, username `sa`, blank password.

The H2 database is file-based so data persists across application restarts. For a clean reset, stop the application and remove the local `data` directory.

## API quick reference

### URLs
- `POST /api/urls` — body `{"url":"https://example.com","expiresAt":"2027-01-01T00:00:00Z"}`; `expiresAt` is optional.
- `GET /api/urls` — returns `{ "urls": [...], "metrics": {...} }`.
- `GET /api/url-metrics` — aggregate URL statistics.
- `GET /api/urls/{code}/analytics` — details for one short URL.
- `GET /{code}` — redirects to the original URL and increments click analytics.

### Workflows
- `GET /api/metrics`
- `GET /api/workflows`
- `POST /api/workflows` — body `{"name":"Example","requirement":"Build a new service","scenario":"AUTO","approvalRequired":false,"safeStop":true}`.
- `GET /api/workflows/{id}`
- `GET /api/workflows/{id}/audit`
- `POST /api/workflows/{id}/run`
- `POST /api/workflows/{id}/resume`
- `POST /api/workflows/{id}/approve`
- `POST /api/workflows/{id}/reject` — optional body `{"reason":"Not ready"}`.
- `POST /api/workflows/{id}/clarify` — body `{"answer":"Use PostgreSQL and REST APIs"}`.
- `POST /api/workflows/{id}/replan`
- `POST /api/workflows/{id}/rollback`

## Try these scenarios

1. **Greenfield:** create a workflow with scenario `AUTO` and requirement `Build a customer management REST API with validation and tests`.
2. **Brownfield:** use `AUTO` and requirement `Modify the existing invoicing API without breaking current clients`.
3. **Ambiguous:** choose `AMBIGUOUS`, create the workflow, then select **Clarify**.
4. **Approval gate:** choose approval required, create the plan, and use **Approve & run**.
5. **Retry/safe-stop:** include `[simulate-failure]` in the requirement. Task `T3` will fail in the demo executor, retry, and then stop safely (when safe-stop is enabled).
6. **URL analytics:** open the **URL analytics** tab, enter the original URL, optionally choose an expiry date/time in the future, and create the short URL. Select **Details** or **View this URL's analytics** to see per-URL metrics. Open the short URL in another tab; when you return to the dashboard, analytics refresh automatically.

## Important implementation boundaries

- The workflow executor simulates engineering work. It does **not** call an LLM, edit your source code, run shell commands, deploy software, or reverse external changes.
- The rollback endpoint is an auditable rollback **request**, not an actual compensating transaction.
- The local deterministic planner is a fallback. For a production AI integration, add an authenticated provider adapter, structured output validation, authorization boundaries, secrets management, and human approval for risky actions.
- H2 is appropriate for local development. Use PostgreSQL and migrations for a shared or production deployment.
- If deployed behind a reverse proxy, configure forwarded headers and HTTPS correctly so generated short links use the public host and scheme.
=======
# agentic-engineering-workbench
Spring Boot URL Shortener with Expiry, URL Analytics, and Agentic Workflow Dashboard
>>>>>>> branch 'main' of https://github.com/kbnkishore223/agentic-engineering-workbench.git
