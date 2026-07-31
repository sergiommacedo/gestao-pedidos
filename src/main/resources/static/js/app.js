document.addEventListener("DOMContentLoaded", () => {

    inicializarMascaras();
    inicializarBuscaCep();
    inicializarMascaraMonetaria();
    inicializarDatasBrasileiras();
    inicializarModalExclusao();
    inicializarModalCancelamentoPedido();
    inicializarModalDetalhesPedido();
    inicializarSelecaoPedidos();
    inicializarDropdownsStatus();
    inicializarImpressaoComandas();
    inicializarFormularioPedido();
    inicializarAlternanciaSenha();
    inicializarModalRedefinirSenhaUsuario();
    inicializarPreviewConfiguracaoEmpresa();

});


function inicializarPreviewConfiguracaoEmpresa() {
    const formulario = document.querySelector("[data-configuracao-empresa-form]");

    if (!formulario) {
        return;
    }

    const preview = formulario.querySelector("[data-config-preview]");
    const logoInput = formulario.querySelector("[data-logo-empresa-input]");
    const logoPreview = formulario.querySelector("[data-logo-empresa-preview]");
    const removerLogo = formulario.querySelector("[data-remover-logo]");
    const nomeInput = formulario.querySelector("[data-preview-nome-empresa]");
    const nomePreview = formulario.querySelector("[data-preview-nome]");
    const classesTema = ["tema-marrom", "tema-azul", "tema-verde", "tema-vinho", "tema-roxo"];
    const logoInicial = logoPreview.src;
    let urlTemporaria;

    formulario.querySelectorAll("[data-theme-css]").forEach(opcao => {
        opcao.addEventListener("change", () => {
            preview.classList.remove(...classesTema);
            preview.classList.add(opcao.dataset.themeCss);
        });
    });

    nomeInput.addEventListener("input", () => {
        nomePreview.textContent = nomeInput.value.trim() || "Empresa";
    });

    logoInput.addEventListener("change", () => {
        if (urlTemporaria) {
            URL.revokeObjectURL(urlTemporaria);
        }

        const arquivo = logoInput.files?.[0];
        if (!arquivo) {
            logoPreview.src = removerLogo?.checked ? preview.dataset.logoPadrao : logoInicial;
            return;
        }

        urlTemporaria = URL.createObjectURL(arquivo);
        logoPreview.src = urlTemporaria;
        if (removerLogo) {
            removerLogo.checked = false;
        }
    });

    removerLogo?.addEventListener("change", () => {
        if (removerLogo.checked) {
            logoInput.value = "";
            if (urlTemporaria) {
                URL.revokeObjectURL(urlTemporaria);
                urlTemporaria = undefined;
            }
            logoPreview.src = preview.dataset.logoPadrao;
        } else {
            logoPreview.src = logoInicial;
        }
    });

    window.addEventListener("beforeunload", () => {
        if (urlTemporaria) {
            URL.revokeObjectURL(urlTemporaria);
        }
    }, {once: true});
}


function inicializarAlternanciaSenha() {

    document.querySelectorAll("[data-password-toggle], [data-alternar-senha]").forEach(botao => {
        const seletorCampo = botao.dataset.passwordTarget || botao.dataset.campoSenha;
        const campo = seletorCampo ? document.querySelector(seletorCampo) : null;

        if (!campo) {
            return;
        }

        botao.addEventListener("click", () => {
            const senhaVisivel = campo.type === "text";
            campo.type = senhaVisivel ? "password" : "text";

            const novoRotulo = senhaVisivel ? "Mostrar senha" : "Ocultar senha";
            botao.setAttribute("aria-label", novoRotulo);
            botao.setAttribute("title", novoRotulo);
            botao.setAttribute("aria-pressed", String(!senhaVisivel));

            const icone = botao.querySelector("i");
            icone?.classList.toggle("bi-eye", senhaVisivel);
            icone?.classList.toggle("bi-eye-slash", !senhaVisivel);

            campo.focus();
        });
    });
}


function inicializarModalRedefinirSenhaUsuario() {
    const modalElemento = document.querySelector("#modalRedefinirSenhaUsuario");

    if (!modalElemento) {
        return;
    }

    const formulario = modalElemento.querySelector("[data-form-redefinir-senha-usuario]");
    const mensagem = modalElemento.querySelector("[data-usuario-redefinicao]");
    const novaSenha = modalElemento.querySelector("[data-nova-senha-usuario]");
    const confirmarSenha = modalElemento.querySelector("[data-confirmar-senha-usuario]");

    modalElemento.addEventListener("show.bs.modal", event => {
        const botao = event.relatedTarget;
        formulario.action = botao?.dataset.redefinirSenhaUrl || "";
        mensagem.textContent = `Informe uma nova senha para ${botao?.dataset.usuarioNome || "o usuário"}.`;
    });

    formulario.addEventListener("submit", event => {
        const senhaValida = novaSenha.value.length >= 6;
        const confirmacaoValida = novaSenha.value === confirmarSenha.value;

        novaSenha.classList.toggle("is-invalid", !senhaValida);
        confirmarSenha.classList.toggle("is-invalid", !confirmacaoValida);

        if (!senhaValida || !confirmacaoValida) {
            event.preventDefault();
            (!senhaValida ? novaSenha : confirmarSenha).focus();
        }
    });

    modalElemento.addEventListener("shown.bs.modal", () => novaSenha.focus());
    modalElemento.addEventListener("hidden.bs.modal", () => {
        formulario.reset();
        formulario.removeAttribute("action");
        mensagem.textContent = "Informe uma nova senha para o usuário.";
        novaSenha.classList.remove("is-invalid");
        confirmarSenha.classList.remove("is-invalid");
    });
}


