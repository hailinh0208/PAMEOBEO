#!/usr/bin/env bash
# Runs the JMeter plan in CLI mode and builds the HTML dashboard.
#   jmeter/run-cb-test.sh                         defaults: 50 threads, 60 s
#   jmeter/run-cb-test.sh -Jthreads=200 -Jduration=90
# GUI: jmeter -t jmeter/circuit-breaker-demo.jmx
set -euo pipefail
cd "$(dirname "$0")"
JMETER=${JMETER:-jmeter}
STAMP=$(date +%Y%m%d-%H%M%S)
mkdir -p results
"$JMETER" -n -t circuit-breaker-demo.jmx -l "results/$STAMP.jtl" -e -o "results/$STAMP-report" "$@"
echo "HTML report: jmeter/results/$STAMP-report/index.html"
