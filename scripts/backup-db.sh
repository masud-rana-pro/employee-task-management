#!/usr/bin/env bash
# Dumps the MySQL database to backups/taskdb-<timestamp>.sql
# Usage:  DB_HOST=... DB_PORT=3306 DB_USER=... DB_PASS=... DB_NAME=taskdb ./scripts/backup-db.sh
set -euo pipefail
: "${DB_HOST:?set DB_HOST}" "${DB_USER:?set DB_USER}" "${DB_PASS:?set DB_PASS}" "${DB_NAME:?set DB_NAME}"
DB_PORT="${DB_PORT:-3306}"
mkdir -p backups
OUT="backups/${DB_NAME}-$(date +%Y%m%d-%H%M%S).sql"
# --ssl-mode=REQUIRED is needed for cloud databases such as Aiven; remove it for a local database.
mysqldump -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" \
  --ssl-mode="${SSL_MODE:-REQUIRED}" --single-transaction --routines --set-gtid-purged=OFF \
  "$DB_NAME" > "$OUT"
echo "Backup written to $OUT"
