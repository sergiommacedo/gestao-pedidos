document.addEventListener("DOMContentLoaded", () => {

    inicializarMascaras();
    inicializarBuscaCep();
    inicializarModalExclusao();
    inicializarSelecaoPedidos();
    inicializarImpressaoComandas();
    inicializarFormularioPedido();

});


function inicializarMascaras() {

    const telefoneInput = document.querySelector("#telefone");
    const cepInput = document.querySelector("#cep");

    if (telefoneInput && typeof Inputmask !== "undefined") {

        Inputmask({
            mask: "(99) 99999-9999",
            clearIncomplete: true
        }).mask(telefoneInput);

    }

    if (cepInput && typeof Inputmask !== "undefined") {

        Inputmask({
            mask: "99999-999",
            clearIncomplete: true
        }).mask(cepInput);

    }

}


function inicializarFormularioPedido() {

    const formulario = document.querySelector("[data-formulario-pedido]");

    if (!formulario) {
        return;
    }

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
    const subtotalPedido = formulario.querySelector("[data-pedido-subtotal]");
    const resumoTaxa = formulario.querySelector("[data-resumo-taxa]");
    const totalPedido = formulario.querySelector("[data-pedido-total]");
    let temporizadorCliente;
    let temporizadorProduto;

    function formatarMoeda(valor) {
        return new Intl.NumberFormat("pt-BR", {
            style: "currency",
            currency: "BRL"
        }).format(Number.isFinite(valor) ? valor : 0);
    }

    function selecionarCliente(cliente) {
        clienteId.value = cliente.id;
        clienteNome.textContent = cliente.nome;
        clienteTelefone.textContent = cliente.telefone;
        clienteSelecionado.classList.remove("d-none");
        resultadosClientes.classList.add("d-none");
        buscaCliente.value = "";
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

        if (termo.length < 2) {
            resultadosClientes.classList.add("d-none");
            return;
        }

        const resposta = await fetch(
            `/pedidos/clientes/buscar?termo=${encodeURIComponent(termo)}`
        );
        const clientes = resposta.ok ? await resposta.json() : [];

        resultadosClientes.replaceChildren();

        clientes.forEach(cliente => {
            const conteudo = document.createElement("div");
            const nome = document.createElement("div");
            const telefone = document.createElement("small");
            nome.className = "fw-semibold";
            nome.textContent = cliente.nome;
            telefone.className = "text-muted";
            telefone.textContent = cliente.telefone;
            conteudo.append(nome, telefone);
            resultadosClientes.appendChild(
                criarBotaoResultado(conteudo, () => selecionarCliente(cliente))
            );
        });

        if (clientes.length === 0) {
            const conteudo = document.createElement("span");
            conteudo.innerHTML = '<i class="bi bi-person-plus me-1"></i>Cadastrar novo cliente';
            const botao = criarBotaoResultado(conteudo, () => {
                const modalElemento = document.querySelector("#modalNovoCliente");
                const modal = bootstrap.Modal.getOrCreateInstance(modalElemento);
                modal.show();
                document.querySelector("#novoClienteNome").value = termo;
            });
            botao.classList.add("text-primary");
            resultadosClientes.appendChild(botao);
        }

        resultadosClientes.classList.remove("d-none");
    }

    buscaCliente.addEventListener("input", () => {
        clearTimeout(temporizadorCliente);
        temporizadorCliente = setTimeout(pesquisarClientes, 250);
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

        itensContainer.querySelectorAll(".item-pedido").forEach(item => {
            const preco = Number.parseFloat(item.dataset.produtoPreco) || 0;
            const quantidade = Number.parseFloat(
                item.querySelector("[data-item-quantidade]").value.replace(",", ".")
            ) || 0;
            const subtotalItem = preco * quantidade;
            item.querySelector("[data-item-subtotal]").textContent = formatarMoeda(subtotalItem);
            subtotal += subtotalItem;
        });

        const taxa = Number.parseFloat(taxaEntrega.value.replace(",", ".")) || 0;
        subtotalPedido.textContent = formatarMoeda(subtotal);
        resumoTaxa.textContent = formatarMoeda(taxa);
        totalPedido.textContent = formatarMoeda(subtotal + taxa);
    }

    function ativarItem(item) {
        item.querySelector("[data-item-quantidade]").addEventListener("input", recalcularPedido);
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
            existente.querySelector("[data-item-quantidade]").focus();
            return;
        }

        const item = document.createElement("div");
        item.className = "card border mb-3 item-pedido";
        item.dataset.produtoId = produto.id;
        item.dataset.produtoPreco = produto.preco;
        item.dataset.produtoUnidade = produto.unidadeVenda;
        item.dataset.permiteAcompanhamento = produto.permiteAcompanhamento;
        const passo = produto.unidadeVenda === "UNIDADE" ? "1" : "0.001";
        const unidade = produto.unidadeVenda === "UNIDADE" ? "Unidade" : "Quilograma";
        const esconderObservacao = produto.permiteAcompanhamento ? "" : " d-none";

        item.innerHTML = `
            <div class="card-body">
                <input type="hidden" value="${produto.id}" data-item-produto-id>
                <div class="row align-items-center g-3">
                    <div class="col-md">
                        <div class="fw-semibold"></div>
                        <small class="text-muted">${unidade} · ${formatarMoeda(Number(produto.preco))}</small>
                    </div>
                    <div class="col-sm-4 col-md-3">
                        <label class="form-label small">Quantidade *</label>
                        <input type="number" class="form-control" value="1" min="0.001"
                               step="${passo}" data-item-quantidade required>
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
                        <input type="text" class="form-control" maxlength="255"
                               placeholder="Ex.: sem farofa" data-item-observacao>
                    </div>
                </div>
            </div>`;
        item.querySelector(".fw-semibold").textContent = produto.nome;
        itensContainer.appendChild(item);
        ativarItem(item);
        reindexarItens();
        recalcularPedido();
        resultadosProdutos.classList.add("d-none");
        buscaProduto.value = "";
    }

    async function pesquisarProdutos() {
        const termo = buscaProduto.value.trim();
        const resposta = await fetch(
            `/pedidos/produtos/buscar?termo=${encodeURIComponent(termo)}`
        );
        const produtos = resposta.ok ? await resposta.json() : [];
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

    buscaProduto.addEventListener("focus", pesquisarProdutos);
    buscaProduto.addEventListener("input", () => {
        clearTimeout(temporizadorProduto);
        temporizadorProduto = setTimeout(pesquisarProdutos, 250);
    });
    taxaEntrega.addEventListener("input", recalcularPedido);
    itensContainer.querySelectorAll(".item-pedido").forEach(ativarItem);
    reindexarItens();
    recalcularPedido();
}


function inicializarSelecaoPedidos() {

    const lista = document.querySelector("[data-lista-selecionavel]");
    const selecionarTodos = document.querySelector("[data-selecionar-todos]");
    const botaoImprimir = document.querySelector("[data-imprimir-selecionados]");

    if (!lista || !selecionarTodos || !botaoImprimir) {
        return;
    }

    const checkboxes = Array.from(
        lista.querySelectorAll("[data-pedido-checkbox]")
    );

    function atualizarEstado() {
        const selecionados = checkboxes.filter(checkbox => checkbox.checked);

        botaoImprimir.disabled = selecionados.length === 0;
        selecionarTodos.checked = checkboxes.length > 0
            && selecionados.length === checkboxes.length;
        selecionarTodos.indeterminate = selecionados.length > 0
            && selecionados.length < checkboxes.length;
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
            document.body.classList.toggle("formato-termica", formato !== "a4");

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

    const cepInput = document.querySelector("#cep");
    const enderecoInput = document.querySelector("#endereco");
    const bairroInput = document.querySelector("#bairro");
    const cidadeInput = document.querySelector("#cidade");
    const numeroInput = document.querySelector("#numero");

    if (!cepInput) {
        return;
    }

    cepInput.addEventListener("blur", buscarCep);

    async function buscarCep() {

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

            const response = await fetch(
                `https://viacep.com.br/ws/${cep}/json/`
            );

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

            if (numeroInput) {
                numeroInput.focus();
            }

        } catch (erro) {

            console.error("Erro na consulta do CEP:", erro);

            mostrarErroCep(
                "Não foi possível consultar o CEP. Tente novamente."
            );

        } finally {

            definirCarregamento(false);

        }

    }

    function preencherCampo(campo, valor) {

        if (!campo) {
            return;
        }

        campo.value = valor ?? "";

        campo.dispatchEvent(
            new Event("input", {
                bubbles: true
            })
        );

    }

    function definirCarregamento(carregando) {

        cepInput.readOnly = carregando;

        if (carregando) {
            cepInput.classList.add("bg-light");
        } else {
            cepInput.classList.remove("bg-light");
        }

    }

    function mostrarErroCep(mensagem) {

        cepInput.classList.add("is-invalid");

        let feedback = document.querySelector("#cep-feedback");

        if (!feedback) {

            feedback = document.createElement("div");
            feedback.id = "cep-feedback";
            feedback.className = "invalid-feedback";

            cepInput.parentElement.appendChild(feedback);

        }

        feedback.textContent = mensagem;

    }

    function limparErroCep() {

        cepInput.classList.remove("is-invalid");

        const feedback = document.querySelector("#cep-feedback");

        if (feedback) {
            feedback.remove();
        }

    }

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
