#!/usr/bin/env bash

set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
shopt -s globstar nullglob
errors=0
checked=0

echo "=== Vérification des rapports JaCoCo ==="

for classes_dir in "$BACKEND_DIR"/**/target/classes; do
  [[ -d "$classes_dir" ]] || continue

  module_dir="${classes_dir%/target/classes}"
  module_name="$(basename "$module_dir")"

  # Ignorer les modules qui ne contiennent aucune classe compilée.
  if ! find "$classes_dir" -type f -name '*.class' -print -quit |
      grep -q .; then
    echo "[INFO] $module_name : aucune classe compilée, contrôle ignoré."
    continue
  fi

  checked=$((checked + 1))

  exec_file="$module_dir/target/jacoco.exec"
  report_file="$module_dir/target/site/jacoco/jacoco.xml"

  module_errors=0

  if [[ ! -s "$exec_file" ]]; then
    echo "[ERREUR] $module_name : fichier jacoco.exec absent ou vide."
    module_errors=1
  fi

  if [[ ! -s "$report_file" ]]; then
    echo "[ERREUR] $module_name : rapport jacoco.xml absent ou vide."
    module_errors=1
  fi

  if [[ "$module_errors" -eq 0 ]]; then
    echo "[OK] $module_name : données et rapport JaCoCo présents."
  else
    errors=$((errors + 1))
  fi
done

echo
echo "Modules contenant des classes compilées contrôlés : $checked"
echo "Modules en erreur : $errors"

if [[ "$checked" -eq 0 ]]; then
  echo "[ERREUR] Aucune classe de production détectée."
  exit 1
fi

if [[ "$errors" -ne 0 ]]; then
  echo "[ÉCHEC] Vérification JaCoCo."
  exit 1
fi

echo "[SUCCÈS] Tous les modules concernés possèdent leurs données JaCoCo."
