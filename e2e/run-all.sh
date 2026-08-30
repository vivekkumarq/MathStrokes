#!/usr/bin/env bash
# Runs the end-to-end checks in order and reports a single tally.
#
# They are a one-shot suite: point them at a freshly migrated database. See README.md.
#
#   MATHSTROKES_API=http://localhost:8080/api ./run-all.sh
set -uo pipefail
cd "$(dirname "$0")"

SCRIPTS=(
  01_start_and_refresh.py
  02_autosave_and_access.py
  03_submit_and_score.py
  04_partial_marking_and_ranking.py
  05_historical_integrity.py
  06_expiry_and_autosubmit.py
)

rm -f .state.json
total=0
failed=0

for script in "${SCRIPTS[@]}"; do
  output=$(python "$script" 2>&1)
  passed=$(printf '%s' "$output" | grep -c "  PASS  ")
  if printf '%s' "$output" | grep -q "ALL PASSED"; then
    printf "  OK    %-38s %3d assertions\n" "$script" "$passed"
    total=$((total + passed))
  else
    printf "  FAIL  %s\n" "$script"
    printf '%s\n' "$output" | grep -E "FAIL|Traceback|Error" | head -5 | sed 's/^/        /'
    failed=$((failed + 1))
  fi
done

echo "---------------------------------------------------------"
if [ "$failed" -eq 0 ]; then
  echo "  $total assertions passed across ${#SCRIPTS[@]} scripts"
else
  echo "  $total assertions passed, $failed script(s) FAILED"
fi
exit "$failed"
