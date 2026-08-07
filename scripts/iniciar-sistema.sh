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

if [[ ! -f ".env" ]]; then
    echo "Erro: arquivo .env não encontrado."
    exit 1
fi

set -a
source .env
set +a

APP_PORT="${APP_PORT:-8080}"

echo "Iniciando Gestão Pedidos..."
echo "Docker Compose: ${COMPOSE[*]}"

"${COMPOSE[@]}" up -d

echo "Aguardando aplicação..."

for tentativa in {1..60}; do
    if curl -fsS "http://localhost:${APP_PORT}" >/dev/null 2>&1; then
        echo
        echo "Sistema iniciado com sucesso."
        echo
        echo "Acesso local:"
        echo "http://localhost:${APP_PORT}"
        echo
        echo "Acesso pela rede:"
        IP_LOCAL="$(hostname -I | awk '{print $1}')"
        echo "http://${IP_LOCAL}:${APP_PORT}"
        exit 0
    fi

    sleep 2
done

echo
echo "Erro: a aplicação não respondeu no tempo esperado."
echo
echo "Status:"
"${COMPOSE[@]}" ps

echo
echo "Logs da aplicação:"
"${COMPOSE[@]}" logs --tail=100 app

exit 1