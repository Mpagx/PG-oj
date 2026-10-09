#!/usr/bin/env bash
set -euo pipefail
umask 077
# MYSQL_CNF contains a [client] section with a dedicated backup user's credentials.
: "${MYSQL_CNF:?Set MYSQL_CNF to a chmod-600 credentials file}"
: "${BACKUP_DIR:?Set BACKUP_DIR to an existing dedicated backup directory}"
: "${DB_NAME:?Set DB_NAME}"
[[ "$DB_NAME" =~ ^[a-zA-Z0-9_]+$ ]] || { echo 'Invalid DB_NAME' >&2; exit 1; }
[[ -d "$BACKUP_DIR" && -f "$MYSQL_CNF" ]] || exit 1
destination="$BACKUP_DIR/$DB_NAME-$(date -u +%Y%m%dT%H%M%SZ).sql.gz"
partial=$(mktemp "$BACKUP_DIR/.backup.XXXXXX")
trap 'rm -f -- "$partial"' EXIT
mysqldump --defaults-extra-file="$MYSQL_CNF" --single-transaction --routines --events --triggers --no-tablespaces --set-gtid-purged=OFF "$DB_NAME" | gzip > "$partial"
gzip -t "$partial"
mv -- "$partial" "$destination"
printf '%s\n' "$destination"
