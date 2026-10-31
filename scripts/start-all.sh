#!/usr/bin/env bash
# Starts the 5 services on the host (infra must be running: docker compose up -d).
# Usage: scripts/start-all.sh            build + start
#        SKIP_BUILD=1 scripts/start-all.sh
#        KITCHEN_MAX_THREADS=5 scripts/start-all.sh   (overload scenario for the CB demo)
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/lib.sh
mkdir -p .run

java_hint() {
  if is_windows; then
    echo '  export JAVA_HOME="/c/Program Files/Java/jdk-21.0.12.1"   # your JDK 21 folder'
  else
    echo '  export JAVA_HOME=$(/usr/libexec/java_home -v 21)          # macOS'
  fi
  echo '  export PATH="$JAVA_HOME/bin:$PATH"'
}
if [[ "$(java_major)" -lt 21 ]]; then
  echo "java on PATH is version $(java_major), the services need Java 21+. Point to a JDK 21 first:"
  java_hint; exit 1
fi

if [[ "${SKIP_BUILD:-0}" != "1" ]]; then
  # Maven compiles with JAVA_HOME, which may differ from the java on PATH
  mvn_java=$(mvn -v 2>/dev/null | sed -nE 's/^Java version: ([0-9]+).*/\1/p')
  if [[ "${mvn_java:-0}" -lt 21 ]]; then
    echo "Maven runs on Java ${mvn_java:-?} (JAVA_HOME=${JAVA_HOME:-unset}), the build needs Java 21+. Point to a JDK 21 first:"
    java_hint; exit 1
  fi
  # Rebuilding replaces the jars that running services load classes from -> stop them first
  running=0
  for port in 9000 9001 9002 9003 9004; do
    port_in_use "$port" && running=1 && break
  done
  if is_windows && [[ -n "$(service_pids)" ]]; then running=1; fi
  if [[ $running == 1 ]]; then
    echo "Services are running: stopping them before rebuilding the jars"
    scripts/stop-all.sh
    sleep 3
  fi
  mvn -q install -DskipTests
fi

start() {
  local name=$1 port=$2; shift 2
  if service_running "$name" "$port"; then
    echo "  $name already running on :$port"; return
  fi
  nohup java -Xmx256m "$@" -jar "$name/target/$name-1.0.0-SNAPSHOT.jar" > ".run/$name.log" 2>&1 &
  echo $! > ".run/$name.pid"
  echo "  starting $name on :$port (log: .run/$name.log)"
}

wait_up() {
  local name=$1 port=$2
  for _ in $(seq 1 90); do
    if curl -fs "localhost:$port/actuator/health" >/dev/null 2>&1; then echo "  $name UP"; return; fi
    sleep 1
  done
  echo "  $name did not start, see .run/$name.log"; exit 1
}

echo "Starting auth-service first (others download its public key)..."
start auth-service 9001
wait_up auth-service 9001

start kitchen-service 9003
start order-service 9002
start notification-service 9004
start api-gateway 9000
for s in "kitchen-service 9003" "order-service 9002" "notification-service 9004" "api-gateway 9000"; do
  wait_up $s
done

cat <<INFO

All services are up.
  Web UI         http://localhost:9000   (admin@pameobeo.vn / Admin@123)
  Kafka UI       http://localhost:8090
  Mailpit        http://localhost:8025
  CB state       http://localhost:9002/actuator/circuitbreakers
INFO
