#!/usr/bin/env bash
# Guard: a document must never be empty or truncated. (A scripted edit once
# emptied docs/decisions.md; git had the copy, but this catches it instantly.)
set -e
cd "$(dirname "$0")/.."
fail=0
for f in docs/*.md ROADMAP.md README.md; do
  [ -f "$f" ] || continue
  lines=$(wc -l < "$f")
  if [ "$lines" -lt 3 ]; then
    echo "TRUNCATED: $f has $lines lines"
    fail=1
  fi
done
[ "$fail" -eq 0 ] && echo "docs ok"
exit $fail
