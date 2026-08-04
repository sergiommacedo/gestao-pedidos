#!/usr/bin/env bash

set -Eeuo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${BASE_DIR}"

if [[ ! -d ".git" ]]; then
    echo "Erro: esta pasta não é um repositório Git."
    exit 1
fi

if [[ -n "$(git status --porcelain)" ]]; then
    echo "Erro: existem alterações locais não salvas."
    echo
    git status --short
    echo
    echo "Faça commit ou reverta as alterações antes de atualizar."
    exit 1
fi

BRANCH_ATUAL="$(git branch --show-current)"

if [[ -z "${BRANCH_ATUAL}" ]]; then
    echo "Erro: não foi possível identificar a branch atual."
    exit 1
fi

echo "========================================"
echo "Atualização Gestão Pedidos"
echo "========================================"
echo "Branch: ${BRANCH_ATUAL}"
echo

echo "1/5 — Criando backup..."
"${BASE_DIR}/scripts/backup.sh"

echo
echo "2/5 — Buscando atualizações..."
git fetch origin
git pull --ff-only origin "${BRANCH_ATUAL}"

echo
echo "3/5 — Construindo nova aplicação..."
docker compose build app

echo
echo "4/5 — Atualizando container..."
docker compose up -d --no-deps app

echo
echo "5/5 — Verificando aplicação..."

set -a
source .env
set +a

APP_PORT="${APP_PORT:-8080}"

for tentativa in {1..60}; do
    if curl -fsS "http://localhost:${APP_PORT}" >/dev/null 2>&1; then
        echo
        echo "Atualização concluída com sucesso."
        echo "Versão Git:"
        git log -1 --oneline
        exit 0
    fi

    sleep 2
done

echo "Erro: aplicação não respondeu após a atualização."
echo
docker compose logs --tail=150 app
exit 1