function inicializarModalDetalhesPedido() {

    const modalElemento = document.querySelector("#modalDetalhesPedido");

    if (!modalElemento) {
        return;
    }

    const conteudo = modalElemento.querySelector("[data-conteudo-detalhes-pedido]");
    let controladorRequisicao;

    const mostrarCarregamento = () => {
        conteudo.innerHTML = `
            <div class="modal-header">
                <h2 class="modal-title fs-5" id="tituloModalDetalhesPedido">
                    <i class="bi bi-receipt me-1"></i> Detalhes do pedido
                </h2>
                <button type="button" class="btn-close" data-bs-dismiss="modal"
                        aria-label="Fechar"></button>
            </div>
            <div class="modal-body py-5 text-center">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Carregando...</span>
                </div>
                <p class="text-muted mb-0 mt-3">Carregando pedido...</p>
            </div>`;
    };

    modalElemento.addEventListener("show.bs.modal", async event => {
        const url = event.relatedTarget?.dataset.detalhesUrl;
        mostrarCarregamento();

        if (!url) {
            conteudo.querySelector(".modal-body").innerHTML = `
                <div class="alert alert-danger mb-0">
                    Não foi possível identificar o pedido selecionado.
                </div>`;
            return;
        }

        controladorRequisicao?.abort();
        controladorRequisicao = new AbortController();

        try {
            const resposta = await fetch(url, {
                headers: {"X-Requested-With": "XMLHttpRequest"},
                signal: controladorRequisicao.signal
            });

            if (!resposta.ok) {
                throw new Error("Falha ao carregar os detalhes do pedido.");
            }

            conteudo.innerHTML = await resposta.text();
        } catch (erro) {
            if (erro.name === "AbortError") {
                return;
            }

            conteudo.querySelector(".modal-body").innerHTML = `
                <div class="alert alert-danger mb-0">
                    <i class="bi bi-exclamation-triangle me-1"></i>
                    Não foi possível carregar os detalhes do pedido. Tente novamente.
                </div>`;
        }
    });

    modalElemento.addEventListener("hidden.bs.modal", () => {
        controladorRequisicao?.abort();
        controladorRequisicao = undefined;
        mostrarCarregamento();
    });
}


function inicializarModalCancelamentoPedido() {

    const modalElemento = document.querySelector("#modalCancelamentoPedido");

    if (!modalElemento || typeof bootstrap === "undefined") {
        return;
    }

    const formulario = modalElemento.querySelector("[data-form-cancelamento-pedido]");
    const mensagem = modalElemento.querySelector("[data-mensagem-cancelamento]");
    const motivo = modalElemento.querySelector("[data-motivo-cancelamento]");
    const camposRetorno = [
        "filtro",
        "statusFiltro",
        "dataAgendada",
        "pagina",
        "tamanho",
        "ordenarPor",
        "direcao",
        "visualizacao",
        "mostrarCancelados"
    ];
    const modal = bootstrap.Modal.getOrCreateInstance(modalElemento);

    document.querySelectorAll("[data-cancelar-pedido]").forEach(botao => {
        botao.addEventListener("click", () => {
            const formularioOrigem = botao.closest("form");
            const pedidoId = botao.dataset.pedidoId || "";
            const cliente = botao.dataset.cliente || "";

            formulario.action = formularioOrigem?.action || "";
            mensagem.textContent = `Deseja cancelar o pedido #${pedidoId} de ${cliente}?`;

            camposRetorno.forEach(nome => {
                const origem = formularioOrigem?.querySelector(`[name="${nome}"]`);
                formulario.querySelector(`[name="${nome}"]`).value = origem?.value || "";
            });

            motivo.value = "";
            motivo.classList.remove("is-invalid");
            modal.show();
        });
    });

    formulario.addEventListener("submit", event => {
        motivo.value = motivo.value.trim();

        if (!motivo.value) {
            event.preventDefault();
            motivo.classList.add("is-invalid");
            motivo.focus();
            return;
        }

        motivo.classList.remove("is-invalid");
    });

    motivo.addEventListener("input", () => {
        motivo.classList.toggle("is-invalid", !motivo.value.trim());
    });

    modalElemento.addEventListener("shown.bs.modal", () => motivo.focus());
    modalElemento.addEventListener("hidden.bs.modal", () => {
        formulario.reset();
        formulario.removeAttribute("action");
        mensagem.textContent = "Deseja cancelar este pedido?";
        motivo.classList.remove("is-invalid");
    });
}


