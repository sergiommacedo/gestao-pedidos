#!/usr/bin/env bash

set -Eeuo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${BASE_DIR}"

echo "Parando Gestão Pedidos..."

docker compose down

echo "Sistema parado."
echo "Volumes e dados foram preservados."