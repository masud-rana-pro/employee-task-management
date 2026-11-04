# Deployment guide (all free, no credit card)

Order matters: **1. Database -> 2. Backend -> 3. Frontend -> 4. connect them (CORS)**. Allow about 60-90 minutes the first time.

| Part | Service | Card needed? |
|---|---|---|
| Source code | GitHub (public repo) | no |
| MySQL 8 | **Aiven** free plan | no (checked on the Aiven docs: "no credit card needed to sign up") |
| Backend (Docker) | **Render** free web service | no |
| Frontend | **Netlify** | no |

Known limits of the free plans (mention them in the interview, they are normal):
* **Render** puts a free service to sleep after 15 minutes without traffic. The first request afterwards needs about a minute.
  **Open `https://<backend>/api/health` a few minutes before anyone tests the app** (and before the interview).
* **Aiven** may power off a free MySQL service that has no activity. It sends an e-mail first, and you can switch it on again in the console.
  The free plan allows at most 76 connections (the app uses a pool of 5).
* Plans and limits change over time - if a screen looks different, follow the same idea.

---

## 0. Push the code to GitHub

```bash
cd task-management-system
git log --oneline            # the project already contains meaningful commits
git remote add origin https://github.com/<your-user>/task-management-system.git
git branch -M main
git push -u origin main
```
Create the empty **public** repository on github.com first (no README, no .gitignore).
The commits are authored with a placeholder name. To use your real name before pushing:
```bash
git config user.name  "Your Name"
git config user.email "you@example.com"
git rebase -r --root --exec 'git commit --amend --no-edit --reset-author'
```

## 1. Database (Aiven MySQL)

1. Sign up at https://aiven.io, create a project, then **Create service -> MySQL -> Free plan** and wait until it shows *Running*.
2. Open the service **Overview** and note: **Host, Port, User, Password, Database name** (usually `defaultdb`).
3. Load the schema. The file to use is **`schema-cloud.sql`** (same as `schema.sql` without the `CREATE DATABASE` / `USE` lines).
   * Easiest (GUI): install **DBeaver** or **MySQL Workbench**, create a connection with the values above and **SSL enabled (Require)**,
     open `schema-cloud.sql` and run the whole script.
   * Command line:
     ```bash
     DB_HOST=<host> DB_PORT=<port> DB_USER=<user> DB_PASS=<password> DB_NAME=defaultdb ./scripts/restore-db.sh schema-cloud.sql
     ```
4. Check: `SELECT email, role FROM users;` must return the 4 demo users.
5. The JDBC URL for the backend is:
   `jdbc:mysql://<host>:<port>/<database>?sslMode=REQUIRED&serverTimezone=UTC`

## 2. Backend (Render, Docker)

1. https://render.com -> sign up with GitHub -> **New -> Web Service** -> select your repository.
2. Settings
   * **Language / Runtime:** Docker
   * **Root Directory:** `backend`
   * **Instance type:** Free
   * **Health Check Path:** `/api/health`
3. **Environment variables** (Advanced):

   | Key | Value |
   |---|---|
   | `DB_URL` | the JDBC URL from step 1.5 |
   | `DB_USERNAME` | Aiven user |
   | `DB_PASSWORD` | Aiven password |
   | `JWT_SECRET` | a long random value, e.g. output of `openssl rand -base64 48` (min. 32 characters) |
   | `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` for now - you change it in step 4 |

4. **Create Web Service**. The first build takes 5-10 minutes (Maven downloads the dependencies).
5. When it is live open `https://<name>.onrender.com/api/health` -> `{"status":"UP"}`.
   Test the login in Postman: `POST /api/auth/login` with `admin@demo.com / Admin@123`.
   If the build or start fails, read the Render **Logs** - typical causes: a wrong `DB_URL` / password, or `JWT_SECRET` shorter than 32 characters.

(`render.yaml` in the repository root is an optional Blueprint that does the same.)

## 3. Frontend (Netlify)

1. https://app.netlify.com -> **Add new site -> Import an existing project** -> GitHub -> your repository.
2. Build settings (they are also in `frontend/netlify.toml`)
   * **Base directory:** `frontend`
   * **Build command:** `npm run build:prod`
   * **Publish directory:** `frontend/dist/frontend`
3. **Environment variable:** `API_URL` = `https://<name>.onrender.com/api` (with `/api`, **no** trailing slash).
4. Deploy. You get `https://<something>.netlify.app` (you can rename it under *Site configuration -> Change site name*).

## 4. Connect frontend and backend

Back in Render -> the backend service -> **Environment** -> set
`CORS_ALLOWED_ORIGINS` = `https://<something>.netlify.app` (exact origin, **no** trailing slash, no path). Save; the service redeploys.
Then open the Netlify URL and log in with the demo users. If the browser console shows a *CORS* error, this value does not match exactly.

## 5. Final checklist before you send the e-mail

- [ ] `/api/health` of the backend answers `UP`
- [ ] Login works for admin, manager and employee on the **Netlify** URL
- [ ] Admin: users list, add, edit, delete; all tasks; PDF download
- [ ] Manager: assign a task to emp1 / emp2; tasks list; PDF
- [ ] Employee: My tasks, Start -> Mark done; cannot open /users (redirected)
- [ ] Take screenshots into `docs/screenshots/` and commit them
- [ ] README: fill in the two live URLs
- [ ] Run the Postman collection against the live backend (change `baseUrl`)

## 6. E-mail to send (reply to the HR e-mail, deadline: 4 November 2026, end of day)

> Subject: Demo project submission - Programmer position
>
> Dear Rahima Akter Rubi,
>
> Please find my demo project submission (Mini Employee & Task Management System). This is a newly built project, not a previously built one.
>
> - GitHub repository: <link>
> - Live frontend: <netlify link>
> - Live backend: <render link> (free hosting: the first request can take about one minute while the server wakes up; /api/health wakes it)
> - Database: MySQL on Aiven (schema.sql with seed data is in the repository root)
> - Postman collection: /postman in the repository
> - UML / ER diagrams: /docs/diagrams
> - README.md: repository root (setup, environment variables, API table, sample credentials)
>
> Sample logins: admin@demo.com / Admin@123, manager@demo.com / Manager@123, emp1@demo.com / Emp@123
>
> Thank you. I look forward to the interview on 5 November at 10:00 AM.
>
> Best regards,
> <your name> - <phone>

Also attach `schema.sql`, the Postman collection and the diagrams to the e-mail, because the HR e-mail asks for them as deliverables.

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| Frontend shows "Cannot reach the server" | Backend is asleep (wait ~1 min, retry) or `API_URL` is wrong - rebuild after fixing it |
| Browser console: CORS error | `CORS_ALLOWED_ORIGINS` must equal the Netlify origin exactly, `https://..netlify.app`, no `/` at the end |
| Page refresh gives 404 on Netlify | `netlify.toml` redirect missing - keep the file in `frontend/` |
| Backend log: `JWT_SECRET ... shorter than 32` | Set a longer secret |
| Backend log: `Communications link failure` / `SSL` | Wrong host/port, or `sslMode=REQUIRED` missing in `DB_URL`, or the Aiven service is powered off |
| Login returns 401 with the right password | Seed data not loaded (run `schema-cloud.sql` again; it is safe to repeat) |
| Netlify build fails on Node version | Keep `NODE_VERSION = "16"` in `netlify.toml` (Angular 13) |