function inicializarDropdownsStatus() {

    if (typeof bootstrap === "undefined") {
        return;
    }

    document.querySelectorAll("[data-status-dropdown]").forEach(botao => {
        bootstrap.Dropdown.getOrCreateInstance(botao, {
            boundary: "viewport",
            popperConfig(configuracaoPadrao) {
                return {
                    ...configuracaoPadrao,
                    strategy: "fixed"
                };
            }
        });
    });
}


function inicializarMascaras() {

    if (typeof Inputmask === "undefined") {
        return;
    }

    document.querySelectorAll("#telefone, [data-mascara-telefone]").forEach(campo => {
        Inputmask({
            mask: "(99) 9999[9]-9999",
            clearIncomplete: true
        }).mask(campo);
    });

    document.querySelectorAll("#cep, [data-mascara-cep]").forEach(campo => {
        Inputmask({
            mask: "99999-999",
            clearIncomplete: true
        }).mask(campo);
    });

}


function formatarTelefone(valor) {
    const digitos = String(valor ?? "").replace(/\D/g, "");

    if (digitos.length === 11) {
        return digitos.replace(/(\d{2})(\d{5})(\d{4})/, "($1) $2-$3");
    }

    if (digitos.length === 10) {
        return digitos.replace(/(\d{2})(\d{4})(\d{4})/, "($1) $2-$3");
    }

    return valor ?? "";
}


function formatarCep(valor) {
    const digitos = String(valor ?? "").replace(/\D/g, "");
    return digitos.length === 8
        ? digitos.replace(/(\d{5})(\d{3})/, "$1-$2")
        : valor ?? "";
}


function converterMoedaBrasileiraParaDecimal(valor) {
    const decimal = converterNumeroBrasileiroParaDecimal(valor);
    const numero = Number.parseFloat(decimal);
    return Number.isFinite(numero) ? numero.toFixed(2) : "0.00";
}


function converterNumeroBrasileiroParaDecimal(valor) {
    let texto = String(valor ?? "")
        .replace(/R\$/g, "")
        .replace(/\s/g, "")
        .trim();

    if (!texto) {
        return "0";
    }

    if (texto.includes(",")) {
        texto = texto.replace(/\./g, "").replace(",", ".");
    } else if ((texto.match(/\./g) || []).length === 1) {
        const casas = texto.length - texto.lastIndexOf(".") - 1;
        texto = casas <= 2 ? texto : texto.replace(".", "");
    } else {
        texto = texto.replace(/\./g, "");
    }

    texto = texto.replace(/[^\d.-]/g, "");
    const numero = Number.parseFloat(texto);
    return Number.isFinite(numero) ? String(numero) : "0";
}


function inicializarMascaraMonetaria() {
    document.querySelectorAll("[data-moeda-visual]").forEach(campoVisual => {
        const campoDecimal = campoVisual.parentElement
            .querySelector("[data-moeda-decimal]");

        if (!campoDecimal) {
            return;
        }

        const sincronizar = () => {
            campoDecimal.value = converterMoedaBrasileiraParaDecimal(campoVisual.value);
            campoDecimal.dispatchEvent(new Event("input", {bubbles: true}));
        };

        campoVisual.addEventListener("input", sincronizar);
        campoVisual.addEventListener("blur", () => {
            sincronizar();
            campoVisual.value = new Intl.NumberFormat("pt-BR", {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2
            }).format(Number(campoDecimal.value));
        });
        sincronizar();
    });
}


function inicializarDatasBrasileiras() {
    document.querySelectorAll("[data-data-brasileira]").forEach(campoVisual => {
        const campoIso = campoVisual.parentElement.querySelector("[data-data-iso]");

        if (!campoIso) {
            return;
        }

        campoVisual.addEventListener("input", () => {
            const digitos = campoVisual.value.replace(/\D/g, "").slice(0, 8);
            campoVisual.value = digitos
                .replace(/^(\d{2})(\d)/, "$1/$2")
                .replace(/^(\d{2}\/\d{2})(\d)/, "$1/$2");

            if (digitos.length === 8) {
                campoIso.value = `${digitos.slice(4, 8)}-${digitos.slice(2, 4)}-${digitos.slice(0, 2)}`;
            } else {
                campoIso.value = "";
            }
        });
    });
}


