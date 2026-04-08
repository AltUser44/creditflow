#!/bin/sh
set -eu

BASE_USER="${BASE_USER:-http://localhost:8081}"
BASE_TX="${BASE_TX:-http://localhost:8082}"
BASE_CREDIT="${BASE_CREDIT:-http://localhost:8083}"

echo "=== 1) Register ==="
EMAIL="demo-$(date +%s)@example.com"
REG=$(curl -sS -X POST "$BASE_USER/users/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"secret123\"}")
echo "$REG"
USER_ID=$(echo "$REG" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')
if [ -z "$USER_ID" ]; then
  echo "Could not parse user id from register response" >&2
  exit 1
fi

echo "=== 2) Login ==="
LOGIN=$(curl -sS -X POST "$BASE_USER/users/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"secret123\"}")
echo "$LOGIN"
TOKEN=$(echo "$LOGIN" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

echo "=== 3) GET /users/me ==="
curl -sS "$BASE_USER/users/me" -H "Authorization: Bearer $TOKEN" | tee /dev/stderr
echo

echo "=== 4) Transactions (3 credits to trigger frequent +2 on 3rd; then debit > 1000) ==="
curl -sS -X POST "$BASE_TX/transactions" -H "Content-Type: application/json" \
  -d "{\"userId\":\"$USER_ID\",\"amount\":100,\"type\":\"credit\"}" | tee /dev/stderr
echo
curl -sS -X POST "$BASE_TX/transactions" -H "Content-Type: application/json" \
  -d "{\"userId\":\"$USER_ID\",\"amount\":100,\"type\":\"credit\"}" | tee /dev/stderr
echo
curl -sS -X POST "$BASE_TX/transactions" -H "Content-Type: application/json" \
  -d "{\"userId\":\"$USER_ID\",\"amount\":100,\"type\":\"credit\"}" | tee /dev/stderr
echo
curl -sS -X POST "$BASE_TX/transactions" -H "Content-Type: application/json" \
  -d "{\"userId\":\"$USER_ID\",\"amount\":1500,\"type\":\"debit\"}" | tee /dev/stderr
echo

echo "=== 5) Sleep for async consumer ==="
sleep 3

echo "=== 6) Credit score ==="
curl -sS "$BASE_CREDIT/credit-score/$USER_ID" | tee /dev/stderr
echo

echo "Done. Expected score (approx): start 600, +5+5+7 (3rd credit has frequent) + (-10) large debit => 607"
