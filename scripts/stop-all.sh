#!/usr/bin/env bash
cd "$(dirname "$0")/.."
source scripts/lib.sh
for pid in .run/*.pid; do
  [[ -f "$pid" ]] || continue
  kill "$(cat "$pid")" 2>/dev/null && echo "stopped $(basename "$pid" .pid)"
  rm -f "$pid"
done
# services started by hand without a pid file
for port in 9000 9001 9002 9003 9004; do
  for p in $(listening_pids "$port"); do
    kill_pid "$p" && echo "stopped process on :$port"
  done
done
# Windows: the pid file holds a Git Bash pid, so the java.exe processes (launcher + JVM) are found
# by their command line instead
if is_windows; then
  for p in $(service_pids); do
    kill_pid "$p" && echo "stopped java.exe pid $p"
  done
  # wait until the JVMs are gone, otherwise a rebuild fails with "Unable to rename ... .jar"
  for _ in $(seq 1 15); do
    [[ -z "$(service_pids)" ]] && break
    sleep 1
  done
  [[ -n "$(service_pids)" ]] && echo "warning: some services are still running: $(service_pids | tr '\n' ' ')"
fi
true