function inicializarFormularioPedido() {

    const formulario = document.querySelector("[data-formulario-pedido]");

    if (!formulario) {
        return;
    }

    const acaoPedido = formulario.querySelector("[data-acao-pedido]");

    const buscaCliente = formulario.querySelector("[data-busca-cliente]");
    const resultadosClientes = formulario.querySelector("[data-resultados-clientes]");
    const clienteId = formulario.querySelector("[data-cliente-id]");
    const clienteSelecionado = formulario.querySelector("[data-cliente-selecionado]");
    const clienteNome = formulario.querySelector("[data-cliente-nome]");
    const clienteTelefone = formulario.querySelector("[data-cliente-telefone]");
    const trocarCliente = formulario.querySelector("[data-trocar-cliente]");
    const buscaProduto = formulario.querySelector("[data-busca-produto]");
    const resultadosProdutos = formulario.querySelector("[data-resultados-produtos]");
    const itensContainer = formulario.querySelector("[data-itens-pedido]");
    const itensVazio = formulario.querySelector("[data-itens-vazio]");
    const totalItens = formulario.querySelector("[data-total-itens]");
    const taxaEntrega = formulario.querySelector("[data-taxa-entrega]");
    const taxaEntregaVisual = formulario.querySelector("[data-moeda-visual]");
    const taxaEntregaContainer = formulario.querySelector("[data-taxa-entrega-container]");
    const tipoEntrega = formulario.querySelector("#tipoEntrega");
    const subtotalPedido = formulario.querySelector("[data-pedido-subtotal]");
    const resumoTaxa = formulario.querySelector("[data-resumo-taxa]");
    const totalPedido = formulario.querySelector("[data-pedido-total]");
    const resumoItens = formulario.querySelector("[data-resumo-itens]");
    const resumoQuantidadeContainer = formulario.querySelector(
        "[data-resumo-quantidade-container]"
    );
    const resumoQuantidade = formulario.querySelector("[data-resumo-quantidade]");
    let temporizadorCliente;
    let temporizadorProduto;
    let requisicaoCliente = 0;
    let requisicaoProduto = 0;

    function fecharResultadosClientes() {
        clearTimeout(temporizadorCliente);
        requisicaoCliente++;
        resultadosClientes.replaceChildren();
        resultadosClientes.classList.add("d-none");
    }

    function fecharResultadosProdutos() {
        clearTimeout(temporizadorProduto);
        requisicaoProduto++;
        resultadosProdutos.replaceChildren();
        resultadosProdutos.classList.add("d-none");
    }

    function formatarMoeda(valor) {
        return new Intl.NumberFormat("pt-BR", {
            style: "currency",
            currency: "BRL"
        }).format(Number.isFinite(valor) ? valor : 0);
    }

    function selecionarCliente(cliente) {
        clienteId.value = cliente.id;
        clienteNome.textContent = cliente.nome;
        clienteTelefone.textContent = formatarTelefone(cliente.telefone);
        clienteSelecionado.classList.remove("d-none");
        buscaCliente.value = "";
        fecharResultadosClientes();
    }

    function criarBotaoResultado(conteudo, aoClicar) {
        const botao = document.createElement("button");
        botao.type = "button";
        botao.className = "list-group-item list-group-item-action";
        botao.appendChild(conteudo);
        botao.addEventListener("click", aoClicar);
        return botao;
    }

    async function pesquisarClientes() {
        const termo = buscaCliente.value.trim();
        const numeroRequisicao = ++requisicaoCliente;

        if (termo.length < 2) {
            fecharResultadosClientes();
            return;
        }

        const resposta = await fetch(
            `/pedidos/clientes/buscar?termo=${encodeURIComponent(termo)}`
        );
        const clientes = resposta.ok ? await resposta.json() : [];

        if (numeroRequisicao !== requisicaoCliente || buscaCliente.value.trim() !== termo) {
            return;
        }

        resultadosClientes.replaceChildren();

        clientes.forEach(cliente => {
            const conteudo = document.createElement("div");
            const nome = document.createElement("div");
            const telefone = document.createElement("small");
            nome.className = "fw-semibold";
            nome.textContent = cliente.nome;
            telefone.className = "text-muted";
            telefone.textContent = formatarTelefone(cliente.telefone);
            conteudo.append(nome, telefone);
            resultadosClientes.appendChild(
                criarBotaoResultado(conteudo, () => selecionarCliente(cliente))
            );
        });

        if (clientes.length === 0) {
            const conteudo = document.createElement("span");
            conteudo.innerHTML = '<i class="bi bi-person-plus me-1"></i>Cadastrar novo cliente';
            const botao = criarBotaoResultado(conteudo, () => {
                fecharResultadosClientes();
                const modalElemento = document.querySelector("#modalNovoCliente");
                const modal = bootstrap.Modal.getOrCreateInstance(modalElemento);
                const formNovoCliente = document.querySelector("[data-form-novo-cliente]");
                formNovoCliente.reset();

                const somenteNumeros = /^\d+$/.test(termo.replace(/\s/g, ""));
                document.querySelector("#novoClienteNome").value = somenteNumeros ? "" : termo;
                document.querySelector("#novoClienteTelefone").value = somenteNumeros ? termo : "";
                modal.show();
            });
            botao.classList.add("text-primary");
            resultadosClientes.appendChild(botao);
        }

        resultadosClientes.classList.remove("d-none");
    }

    buscaCliente.addEventListener("input", () => {
        clearTimeout(temporizadorCliente);
        requisicaoCliente++;
        temporizadorCliente = setTimeout(pesquisarClientes, 250);
    });

    buscaCliente.addEventListener("keydown", event => {
        if (event.key === "Escape") {
            fecharResultadosClientes();
        }
    });

    trocarCliente.addEventListener("click", () => {
        clienteId.value = "";
        clienteSelecionado.classList.add("d-none");
        buscaCliente.focus();
    });

    if (clienteId.value) {
        clienteSelecionado.classList.remove("d-none");
    }

    const formNovoCliente = document.querySelector("[data-form-novo-cliente]");

    if (formNovoCliente) {
        formNovoCliente.addEventListener("submit", async event => {
            event.preventDefault();
            const errosContainer = formNovoCliente.querySelector("[data-erros-cliente]");
            const resposta = await fetch(formNovoCliente.action, {
                method: "POST",
                body: new FormData(formNovoCliente)
            });
            const dados = await resposta.json();

            if (!resposta.ok) {
                errosContainer.replaceChildren();
                Object.values(dados).forEach(mensagem => {
                    const linha = document.createElement("div");
                    linha.textContent = mensagem;
                    errosContainer.appendChild(linha);
                });
                errosContainer.classList.remove("d-none");
                return;
            }

            errosContainer.classList.add("d-none");
            selecionarCliente(dados);
            const modalElemento = document.querySelector("#modalNovoCliente");
            bootstrap.Modal.getInstance(modalElemento)?.hide();
            formNovoCliente.reset();
        });
    }

    function reindexarItens() {
        const itens = Array.from(itensContainer.querySelectorAll(".item-pedido"));

        itens.forEach((item, indice) => {
            item.querySelector("[data-item-produto-id]").name = `itens[${indice}].produtoId`;
            item.querySelector("[data-item-quantidade]").name = `itens[${indice}].quantidade`;
            item.querySelector("[data-item-observacao]").name = `itens[${indice}].observacao`;
        });

        itensVazio.classList.toggle("d-none", itens.length > 0);
        totalItens.textContent = `${itens.length} ${itens.length === 1 ? "item" : "itens"}`;
    }

    function recalcularPedido() {
        let subtotal = 0;
        let quantidadeTotal = 0;
        let unidadeComum = null;
        let unidadesMisturadas = false;
        const itens = Array.from(itensContainer.querySelectorAll(".item-pedido"));

        itens.forEach(item => {
            const preco = Number.parseFloat(item.dataset.produtoPreco) || 0;
            const quantidade = Number.parseFloat(
                item.querySelector("[data-item-quantidade]").value
            ) || 0;
            const unidade = item.dataset.produtoUnidade;
            const subtotalItem = preco * quantidade;
            item.querySelector("[data-item-subtotal]").textContent = formatarMoeda(subtotalItem);
            subtotal += subtotalItem;
            quantidadeTotal += quantidade;

            if (unidadeComum === null) {
                unidadeComum = unidade;
            } else if (unidadeComum !== unidade) {
                unidadesMisturadas = true;
            }
        });

        const taxa = Number.parseFloat(taxaEntrega.value) || 0;
        resumoItens.textContent = String(itens.length);

        if (itens.length > 0 && !unidadesMisturadas) {
            resumoQuantidadeContainer.classList.remove("d-none");
            resumoQuantidade.textContent = unidadeComum === "QUILOGRAMA"
                ? `${new Intl.NumberFormat("pt-BR", {
                    minimumFractionDigits: 3,
                    maximumFractionDigits: 3
                }).format(quantidadeTotal)} kg`
                : `${new Intl.NumberFormat("pt-BR", {
                    maximumFractionDigits: 0
                }).format(quantidadeTotal)} un.`;
        } else {
            resumoQuantidadeContainer.classList.add("d-none");
            resumoQuantidade.textContent = "";
        }

        subtotalPedido.textContent = formatarMoeda(subtotal);
        resumoTaxa.textContent = formatarMoeda(taxa);
        totalPedido.textContent = formatarMoeda(subtotal + taxa);
    }

    function ativarItem(item) {
        const quantidadeVisual = item.querySelector("[data-item-quantidade-visual]");
        const quantidadeDecimal = item.querySelector("[data-item-quantidade]");
        const quilograma = item.dataset.produtoUnidade === "QUILOGRAMA";

        const sincronizarQuantidade = () => {
            quantidadeDecimal.value = converterNumeroBrasileiroParaDecimal(
                quantidadeVisual.value
            );
            recalcularPedido();
        };

        quantidadeVisual.addEventListener("input", sincronizarQuantidade);
        quantidadeVisual.addEventListener("blur", () => {
            sincronizarQuantidade();
            let numero = Number.parseFloat(quantidadeDecimal.value) || 0;

            if (!quilograma && numero < 1) {
                numero = 1;
                quantidadeDecimal.value = "1";
                recalcularPedido();
            }

            quantidadeVisual.value = quilograma
                ? new Intl.NumberFormat("pt-BR", {
                    minimumFractionDigits: 3,
                    maximumFractionDigits: 3
                }).format(numero)
                : new Intl.NumberFormat("pt-BR", {
                    minimumFractionDigits: 0,
                    maximumFractionDigits: 3
                }).format(numero);
        });

        const diminuir = item.querySelector("[data-diminuir-quantidade]");
        const aumentar = item.querySelector("[data-aumentar-quantidade]");

        diminuir?.addEventListener("click", () => {
            const quantidadeAtual = Number.parseInt(quantidadeDecimal.value, 10) || 1;
            const novaQuantidade = Math.max(1, quantidadeAtual - 1);
            quantidadeVisual.value = String(novaQuantidade);
            quantidadeDecimal.value = String(novaQuantidade);
            recalcularPedido();
        });

        aumentar?.addEventListener("click", () => {
            const quantidadeAtual = Number.parseInt(quantidadeDecimal.value, 10) || 0;
            const novaQuantidade = Math.max(1, quantidadeAtual + 1);
            quantidadeVisual.value = String(novaQuantidade);
            quantidadeDecimal.value = String(novaQuantidade);
            recalcularPedido();
        });
        item.querySelector("[data-remover-item]").addEventListener("click", () => {
            item.remove();
            reindexarItens();
            recalcularPedido();
        });
    }

    function adicionarProduto(produto) {
        const existente = itensContainer.querySelector(
            `.item-pedido[data-produto-id="${produto.id}"]`
        );

        if (existente) {
            buscaProduto.value = "";

            if (produto.unidadeVenda === "UNIDADE") {
                existente.querySelector("[data-aumentar-quantidade]").click();
            }

            fecharResultadosProdutos();
            buscaProduto.focus();
            return;
        }

        const item = document.createElement("div");
        item.className = "card border mb-3 item-pedido";
        item.dataset.produtoId = produto.id;
        item.dataset.produtoPreco = produto.preco;
        item.dataset.produtoUnidade = produto.unidadeVenda;
        item.dataset.permiteAcompanhamento = produto.permiteAcompanhamento;
        const unidade = produto.unidadeVenda === "UNIDADE" ? "Unidade" : "Quilograma";
        const esconderObservacao = produto.permiteAcompanhamento ? "" : " d-none";

        item.innerHTML = `
            <div class="card-body">
                <input type="hidden" name="itemIds" value="" data-item-id>
                <input type="hidden" value="${produto.id}" data-item-produto-id>
                <div class="row align-items-center g-3">
                    <div class="col-md">
                        <div class="fw-semibold"></div>
                        <small class="text-muted">${unidade} · ${formatarMoeda(Number(produto.preco))}</small>
                    </div>
                    <div class="col-sm-4 col-md-3">
                        <label class="form-label small">Quantidade *</label>
                        <div class="input-group">
                            ${produto.unidadeVenda === "UNIDADE" ? `
                            <button type="button" class="btn btn-outline-secondary" data-diminuir-quantidade
                                    aria-label="Diminuir quantidade"><i class="bi bi-dash"></i></button>` : ""}
                            <input type="text" class="form-control text-center" value="${produto.unidadeVenda === "UNIDADE" ? "1" : "1,000"}"
                                   inputmode="decimal" data-item-quantidade-visual required>
                            ${produto.unidadeVenda === "UNIDADE" ? `
                            <button type="button" class="btn btn-outline-secondary" data-aumentar-quantidade
                                    aria-label="Aumentar quantidade"><i class="bi bi-plus"></i></button>` : ""}
                        </div>
                        <input type="hidden" value="1" data-item-quantidade>
                    </div>
                    <div class="col-sm-5 col-md-3 text-md-end">
                        <small class="text-muted d-block">Subtotal</small>
                        <strong data-item-subtotal>${formatarMoeda(Number(produto.preco))}</strong>
                    </div>
                    <div class="col-sm-3 col-md-auto">
                        <button type="button" class="btn btn-outline-danger" data-remover-item
                                aria-label="Remover item"><i class="bi bi-trash"></i></button>
                    </div>
                    <div class="col-12${esconderObservacao}" data-item-observacao-container>
                        <label class="form-label small">Observação do item</label>
                        <input type="text" class="form-control form-control-sm" maxlength="255"
                               placeholder="Ex.: sem farofa" data-item-observacao>
                    </div>
                </div>
            </div>`;
        item.querySelector(".fw-semibold").textContent = produto.nome;
        itensContainer.appendChild(item);
        ativarItem(item);
        reindexarItens();
        recalcularPedido();
        buscaProduto.value = "";
        fecharResultadosProdutos();
        buscaProduto.focus();
    }

    async function pesquisarProdutos() {
        const termo = buscaProduto.value.trim();
        const numeroRequisicao = ++requisicaoProduto;
        const resposta = await fetch(
            `/pedidos/produtos/buscar?termo=${encodeURIComponent(termo)}`
        );
        const produtos = resposta.ok ? await resposta.json() : [];

        if (numeroRequisicao !== requisicaoProduto || buscaProduto.value.trim() !== termo) {
            return;
        }

        resultadosProdutos.replaceChildren();

        produtos.forEach(produto => {
            const conteudo = document.createElement("div");
            const nome = document.createElement("div");
            const detalhes = document.createElement("small");
            nome.className = "fw-semibold";
            nome.textContent = produto.nome;
            detalhes.className = "text-muted";
            detalhes.textContent = `${produto.unidadeVenda === "UNIDADE" ? "Unidade" : "Quilograma"} · ${formatarMoeda(Number(produto.preco))}`;
            conteudo.append(nome, detalhes);
            resultadosProdutos.appendChild(
                criarBotaoResultado(conteudo, () => adicionarProduto(produto))
            );
        });

        if (produtos.length === 0) {
            const vazio = document.createElement("div");
            vazio.className = "list-group-item text-muted";
            vazio.textContent = "Nenhum produto ativo encontrado.";
            resultadosProdutos.appendChild(vazio);
        }

        resultadosProdutos.classList.remove("d-none");
    }

    buscaProduto.addEventListener("input", () => {
        clearTimeout(temporizadorProduto);
        requisicaoProduto++;
        temporizadorProduto = setTimeout(pesquisarProdutos, 250);
    });
    buscaProduto.addEventListener("keydown", event => {
        if (event.key === "Escape") {
            fecharResultadosProdutos();
        }
    });
    document.addEventListener("click", event => {
        if (!buscaCliente.contains(event.target) && !resultadosClientes.contains(event.target)) {
            fecharResultadosClientes();
        }

        if (!buscaProduto.contains(event.target) && !resultadosProdutos.contains(event.target)) {
            fecharResultadosProdutos();
        }
    });
    taxaEntrega.addEventListener("input", recalcularPedido);
    tipoEntrega.addEventListener("change", atualizarTipoEntrega);
    itensContainer.querySelectorAll(".item-pedido").forEach(ativarItem);
    reindexarItens();
    atualizarTipoEntrega();
    recalcularPedido();

    formulario.addEventListener("submit", event => {
        if (acaoPedido) {
            acaoPedido.value = event.submitter?.value || "salvar";
        }

        formulario.querySelectorAll("[data-botao-salvar]").forEach(botao => {
            botao.disabled = true;
            botao.setAttribute("aria-busy", "true");

            if (!botao.querySelector(".spinner-border")) {
                const spinner = document.createElement("span");
                spinner.className = "spinner-border spinner-border-sm me-1";
                spinner.setAttribute("aria-hidden", "true");
                botao.prepend(spinner);
            }
        });
    });

    function atualizarTipoEntrega() {
        const retirada = tipoEntrega.value === "RETIRADA";
        taxaEntregaContainer.classList.toggle("d-none", retirada);

        if (retirada) {
            taxaEntregaVisual.value = "0,00";
            taxaEntrega.value = "0.00";
        }

        recalcularPedido();
    }
}


