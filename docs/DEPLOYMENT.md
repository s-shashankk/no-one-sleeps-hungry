# Deployment and production configuration

**Host not decided yet** (PLAN §8). Shared hosting cannot run Spring Boot. You need a Linux VPS (or any host that runs Java 21 and MySQL 8). The steps below assume Ubuntu with nginx.

## 1. What runs where
| Piece | How |
|---|---|
| Frontend | `npm ci && npm run build` in `frontend/`; serve `frontend/dist` as static files from nginx |
| API | `backend` jar run by systemd on `127.0.0.1:8080`; nginx proxies `/api` to it |
| Database | MySQL 8, database `mealup`, a dedicated user (not root) |

Serve **both** from one domain over HTTPS (e.g. `https://mealup.example.org`). The session cookie is `Secure` in production, so plain HTTP will not keep people signed in.

## 2. Environment variables (profile `prod`)
| Variable | Required | Meaning |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | yes | `prod` |
| `DB_URL` | yes | e.g. `jdbc:mysql://127.0.0.1:3306/mealup?connectionTimeZone=UTC` |
| `DB_USER`, `DB_PASSWORD` | yes | database login |
| `MEALUP_ADMIN_NAME`, `_PHONE`, `_EMAIL`, `_PASSWORD` | first start only | creates the admin account if no account exists. Remove afterwards. |
| `MEALUP_RATE_PUBLIC_PER_HOUR` | no (20) | public form submissions per address per hour |
| `MEALUP_RATE_LOGIN_PER_10_MIN` | no (10) | sign-in attempts per address |
Frontend (build time, optional): `VITE_CONTACT_EMAIL` shows a contact address on the privacy page.

Keep secrets in a root-only env file (`chmod 600 /etc/mealup/mealup.env`), never in git. Use a strong founder password and change it after first sign-in. Swagger is disabled in prod.

## 3. systemd unit `/etc/systemd/system/mealup.service`
```
[Unit]
Description=MealUp API
After=network.target mysql.service
[Service]
User=mealup
EnvironmentFile=/etc/mealup/mealup.env
ExecStart=/usr/bin/java -Xmx512m -jar /opt/mealup/mealup.jar
Restart=on-failure
[Install]
WantedBy=multi-user.target
```
Build the jar with `cd backend && ./mvnw -DskipTests package`. Flyway applies migrations on startup.

## 4. nginx
```
server {
  listen 443 ssl http2;
  server_name mealup.example.org;
  # ssl_certificate ... (certbot)
  root /opt/mealup/frontend;
  add_header X-Content-Type-Options nosniff always;
  add_header X-Frame-Options DENY always;
  add_header Referrer-Policy strict-origin-when-cross-origin always;
  add_header Strict-Transport-Security "max-age=31536000" always;
  add_header Content-Security-Policy "default-src 'self'; img-src 'self' blob:; style-src 'self' 'unsafe-inline'; frame-ancestors 'none'" always;
  location /api/ {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $remote_addr;   # overwrite, do not append, so clients cannot fake it
    proxy_set_header X-Forwarded-Proto $scheme;
  }
  location / { try_files $uri /index.html; }
}
server { listen 80; server_name mealup.example.org; return 301 https://$host$request_uri; }
```
`forward-headers-strategy: native` makes Tomcat trust forwarded headers only from a proxy on the same machine or private network. This matters: the rate limiter keys on the client address. If the API is reached through a different kind of proxy/CDN, check this.

## 5. Backups and restore
- `ops/backup.sh` (database dump, keeps 14 days). Cron: `15 2 * * * env-file-loaded ops/backup.sh`. Also copy backups off the server.
- `ops/restore-check.sh <dump.gz>` restores into a scratch database and compares table counts.
- **Do the restore check once before the pilot and record the date in docs/PILOT.md.** An untested backup is not a backup.
- Full restore: stop the service, `gunzip -c db-*.sql.gz | mysql mealup`, start the service.

## 6. Smoke test after each deploy
`GET /api/v1/health` returns UP; sign in; open the dashboard; submit a test report from a phone; confirm a 21st public submission in an hour gets the "too many requests" message.
