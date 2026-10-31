#!/usr/bin/env bash
# Circuit Breaker demo without JMeter (the JMeter plan is in jmeter/). Shows CLOSED -> OPEN -> HALF_OPEN -> CLOSED.
#   scripts/cb-demo.sh            slow kitchen (chaos delay 3s > read timeout 2s)
#   MODE=overload scripts/cb-demo.sh
#       real overload: start kitchen with KITCHEN_MAX_THREADS=5 and a small delay (300ms) so that
#       100 parallel requests queue up in the kitchen and become slow calls.
#   MODE=compare scripts/cb-demo.sh
#       same slow kitchen, first with the breaker DISABLED, then enabled: compare response times.
set -uo pipefail
GW=${GW:-http://localhost:9000}
ORDER=${ORDER:-http://localhost:9002}
cb() { curl -s -XPOST "$ORDER/actuator/circuitbreakers/kitchen" -H 'Content-Type: application/json' \
         -d "{\"updateState\":\"$1\"}" >/dev/null; }
MODE=${MODE:-slow}
REQUESTS=${REQUESTS:-150}
PARALLEL=${PARALLEL:-30}
CHICKEN=11111111-1111-1111-1111-111111111111

# $1 is a JS expression over the parsed body d (node is installed for the frontend anyway)
json() { node -e "const d = JSON.parse(require('fs').readFileSync(0, 'utf8')); console.log($1)"; }
state() { curl -s "$ORDER/actuator/circuitbreakers" | json "d['circuitBreakers']['kitchen']['state']"; }
chaos() { curl -s -XPUT "$GW/api/kitchen/admin/chaos" -H "Authorization: Bearer $ADMIN" \
            -H 'Content-Type: application/json' -d "{\"delayMs\":$1,\"failurePercent\":0}" >/dev/null; }
login() { curl -s -XPOST "$GW/api/auth/login" -H 'Content-Type: application/json' \
            -d "{\"email\":\"$1\",\"password\":\"$2\"}" | json "d['accessToken']"; }

fire() { # $1 = number of requests -> prints "<http code> <seconds>" per request
  seq "$1" | xargs -P "$PARALLEL" -I{} curl -s -o /dev/null -w "%{http_code} %{time_total}\n" \
    -XPOST "$GW/api/orders" -H "Authorization: Bearer $CUSTOMER" -H 'Content-Type: application/json' \
    -d "{\"items\":[{\"menuItemId\":\"$CHICKEN\",\"quantity\":1}],\"deliveryAddress\":\"CB demo\",\"confirmNextCookingDay\":true}"
}

summary() { # reads "<code> <time>" lines
  node -e '
const by = {};
for (const l of require("fs").readFileSync(0, "utf8").split("\n")) {
  const [code, t] = l.trim().split(/\s+/);
  if (code) (by[code] ??= []).push(parseFloat(t));
}
for (const code of Object.keys(by).sort()) {
  const ts = by[code], ms = x => String(Math.round(x * 1000)).padStart(7);
  console.log(`   HTTP ${code}: ${String(ts.length).padStart(4)} requests, avg ${ms(ts.reduce((a, b) => a + b) / ts.length)} ms, max ${ms(Math.max(...ts))} ms`);
}
'
}

ADMIN=$(login admin@pameobeo.vn Admin@123)
EMAIL="cb$(date +%s)@test.vn"
curl -s -XPOST "$GW/api/auth/register" -H 'Content-Type: application/json' \
     -d "{\"email\":\"$EMAIL\",\"password\":\"Password1\",\"fullName\":\"CB Demo\"}" >/dev/null
CUSTOMER=$(login "$EMAIL" Password1)
DAY=$(curl -s "$GW/api/kitchen/availability" | json "d['cookingDate']")
ORIGINAL_CAPACITY=$(curl -s "$GW/api/kitchen/availability?date=$DAY" | json "d['capacity']")
# The demo raises the capacity and places real orders: at the end, cancel them (gives the jars back) and put
# the real capacity back (50 by default), so the shop is left as it was.
cleanup() {
  echo
  echo "Cleaning up: cancelling the demo orders and restoring the capacity of $DAY..."
  curl -s "$GW/api/orders" -H "Authorization: Bearer $CUSTOMER" \
    | json "d.filter(o => o.status !== 'CANCELLED').map(o => o.id).join('\n')" \
    | while read -r id; do
        [[ -n "$id" ]] && curl -s -o /dev/null -XPOST "$GW/api/orders/$id/cancel" -H "Authorization: Bearer $CUSTOMER"
      done
  local reserved capacity result
  for _ in $(seq 1 30); do # jars are released asynchronously (OrderCancelled -> kitchen)
    reserved=$(curl -s "$GW/api/kitchen/availability?date=$DAY" | json "d.reserved")
    capacity=$(( reserved > ORIGINAL_CAPACITY ? reserved : ORIGINAL_CAPACITY ))
    result=$(curl -s -XPUT "$GW/api/kitchen/admin/cooking-days/$DAY/capacity" -H "Authorization: Bearer $ADMIN" \
             -H 'Content-Type: application/json' -d "{\"capacity\":$capacity}" | json "d.capacity")
    [[ "$result" == "$ORIGINAL_CAPACITY" ]] && break
    sleep 1
  done
  echo "(capacity of $DAY: $result jars, $reserved reserved)"
}
trap cleanup EXIT
curl -s -XPUT "$GW/api/kitchen/admin/cooking-days/$DAY/capacity" -H "Authorization: Bearer $ADMIN" \
     -H 'Content-Type: application/json' -d '{"capacity":100000}' >/dev/null

cb CLOSE
if [[ "$MODE" == "compare" ]]; then
  echo "A) WITHOUT circuit breaker (DISABLED), kitchen slow (3s), $REQUESTS requests x $PARALLEL parallel"
  cb DISABLE
  chaos 3000
  fire "$REQUESTS" | summary
  echo "   every request waits for the 2s timeout; order-service threads stay blocked the whole time"
  cb CLOSE
  echo "B) WITH circuit breaker, same load"
  fire "$REQUESTS" | summary
  echo "   after ~10 slow calls the breaker opens; the rest fail fast in a few ms"
  chaos 0
  cb CLOSE
  exit 0
fi

echo "1) Healthy kitchen. Breaker: $(state)"
chaos 0
fire 20 | summary

if [[ "$MODE" == "overload" ]]; then
  echo "2) Overload: kitchen limited to few threads + 300ms work per request, $REQUESTS requests x $PARALLEL parallel"
  chaos 300
else
  echo "2) Kitchen becomes slow (3s per quote, order-service read timeout is 2s), $REQUESTS requests x $PARALLEL parallel"
  chaos 3000
fi
fire "$REQUESTS" | summary
echo "   Breaker now: $(state)   <- OPEN: requests fail fast with 503 SERVICE_BUSY without calling the kitchen"

echo "3) Kitchen recovers. Waiting for OPEN -> HALF_OPEN (wait-duration-in-open-state = 10s)..."
chaos 0
for _ in $(seq 1 15); do [[ "$(state)" == "HALF_OPEN" ]] && break; sleep 1; done
echo "   Breaker: $(state)"
echo "4) A few trial calls in HALF_OPEN"
PARALLEL=1 fire 6 | summary
echo "   Breaker: $(state)   <- back to CLOSED"

echo
echo "State transitions recorded by Resilience4j:"
curl -s "$ORDER/actuator/circuitbreakerevents/kitchen" | node -e '
for (const e of JSON.parse(require("fs").readFileSync(0, "utf8")).circuitBreakerEvents)
  if (e.type === "STATE_TRANSITION") console.log("  ", e.creationTime.slice(11, 23), e.stateTransition);
'