function inicializarSelecaoPedidos() {

    const lista = document.querySelector("[data-lista-selecionavel]");
    const selecionarTodos = document.querySelector("[data-selecionar-todos]");
    const botaoImprimir = document.querySelector("[data-imprimir-selecionados]");
    const textoBotao = botaoImprimir?.querySelector(
        "[data-texto-imprimir-selecionados]"
    );

    if (!lista || !selecionarTodos || !botaoImprimir) {
        return;
    }

    const checkboxes = Array.from(
        lista.querySelectorAll("[data-pedido-checkbox]")
    );

    function atualizarEstado() {
        const selecionados = checkboxes.filter(checkbox => checkbox.checked);

        botaoImprimir.disabled = selecionados.length === 0;
        if (textoBotao) {
            textoBotao.textContent = selecionados.length > 0
                ? `Imprimir selecionados (${selecionados.length})`
                : "Imprimir selecionados";
        }
        selecionarTodos.checked = checkboxes.length > 0
            && selecionados.length === checkboxes.length;
        selecionarTodos.indeterminate = selecionados.length > 0
            && selecionados.length < checkboxes.length;
        selecionarTodos.disabled = checkboxes.length === 0;
    }

    selecionarTodos.addEventListener("change", () => {
        checkboxes.forEach(checkbox => {
            checkbox.checked = selecionarTodos.checked;
        });
        atualizarEstado();
    });

    checkboxes.forEach(checkbox => {
        checkbox.addEventListener("change", atualizarEstado);
    });

    botaoImprimir.addEventListener("click", () => {
        const url = botaoImprimir.dataset.url;
        const parametros = new URLSearchParams();

        checkboxes
            .filter(checkbox => checkbox.checked)
            .forEach(checkbox => parametros.append("ids", checkbox.value));

        if (!url || !parametros.has("ids")) {
            return;
        }

        window.open(`${url}?${parametros.toString()}`, "_blank", "noopener");
    });

    atualizarEstado();
}


