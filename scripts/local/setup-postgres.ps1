<#
  Provisions CreditFlow PostgreSQL: role, three databases, tables.
  Requires: psql on PATH, PostgreSQL superuser credentials.

  Usage:
    $env:PGPASSWORD = "<postgres superuser password>"
    .\scripts\local\setup-postgres.ps1

  Or:
    .\scripts\local\setup-postgres.ps1 -PostgresUser postgres -PostgresHost localhost -PostgresPort 5432
#>
param(
  [string]$PostgresHost = "localhost",
  [int]$PostgresPort = 5432,
  [string]$PostgresUser = "postgres",
  [string]$AppUser = "creditflow",
  [string]$AppPassword = "creditflow"
)

$ErrorActionPreference = "Stop"
$sqlDir = Join-Path $PSScriptRoot "sql"

if (-not $env:PGPASSWORD) {
  Write-Host "Set PGPASSWORD to the PostgreSQL superuser ($PostgresUser) password, then re-run." -ForegroundColor Yellow
  Write-Host 'Example: $env:PGPASSWORD = "yourpassword"' -ForegroundColor Gray
  exit 1
}

function Invoke-Psql {
  param([string[]]$PsqlArgs)
  & psql @PsqlArgs
  if ($LASTEXITCODE -ne 0) { throw "psql failed: $($PsqlArgs -join ' ')" }
}

Write-Host "==> Creating role (if needed)..." -ForegroundColor Cyan
Invoke-Psql @("-h", $PostgresHost, "-p", "$PostgresPort", "-U", $PostgresUser, "-d", "postgres", "-v", "ON_ERROR_STOP=1", "-f", (Join-Path $sqlDir "01-role-and-databases.sql"))

foreach ($db in @("users_db", "transactions_db", "credit_db")) {
  $exists = & psql -h $PostgresHost -p $PostgresPort -U $PostgresUser -d postgres -Atc "SELECT 1 FROM pg_database WHERE datname = '$db'"
  if ($exists -match "1") {
    Write-Host "==> Database $db already exists, skipping CREATE." -ForegroundColor DarkGray
  } else {
    Write-Host "==> Creating database $db..." -ForegroundColor Cyan
    Invoke-Psql @("-h", $PostgresHost, "-p", "$PostgresPort", "-U", $PostgresUser, "-d", "postgres", "-v", "ON_ERROR_STOP=1", "-c", "CREATE DATABASE $db OWNER $AppUser;")
    Invoke-Psql @("-h", $PostgresHost, "-p", "$PostgresPort", "-U", $PostgresUser, "-d", "postgres", "-v", "ON_ERROR_STOP=1", "-c", "GRANT ALL PRIVILEGES ON DATABASE $db TO $AppUser;")
  }
}

$env:PGPASSWORD = $AppPassword

Write-Host "==> Schema: users_db..." -ForegroundColor Cyan
Invoke-Psql @("-h", $PostgresHost, "-p", "$PostgresPort", "-U", $AppUser, "-d", "users_db", "-v", "ON_ERROR_STOP=1", "-f", (Join-Path $sqlDir "02-users_db.sql"))

Write-Host "==> Schema: transactions_db..." -ForegroundColor Cyan
Invoke-Psql @("-h", $PostgresHost, "-p", "$PostgresPort", "-U", $AppUser, "-d", "transactions_db", "-v", "ON_ERROR_STOP=1", "-f", (Join-Path $sqlDir "03-transactions_db.sql"))

Write-Host "==> Schema: credit_db..." -ForegroundColor Cyan
Invoke-Psql @("-h", $PostgresHost, "-p", "$PostgresPort", "-U", $AppUser, "-d", "credit_db", "-v", "ON_ERROR_STOP=1", "-f", (Join-Path $sqlDir "04-credit_db.sql"))

Write-Host "Done. You can start the services with sbt (see README: Local development without Docker)." -ForegroundColor Green
