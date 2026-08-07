#!/usr/bin/env bash

set -Eeuo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${BASE_DIR}"

# Detectar Docker Compose
if docker compose version >/dev/null 2>&1; then
    COMPOSE=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
    COMPOSE=(docker-compose)
else
    echo "Erro: Docker Compose não encontrado."
    exit 1
fi

echo "Parando Gestão Pedidos..."
echo "Docker Compose: ${COMPOSE[*]}"

"${COMPOSE[@]}" down

echo
echo "Sistema parado."
echo "Volumes e dados foram preservados."