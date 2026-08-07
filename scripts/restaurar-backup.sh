#!/usr/bin/env bash

set -Eeuo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="${BASE_DIR}/backups"

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

if [[ ! -f "${BASE_DIR}/.env" ]]; then
    echo "Erro: arquivo .env não encontrado."
    exit 1
fi

if [[ ! -d "${BACKUP_DIR}" ]]; then
    echo "Erro: pasta de backups não encontrada."
    exit 1
fi

if ! "${COMPOSE[@]}" ps mysql 2>/dev/null | grep -q "Up"; then
    echo "Erro: o MySQL não está em execução."
    exit 1
fi

# ------------------------------------------------
# Localizar backups
# ------------------------------------------------

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
    echo "Nenhum backup encontrado em:"
    echo "${BACKUP_DIR}"
    exit 1
fi

# ------------------------------------------------
# Menu
# ------------------------------------------------

echo "========================================"
echo "Restaurar Gestão Pedidos"
echo "========================================"
echo
echo "Docker Compose: ${COMPOSE[*]}"
echo
echo "Backups disponíveis:"
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

# ------------------------------------------------
# Validar backup
# ------------------------------------------------

echo
echo "Validando backup..."

if ! gzip -t "${BACKUP}"; then
    echo "Erro: arquivo compactado inválido."
    exit 1
fi

if ! gzip -dc "${BACKUP}" | grep -q "CREATE TABLE"; then
    echo "Erro: o arquivo não parece conter um backup válido do banco."
    exit 1
fi

if [[ -f "${CHECKSUM}" ]]; then
    if ! sha256sum -c "${CHECKSUM}"; then
        echo "Erro: checksum inválido."
        exit 1
    fi

    echo "Checksum: OK"
else
    echo "Aviso: este backup não possui checksum."
fi

# ------------------------------------------------
# Confirmação
# ------------------------------------------------

echo
echo "========================================"
echo "ATENÇÃO"
echo "========================================"
echo
echo "O banco atual será substituído por:"
echo
echo "$(basename "${BACKUP}")"
echo
echo "Antes da restauração será criado um backup"
echo "de segurança do banco atual."
echo

read -r -p "Digite RESTAURAR para continuar: " CONFIRMACAO

if [[ "${CONFIRMACAO}" != "RESTAURAR" ]]; then
    echo "Operação cancelada."
    exit 0
fi

# ------------------------------------------------
# Backup de segurança
# ------------------------------------------------

echo
echo "Criando backup de segurança..."

"${BASE_DIR}/scripts/backup.sh"

# ------------------------------------------------
# Parar aplicação
# ------------------------------------------------

echo
echo "Parando somente a aplicação..."

"${COMPOSE[@]}" stop app

# ------------------------------------------------
# Recriar banco
# ------------------------------------------------

echo
echo "Recriando banco..."

"${COMPOSE[@]}" exec -T mysql sh -c '
    MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -u root -e "
        DROP DATABASE IF EXISTS \`$MYSQL_DATABASE\`;
        CREATE DATABASE \`$MYSQL_DATABASE\`
            CHARACTER SET utf8mb4
            COLLATE utf8mb4_unicode_ci;
        GRANT ALL PRIVILEGES
            ON \`$MYSQL_DATABASE\`.*
            TO '\''$MYSQL_USER'\''@'\''%'\'';
        FLUSH PRIVILEGES;
    "
'

# ------------------------------------------------
# Importar
# ------------------------------------------------

echo
echo "Importando backup..."

if ! gzip -dc "${BACKUP}" |
    "${COMPOSE[@]}" exec -T mysql sh -c '
        MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql \
            -u root \
            "$MYSQL_DATABASE"
    '; then

    echo
    echo "ERRO durante a restauração."
    echo "A aplicação permanecerá parada."
    echo
    echo "O backup de segurança criado antes da operação"
    echo "está disponível em:"
    echo "${BACKUP_DIR}"
    exit 1
fi

# ------------------------------------------------
# Iniciar aplicação
# ------------------------------------------------

echo
echo "Iniciando aplicação..."

"${COMPOSE[@]}" start app

# ------------------------------------------------
# Resultado
# ------------------------------------------------

echo
echo "========================================"
echo "Restauração concluída com sucesso"
echo "========================================"
echo
echo "Backup restaurado:"
echo "${BACKUP}"