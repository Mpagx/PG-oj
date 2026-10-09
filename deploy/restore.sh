#!/usr/bin/env bash
set -euo pipefail
: "${MYSQL_CNF:?Set MYSQL_CNF}"
: "${RESTORE_DB:?Use a new empty recovery database name}"
[[ "$RESTORE_DB" =~ ^poj_restore_[a-zA-Z0-9_]+$ ]] || { echo 'RESTORE_DB must start with poj_restore_' >&2; exit 1; }
[[ $# == 1 && -f "$1" ]] || { echo 'Usage: restore.sh backup.sql.gz' >&2; exit 1; }
gzip -t "$1"
mysql --defaults-extra-file="$MYSQL_CNF" -e "CREATE DATABASE \`$RESTORE_DB\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
gzip -dc "$1" | mysql --defaults-extra-file="$MYSQL_CNF" "$RESTORE_DB"
echo "Restored into $RESTORE_DB; verify row counts and application tests before switching traffic."
