#!/usr/bin/env bash
# Provisions CreditFlow PostgreSQL: role, three databases, tables.
# Requires: psql on PATH.
#
# Usage:
#   export PGPASSWORD='<postgres superuser password>'
#   ./scripts/local/setup-postgres.sh
#
# Optional: POSTGRES_HOST=localhost POSTGRES_PORT=5432 POSTGRES_USER=postgres

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SQL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/sql" && pwd)"

POSTGRES_HOST="${POSTGRES_HOST:-localhost}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
APP_USER="${APP_USER:-creditflow}"
APP_PASSWORD="${APP_PASSWORD:-creditflow}"

if [[ -z "${PGPASSWORD:-}" ]]; then
  echo "Set PGPASSWORD to the PostgreSQL superuser (${POSTGRES_USER}) password, then re-run."
  exit 1
fi

psql_h() { psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" "$@"; }

echo "==> Creating role (if needed)..."
psql_h -d postgres -v ON_ERROR_STOP=1 -f "$SQL_DIR/01-role-and-databases.sql"

for db in users_db transactions_db credit_db; do
  exists=$(psql_h -d postgres -Atc "SELECT 1 FROM pg_database WHERE datname = '$db'" || true)
  if [[ "$exists" == "1" ]]; then
    echo "==> Database $db already exists, skipping CREATE."
  else
    echo "==> Creating database $db..."
    psql_h -d postgres -v ON_ERROR_STOP=1 -c "CREATE DATABASE $db OWNER $APP_USER;"
    psql_h -d postgres -v ON_ERROR_STOP=1 -c "GRANT ALL PRIVILEGES ON DATABASE $db TO $APP_USER;"
  fi
done

export PGPASSWORD="$APP_PASSWORD"

echo "==> Schema: users_db..."
psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$APP_USER" -d users_db -v ON_ERROR_STOP=1 -f "$SQL_DIR/02-users_db.sql"

echo "==> Schema: transactions_db..."
psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$APP_USER" -d transactions_db -v ON_ERROR_STOP=1 -f "$SQL_DIR/03-transactions_db.sql"

echo "==> Schema: credit_db..."
psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$APP_USER" -d credit_db -v ON_ERROR_STOP=1 -f "$SQL_DIR/04-credit_db.sql"

echo "Done."
