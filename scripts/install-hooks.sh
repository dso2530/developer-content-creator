#!/usr/bin/env bash
set -euo pipefail
git config core.hooksPath .githooks
chmod +x .githooks/*
echo "✅ Hooks Git installés (core.hooksPath=.githooks)"