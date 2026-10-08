#!/usr/bin/env bash
#
# W42 compile-gate (PR #233) — resolve each corpus cell's UPSTREAM 9.83.0 javac
# classpath into the directory the D11CompileGateTest harness reads.
#
# WHY: generated Java targets the upstream 9.83.0 runtime (com.regnosys.rosetta.lib /
# org.finos.rune:rune-runtime at 9.83.0), NOT the fork's own rune-runtime
# (0.0.0.main-SNAPSHOT, whose interfaces have drifted). Compiling the goldens against
# the fork runtime fails ~1110 @Override checks for cdm5; against the upstream 9.83.0
# classpath it is 0 errors (verified at the cdm5 proving checkpoint). Each cell's own
# rosetta-source/pom.xml declares the exact upstream deps, so build-classpath against it
# yields the right per-cell closure (cdm/iso/drr → com.regnosys.rosetta.lib:9.83.0;
# rune-fpml → org.finos.rune:rune-runtime:9.83.0).
#
# USAGE (from anywhere; resolves repo root from this script's location):
#   scripts/compile-gate/resolve-classpaths.sh
# Then run the gate, e.g.:
#   mvn -f rune-java-generator/pom.xml test -Dtest=D11CompileGateTest#cdm5 -Drune.excludedGroups=none
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$ROOT/rune-java-generator/target/compile-gate"

# cell-key -> test-corpus relative cell dir (key matches CellSpec corpus-version).
CELLS=(
  "cdm-5.38.0:cdm/cdm-5.38.0"
  "cdm-6.20.6:cdm/cdm-6.20.6"
  "drr-6.34.1:drr/drr-6.34.1"
  "iso20022-1.38.0:iso20022/iso20022-1.38.0"
  "rune-fpml-2.0.0:rune-fpml/rune-fpml-2.0.0"
)

for entry in "${CELLS[@]}"; do
  key="${entry%%:*}"
  rel="${entry#*:}"
  pom="$ROOT/test-corpus/$rel/rosetta-source/pom.xml"
  dest="$OUT/$key"
  if [[ ! -f "$pom" ]]; then
    echo "SKIP $key — no pom at $pom"
    continue
  fi
  mkdir -p "$dest"
  echo "Resolving $key classpath ..."
  mvn -q -f "$pom" dependency:build-classpath -Dmdep.outputFile="$dest/upstream-cp.txt"
done

echo "Done. Per-cell classpaths under $OUT/<cell>/upstream-cp.txt"
