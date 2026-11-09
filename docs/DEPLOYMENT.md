# Deployment and production configuration

**Host not decided yet** (PLAN §8). Shared hosting cannot run Spring Boot. You need a Linux VPS (or any host that runs Java 21 and MySQL 8). The steps below assume Ubuntu with nginx.

## 0. Fastest way: Docker on one server
On any Linux VPS with Docker and a domain name pointing at it:
1. `cp deploy/.env.example deploy/.env` and fill it in (domain, passwords, first admin).
2. `docker compose -f docker-compose.prod.yml --env-file deploy/.env up -d --build`
3. Open `https://your-domain`. HTTPS is automatic (Caddy). Sign in with the admin from `.env`, then delete the `MEALUP_ADMIN_*` lines and run the same command again.
4. Database data lives in the `mysql_data` Docker volume. Back it up (section 5).
The sections below describe the same setup done by hand (systemd + nginx).

## 0b. Hostinger KVM 2, step by step (Docker)
1. In hPanel create the VPS with the **Ubuntu + Docker** template. Point the domain's A records (`@` and `www`) at the VPS IP.
2. `ssh root@VPS_IP`, then: `adduser deploy && usermod -aG sudo,docker deploy`, and sign in as `deploy` from now on.
3. Firewall: `sudo ufw allow OpenSSH && sudo ufw allow 80 && sudo ufw allow 443 && sudo ufw enable`. Never open 3306 or 8080.
4. Swap (the build needs memory): `sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile && echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab`.
5. `git clone https://github.com/s-shashankk/no-one-sleeps-hungry.git mealup && cd mealup`.
6. `cp deploy/.env.example deploy/.env` and `cp deploy/application-local.yml.example deploy/application-local.yml`; fill both in with `nano`; `chmod 600 deploy/.env`, `chmod 644 deploy/application-local.yml`.
7. `docker compose -f docker-compose.prod.yml --env-file deploy/.env up -d --build` (the first build takes several minutes).
8. Check: `docker compose -f docker-compose.prod.yml ps`, `docker compose -f docker-compose.prod.yml logs api --tail 50`, then open `https://your-domain/api/v1/health`.
9. Update later: `git pull && docker compose -f docker-compose.prod.yml --env-file deploy/.env up -d --build`.
10. Nightly backup: `crontab -e` and add `15 2 * * * cd ~/mealup && ./ops/backup.sh >> ~/mealup-backup.log 2>&1`.

`deploy/application-local.yml` creates the partner hotel (Sherpa), the plates served before launch (`opening-plates`) and the extra logins. It is read on every start and never duplicates anything.

## 0c. Server that already hosts another site
Use `docker-compose.shared.yml` instead of `docker-compose.prod.yml`. It never uses ports 80/443 and has its own project name, network and database volume, so the existing site is not touched.
1. Look first (read-only): `sudo ss -tlnp | grep -E ':80|:443'`, `docker ps`, `free -h`, `df -h`, `sudo ufw status`. Do not run `ufw enable` or change the firewall if it is already set up.
2. Start: `docker compose -f docker-compose.shared.yml --env-file deploy/.env up -d --build` (run it as `nice -n 10 docker compose ...` so the build does not slow the other site).
3. Test locally on the server: `curl -s http://127.0.0.1:8081/api/v1/health`.
4. Make the existing web server send the new domain to `127.0.0.1:8081`:
   - nginx: a new file in `/etc/nginx/sites-available/helpsomeone` with
     `server { server_name helpsomeone.in www.helpsomeone.in; location / { proxy_pass http://127.0.0.1:8081; proxy_set_header Host $host; proxy_set_header X-Forwarded-For $remote_addr; proxy_set_header X-Forwarded-Proto $scheme; } }`,
     link it into `sites-enabled`, then `sudo nginx -t && sudo systemctl reload nginx` and `sudo certbot --nginx -d helpsomeone.in -d www.helpsomeone.in`.
   - Caddy on the host: `helpsomeone.in, www.helpsomeone.in { reverse_proxy 127.0.0.1:8081 }`, then reload.
   - Another tool (Traefik, Nginx Proxy Manager, hPanel): add a host `helpsomeone.in` that forwards to `127.0.0.1:8081` and passes `X-Forwarded-For` and `X-Forwarded-Proto`.
5. The forwarded visitor address matters: the sign-in limit and the rate limits use it. Without `X-Forwarded-For` every visitor looks like one person.
Updating later: `git pull` and the same `up -d --build` command. Stopping only this site: `docker compose -f docker-compose.shared.yml down` (never add `-v`, it deletes the database).

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
- `ops/backup.sh` (database dump taken through Docker, keeps 14 days). Run it from the project folder. Cron: `15 2 * * * cd /opt/mealup && ./ops/backup.sh >> /var/log/mealup-backup.log 2>&1`. Also copy backups off the server.
- The compose file limits container memory and log size, so one container cannot use the whole server or fill the disk.
- Public numbers (meals served, areas, partner hotels) are kept in memory and rebuilt once a day at 00:05 India time and on each restart. The editable website wording is kept in memory for 10 minutes and hotel logos for 6 hours (both are refreshed at once when an admin saves or uploads), so visitors rarely reach the database for them. Every signed-in request used to re-check the person in the database; that check is now remembered for 45 seconds (dropped at once when an admin disables someone or changes their role). Places left today for a hotel stay live. To show a change straight away (a new hotel, say), restart the `api` container: `docker compose -f docker-compose.prod.yml restart api`.
- `ops/restore-check.sh <dump.gz>` restores into a scratch database and compares table counts.
- **Do the restore check once before the pilot and record the date in docs/PILOT.md.** An untested backup is not a backup.
- Full restore: stop the service, `gunzip -c db-*.sql.gz | mysql mealup`, start the service.

## 6. Smoke test after each deploy
`GET /api/v1/health` returns UP; sign in; open the dashboard; submit a test report from a phone; confirm a 21st public submission in an hour gets the "too many requests" message.
