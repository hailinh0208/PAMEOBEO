#!/usr/bin/env bash
# End-to-end walkthrough of every assignment criterion through the API gateway.
# Requires: services running (scripts/start-all.sh), node (already needed for the frontend).
#   scripts/demo.sh            run all scenarios
#   scripts/demo.sh security   run one scenario: security | order | saga | cancel | idempotency | async
set -uo pipefail
GW=${GW:-http://localhost:9000}
MAILPIT=${MAILPIT:-http://localhost:8025}
CHICKEN=11111111-1111-1111-1111-111111111111
SALMON=22222222-2222-2222-2222-222222222222

# Small JSON helper (node is installed for the frontend anyway, no jq needed; works on Windows Git Bash too):
#   echo "$json" | j status              -> value of a field ("None" if missing)
#   echo "$json" | j status,totalVnd     -> {"status": ..., "totalVnd": ...}
#   echo "$json" | j keys.0.kid          -> nested path
j() {
  node -e '
let d = JSON.parse(require("fs").readFileSync(0, "utf8"));
const arg = process.argv[1];
if (arg.includes(",")) {
  console.log(JSON.stringify(Object.fromEntries(arg.split(",").map(k => [k, d?.[k] ?? null]))));
} else {
  for (const part of arg.split(".")) d = d?.[part];
  console.log(d == null ? "None" : typeof d === "object" ? JSON.stringify(d) : d);
}
' "$1"
}

uuid() { node -e 'console.log(require("crypto").randomUUID())'; }

bold() { printf "\n\033[1m== %s ==\033[0m\n" "$*"; }
step() { printf "\033[36m-> %s\033[0m\n" "$*"; }
code() { curl -s -o /dev/null -w "%{http_code}" "$@"; }

login() {
  curl -s -XPOST "$GW/api/auth/login" -H 'Content-Type: application/json' \
       -d "{\"email\":\"$1\",\"password\":\"$2\"}" | j accessToken
}

setup() {
  ADMIN=$(login admin@pameobeo.vn Admin@123)
  EMAIL="customer$(date +%s)@test.vn"
  curl -s -XPOST "$GW/api/auth/register" -H 'Content-Type: application/json' \
       -d "{\"email\":\"$EMAIL\",\"password\":\"Password1\",\"fullName\":\"Khach Demo\"}" >/dev/null
  CUSTOMER=$(login "$EMAIL" Password1)
  COOKING_DATE=$(curl -s "$GW/api/kitchen/availability" | j cookingDate)
  ORIGINAL_CAPACITY=$(curl -s "$GW/api/kitchen/availability?date=$COOKING_DATE" | j capacity)
  trap cleanup EXIT
  # previous runs (JMeter, demo) may have used up the jars: always start with free capacity
  local reserved
  reserved=$(curl -s "$GW/api/kitchen/availability?date=$COOKING_DATE" | j reserved)
  curl -s -XPUT "$GW/api/kitchen/admin/cooking-days/$COOKING_DATE/capacity" -H "Authorization: Bearer $ADMIN" \
       -H 'Content-Type: application/json' -d "{\"capacity\":$((reserved + 50))}" >/dev/null
}

# The scenarios raise the capacity and place real orders: when the demo ends, cancel them (gives the jars back)
# and put the real capacity back (50 by default), so the shop is left as it was.
cleanup() {
  printf "\nCleaning up: cancelling the demo orders and restoring the capacity of %s...\n" "$COOKING_DATE"
  curl -s "$GW/api/orders" -H "Authorization: Bearer $CUSTOMER" \
    | node -e 'for (const o of JSON.parse(require("fs").readFileSync(0, "utf8"))) if (o.status !== "CANCELLED") console.log(o.id)' \
    | while read -r id; do
        curl -s -o /dev/null -XPOST "$GW/api/orders/$id/cancel" -H "Authorization: Bearer $CUSTOMER"
      done
  local reserved capacity result
  for _ in $(seq 1 30); do # jars are released asynchronously (OrderCancelled -> kitchen)
    reserved=$(curl -s "$GW/api/kitchen/availability?date=$COOKING_DATE" | j reserved)
    capacity=$(( reserved > ORIGINAL_CAPACITY ? reserved : ORIGINAL_CAPACITY ))
    result=$(curl -s -XPUT "$GW/api/kitchen/admin/cooking-days/$COOKING_DATE/capacity" -H "Authorization: Bearer $ADMIN" \
             -H 'Content-Type: application/json' -d "{\"capacity\":$capacity}" | j capacity)
    [[ "$result" == "$ORIGINAL_CAPACITY" ]] && break
    sleep 1
  done
  echo "(capacity of $COOKING_DATE: $result jars, $reserved reserved)"
}

place_order() { # $1 token, $2 jars, $3 idempotency key (optional)
  curl -s -XPOST "$GW/api/orders" -H "Authorization: Bearer $1" -H 'Content-Type: application/json' \
       ${3:+-H "Idempotency-Key: $3"} \
       -d "{\"items\":[{\"menuItemId\":\"$CHICKEN\",\"quantity\":$2}],\"deliveryAddress\":\"12 Nguyen Hue, Q1\",\"confirmNextCookingDay\":true}"
}

wait_status() { # $1 token, $2 order id -> prints final status
  for _ in $(seq 1 20); do
    s=$(curl -s "$GW/api/orders/$2" -H "Authorization: Bearer $1" | j status)
    [[ "$s" != "PENDING" ]] && { echo "$s"; return; }
    sleep 0.5
  done
  echo PENDING
}

scenario_security() {
  bold "1. Security: auth-service (JWT RS256 + JWKS) + gateway + resource servers"
  step "GET /api/orders without token          -> $(code "$GW/api/orders")  (expected 401)"
  step "GET /api/orders with customer token    -> $(code "$GW/api/orders" -H "Authorization: Bearer $CUSTOMER")  (expected 200)"
  step "GET /api/orders/admin with customer    -> $(code "$GW/api/orders/admin" -H "Authorization: Bearer $CUSTOMER")  (expected 403)"
  step "GET /api/orders/admin with admin token -> $(code "$GW/api/orders/admin" -H "Authorization: Bearer $ADMIN")  (expected 200)"
  step "Tampered token                         -> $(code "$GW/api/orders" -H "Authorization: Bearer ${CUSTOMER}x")  (expected 401)"
  step "Public key published at /.well-known/jwks.json: kid=$(curl -s "$GW/.well-known/jwks.json" | j keys.0.kid)"
}

scenario_order() {
  bold "2. Place an order: order -> kitchen (REST + Circuit Breaker) -> Kafka saga -> CONFIRMED"
  step "Availability of cooking day $COOKING_DATE: $(curl -s "$GW/api/kitchen/availability" | j remaining,capacity)"
  RESP=$(place_order "$CUSTOMER" 2)
  ORDER=$(echo "$RESP" | j id)
  step "POST /api/orders -> $(echo "$RESP" | j status,message,cookingDate,totalVnd)"
  step "final status after the saga: $(wait_status "$CUSTOMER" "$ORDER")"
}

scenario_saga() {
  bold "3. Saga compensation: two customers race for the last jar"
  local reserved
  reserved=$(curl -s "$GW/api/kitchen/availability?date=$COOKING_DATE" | j reserved)
  curl -s -XPUT "$GW/api/kitchen/admin/cooking-days/$COOKING_DATE/capacity" -H "Authorization: Bearer $ADMIN" \
       -H 'Content-Type: application/json' -d "{\"capacity\":$((reserved + 1))}" >/dev/null
  step "admin set capacity so that exactly 1 jar is left: $(curl -s "$GW/api/kitchen/availability?date=$COOKING_DATE" | j capacity,reserved,remaining)"
  local other
  other=$(login "$EMAIL" Password1)
  place_order "$CUSTOMER" 1 > /tmp/pameobeo-a.json &
  place_order "$other" 1 > /tmp/pameobeo-b.json &
  wait
  local a b
  a=$(j id < /tmp/pameobeo-a.json); b=$(j id < /tmp/pameobeo-b.json)
  step "both requests accepted as PENDING (quote saw 1 jar left for each)"
  step "order A -> $(wait_status "$CUSTOMER" "$a")"
  step "order B -> $(wait_status "$CUSTOMER" "$b")"
  step "one CONFIRMED, one CANCELLED by compensation (reason SOLD_OUT); cooking day never overbooked:"
  step "  $(curl -s "$GW/api/kitchen/availability?date=$COOKING_DATE" | j capacity,reserved,remaining)"
  reserved=$(curl -s "$GW/api/kitchen/availability?date=$COOKING_DATE" | j reserved)
  curl -s -XPUT "$GW/api/kitchen/admin/cooking-days/$COOKING_DATE/capacity" -H "Authorization: Bearer $ADMIN" \
       -H 'Content-Type: application/json' -d "{\"capacity\":$((reserved + 1000))}" >/dev/null
  step "capacity raised again (+1000) for the next scenarios"
}

scenario_cancel() {
  bold "4. Customer cancels before 18:00 -> OrderCancelled -> kitchen releases the jars"
  local before order after
  before=$(curl -s "$GW/api/kitchen/availability?date=$COOKING_DATE" | j reserved)
  order=$(place_order "$CUSTOMER" 3 | j id)
  step "order $order -> $(wait_status "$CUSTOMER" "$order"), reserved jars: $before -> $(curl -s "$GW/api/kitchen/availability?date=$COOKING_DATE" | j reserved)"
  step "cancel -> $(curl -s -XPOST "$GW/api/orders/$order/cancel" -H "Authorization: Bearer $CUSTOMER" | j status,statusReason)"
  sleep 2
  after=$(curl -s "$GW/api/kitchen/availability?date=$COOKING_DATE" | j reserved)
  step "reserved jars after release: $after (back to $before)"
}

scenario_idempotency() {
  bold "5. Idempotency: the same request sent twice creates one order"
  local key a b
  key=$(uuid)
  a=$(place_order "$CUSTOMER" 1 "$key" | j id)
  b=$(place_order "$CUSTOMER" 1 "$key" | j id)
  if [[ "$a" == "None" || -z "$a" ]]; then
    step "order was not accepted: $(place_order "$CUSTOMER" 1 | j code,message)"
  else
    step "Idempotency-Key $key -> $a / $b  $([[ "$a" == "$b" ]] && echo '(same order)' || echo '(DIFFERENT!)')"
  fi
}

scenario_async() {
  bold "6. Async notification-service (Kafka + Java 21 virtual threads)"
  # right after start-all the first Kafka delivery can take ~15s: wait until emails show up
  for _ in $(seq 1 20); do
    [[ "$(curl -s "$MAILPIT/api/v1/messages" | j total)" =~ ^[1-9] ]] && break
    sleep 1
  done
  step "last emails sent, with the thread that sent them:"
  curl -s "$GW/api/notifications/admin/recent?limit=4" -H "Authorization: Bearer $ADMIN" \
    | node -e 'for (const e of JSON.parse(require("fs").readFileSync(0, "utf8"))) console.log("   ", e.recipient, e.toAddress, "|", e.subject, "|", e.thread)'
  step "Mailpit inbox: $MAILPIT  ($(curl -s "$MAILPIT/api/v1/messages" | j total) messages)"
}

setup
case "${1:-all}" in
  security) scenario_security ;;
  order) scenario_order ;;
  saga) scenario_saga ;;
  cancel) scenario_cancel ;;
  idempotency) scenario_idempotency ;;
  async) scenario_async ;;
  all) scenario_security; scenario_order; scenario_saga; scenario_cancel; scenario_idempotency; scenario_async ;;
esac
