#!/usr/bin/env bash

set -Eeuo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="${BASE_DIR}/backups"
TEMP_DIR="${BACKUP_DIR}/.temp"

cd "${BASE_DIR}"

# ------------------------------------------------
# Docker
# ------------------------------------------------

if ! command -v docker >/dev/null 2>&1; then
    echo "Erro: Docker não está instalado."
    exit 1
fi

# ------------------------------------------------
# Detectar Docker Compose
# ------------------------------------------------

if docker compose version >/dev/null 2>&1; then
    COMPOSE=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
    COMPOSE=(docker-compose)
else
    echo "Erro: Docker Compose não está disponível."
    exit 1
fi

# ------------------------------------------------
# .env
# ------------------------------------------------

if [[ ! -f "${BASE_DIR}/.env" ]]; then
    echo "Erro: arquivo .env não encontrado."
    exit 1
fi

# ------------------------------------------------
# Verificar MySQL
# ------------------------------------------------

if ! "${COMPOSE[@]}" ps mysql 2>/dev/null | grep -q "Up"; then
    echo "Erro: o serviço MySQL não está em execução."
    exit 1
fi

BANCO="$(
    "${COMPOSE[@]}" exec -T mysql printenv MYSQL_DATABASE |
    tr -d '\r\n'
)"

if [[ -z "${BANCO}" ]]; then
    echo "Erro: não foi possível identificar o banco."
    exit 1
fi

# ------------------------------------------------
# Arquivos
# ------------------------------------------------

DATA_HORA="$(date '+%Y-%m-%d_%H-%M-%S')"
NOME_BASE="${BANCO}_${DATA_HORA}"

ARQUIVO_TEMP="${TEMP_DIR}/${NOME_BASE}.sql.gz.tmp"
ARQUIVO_FINAL="${BACKUP_DIR}/${NOME_BASE}.sql.gz"
CHECKSUM_FINAL="${ARQUIVO_FINAL}.sha256"

mkdir -p "${BACKUP_DIR}" "${TEMP_DIR}"
rm -f "${ARQUIVO_TEMP}"

INICIO="$(date +%s)"

echo "========================================"
echo "Backup Gestão Pedidos"
echo "========================================"
echo "Banco:          ${BANCO}"
echo "Docker Compose: ${COMPOSE[*]}"
echo "Data:           $(date '+%d/%m/%Y %H:%M:%S')"
echo
echo "Criando backup..."

# ------------------------------------------------
# Dump
# ------------------------------------------------

if ! "${COMPOSE[@]}" exec -T mysql sh -c '
    MYSQL_PWD="$MYSQL_PASSWORD" mysqldump \
        -u "$MYSQL_USER" \
        --single-transaction \
        --quick \
        --routines \
        --triggers \
        --events \
        --no-tablespaces \
        --default-character-set=utf8mb4 \
        "$MYSQL_DATABASE"
' | gzip -c > "${ARQUIVO_TEMP}"; then

    rm -f "${ARQUIVO_TEMP}"

    echo
    echo "Erro: o mysqldump falhou."
    exit 1
fi

# ------------------------------------------------
# Validar tamanho
# ------------------------------------------------

if [[ ! -s "${ARQUIVO_TEMP}" ]]; then
    rm -f "${ARQUIVO_TEMP}"

    echo "Erro: o arquivo de backup foi criado vazio."
    exit 1
fi

# ------------------------------------------------
# Validar GZIP
# ------------------------------------------------

if ! gzip -t "${ARQUIVO_TEMP}"; then
    rm -f "${ARQUIVO_TEMP}"

    echo "Erro: o arquivo compactado está corrompido."
    exit 1
fi

# ------------------------------------------------
# Validar SQL
# ------------------------------------------------

if ! gzip -dc "${ARQUIVO_TEMP}" | grep -q "CREATE TABLE"; then
    rm -f "${ARQUIVO_TEMP}"

    echo "Erro: o backup não contém estruturas de tabelas."
    exit 1
fi

# ------------------------------------------------
# Finalizar
# ------------------------------------------------

mv "${ARQUIVO_TEMP}" "${ARQUIVO_FINAL}"

sha256sum "${ARQUIVO_FINAL}" > "${CHECKSUM_FINAL}"

if ! sha256sum -c "${CHECKSUM_FINAL}" >/dev/null 2>&1; then

    rm -f "${ARQUIVO_FINAL}" "${CHECKSUM_FINAL}"

    echo "Erro: falha na validação do checksum."
    exit 1
fi

# ------------------------------------------------
# Limpeza — manter 30 dias
# ------------------------------------------------

find "${BACKUP_DIR}" \
    -maxdepth 1 \
    -type f \
    \( -name "${BANCO}_*.sql.gz" -o -name "${BANCO}_*.sql.gz.sha256" \) \
    -mtime +30 \
    -delete

# ------------------------------------------------
# Resultado
# ------------------------------------------------

FIM="$(date +%s)"
TEMPO="$((FIM - INICIO))"
TAMANHO="$(du -h "${ARQUIVO_FINAL}" | cut -f1)"

echo
echo "========================================"
echo "Backup concluído com sucesso"
echo "========================================"
echo "Arquivo:  ${ARQUIVO_FINAL}"
echo "Tamanho:  ${TAMANHO}"
echo "Checksum: ${CHECKSUM_FINAL}"
echo "Tempo:    ${TEMPO}s"