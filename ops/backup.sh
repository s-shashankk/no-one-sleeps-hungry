#!/usr/bin/env bash
# Nightly backup of the database. Run from cron, e.g.  15 2 * * * /opt/mealup/ops/backup.sh
# Needs: DB_NAME, DB_USER, DB_PASSWORD (env). Optional: DB_HOST, BACKUP_DIR (default /var/backups/mealup), KEEP_DAYS (14)
set -euo pipefail
: "${DB_NAME:?}" "${DB_USER:?}" "${DB_PASSWORD:?}"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/mealup}"
KEEP_DAYS="${KEEP_DAYS:-14}"
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$BACKUP_DIR"
umask 077
MYSQL_PWD="$DB_PASSWORD" mysqldump --single-transaction --routines --no-tablespaces \
  -h "${DB_HOST:-127.0.0.1}" -u "$DB_USER" "$DB_NAME" | gzip > "$BACKUP_DIR/db-$stamp.sql.gz"
find "$BACKUP_DIR" -type f -name 'db-*.sql.gz' -mtime +"$KEEP_DAYS" -delete
echo "Backup written: $BACKUP_DIR/db-$stamp.sql.gz"
echo "Copy it off the server too (another machine or cloud storage); a backup on the same disk is not enough."
