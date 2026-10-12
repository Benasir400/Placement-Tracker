# PlaceTrack Cloud Deployment Guide

This guide provides end-to-end instructions for deploying PlaceTrack to the cloud.

---

## Architecture Overview

PlaceTrack consists of:
- **Backend**: Spring Boot 3 (Java 17/21) REST API. Supports H2 database out-of-the-box or external MySQL.
- **Frontend**: React + Vite SPA with React Router.
- **Deployment Models Supported**:
  1. **Decoupled Cloud Hosting (Recommended)**: Spring Boot on Render/Railway + React SPA on Vercel.
  2. **1-Click Render Blueprint**: Both services deployed together via `render.yaml`.
  3. **Containerized Multi-Service**: Docker Compose on any VPS (AWS EC2, DigitalOcean, Hetzner).
  4. **Single Full-Stack Executable JAR**: React frontend bundled directly inside the Spring Boot JAR.

---

## Strategy 1: Free Tier Cloud Hosting (Render + Vercel) [Recommended]

This is the easiest, zero-cost production deployment strategy.

### Step 1: Deploy Backend on Render

1. Go to [render.com](https://render.com) and sign in with your GitHub account.
2. Click **New +** > **Web Service**.
3. Select your repository: `Placement-Tracker` (or your fork).
4. Configure the service settings:
   - **Name**: `placetrack-backend`
   - **Region**: Select closest to your users (e.g., *Singapore*, *Frankfurt*, or *Oregon*)
   - **Branch**: `main`
   - **Root Directory**: `backend`
   - **Runtime**: `Docker`
   - **Instance Type**: `Free`
5. Under **Environment Variables**, add:
   | Key | Value | Description |
   |---|---|---|
   | `PORT` | `8080` | Server listening port |
   | `JWT_SECRET` | *32+ character random string* | Secret key for signing auth tokens |
   | `CORS_ALLOWED_ORIGINS` | `https://your-frontend-app.vercel.app` | Allowed frontend domain (update after Vercel deploy) |
   | `EMAIL_DEVELOPMENT_MODE` | `true` | Allows testing without real SMTP credentials |
   | `FRONTEND_BASE_URL` | `https://your-frontend-app.vercel.app` | URL used in invitation links |

6. Click **Create Web Service**.
7. Once deployed, copy your backend URL (e.g. `https://placetrack-backend.onrender.com`).
8. Verify by opening in browser: `https://placetrack-backend.onrender.com/api/public/health`. You should see `{"status":"UP"}`.

---

### Step 2: Deploy Frontend on Vercel

1. Go to [vercel.com](https://vercel.com) and log in with GitHub.
2. Click **Add New...** > **Project**.
3. Import your `Placement-Tracker` repository.
4. In the configuration screen:
   - **Framework Preset**: `Vite`
   - **Root Directory**: Click *Edit* and select `frontend`
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
5. Under **Environment Variables**, add:
   | Key | Value | Description |
   |---|---|---|
   | `VITE_API_BASE_URL` | `https://placetrack-backend.onrender.com/api` | Point to your Render backend API |
6. Click **Deploy**.
7. Once finished, Vercel gives you a public domain (e.g. `https://placetrack-frontend.vercel.app`).

### Step 3: Connect Frontend and Backend

1. Return to [Render Dashboard](https://dashboard.render.com).
2. Open `placetrack-backend` > **Environment**.
3. Update `CORS_ALLOWED_ORIGINS` to match your Vercel URL:
   ```text
   https://placetrack-frontend.vercel.app
   ```
4. Update `FRONTEND_BASE_URL` to match your Vercel URL:
   ```text
   https://placetrack-frontend.vercel.app
   ```
5. Render will automatically redeploy with the new settings.
6. Open your Vercel website URL — PlaceTrack is live!

---

## Strategy 2: 1-Click Render Blueprint (`render.yaml`)

The repository includes a ready-to-use `render.yaml` configuration.

1. Push your repository to GitHub.
2. In Render, click **New +** > **Blueprint**.
3. Connect your repository. Render automatically reads `render.yaml` and defines:
   - `placetrack-backend` (Docker web service with health check `/api/public/health`)
   - `placetrack-frontend` (Static site with SPA routing rewrites)
4. Click **Apply**.
5. Both services deploy and auto-link to each other.

---

## Strategy 3: Railway All-in-One Deployment

1. Go to [railway.app](https://railway.app) and create a new project.
2. Select **Deploy from GitHub repo**.
3. Add a **MySQL** database service:
   - Click `+ New` > `Database` > `MySQL`.
4. Add the **Backend Service**:
   - Click `+ New` > `GitHub Repo` > select your repository.
   - Go to **Settings** > **Root Directory** > set to `/backend`.
   - Under **Variables**, link Railway's MySQL variables:
     - `DB_URL`: `jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true`
     - `DB_USERNAME`: `${{MySQL.MYSQLUSER}}`
     - `DB_PASSWORD`: `${{MySQL.MYSQLPASSWORD}}`
     - `DB_DRIVER`: `com.mysql.cj.jdbc.Driver`
     - `JWT_SECRET`: `${{RAILWAY_SERVICE_ID}}-secret-key-salt`
     - `CORS_ALLOWED_ORIGINS`: `*`
     - `EMAIL_DEVELOPMENT_MODE`: `true`
5. Add the **Frontend Service**:
   - Click `+ New` > `GitHub Repo` > select your repository.
   - Set **Root Directory** to `/frontend`.
   - Add environment variable `VITE_API_BASE_URL` set to `${{Backend.RAILWAY_PUBLIC_DOMAIN}}/api`.
   - Generate a domain under **Networking**.

---

## Strategy 4: Self-Hosted VPS / Cloud VM (Docker Compose)

For deployment on any Linux server (AWS EC2, DigitalOcean Droplet, Linode, Hetzner):

1. SSH into your server:
   ```bash
   ssh user@your-server-ip
   ```
2. Clone your repository:
   ```bash
   git clone https://github.com/Benasir400/Placement-Tracker.git
   cd Placement-Tracker
   ```
3. Set your production secrets in `docker-compose.yml` or a `.env` file (e.g. `JWT_SECRET`, database passwords).
4. Run the containers:
   ```bash
   docker compose up -d --build
   ```
5. Check container statuses:
   ```bash
   docker compose ps
   ```
6. Visit `http://your-server-ip` to access the application.

---

## Strategy 5: Single-JAR Full-Stack Deployment

If you want to host on a platform that only allows 1 free web service without paying for separate frontend hosting:

1. Run the packaging script from the project root:
   - **Windows PowerShell**:
     ```powershell
     .\scripts\package-fullstack-jar.ps1
     ```
   - **Linux / macOS**:
     ```bash
     chmod +x ./scripts/package-fullstack-jar.sh
     ./scripts/package-fullstack-jar.sh
     ```
2. This bundles the compiled React frontend directly into `backend/src/main/resources/static/`.
3. The single output jar is located at:
   ```text
   backend/target/placetrack-backend-0.0.1-SNAPSHOT.jar
   ```
4. Deploy this single JAR or its Dockerfile to any cloud provider! Spring Boot will serve both the React web application and the REST APIs from port 8080.

---

## Environment Variables Reference

### Backend

| Variable | Default Value | Notes |
|---|---|---|
| `PORT` or `SERVER_PORT` | `8080` | Port listened to by Spring Boot |
| `DB_URL` | `jdbc:h2:file:./data/placetrack;MODE=MySQL...` | Change to MySQL URL in production |
| `DB_USERNAME` | `sa` | Database username |
| `DB_PASSWORD` | *(empty)* | Database password |
| `DB_DRIVER` | `org.h2.Driver` | Use `com.mysql.cj.jdbc.Driver` for MySQL |
| `JWT_SECRET` | *change-this-...* | **Crucial:** Must be a long, random secret in production |
| `JWT_EXPIRATION_MINUTES`| `60` | Token validity lifetime in minutes |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,...` | Comma-separated list of allowed frontend origins (or `*`) |
| `EMAIL_DEVELOPMENT_MODE`| `true` | When true, logs invitation tokens without SMTP |
| `SMTP_HOST` | `smtp.gmail.com` | SMTP host for outgoing invitation emails |
| `SMTP_PORT` | `587` | SMTP port |
| `SMTP_USERNAME` | *(optional)* | Email account username |
| `SMTP_PASSWORD` | *(optional)* | Email account password or app password |
| `FRONTEND_BASE_URL` | `http://127.0.0.1:5173` | Base URL used to construct invitation email links |

### Frontend

| Variable | Default Value | Notes |
|---|---|---|
| `VITE_API_BASE_URL` | `/api` | Base URL for API requests. In cross-origin deployments, set to `https://<backend-host>/api` |

---

## Post-Deployment Verification Checklist

1. [ ] **Public Health Check**: Visit `<backend-url>/api/public/health` -> should return `{"status":"UP"}`.
2. [ ] **Database Connectivity**: Visit `<backend-url>/api/public/summary` -> should return numeric summary metrics.
3. [ ] **Frontend Routing**: Open `<frontend-url>`, navigate to `/login`, and refresh the page -> should not return 404.
4. [ ] **Authentication**: Log in with seeded demo credentials or create an admin account.
5. [ ] **CORS Verification**: Open DevTools Network tab and confirm no CORS preflight failures on API calls.
