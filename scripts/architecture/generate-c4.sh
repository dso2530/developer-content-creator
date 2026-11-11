#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

WORKSPACE="$ROOT_DIR/architecture/structurizr/workspace.dsl"
DIAGRAMS_DIR="$ROOT_DIR/docs/architecture/c4/diagrams"

STRUCTURIZR_VERSION="2026.10.26"
STRUCTURIZR_SHA256="495443517dc2186087c32c2eb6d762a9a265c8deabe509b072d49bea8d3049a4"

CACHE_DIR="${XDG_CACHE_HOME:-$HOME/.cache}/structurizr"
STRUCTURIZR_JAR="$CACHE_DIR/structurizr-${STRUCTURIZR_VERSION}.war"

MODE="${1:---write}"
OUTPUT_DIR="${2:-}"

case "$MODE" in
  --write|--check)
    ;;
  *)
    echo "Usage: $0 [--write|--check] [output-directory]"
    exit 1
    ;;
esac

test -f "$WORKSPACE" || {
  echo "Structurizr workspace not found: $WORKSPACE"
  exit 1
}

mkdir -p "$CACHE_DIR"

if [[ ! -f "$STRUCTURIZR_JAR" ]]; then
  echo "Downloading Structurizr $STRUCTURIZR_VERSION..."

  curl --fail --location --silent --show-error \
    "https://download.structurizr.com/structurizr-${STRUCTURIZR_VERSION}.war" \
    --output "$STRUCTURIZR_JAR"
fi

ACTUAL_SHA256="$(sha256sum "$STRUCTURIZR_JAR" | cut -d ' ' -f 1)"

if [[ "$ACTUAL_SHA256" != "$STRUCTURIZR_SHA256" ]]; then
  echo "ERROR: Structurizr SHA-256 mismatch"
  echo "Expected: $STRUCTURIZR_SHA256"
  echo "Actual:   $ACTUAL_SHA256"
  rm -f "$STRUCTURIZR_JAR"
  exit 1
fi

echo "Validating Structurizr workspace..."

java -jar "$STRUCTURIZR_JAR" validate \
  -workspace "$WORKSPACE"

TEMP_DIR=""

if [[ -z "$OUTPUT_DIR" ]]; then
  TEMP_DIR="$(mktemp -d)"
  OUTPUT_DIR="$TEMP_DIR"
fi

cleanup() {
  if [[ -n "$TEMP_DIR" ]]; then
    rm -rf "$TEMP_DIR"
  fi
}

trap cleanup EXIT

mkdir -p "$OUTPUT_DIR"

echo "Exporting Mermaid diagrams..."

java -jar "$STRUCTURIZR_JAR" export \
  -workspace "$WORKSPACE" \
  -format mermaid \
  -output "$OUTPUT_DIR"

case "$MODE" in
  --write)
    mkdir -p "$DIAGRAMS_DIR"

    rsync -a --delete \
      "$OUTPUT_DIR/" \
      "$DIAGRAMS_DIR/"

    echo "C4 diagrams successfully synchronized."
    echo "Destination: $DIAGRAMS_DIR"
    ;;

  --check)
    echo "Checking generated diagrams..."

    if ! diff -ru "$DIAGRAMS_DIR" "$OUTPUT_DIR"; then
      echo
      echo "ERROR: C4 diagrams are out of sync."
      echo "Run: ./scripts/architecture/generate-c4.sh --write"
      exit 1
    fi

    echo "C4 diagrams are synchronized."
    ;;
esac