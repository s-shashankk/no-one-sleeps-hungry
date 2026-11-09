#!/usr/bin/env bash
# Nightly backup of the database when MealUp runs with docker-compose.prod.yml (MySQL inside Docker).
# Run it from the project folder on the server, e.g. cron:
#   15 2 * * * cd /opt/mealup && ./ops/backup.sh >> /var/log/mealup-backup.log 2>&1
# Optional: BACKUP_DIR (default /var/backups/mealup), KEEP_DAYS (14), COMPOSE_FILE (docker-compose.prod.yml)
set -euo pipefail
cd "$(dirname "$0")/.."
BACKUP_DIR="${BACKUP_DIR:-/var/backups/mealup}"
KEEP_DAYS="${KEEP_DAYS:-14}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yml}"
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$BACKUP_DIR"
umask 077
out="$BACKUP_DIR/db-$stamp.sql.gz"
# The password is read inside the container, so it never appears on this command line.
docker compose -f "$COMPOSE_FILE" exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_PASSWORD" mysqldump --single-transaction --routines --no-tablespaces -u"$MYSQL_USER" "$MYSQL_DATABASE"' \
  | gzip > "$out"
# A dump that is only a few bytes means the dump failed.
[ "$(stat -c %s "$out")" -gt 1000 ] || { echo "Backup looks empty, check the database." >&2; rm -f "$out"; exit 1; }
find "$BACKUP_DIR" -type f -name 'db-*.sql.gz' -mtime +"$KEEP_DAYS" -delete
echo "Backup written: $out"
echo "Copy it off the server too (another machine or cloud storage); a backup on the same disk is not enough."
