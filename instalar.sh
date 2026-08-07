#!/usr/bin/env bash

set -Eeuo pipefail

REPOSITORIO="https://github.com/sergiommacedo/gestao-pedidos.git"
PASTA="/opt/gestao-pedidos"
USUARIO_REAL="${SUDO_USER:-$USER}"

echo "========================================"
echo " Instalador - Gestão Pedidos"
echo "========================================"
echo

if [[ "$EUID" -ne 0 ]]; then
    echo "Execute com:"
    echo "sudo ./instalar.sh"
    exit 1
fi

if [[ ! -f /etc/os-release ]]; then
    echo "Erro: não foi possível identificar o Ubuntu."
    exit 1
fi

source /etc/os-release

if [[ "${ID}" != "ubuntu" ]]; then
    echo "Erro: este instalador foi preparado para Ubuntu."
    exit 1
fi

echo "Sistema: ${PRETTY_NAME}"
echo

# ------------------------------------------------
# Dependências básicas
# ------------------------------------------------

echo "[1/8] Instalando dependências..."

apt-get update
apt-get install -y \
    ca-certificates \
    curl \
    git \
    gnupg \
    openssl

# ------------------------------------------------
# Docker
# ------------------------------------------------

if command -v docker >/dev/null 2>&1; then
    echo "[2/8] Docker já está instalado."
else
    echo "[2/8] Instalando Docker..."

    install -m 0755 -d /etc/apt/keyrings

    curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
        -o /etc/apt/keyrings/docker.asc

    chmod a+r /etc/apt/keyrings/docker.asc

    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
      ${UBUNTU_CODENAME:-$VERSION_CODENAME} stable" \
      > /etc/apt/sources.list.d/docker.list

    apt-get update

    apt-get install -y \
        docker-ce \
        docker-ce-cli \
        containerd.io \
        docker-buildx-plugin \
        docker-compose-plugin

    systemctl enable docker
    systemctl start docker
fi

# ------------------------------------------------
# Usuário Docker
# ------------------------------------------------

echo "[3/8] Configurando usuário..."

usermod -aG docker "${USUARIO_REAL}" || true

# ------------------------------------------------
# Projeto
# ------------------------------------------------

echo "[4/8] Baixando Gestão Pedidos..."

if [[ -d "${PASTA}/.git" ]]; then

    git -C "${PASTA}" fetch origin main
    git -C "${PASTA}" reset --hard origin/main

else

    rm -rf "${PASTA}"

    git clone \
        --branch main \
        "${REPOSITORIO}" \
        "${PASTA}"
fi

mkdir -p "${PASTA}/backups"

chown -R "${USUARIO_REAL}:${USUARIO_REAL}" "${PASTA}"

# ------------------------------------------------
# .env
# ------------------------------------------------

echo "[5/8] Configurando banco e aplicação..."

if [[ ! -f "${PASTA}/.env" ]]; then

    MYSQL_PASSWORD="$(openssl rand -hex 24)"
    MYSQL_ROOT_PASSWORD="$(openssl rand -hex 24)"
    JWT_SECRET="$(openssl rand -base64 48 | tr -d '\n')"

    cat > "${PASTA}/.env" <<EOF
MYSQL_DATABASE=gestao_pedidos
MYSQL_USER=gestao_user
MYSQL_PASSWORD=${MYSQL_PASSWORD}
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}

APP_PORT=8080
APP_VERSION=latest

JWT_SECRET=${JWT_SECRET}

TZ=America/Sao_Paulo
EOF

    chmod 600 "${PASTA}/.env"
    chown "${USUARIO_REAL}:${USUARIO_REAL}" "${PASTA}/.env"

else
    echo ".env existente será preservado."
fi

# ------------------------------------------------
# Permissões dos scripts
# ------------------------------------------------

echo "[6/8] Preparando scripts..."

chmod +x "${PASTA}/gestao.sh" 2>/dev/null || true
chmod +x "${PASTA}/scripts/"*.sh 2>/dev/null || true
chmod +x "${PASTA}/instalar.sh"

# ------------------------------------------------
# Docker
# ------------------------------------------------

echo "[7/8] Baixando containers..."

cd "${PASTA}"

docker compose pull

echo
echo "Iniciando MySQL e aplicação..."

docker compose up -d

# ------------------------------------------------
# Backup automático
# ------------------------------------------------

echo "[8/8] Configurando backup automático..."

cat > /etc/cron.d/gestao-pedidos <<EOF
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

0 22 * * * root cd ${PASTA} && ./scripts/backup.sh >> ${PASTA}/backups/backup.log 2>&1
EOF

chmod 644 /etc/cron.d/gestao-pedidos

# ------------------------------------------------
# Aguarda aplicação
# ------------------------------------------------

echo
echo "Aguardando inicialização..."

SUCESSO=false

for tentativa in {1..90}; do

    if curl -fsS http://localhost:8080 >/dev/null 2>&1; then
        SUCESSO=true
        break
    fi

    sleep 2

done

echo

if [[ "${SUCESSO}" == "true" ]]; then

    IP_LOCAL="$(hostname -I | awk '{print $1}')"

    echo "========================================"
    echo " INSTALAÇÃO CONCLUÍDA"
    echo "========================================"
    echo
    echo "Sistema:"
    echo "http://localhost:8080"
    echo
    echo "Pela rede local:"
    echo "http://${IP_LOCAL}:8080"
    echo
    echo "Pasta:"
    echo "${PASTA}"
    echo
    echo "Banco criado automaticamente:"
    echo "gestao_pedidos"
    echo
    echo "Usuário MySQL:"
    echo "gestao_user"
    echo
    echo "Backup automático:"
    echo "Todos os dias às 22:00"
    echo
    echo "Administração:"
    echo "cd ${PASTA}"
    echo "./gestao.sh"
    echo
    echo "IMPORTANTE:"
    echo "As senhas do banco estão armazenadas em:"
    echo "${PASTA}/.env"
    echo
    echo "Não apague este arquivo."
    echo "========================================"

else

    echo "A aplicação não respondeu."
    echo
    docker compose ps
    echo
    docker compose logs --tail=100 app

    exit 1
fi