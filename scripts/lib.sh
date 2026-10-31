# Helpers shared by start-all.sh and stop-all.sh. Works on macOS/Linux (lsof) and Windows Git Bash (netstat).

is_windows() { [[ "$(uname -s)" == MINGW* || "$(uname -s)" == MSYS* || "$(uname -s)" == CYGWIN* ]]; }

# Prints the pid(s) listening on TCP port $1 (nothing if the port is free)
listening_pids() {
  if command -v lsof >/dev/null 2>&1; then
    lsof -tiTCP:"$1" -sTCP:LISTEN 2>/dev/null
  else
    netstat -ano 2>/dev/null | tr -d '\r' \
      | awk -v p=":$1" '$1 == "TCP" && substr($2, length($2) - length(p) + 1) == p && $4 == "LISTENING" { print $5 }' \
      | sort -u
  fi
}

port_in_use() { [[ -n "$(listening_pids "$1")" ]]; }

SERVICES_REGEX='(auth-service|kitchen-service|order-service|notification-service|api-gateway)'

# Windows only: pids of the java.exe processes running service $1 (all 5 services if omitted).
# Matches the command line "java ... -jar <service>/target/..." used by start-all.sh. Needed because
# on Windows the .pid file holds a Git Bash pid and netstat may not list the listening ports.
service_pids() {
  local name=${1:-$SERVICES_REGEX}
  powershell.exe -NoProfile -NonInteractive -Command \
    "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -match '-jar $name/target/' } | ForEach-Object { \$_.ProcessId }" \
    2>/dev/null | tr -d '\r'
}

# True if service $1 (listening on port $2) is running
service_running() {
  port_in_use "$2" && return 0
  is_windows && [[ -n "$(service_pids "$1")" ]]
}

kill_pid() {
  if is_windows; then taskkill //PID "$1" //F >/dev/null 2>&1; else kill "$1" 2>/dev/null; fi
}

# Major version of the java on PATH, e.g. 21
java_major() { java -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+).*/\1/'; }
