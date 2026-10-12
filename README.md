# PlaceTrack - Smart Placement Drive Management System

PlaceTrack is a staged full-stack portfolio project for managing college placement drives. Phase 1 creates the production-minded foundation: Spring Boot backend, React frontend, database schema migrations, security baseline, environment documentation, and the first working API connected to the frontend.

## Current Status

Implemented now:

- `backend/` Spring Boot 3 project using Java 17-compatible settings.
- `frontend/` React + Vite project using JavaScript.
- Flyway migration for the required core tables.
- JPA entities and repositories for users, student profiles, invitations, companies, drives, applications, events, bookmarks, notifications, and audit logs.
- BCrypt password hashing, JWT authentication, and stateless role-based Spring Security.
- Public health endpoint: `GET /api/public/health`.
- Public database-backed summary endpoint: `GET /api/public/summary`.
- Coordinator and student dashboards connected to protected backend APIs.
- Invitation creation, invitation acceptance, seeded demo login, profile editing, drive creation, publishing, eligibility checks, applications, bookmarks, notifications, and coordinator status updates.
- Rule-based placement announcement extraction endpoint.
- React dashboard/workspace polling so counts, invitations, applications, and notifications refresh from the backend without a page reload.
- `.env.example` files and sample student CSV template.

Not implemented yet:

- CSV import workflow.
- SMTP email delivery beyond development-mode invitation links.
- Full company and drive edit/close/archive screens.
- Application status history timeline UI.
- Applicant CSV export.
- Branch-wise and selection analytics beyond the current dashboard metrics.
- Audit log APIs and UI.
- Optional AI provider integration for announcement parsing.

Those belong to later phases from the project brief.

## Architecture

```text
PlaceTrack/
  backend/
    src/main/java/com/placetrack/
      config/       Spring Security and app configuration
      controller/   REST API controllers
      dto/          Request and response DTOs
      entity/       JPA entity model
      exception/    Centralized API error handling
      repository/   Spring Data JPA repositories
      service/      Business services in later phases
    src/main/resources/db/migration/
      V1__initial_schema.sql
  frontend/
    src/
      components/   Layout and reusable UI
      pages/        Routed pages
      services/     Axios API client
      styles/       Global CSS
```

## Technologies

Backend:

- Java 17+
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Spring Security
- Spring Boot Validation
- Spring Boot Mail
- Flyway
- MySQL Connector
- JUnit and Spring Security Test

Frontend:

- React
- Vite
- JavaScript
- React Router
- Axios
- Recharts
- Lucide React
- CSS3

## Database Setup

Phase 1 defaults to an in-memory H2 database in MySQL compatibility mode so the app can start without local MySQL while the schema is still validated by Flyway.

For MySQL, create a database and run with the `mysql` profile:

```bash
mysql -u root -p
CREATE DATABASE placetrack;
```

Then configure environment variables from `backend/.env.example` and start:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

Flyway runs `V1__initial_schema.sql` automatically.

## Run Locally

Start backend:

```bash
cd B:\PlaceTrack\PlaceTrack\backend
mvn spring-boot:run
```

Backend health:

```text
http://127.0.0.1:8080/api/public/health
http://127.0.0.1:8080/api/public/summary
```

Start frontend:

```bash
cd B:\PlaceTrack\PlaceTrack\frontend
npm install
npm run dev
```

On Windows PowerShell, if script execution blocks `npm`, use `npm.cmd run dev`.

Frontend:

```text
http://127.0.0.1:5173/
```

## Test Commands

Backend:

```bash
cd backend
mvn test
```

Frontend:

```bash
cd frontend
npm run build
```

On Windows PowerShell, `npm.cmd run build` avoids local script execution policy issues.

## Deployment

PlaceTrack is configured for multi-cloud deployment with ready-made configurations:

- **Free Tier Cloud (Render + Vercel)**: Follow the complete step-by-step walkthrough in [DEPLOYMENT_GUIDE.md](file:///b:/PlaceTrack/PlaceTrack/docs/DEPLOYMENT_GUIDE.md).
- **1-Click Render Blueprint**: Uses [render.yaml](file:///b:/PlaceTrack/PlaceTrack/render.yaml) to automatically spin up both backend and frontend.
- **Docker Compose**: Uses [docker-compose.yml](file:///b:/PlaceTrack/PlaceTrack/docker-compose.yml) to run Backend, Frontend (Nginx), and MySQL (`docker compose up --build`).
- **Railway**: Configured with [railway.json](file:///b:/PlaceTrack/PlaceTrack/railway.json).
- **Single-JAR Full-Stack**: Run `.\scripts\package-fullstack-jar.ps1` to embed the React frontend into the Spring Boot backend JAR for single-service hosting.

## Environment Variables

Backend variables are documented in `backend/.env.example`.

Important values:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`: MySQL connection.
- `JWT_SECRET`: long random JWT signing secret for later auth phases.
- `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`: email delivery.
- `EMAIL_DEVELOPMENT_MODE`: when `true`, later email flows can be tested without real SMTP.
- `AI_PROVIDER`: optional future announcement extraction provider. Phase 1 does not call AI.

Frontend variables are documented in `frontend/.env.example`.

## Sample CSV Template

See:

```text
docs/student-import-template.csv
```

Required columns:

```csv
name,collegeEmail,branch
```

## Security Notes

- Passwords will be stored with BCrypt in the auth phase.
- Raw invitation tokens must never be stored; only hashes should be persisted.
- Public Phase 1 endpoints are intentionally limited to health checks.
- All future student/coordinator APIs must enforce role checks on the backend.
- `.env` files, uploads, exports, and real student data are ignored by Git.

## Demo Workflow For Phase 1

1. Start the backend.
2. Open `/api/public/health` and confirm `status: "UP"`.
3. Open `/api/public/summary` and confirm the JSON count fields return numeric values.
4. Start the frontend.
5. Open the React app and confirm the health card and analytics chart load from the backend.

## Next Phase

Next, continue with the missing pieces from the staged brief:

- CSV upload, validation, duplicate detection, review, and confirmed invitation import.
- Real SMTP delivery with resend status tracking.
- Drive edit, preview, close, and archive actions.
- Application timeline UI backed by `application_events`.
- CSV export, audit log views, and richer coordinator analytics.