function inicializarImpressaoComandas() {

    const paginaComandas = document.querySelector("[data-pagina-comandas]");

    if (!paginaComandas) {
        return;
    }

    document.querySelectorAll("[data-formato-impressao]").forEach(botao => {
        botao.addEventListener("click", () => {
            const formato = botao.dataset.formatoImpressao;

            document.body.classList.toggle("formato-a4", formato === "a4");
            document.body.classList.toggle("formato-etiqueta", formato === "etiqueta");

            document.querySelectorAll("[data-formato-impressao]").forEach(item => {
                item.classList.toggle("active", item === botao);
            });
        });
    });

    const botaoImprimir = document.querySelector("[data-imprimir-pagina]");

    if (botaoImprimir) {
        botaoImprimir.addEventListener("click", () => window.print());
    }
}


function inicializarBuscaCep() {

    document.querySelectorAll("#cep, [data-cep]").forEach(cepInput => {
        const obterCampo = (atributo, seletorPadrao) => document.querySelector(
            cepInput.dataset[atributo] || seletorPadrao
        );
        const enderecoInput = obterCampo("endereco", "#endereco");
        const bairroInput = obterCampo("bairro", "#bairro");
        const cidadeInput = obterCampo("cidade", "#cidade");
        const numeroInput = obterCampo("numero", "#numero");

        cepInput.addEventListener("blur", async () => {
            const cep = cepInput.value.replace(/\D/g, "");
            limparErroCep();

            if (cep.length === 0) {
                return;
            }

            if (cep.length !== 8) {
                mostrarErroCep("Informe um CEP válido.");
                return;
            }

            definirCarregamento(true);

            try {
                const response = await fetch(`https://viacep.com.br/ws/${cep}/json/`);

                if (!response.ok) {
                    throw new Error("Erro ao consultar o CEP.");
                }

                const dados = await response.json();

                if (dados.erro) {
                    mostrarErroCep("CEP não encontrado.");
                    return;
                }

                preencherCampo(enderecoInput, dados.logradouro);
                preencherCampo(bairroInput, dados.bairro);
                preencherCampo(cidadeInput, dados.localidade);
                numeroInput?.focus();
            } catch (erro) {
                console.error("Erro na consulta do CEP:", erro);
                mostrarErroCep("Não foi possível consultar o CEP. Tente novamente.");
            } finally {
                definirCarregamento(false);
            }
        });

        function preencherCampo(campo, valor) {
            if (!campo) {
                return;
            }

            campo.value = valor ?? "";
            campo.dispatchEvent(new Event("input", {bubbles: true}));
        }

        function definirCarregamento(carregando) {
            cepInput.readOnly = carregando;
            cepInput.classList.toggle("bg-light", carregando);
        }

        function mostrarErroCep(mensagem) {
            cepInput.classList.add("is-invalid");
            let feedback = cepInput.parentElement.querySelector("[data-cep-feedback]");

            if (!feedback) {
                feedback = document.createElement("div");
                feedback.className = "invalid-feedback";
                feedback.dataset.cepFeedback = "";
                cepInput.parentElement.appendChild(feedback);
            }

            feedback.textContent = mensagem;
        }

        function limparErroCep() {
            cepInput.classList.remove("is-invalid");
            cepInput.parentElement.querySelector("[data-cep-feedback]")?.remove();
        }
    });

}


