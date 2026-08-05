#!/usr/bin/env bash

set -Eeuo pipefail

REPOSITORIO="https://github.com/sergiommacedo/gestao-pedidos.git"
PASTA_INSTALACAO="/opt/gestao-pedidos"
USUARIO_REAL="${SUDO_USER:-$USER}"

verificar_root() {
    if [[ "${EUID}" -ne 0 ]]; then
        echo "Execute este instalador com sudo:"
        echo "sudo ./instalar.sh"
        exit 1
    fi
}

verificar_ubuntu() {
    if [[ ! -f /etc/os-release ]]; then
        echo "Não foi possível identificar o sistema operacional."
        exit 1
    fi

    source /etc/os-release

    if [[ "${ID}" != "ubuntu" ]]; then
        echo "Este instalador foi preparado para Ubuntu."
        exit 1
    fi

    echo "Ubuntu detectado: ${PRETTY_NAME}"
}

instalar_dependencias() {
    apt-get update
    apt-get install -y ca-certificates curl git gnupg openssl
}

instalar_docker() {
    if command -v docker >/dev/null 2>&1; then
        echo "Docker já está instalado."
        return
    fi

    echo "Instalando Docker..."

    install -m 0755 -d /etc/apt/keyrings

    curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
        -o /etc/apt/keyrings/docker.asc

    chmod a+r /etc/apt/keyrings/docker.asc

    source /etc/os-release

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

    systemctl enable --now docker
}

configurar_usuario_docker() {
    usermod -aG docker "${USUARIO_REAL}" || true
}

baixar_projeto() {
    if [[ -d "${PASTA_INSTALACAO}/.git" ]]; then
        echo "Projeto já existe. Atualizando..."
        git -C "${PASTA_INSTALACAO}" pull --ff-only origin main
    else
        rm -rf "${PASTA_INSTALACAO}"
        git clone --branch main "${REPOSITORIO}" "${PASTA_INSTALACAO}"
    fi

    chown -R "${USUARIO_REAL}:${USUARIO_REAL}" "${PASTA_INSTALACAO}"
}

gerar_segredo() {
    openssl rand -hex 32
}

configurar_env() {
    local ENV_FILE="${PASTA_INSTALACAO}/.env"

    if [[ -f "${ENV_FILE}" ]]; then
        echo ".env já existe. Mantendo configuração atual."
        return
    fi

    local MYSQL_PASSWORD
    local MYSQL_ROOT_PASSWORD
    local JWT_SECRET

    MYSQL_PASSWORD="$(gerar_segredo)"
    MYSQL_ROOT_PASSWORD="$(gerar_segredo)"
    JWT_SECRET="$(gerar_segredo)"

    cat > "${ENV_FILE}" <<EOF
MYSQL_DATABASE=gestao_pedidos
MYSQL_USER=gestao_user
MYSQL_PASSWORD=${MYSQL_PASSWORD}
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}

APP_PORT=8080
APP_VERSION=latest
JWT_SECRET=${JWT_SECRET}
TZ=America/Sao_Paulo
EOF

    chmod 600 "${ENV_FILE}"
    chown "${USUARIO_REAL}:${USUARIO_REAL}" "${ENV_FILE}"
}

preparar_pastas() {
    mkdir -p \
        "${PASTA_INSTALACAO}/backups" \
        "${PASTA_INSTALACAO}/uploads"

    chmod +x \
        "${PASTA_INSTALACAO}/gestao.sh" \
        "${PASTA_INSTALACAO}/scripts/"*.sh \
        "${PASTA_INSTALACAO}/instalar.sh"

    chown -R "${USUARIO_REAL}:${USUARIO_REAL}" "${PASTA_INSTALACAO}"
}

iniciar_sistema() {
    cd "${PASTA_INSTALACAO}"

    docker compose pull
    docker compose up -d

    echo "Aguardando inicialização..."

    for tentativa in {1..90}; do
        if curl -fsS "http://localhost:8080" >/dev/null 2>&1; then
            return
        fi

        sleep 2
    done

    echo "A aplicação não respondeu no tempo esperado."
    docker compose ps
    docker compose logs --tail=150 app
    exit 1
}

configurar_backup_automatico() {
    local CRON_FILE="/etc/cron.d/gestao-pedidos"

    cat > "${CRON_FILE}" <<EOF
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

0 22 * * * ${USUARIO_REAL} ${PASTA_INSTALACAO}/scripts/backup.sh >> ${PASTA_INSTALACAO}/backups/backup.log 2>&1
30 7 * * 6 ${USUARIO_REAL} ${PASTA_INSTALACAO}/scripts/backup.sh >> ${PASTA_INSTALACAO}/backups/backup.log 2>&1
EOF

    chmod 644 "${CRON_FILE}"
}

mostrar_resultado() {
    local IP_LOCAL
    IP_LOCAL="$(hostname -I | awk '{print $1}')"

    echo
    echo "========================================"
    echo "Instalação concluída"
    echo "========================================"
    echo
    echo "Acesso nesta máquina:"
    echo "http://localhost:8080"
    echo
    echo "Acesso em outro aparelho da mesma rede:"
    echo "http://${IP_LOCAL}:8080"
    echo
    echo "Pasta instalada:"
    echo "${PASTA_INSTALACAO}"
    echo
    echo "Menu administrativo:"
    echo "cd ${PASTA_INSTALACAO}"
    echo "./gestao.sh"
    echo
    echo "Importante:"
    echo "Saia da sessão e entre novamente para usar Docker sem sudo."
}

verificar_root
verificar_ubuntu
instalar_dependencias
instalar_docker
configurar_usuario_docker
baixar_projeto
configurar_env
preparar_pastas
iniciar_sistema
configurar_backup_automatico
mostrar_resultado
