#!/usr/bin/env bash

set -Eeuo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="${BASE_DIR}/backups"

cd "${BASE_DIR}"

if [[ ! -f "${BASE_DIR}/.env" ]]; then
    echo "Erro: arquivo .env não encontrado."
    exit 1
fi

mapfile -t BACKUPS < <(
    find "${BACKUP_DIR}" \
        -maxdepth 1 \
        -type f \
        -name "gestao_pedidos_*.sql.gz" \
        -printf '%T@ %p\n' |
    sort -nr |
    cut -d' ' -f2-
)

if [[ ${#BACKUPS[@]} -eq 0 ]]; then
    echo "Nenhum backup encontrado em ${BACKUP_DIR}."
    exit 1
fi

echo "========================================"
echo "Restaurar Gestão Pedidos"
echo "========================================"
echo

for i in "${!BACKUPS[@]}"; do
    NUMERO=$((i + 1))
    TAMANHO="$(du -h "${BACKUPS[$i]}" | cut -f1)"
    echo "${NUMERO}) $(basename "${BACKUPS[$i]}") — ${TAMANHO}"
done

echo
read -r -p "Escolha o número do backup: " ESCOLHA

if ! [[ "${ESCOLHA}" =~ ^[0-9]+$ ]]; then
    echo "Opção inválida."
    exit 1
fi

INDICE=$((ESCOLHA - 1))

if (( INDICE < 0 || INDICE >= ${#BACKUPS[@]} )); then
    echo "Opção inválida."
    exit 1
fi

BACKUP="${BACKUPS[$INDICE]}"
CHECKSUM="${BACKUP}.sha256"

if ! gzip -t "${BACKUP}"; then
    echo "Erro: arquivo compactado inválido."
    exit 1
fi

if [[ -f "${CHECKSUM}" ]]; then
    if ! sha256sum -c "${CHECKSUM}"; then
        echo "Erro: checksum inválido."
        exit 1
    fi
else
    echo "Aviso: este backup não possui checksum."
fi

echo
echo "ATENÇÃO!"
echo "O banco atual será substituído por:"
echo "${BACKUP}"
echo
read -r -p "Digite RESTAURAR para continuar: " CONFIRMACAO

if [[ "${CONFIRMACAO}" != "RESTAURAR" ]]; then
    echo "Operação cancelada."
    exit 0
fi

echo
echo "Criando backup de segurança antes da restauração..."

"${BASE_DIR}/scripts/backup.sh"

echo
echo "Parando a aplicação..."

docker compose stop app

echo "Recriando o banco..."

docker compose exec -T mysql sh -c '
    MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -u root -e "
        DROP DATABASE IF EXISTS \`$MYSQL_DATABASE\`;
        CREATE DATABASE \`$MYSQL_DATABASE\`
        CHARACTER SET utf8mb4
        COLLATE utf8mb4_unicode_ci;
        GRANT ALL PRIVILEGES ON \`$MYSQL_DATABASE\`.* TO
        '\''$MYSQL_USER'\''@'\''%'\'';
        FLUSH PRIVILEGES;
    "
'

echo "Importando o backup..."

if ! gzip -dc "${BACKUP}" |
    docker compose exec -T mysql sh -c '
        MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql \
            -u root \
            "$MYSQL_DATABASE"
    '; then

    echo "Erro durante a restauração."
    echo "A aplicação permanecerá parada."
    exit 1
fi

echo "Iniciando a aplicação..."

docker compose start app

echo
echo "Restauração concluída com sucesso."
echo "Backup utilizado:"
echo "${BACKUP}"