#!/usr/bin/env bash

set -Eeuo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${BASE_DIR}"

# ------------------------------------------------
# Detectar Docker Compose
# ------------------------------------------------

if docker compose version >/dev/null 2>&1; then
    COMPOSE=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
    COMPOSE=(docker-compose)
else
    echo "Erro: Docker Compose não encontrado."
    exit 1
fi

# ------------------------------------------------
# Validações
# ------------------------------------------------

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
echo "Docker Compose: ${COMPOSE[*]}"
echo

# ------------------------------------------------
# 1. Backup
# ------------------------------------------------

echo "1/5 — Criando backup..."

"${BASE_DIR}/scripts/backup.sh"

echo
echo "Backup concluído."

# ------------------------------------------------
# 2. Atualizar arquivos do GitHub
# ------------------------------------------------

echo
echo "2/5 — Atualizando scripts e configurações..."

git fetch origin main
git reset --hard origin/main

echo
echo "Versão recebida:"
git log -1 --oneline

# ------------------------------------------------
# 3. Baixar imagem Docker atualizada
# ------------------------------------------------

echo
echo "3/5 — Baixando imagem mais recente..."

"${COMPOSE[@]}" pull app

# ------------------------------------------------
# 4. Recriar somente a aplicação
# ------------------------------------------------

echo
echo "4/5 — Atualizando aplicação..."

"${COMPOSE[@]}" up -d --no-deps app

# ------------------------------------------------
# 5. Verificar aplicação
# ------------------------------------------------

echo
echo "5/5 — Verificando aplicação..."

set -a
source .env
set +a

APP_PORT="${APP_PORT:-8080}"

for tentativa in {1..90}; do

    if curl -fsS "http://localhost:${APP_PORT}" >/dev/null 2>&1; then

        echo
        echo "========================================"
        echo "ATUALIZAÇÃO CONCLUÍDA"
        echo "========================================"
        echo
        echo "Aplicação:"
        echo "http://localhost:${APP_PORT}"
        echo
        echo "Versão Git:"
        git log -1 --oneline
        echo

        docker image prune -f >/dev/null 2>&1 || true

        exit 0
    fi

    sleep 2
done

echo
echo "Erro: aplicação não respondeu após atualização."
echo
echo "Status dos containers:"
"${COMPOSE[@]}" ps

echo
echo "Últimos logs da aplicação:"
"${COMPOSE[@]}" logs --tail=150 app

exit 1