#!/usr/bin/env bash

set -u

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

while true; do
    clear

    echo "========================================"
    echo "       GESTÃO PEDIDOS — VOVÓ DAN"
    echo "========================================"
    echo
    echo "1 - Iniciar sistema"
    echo "2 - Parar sistema"
    echo "3 - Reiniciar sistema"
    echo "4 - Criar backup"
    echo "5 - Restaurar backup"
    echo "6 - Atualizar sistema"
    echo "7 - Ver status"
    echo "8 - Ver logs da aplicação"
    echo "9 - Ver logs do MySQL"
    echo "0 - Sair"
    echo
    echo "========================================"

    read -r -p "Escolha uma opção: " OPCAO

    case "${OPCAO}" in
        1)
            "${BASE_DIR}/scripts/iniciar-sistema.sh"
            ;;
        2)
            "${BASE_DIR}/scripts/parar-sistema.sh"
            ;;
        3)
            docker compose -f "${BASE_DIR}/docker-compose.yml" restart
            ;;
        4)
            "${BASE_DIR}/scripts/backup.sh"
            ;;
        5)
            "${BASE_DIR}/scripts/restaurar-backup.sh"
            ;;
        6)
            "${BASE_DIR}/scripts/atualizar-sistema.sh"
            ;;
        7)
            cd "${BASE_DIR}" && docker compose ps
            ;;
        8)
            cd "${BASE_DIR}" && docker compose logs --tail=150 app
            ;;
        9)
            cd "${BASE_DIR}" && docker compose logs --tail=150 mysql
            ;;
        0)
            echo "Até mais."
            exit 0
            ;;
        *)
            echo "Opção inválida."
            ;;
    esac

    echo
    read -r -p "Pressione Enter para continuar..."
done