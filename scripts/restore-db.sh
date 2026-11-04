#!/usr/bin/env bash
# Loads a .sql file (schema.sql or a backup) into a MySQL database.
# Usage:  DB_HOST=... DB_USER=... DB_PASS=... DB_NAME=... ./scripts/restore-db.sh schema.sql
set -euo pipefail
: "${DB_HOST:?set DB_HOST}" "${DB_USER:?set DB_USER}" "${DB_PASS:?set DB_PASS}" "${DB_NAME:?set DB_NAME}"
FILE="${1:?usage: restore-db.sh <file.sql>}"
DB_PORT="${DB_PORT:-3306}"
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" --ssl-mode="${SSL_MODE:-REQUIRED}" "$DB_NAME" < "$FILE"
echo "Loaded $FILE into $DB_NAME"
