#!/usr/bin/env bash

set -Eeuo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${BASE_DIR}"

if [[ ! -f ".env" ]]; then
    echo "Erro: arquivo .env não encontrado."
    exit 1
fi

if [[ ! -d ".git" ]]; then
    echo "Erro: instalação não está vinculada ao Git."
    exit 1
fi

echo "========================================"
echo "Atualização Gestão Pedidos"
echo "========================================"

echo
echo "1/5 — Criando backup..."
"${BASE_DIR}/scripts/backup.sh"

echo
echo "2/5 — Atualizando scripts e configurações..."
git fetch origin main
git reset --hard origin/main

echo
echo "3/5 — Baixando imagem mais recente..."
docker compose pull app

echo
echo "4/5 — Recriando aplicação..."
docker compose up -d --no-deps app

echo
echo "5/5 — Verificando aplicação..."

set -a
source .env
set +a

APP_PORT="${APP_PORT:-8080}"

for tentativa in {1..90}; do
    if curl -fsS "http://localhost:${APP_PORT}" >/dev/null 2>&1; then
        echo
        echo "Atualização concluída com sucesso."
        docker image prune -f >/dev/null 2>&1 || true
        exit 0
    fi

    sleep 2
done

echo "Erro: aplicação não respondeu após atualização."
docker compose logs --tail=150 app
exit 1