# Deploying DevCollab Backend & PostgreSQL on Render

This guide walks you through migrating the DevCollab backend and database from Railway to Render.

---

## What Has Been Configured

1. **`render.yaml` Blueprint**: Declarative specification for spinning up both the PostgreSQL database and Dockerized Web Service with 1-click.
2. **Dynamic Database URL Parser (`DataSourceConfig.java`)**: Automatically detects Render internal/external database URLs (`postgresql://` or `postgres://`), parses SSL parameters, handles URL-encoded credentials, and optimizes connection pools for free-tier resource limits.
3. **512MB RAM Optimization (`Dockerfile`)**: Pre-configured with `-XX:+UseContainerSupport -Xmx256m -Xms128m -XX:+UseSerialGC -Xss512k` to prevent Render Free Tier Out-Of-Memory (OOM / exit code 137) errors.
4. **Health Check Endpoint (`/health`)**: Publicly accessible health check returning `HTTP 200` to satisfy Render's automated zero-downtime health checker.
5. **TLS Redis Support (`RedisConfig.java`)**: Works out of the box with `rediss://` for free Redis providers like Upstash, or cleanly falls back to in-memory presence if Redis is omitted.

---

## Deployment Option 1: Automatic 1-Click Blueprint (Recommended)

1. **Push your code to GitHub**:
   ```bash
   git add .
   git commit -m "chore: configure Render blueprint, health check, and memory tuning"
   git push origin main
   ```

2. **Go to Render**:
   - Log in at [dashboard.render.com](https://dashboard.render.com).
   - Click the **New +** button in the top right.
   - Select **Blueprint**.

3. **Connect Repository**:
   - Select your repository `DevCollab-backend`.
   - Render reads `render.yaml` and will detect:
     - **Database**: `devcollab-db` (PostgreSQL, Free Plan)
     - **Web Service**: `devcollab-backend` (Docker, Free Plan)

4. **Fill in Environment Variables**:
   Render will prompt you for variables marked `sync: false`:
   - `APP_FRONTEND_URL`: URL of your deployed frontend (e.g. `https://your-app.vercel.app`).
   - `GOOGLE_CLIENT_ID`: Your Google OAuth Client ID.
   - `GOOGLE_CLIENT_SECRET`: Your Google OAuth Client Secret.
   - `GITHUB_CLIENT_ID`: Your GitHub OAuth Client ID.
   - `GITHUB_CLIENT_SECRET`: Your GitHub OAuth Client Secret.
   - `REDIS_URL`: *(Optional)* Upstash Redis connection string, or leave blank to use in-memory presence.

5. **Click "Apply"**:
   Render will provision the database first, then automatically inject the database URL into the backend and build the Docker container. Flyway migrations (V1 to V9) run automatically on start.

---

## Deployment Option 2: Manual Dashboard Setup

If you prefer to create resources manually:

### Step 1: Create PostgreSQL Database
1. In Render, click **New +** -> **PostgreSQL**.
2. Set:
   - **Name**: `devcollab-db`
   - **Database**: `devcollab`
   - **User**: `devcollab`
   - **Region**: Oregon (or your preferred region)
   - **Plan**: Free
3. Click **Create Database**.
4. Once created, copy the **Internal Database URL** (e.g. `postgresql://devcollab:password@dpg-xxxxxx-a/devcollab`).

### Step 2: Create Web Service
1. In Render, click **New +** -> **Web Service**.
2. Connect your `DevCollab-backend` repository.
3. Configure:
   - **Runtime**: Docker
   - **Region**: Choose the **same region** as your database (e.g. Oregon).
   - **Plan**: Free
   - **Health Check Path**: `/health`
4. Under **Environment Variables**, add:
   - `DATABASE_URL`: *(Paste the Internal Database URL from Step 1)*
   - `PORT`: `8080`
   - `APP_FRONTEND_URL`: `https://your-app.vercel.app`
   - `APP_CORS_ALLOWED_ORIGINS`: `https://*.vercel.app,http://localhost:3000,http://localhost:5173`
   - `GOOGLE_CLIENT_ID`: `<your-google-client-id>`
   - `GOOGLE_CLIENT_SECRET`: `<your-google-client-secret>`
   - `GITHUB_CLIENT_ID`: `<your-github-client-id>`
   - `GITHUB_CLIENT_SECRET`: `<your-github-client-secret>`
5. Click **Create Web Service**.

---

## Post-Deployment Checklist

### 1. Update OAuth Redirect URIs
Once your backend is running on Render (e.g. `https://devcollab-backend.onrender.com`), update your OAuth redirect URIs:

- **Google Cloud Console** (Credentials -> OAuth 2.0 Client IDs -> Authorized redirect URIs):
  ```
  https://devcollab-backend.onrender.com/login/oauth2/code/google
  ```
- **GitHub Developer Settings** (OAuth Apps -> Authorization callback URL):
  ```
  https://devcollab-backend.onrender.com/login/oauth2/code/github
  ```

### 2. Update Frontend Environment Variable
In your Vercel/Netlify frontend project settings, update:
```
NEXT_PUBLIC_API_URL=https://devcollab-backend.onrender.com
# or
VITE_API_URL=https://devcollab-backend.onrender.com
```
Then redeploy the frontend.

---

## Free Tier Tips & Limitations

1. **Render Free Web Services Sleep**:
   - Render spins down free web services after 15 minutes of inactivity.
   - The first request wakes it up in ~30–40 seconds.
   - *Tip*: To keep it warm, set up a free monitor (e.g. [UptimeRobot](https://uptimerobot.com) or [cron-job.org](https://cron-job.org)) to ping `https://devcollab-backend.onrender.com/health` every 10–14 minutes.

2. **Render Free PostgreSQL 30-Day Policy**:
   - Render's free PostgreSQL tier expires 30 days after creation.
   - *Alternative*: If you want a database that never expires on a free tier, you can use [Neon.tech](https://neon.tech) (PostgreSQL serverless, 0.5 GB free forever). Simply copy Neon's connection string and paste it into `DATABASE_URL` in Render.

3. **Redis**:
   - Redis is completely optional in DevCollab. If not provided, active user presence automatically runs in memory.
   - If you want cross-instance distributed presence, use [Upstash Redis](https://upstash.com) (free 10,000 commands/day) and set `REDIS_URL=rediss://default:token@host:6379`.
