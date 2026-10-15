#!/usr/bin/env bash
# Proves a backup can be restored. Restores into a throw-away database and compares row counts.
# Usage: ops/restore-check.sh /var/backups/mealup/db-XXXX.sql.gz
# Needs: DB_NAME, DB_USER, DB_PASSWORD (user must be allowed to create databases)
set -euo pipefail
: "${DB_NAME:?}" "${DB_USER:?}" "${DB_PASSWORD:?}"
dump="${1:?path to db-*.sql.gz}"
scratch="${DB_NAME}_restore_check"
host="${DB_HOST:-127.0.0.1}"
export MYSQL_PWD="$DB_PASSWORD"
m() { mysql -h "$host" -u "$DB_USER" "$@"; }
m -e "DROP DATABASE IF EXISTS \`$scratch\`; CREATE DATABASE \`$scratch\` CHARACTER SET utf8mb4;"
gunzip -c "$dump" | m "$scratch"
fail=0
for t in users vendors coworkers requests; do
  live=$(m -N -e "SELECT COUNT(*) FROM \`$DB_NAME\`.\`$t\`" 2>/dev/null || echo "?")
  rest=$(m -N -e "SELECT COUNT(*) FROM \`$scratch\`.\`$t\`" 2>/dev/null || echo "MISSING")
  printf '%-20s live=%-8s restored=%s\n' "$t" "$live" "$rest"
  [ "$rest" = "MISSING" ] && fail=1
done
m -e "DROP DATABASE \`$scratch\`;"
if [ $fail -eq 0 ]; then echo "OK: backup restores. (Counts may differ slightly if data changed after the backup.)"; else echo "FAILED: some tables missing"; exit 1; fi