function inicializarModalExclusao() {

    const modalExclusao = document.querySelector("#modalExclusao");

    if (!modalExclusao) {
        return;
    }

    const descricaoModal =
        modalExclusao.querySelector("#descricaoExclusao");

    const formulario =
        modalExclusao.querySelector("#formExclusao");

    if (!descricaoModal || !formulario) {

        console.error(
            "O modal de exclusão não possui os elementos obrigatórios."
        );

        return;

    }

    modalExclusao.addEventListener("show.bs.modal", event => {

        const triggerButton = event.relatedTarget;

        if (!triggerButton) {
            return;
        }

        const descricao =
            triggerButton.dataset.exclusaoDescricao;

        const url =
            triggerButton.dataset.exclusaoUrl;

        descricaoModal.textContent =
            descricao || "este registro";

        if (!url) {

            console.error(
                "A URL de exclusão não foi informada no botão."
            );

            formulario.removeAttribute("action");

            return;

        }

        formulario.action = url;

    });

    modalExclusao.addEventListener("hidden.bs.modal", () => {

        descricaoModal.textContent = "";

        formulario.removeAttribute("action");

    });

    formulario.addEventListener("submit", event => {

        if (!formulario.getAttribute("action")) {

            event.preventDefault();

            console.error(
                "Exclusão cancelada porque a URL não foi definida."
            );

        }

    });

}
