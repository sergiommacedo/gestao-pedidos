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
    inicializarAtalhosPeriodoRelatorio();
    inicializarPreviewProducao();
    inicializarFormularioInsumo();
    inicializarFormularioCompra();
    inicializarFormularioProduto();
    inicializarFormularioEstoque();
    inicializarFormularioFichaTecnica();
    inicializarItensProducao();
    inicializarPreviaRendimentoProducao();
    inicializarComposicaoProduto();

});

function inicializarComposicaoProduto() {
    const formulario = document.querySelector("[data-form-composicao]");
    if (!formulario) return;
    const container = formulario.querySelector("[data-composicao-itens]");
    const template = document.querySelector("[data-composicao-template]");
    const moeda = valor => new Intl.NumberFormat("pt-BR", {style: "currency", currency: "BRL"}).format(valor || 0);
    const renumerar = () => container.querySelectorAll("[data-composicao-item]").forEach((linha, indice) => linha.querySelectorAll("[data-campo]").forEach(campo => campo.name = `itens[${indice}].${campo.dataset.campo}`));
    const totalizar = () => {
        let total = 0;
        container.querySelectorAll("[data-composicao-item]").forEach(linha => {
            const opcao = linha.querySelector("[data-composicao-referencia]").selectedOptions[0];
            const custo = Number(opcao?.dataset.custo || 0) * Number(linha.querySelector("[data-composicao-quantidade]").value || 0);
            linha.querySelector("[data-composicao-custo]").textContent = moeda(custo); total += custo;
        });
        formulario.querySelector("[data-composicao-total]").textContent = moeda(total);
    };
    const carregar = async linha => {
        const tipo = linha.querySelector("[data-composicao-tipo]").value;
        const select = linha.querySelector("[data-composicao-referencia]");
        const atual = select.dataset.valorAtual || select.value;
        select.innerHTML = '<option value="">Selecione</option>';
        if (!tipo) return;
        const resposta = await fetch(`/composicoes-produtos/componentes?tipo=${encodeURIComponent(tipo)}`);
        if (!resposta.ok) return;
        for (const item of await resposta.json()) {
            const opcao = document.createElement("option"); opcao.value = item.id; opcao.textContent = item.nome;
            opcao.dataset.unidade = item.unidade; opcao.dataset.simbolo = ({UNIDADE:"un", QUILOGRAMA:"kg", GRAMA:"g", LITRO:"L", MILILITRO:"ml"})[item.unidade] || item.unidade;
            opcao.dataset.custo = item.custoMedio; if (String(item.id) === String(atual)) opcao.selected = true; select.appendChild(opcao);
        }
        select.dataset.valorAtual = ""; atualizarLinha(linha);
    };
    const atualizarLinha = linha => { const opcao = linha.querySelector("[data-composicao-referencia]").selectedOptions[0]; linha.querySelector("[data-composicao-unidade]").textContent = opcao?.dataset.simbolo || "—"; totalizar(); };
    const preparar = linha => {
        linha.querySelector("[data-composicao-tipo]").addEventListener("change", () => carregar(linha));
        linha.querySelector("[data-composicao-referencia]").addEventListener("change", () => atualizarLinha(linha));
        linha.querySelector("[data-composicao-quantidade]").addEventListener("input", totalizar);
        linha.querySelector("[data-composicao-remover]").addEventListener("click", () => { if (container.children.length > 1) { linha.remove(); renumerar(); totalizar(); } });
        if (linha.querySelector("[data-composicao-tipo]").value) carregar(linha);
    };
    container.querySelectorAll("[data-composicao-item]").forEach(preparar);
    formulario.querySelector("[data-composicao-adicionar]").addEventListener("click", () => { const linha = template.content.firstElementChild.cloneNode(true); container.appendChild(linha); renumerar(); preparar(linha); });
}

function inicializarFormularioFichaTecnica() {
    const formulario = document.querySelector("[data-form-ficha-tecnica]");
    if (!formulario) return;
    const busca = formulario.querySelector("[data-ficha-busca]");
    const resultados = formulario.querySelector("[data-ficha-resultados]");
    const adicionar = formulario.querySelector("[data-ficha-adicionar]");
    const container = formulario.querySelector("[data-ficha-itens]");
    const vazio = formulario.querySelector("[data-ficha-vazio]");
    const erro = formulario.querySelector("[data-ficha-erro]");
    const produto = formulario.querySelector("[data-ficha-produto]");
    const rendimento = formulario.querySelector("[data-ficha-rendimento]");
    const unidade = formulario.querySelector("[data-ficha-unidade]");
    const unidadeTexto = formulario.querySelector("[data-ficha-unidade-texto]");
    let selecionado = null, temporizador, requisicao = 0;
    const moeda = valor => new Intl.NumberFormat("pt-BR", {style: "currency", currency: "BRL"}).format(valor || 0);
    const numero = valor => Number.parseFloat(String(valor ?? "0").replace(",", ".")) || 0;
    const escapar = valor => String(valor ?? "").replace(/[&<>'"]/g, caractere => ({"&":"&amp;","<":"&lt;",">":"&gt;","'":"&#39;",'"':"&quot;"})[caractere]);
    const fechar = () => { clearTimeout(temporizador); requisicao++; resultados.replaceChildren(); resultados.classList.add("d-none"); };
    const avisar = mensagem => { erro.textContent = mensagem; erro.classList.remove("d-none"); };

    function atualizarUnidadeRendimento() {
        const opcao = produto?.options[produto.selectedIndex];
        const porQuilo = opcao?.dataset.unidade === "QUILOGRAMA";
        unidade.textContent = !opcao?.value ? "—" : porQuilo ? "kg" : "un";
        unidadeTexto.textContent = porQuilo ? "kg" : "unidade";
        if (rendimento) { rendimento.step = porQuilo ? "0.001" : "1"; rendimento.min = porQuilo ? "0.001" : "1"; }
        reindexar();
    }

    function reindexar() {
        const linhas = [...container.querySelectorAll("[data-ficha-item]")];
        linhas.forEach((linha, indice) => {
            linha.querySelector("[data-ficha-item-id]").name = `itens[${indice}].id`;
            linha.querySelector("[data-ficha-insumo-id]").name = `itens[${indice}].insumoId`;
            linha.querySelector("[data-ficha-quantidade-item]").name = `itens[${indice}].quantidade`;
        });
        vazio.classList.toggle("d-none", linhas.length > 0);
        formulario.querySelector("[data-ficha-quantidade]").textContent = String(linhas.length);
        let total = 0, pendentes = 0;
        linhas.forEach(linha => {
            const quantidade = numero(linha.querySelector("[data-ficha-quantidade-item]").value);
            const custo = numero(linha.dataset.custo);
            const possui = linha.dataset.possuiCusto === "true";
            if (!possui) pendentes++;
            const estimado = possui ? quantidade * custo : 0;
            linha.querySelector("[data-ficha-custo-estimado]").textContent = moeda(estimado);
            total += estimado;
        });
        formulario.querySelector("[data-ficha-total]").textContent = moeda(total);
        const rendimentoEsperado = numero(rendimento?.value);
        formulario.querySelector("[data-ficha-custo-unitario]").textContent = moeda(rendimentoEsperado > 0 ? total / rendimentoEsperado : 0);
        const situacao = formulario.querySelector("[data-ficha-situacao]");
        situacao.textContent = pendentes ? "Custo pendente" : "Custo completo";
        situacao.className = `badge ms-2 ${pendentes ? "text-bg-warning" : "text-bg-success"}`;
        formulario.querySelector("[data-ficha-aviso]").classList.toggle("d-none", pendentes === 0);
    }

    function adicionarLinha(item) {
        if ([...container.querySelectorAll("[data-ficha-item]")].some(l => l.dataset.insumoId === String(item.id))) {
            avisar("Este insumo já foi adicionado à ficha técnica."); return;
        }
        const unidade = item.unidade;
        const linha = document.createElement("div");
        linha.className = "border rounded p-3 mb-3";
        linha.dataset.fichaItem = ""; linha.dataset.insumoId = item.id;
        linha.dataset.custo = item.custoMedio ?? 0; linha.dataset.possuiCusto = String(item.possuiCusto);
        linha.innerHTML = `<input type="hidden" data-ficha-item-id value="${escapar(item.itemId || "")}"><input type="hidden" data-ficha-insumo-id value="${escapar(item.id)}"><div class="row g-3 align-items-end"><div class="col-md-3"><label class="form-label">Insumo</label><div class="form-control bg-body-tertiary">${escapar(item.nome)}</div></div><div class="col-sm-6 col-md-2"><label class="form-label">Quantidade utilizada</label><input class="form-control" type="number" min="${unidade === "UNIDADE" ? "1" : "0.001"}" step="${unidade === "UNIDADE" ? "1" : "0.001"}" value="${escapar(item.quantidade ?? (unidade === "UNIDADE" ? "1" : "0.001"))}" data-ficha-quantidade-item required></div><div class="col-sm-6 col-md-1"><label class="form-label">Unidade</label><div class="form-control bg-body-tertiary">${escapar(item.simbolo)}</div></div><div class="col-md-2"><label class="form-label">Custo médio</label><div class="form-control bg-body-tertiary">${item.possuiCusto ? escapar(moeda(item.custoMedio)) : "Não disponível"}</div></div><div class="col-md-2"><label class="form-label">Custo estimado</label><div class="form-control bg-body-tertiary" data-ficha-custo-estimado>R$ 0,00</div></div><div class="col-md-2"><button type="button" class="btn btn-outline-danger w-100" data-ficha-remover><i class="bi bi-trash me-1"></i> Remover</button></div></div>`;
        container.append(linha); erro.classList.add("d-none"); reindexar();
    }

    busca.addEventListener("input", () => {
        selecionado = null; adicionar.disabled = true; fechar();
        const termo = busca.value.trim(); if (termo.length < 1) return;
        const atual = ++requisicao;
        temporizador = setTimeout(async () => {
            try {
                const resposta = await fetch(`/fichas-tecnicas/insumos/buscar?termo=${encodeURIComponent(termo)}`);
                if (!resposta.ok) throw new Error();
                const itens = await resposta.json(); if (atual !== requisicao) return;
                resultados.replaceChildren();
                itens.forEach(item => {
                    const botao = document.createElement("button"); botao.type = "button";
                    botao.className = "list-group-item list-group-item-action";
                    botao.textContent = `${item.nome} · ${item.unidade.descricao || item.unidade}`;
                    botao.addEventListener("click", () => {
                        selecionado = {...item, simbolo: ({UNIDADE:"un",QUILOGRAMA:"kg",GRAMA:"g",LITRO:"L",MILILITRO:"ml"})[item.unidade] || ""};
                        busca.value = item.nome; adicionar.disabled = false; fechar(); adicionar.focus();
                    });
                    resultados.append(botao);
                });
                resultados.classList.toggle("d-none", itens.length === 0);
            } catch { if (atual === requisicao) avisar("Não foi possível pesquisar os insumos."); }
        }, 250);
    });
    adicionar.addEventListener("click", () => { if (selecionado) { adicionarLinha(selecionado); selecionado = null; busca.value = ""; adicionar.disabled = true; busca.focus(); } });
    container.addEventListener("input", event => { if (event.target.matches("[data-ficha-quantidade-item]")) reindexar(); });
    container.addEventListener("click", event => { const botao = event.target.closest("[data-ficha-remover]"); if (botao) { botao.closest("[data-ficha-item]").remove(); reindexar(); } });
    busca.addEventListener("keydown", event => { if (event.key === "Escape") fechar(); });
    document.addEventListener("click", event => { if (!resultados.contains(event.target) && event.target !== busca) fechar(); });
    produto?.addEventListener("change", atualizarUnidadeRendimento);
    rendimento?.addEventListener("input", reindexar);
    atualizarUnidadeRendimento();
    formulario.querySelectorAll("[data-ficha-item-inicial]").forEach(i => adicionarLinha({itemId:i.dataset.id,id:i.dataset.insumoId,nome:i.dataset.nome,unidade:i.dataset.unidade,simbolo:i.dataset.simbolo,quantidade:i.dataset.quantidade,custoMedio:i.dataset.custo,estoqueAtual:i.dataset.estoque,possuiCusto:i.dataset.possuiCusto === "true"}));
    reindexar();
}

function inicializarPreviaRendimentoProducao() {
    const formulario = document.querySelector("[data-producao-rendimento]");
    if (!formulario) return;
    const moeda = valor => new Intl.NumberFormat("pt-BR", {style:"currency",currency:"BRL"}).format(valor || 0);
    const quantidade = (valor, unidade) => `${new Intl.NumberFormat("pt-BR", {minimumFractionDigits:unidade === "UNIDADE" ? 0 : 3,maximumFractionDigits:3}).format(valor || 0)} ${unidade === "UNIDADE" ? "un" : unidade === "QUILOGRAMA" ? "kg" : unidade}`;
    let temporizador;
    async function atualizar(linha) {
        const produto = linha.querySelector("[data-produto-producao]")?.value;
        const real = linha.querySelector("[data-rendimento-real]")?.value;
        const previa = linha.querySelector("[data-previa-producao]");
        if (!produto || !real || Number(real) <= 0) { previa.classList.add("d-none"); return; }
        try {
            const produtosSelecionados = [...formulario.querySelectorAll("[data-produto-producao]")].map(campo => Number(campo.value)).filter(Number.isFinite);
            const recebeAdicionais = Number(produto) === Math.max(...produtosSelecionados);
            const gas = recebeAdicionais ? formulario.querySelector("[data-producao-gas]")?.value || "0" : "0";
            const outros = recebeAdicionais ? formulario.querySelector("[data-producao-outros]")?.value || "0" : "0";
            const resposta = await fetch(`/producoes/previa?produtoId=${encodeURIComponent(produto)}&rendimentoReal=${encodeURIComponent(real)}&valorGasEnergia=${encodeURIComponent(gas)}&valorOutros=${encodeURIComponent(outros)}`);
            if (!resposta.ok) throw new Error();
            const dados = await resposta.json(); previa.classList.remove("d-none");
            previa.querySelector("[data-previa-esperado]").textContent = quantidade(dados.rendimentoEsperado,dados.unidade);
            previa.querySelector("[data-previa-fator]").textContent = new Intl.NumberFormat("pt-BR",{minimumFractionDigits:6,maximumFractionDigits:9}).format(dados.fatorProducao);
            previa.querySelector("[data-previa-custo-unitario]").textContent = `${moeda(dados.custoEstimadoPorUnidade)}/${dados.unidade === "UNIDADE" ? "un" : "kg"}`;
            previa.querySelector("[data-previa-insumos-total]").textContent = moeda(dados.valorInsumos);
            previa.querySelector("[data-previa-adicionais]").textContent = moeda(dados.gastosAdicionais);
            previa.querySelector("[data-previa-total]").textContent = moeda(dados.custoTotalEstimado);
            const corpo = previa.querySelector("[data-previa-insumos]"); corpo.replaceChildren();
            dados.insumos.forEach(item => { const tr=document.createElement("tr");if(!item.estoqueSuficiente)tr.className="table-warning";[item.nome,quantidade(item.quantidadeNecessaria,item.unidade),quantidade(item.estoqueDisponivel,item.unidade),moeda(item.custoMedio),moeda(item.valorEstimado)].forEach(valor=>{const td=document.createElement("td");td.textContent=valor;tr.append(td);});corpo.append(tr); });
            const insuficiente = dados.insumos.some(item => !item.estoqueSuficiente);const aviso=previa.querySelector("[data-previa-erro]");const mensagens=[];if(insuficiente)mensagens.push("Um ou mais Insumos não possuem estoque suficiente para esta Produção.");if(!dados.custoCompleto)mensagens.push(`Custo incompleto: ${dados.insumosSemCusto.join(", ")}.`);aviso.textContent=mensagens.join(" ");aviso.classList.toggle("d-none",mensagens.length===0);
        } catch { previa.classList.remove("d-none");const aviso=previa.querySelector("[data-previa-erro]");aviso.textContent="Não foi possível calcular a prévia da Produção.";aviso.classList.remove("d-none"); }
    }
    formulario.addEventListener("input", evento => { if(!evento.target.matches("[data-rendimento-real]"))return;clearTimeout(temporizador);temporizador=setTimeout(()=>atualizar(evento.target.closest("[data-item-producao]")),250); });
    formulario.addEventListener("change", evento => { if(evento.target.matches("[data-produto-producao]"))atualizar(evento.target.closest("[data-item-producao]")); });
    formulario.addEventListener("input", evento => { if(!evento.target.matches("[data-producao-gas],[data-producao-outros]"))return;clearTimeout(temporizador);temporizador=setTimeout(()=>formulario.querySelectorAll("[data-item-producao]").forEach(atualizar),250); });
    formulario.querySelectorAll("[data-item-producao]").forEach(atualizar);
}

function inicializarItensProducao() {
    const area = document.querySelector("[data-itens-producao]");
    if (!area) return;
    const lista = area.querySelector("[data-lista-produtos-producao]");
    const template = area.querySelector("[data-template-produto-producao]");
    const reindexar = () => {
        lista.querySelectorAll("[data-item-producao]").forEach((linha, indice) => {
            linha.querySelectorAll("[data-campo]").forEach(campo => {
                campo.name = `itens[${indice}].${campo.dataset.campo}`;
                campo.id = `itens${indice}.${campo.dataset.campo}`;
            });
        });
    };
    const atualizarUnidade = linha => {
        const select = linha.querySelector("[data-produto-producao]");
        const unidade = select?.selectedOptions[0]?.dataset.unidade;
        const alvo = linha.querySelector("[data-unidade-producao]");
        if (alvo) alvo.textContent = unidade === "QUILOGRAMA" ? "kg" : unidade === "UNIDADE" ? "un" : "—";
    };
    area.addEventListener("click", event => {
        if (event.target.closest("[data-adicionar-produto-producao]")) {
            lista.append(template.content.cloneNode(true)); reindexar();
        }
        const remover = event.target.closest("[data-remover-produto-producao]");
        if (remover && lista.querySelectorAll("[data-item-producao]").length > 1) {
            remover.closest("[data-item-producao]").remove(); reindexar();
        }
    });
    area.addEventListener("change", event => {
        if (!event.target.matches("[data-produto-producao]")) return;
        const atual = event.target.value;
        const duplicado = [...lista.querySelectorAll("[data-produto-producao]")]
            .some(outro => outro !== event.target && atual && outro.value === atual);
        if (duplicado) { event.target.value = ""; alert("Este produto já foi adicionado à produção."); }
        atualizarUnidade(event.target.closest("[data-item-producao]"));
    });
    lista.querySelectorAll("[data-item-producao]").forEach(atualizarUnidade);
}

function inicializarFormularioEstoque() {
    const formulario = document.querySelector("[data-form-estoque]");
    if (!formulario) return;
    const tipo = formulario.querySelector("[data-estoque-tipo]");
    const referencia = formulario.querySelector("[data-estoque-referencia]");
    const valorInicial = referencia.value;
    const atualizar = () => {
        const seletor = tipo.value === "INSUMO" ? "[data-opcoes-insumo]"
            : tipo.value === "PRODUTO_REVENDA" ? "[data-opcoes-revenda]"
            : tipo.value === "PREPARACAO_PRODUZIDA" ? "[data-opcoes-preparacao]" : "";
        const template = seletor ? formulario.querySelector(seletor) : null;
        referencia.replaceChildren(new Option(tipo.value ? "Selecione" : "Selecione o tipo primeiro", ""));
        if (template) referencia.append(template.content.cloneNode(true));
        if (valorInicial && [...referencia.options].some(opcao => opcao.value === valorInicial)) {
            referencia.value = valorInicial;
        }
    };
    tipo.addEventListener("change", () => { referencia.value = ""; atualizar(); });
    atualizar();
}

function inicializarFormularioProduto() {
    const formulario = document.querySelector("[data-form-produto]");
    if (!formulario) return;
    const tipo = formulario.querySelector("#tipoProduto");
    const unidade = formulario.querySelector("#unidadeVenda");
    const grupo = formulario.querySelector("[data-estoque-minimo-produto]");
    const minimo = formulario.querySelector("#estoqueMinimo");
    const precoGrupo = formulario.querySelector("[data-preco-produto]");
    const preco = formulario.querySelector("#preco");
    const acompanhamentoGrupo = formulario.querySelector("[data-acompanhamento-produto]");
    const acompanhamento = formulario.querySelector("#permiteAcompanhamento");
    const vendavelGrupo = formulario.querySelector("[data-vendavel-produto]");
    const vendavel = formulario.querySelector("#vendavel");
    const ajuda = formulario.querySelector("[data-ajuda-preparacao]");
    const labelUnidade = formulario.querySelector("[data-label-unidade-produto]");
    const atualizar = () => {
        const preparacao = tipo.value === "PREPARACAO_PRODUZIDA";
        const comercial = tipo.value === "PRODUTO_COMERCIAL";
        const controlaEstoque = preparacao || tipo.value === "PRODUTO_REVENDA";
        grupo.classList.toggle("d-none", !controlaEstoque); minimo.disabled = !controlaEstoque;
        minimo.step = unidade.value === "UNIDADE" ? "1" : "0.001";
        if (comercial) minimo.value = "0";
        precoGrupo.classList.toggle("d-none", preparacao); preco.disabled = preparacao; preco.required = !preparacao;
        acompanhamentoGrupo.classList.toggle("d-none", preparacao); vendavelGrupo.classList.toggle("d-none", preparacao); ajuda.classList.toggle("d-none", !preparacao);
        labelUnidade.textContent = preparacao ? "Unidade de controle *" : "Unidade de venda *";
        if (preparacao) { preco.value = ""; vendavel.checked = false; acompanhamento.checked = false; }
    };
    tipo.addEventListener("change", atualizar); unidade.addEventListener("change", atualizar); atualizar();
}

function inicializarFormularioInsumo() {
    const formulario = document.querySelector("[data-form-insumo]");
    if (!formulario) return;
    const unidade = formulario.querySelector("[data-unidade-medida-insumo]");
    const quantidade = formulario.querySelector("[data-quantidade-insumo]");
    const simbolo = formulario.querySelector("[data-simbolo-insumo]");
    const atualizarUnidade = () => {
        const opcao = unidade.options[unidade.selectedIndex];
        simbolo.textContent = opcao?.dataset.simbolo || "—";
        quantidade.setAttribute("inputmode", unidade.value === "UNIDADE" ? "numeric" : "decimal");
        quantidade.dataset.casasDecimais = unidade.value === "UNIDADE" ? "0" : "3";
        quantidade.setAttribute("aria-describedby", "ajudaEstoqueMinimo");
    };
    unidade.addEventListener("change", atualizarUnidade);
    atualizarUnidade();
}

function inicializarFormularioCompra() {
    const formulario = document.querySelector("[data-form-compra]");
    if (!formulario) return;
    const busca = formulario.querySelector("[data-busca-item-compra]");
    const resultados = formulario.querySelector("[data-resultados-item-compra]");
    const adicionar = formulario.querySelector("[data-adicionar-item-compra]");
    const container = formulario.querySelector("[data-itens-compra]");
    const vazio = formulario.querySelector("[data-itens-compra-vazio]");
    const erro = formulario.querySelector("[data-erro-itens-compra]");
    const totalSaida = formulario.querySelector("[data-total-compra]");
    const quantidadeSaida = formulario.querySelector("[data-quantidade-itens-compra]");
    const financeiroBloqueado = formulario.dataset.financeiroBloqueado === "true";
    let selecionado = null, temporizador, numeroRequisicao = 0;
    const moeda = valor => new Intl.NumberFormat("pt-BR", {style: "currency", currency: "BRL"}).format(valor || 0);
    const decimal = valor => Number.parseFloat(String(valor ?? "0").replace(",", ".")) || 0;
    const fechar = () => { clearTimeout(temporizador); numeroRequisicao++; resultados.replaceChildren(); resultados.classList.add("d-none"); };
    const avisar = mensagem => { erro.textContent = mensagem; erro.classList.remove("d-none"); };
    const limparAviso = () => erro.classList.add("d-none");

    function reindexar() {
        const linhas = [...container.querySelectorAll("[data-item-compra]")];
        linhas.forEach((linha, indice) => {
            linha.querySelector("[data-item-id]").name = `itens[${indice}].id`;
            linha.querySelector("[data-tipo-item]").name = `itens[${indice}].tipoItem`;
            linha.querySelector("[data-referencia-id]").name = `itens[${indice}].referenciaId`;
            linha.querySelector("[data-quantidade-decimal]").name = `itens[${indice}].quantidade`;
            linha.querySelector("[data-valor-decimal]").name = `itens[${indice}].valorTotalItem`;
        });
        vazio.classList.toggle("d-none", linhas.length > 0);
        quantidadeSaida.textContent = String(linhas.length);
        const total = linhas.reduce((soma, linha) => soma + decimal(linha.querySelector("[data-valor-decimal]").value), 0);
        totalSaida.textContent = moeda(total);
    }

    function criarCampo(rotulo, tipo) {
        const grupo = document.createElement("div"); grupo.className = tipo === "quantidade" ? "col-sm-6 col-lg-2" : "col-sm-6 col-lg-2";
        const label = document.createElement("label"); label.className = "form-label small"; label.textContent = rotulo;
        const input = document.createElement("input"); input.type = "text"; input.className = "form-control"; input.inputMode = "decimal";
        grupo.append(label, input); return {grupo, input};
    }

    function adicionarLinha(item, priorizar = false) {
        const chave = `${item.tipoItem}:${item.referenciaId}`;
        const existente = [...container.querySelectorAll("[data-item-compra]")].find(l => l.dataset.chaveItem === chave);
        if (existente) {
            avisar("Este item já foi adicionado à compra.");
            existente.classList.add("border-warning", "bg-warning-subtle");
            existente.scrollIntoView({behavior: "smooth", block: "center"});
            const campo = existente.querySelector("[data-quantidade-item]");
            window.setTimeout(() => { existente.classList.remove("border-warning", "bg-warning-subtle"); campo.focus(); campo.select(); }, 300);
            return null;
        }
        limparAviso();
        const linha = document.createElement("div"); linha.className = "row g-2 align-items-end border rounded p-3 mb-3"; linha.dataset.itemCompra = ""; linha.dataset.chaveItem = chave;
        const id = document.createElement("input"); id.type = "hidden"; id.value = item.id || ""; id.dataset.itemId = "";
        const tipoItem = document.createElement("input"); tipoItem.type = "hidden"; tipoItem.value = item.tipoItem; tipoItem.dataset.tipoItem = "";
        const referenciaId = document.createElement("input"); referenciaId.type = "hidden"; referenciaId.value = item.referenciaId; referenciaId.dataset.referenciaId = "";
        const nomeGrupo = document.createElement("div"); nomeGrupo.className = "col-lg-3";
        const nomeLabel = document.createElement("div"); nomeLabel.className = "form-label small"; nomeLabel.textContent = "Item comprado";
        const nome = document.createElement("div"); nome.className = "form-control bg-body-tertiary h-auto"; const tituloNome=document.createElement("div");tituloNome.textContent=item.nome;const categoria=document.createElement("small");categoria.className="text-body-secondary";categoria.textContent=item.categoria;nome.append(tituloNome,categoria);nomeGrupo.append(nomeLabel, nome);
        const quantidade = criarCampo("Quantidade", "quantidade"); quantidade.input.value = new Intl.NumberFormat("pt-BR", {minimumFractionDigits: item.unidade === "UNIDADE" ? 0 : 3, maximumFractionDigits: 3}).format(decimal(item.quantidade));
        quantidade.input.dataset.quantidadeItem = "";
        const quantidadeDecimal = document.createElement("input"); quantidadeDecimal.type = "hidden"; quantidadeDecimal.dataset.quantidadeDecimal = ""; quantidadeDecimal.value = decimal(item.quantidade).toFixed(3); quantidade.grupo.appendChild(quantidadeDecimal);
        const unidade = document.createElement("div"); unidade.className = "col-sm-6 col-lg-1"; const ul = document.createElement("div"); ul.className = "form-label small"; ul.textContent = "Unidade"; const uv = document.createElement("div"); uv.className = "form-control bg-body-tertiary"; uv.textContent = item.simbolo; unidade.append(ul, uv);
        const valor = criarCampo("Valor total pago", "valor");
        valor.input.dataset.compraValorItem = "";
        valor.input.placeholder = "Ex.: 120,00";
        valor.input.value = String(item.valor ?? "").trim() === "" ? "" : new Intl.NumberFormat("pt-BR", {minimumFractionDigits:2,maximumFractionDigits:2}).format(decimal(item.valor));
        const valorDecimal = document.createElement("input"); valorDecimal.type = "hidden"; valorDecimal.dataset.valorDecimal = ""; valorDecimal.value = decimal(item.valor).toFixed(2); valor.grupo.appendChild(valorDecimal);
        const custoGrupo = document.createElement("div"); custoGrupo.className = "col-sm-8 col-lg-3"; const cl = document.createElement("div"); cl.className = "form-label small"; cl.textContent = "Custo unitário"; const custo = document.createElement("div"); custo.className = "form-control bg-body-tertiary"; custoGrupo.append(cl, custo);
        const removerGrupo = document.createElement("div"); removerGrupo.className = "col-sm-4 col-lg-1"; const remover = document.createElement("button"); remover.type = "button"; remover.className = "btn btn-outline-danger w-100"; remover.title = "Remover item"; remover.innerHTML = '<i class="bi bi-trash"></i>'; removerGrupo.appendChild(remover);
        quantidade.input.disabled = financeiroBloqueado; valor.input.disabled = financeiroBloqueado; remover.disabled = financeiroBloqueado;
        linha.append(id, tipoItem, referenciaId, nomeGrupo, quantidade.grupo, unidade, valor.grupo, custoGrupo, removerGrupo); priorizar?container.prepend(linha):container.appendChild(linha);
        const atualizar = () => { const q = decimal(converterMoedaBrasileiraParaDecimal(quantidade.input.value)); const valorVazio = valor.input.value.trim() === ""; const v = valorVazio ? 0 : decimal(converterMoedaBrasileiraParaDecimal(valor.input.value)); quantidadeDecimal.value = q.toFixed(3); valorDecimal.value = valorVazio ? "" : v.toFixed(2); custo.textContent = `${moeda(q > 0 ? v / q : 0)}/${item.simbolo}`; reindexar(); };
        quantidade.input.addEventListener("input", atualizar);
        valor.input.addEventListener("input", () => { valor.input.value = valor.input.value.replace(/[^0-9.,]/g, ""); atualizar(); });
        valor.input.addEventListener("blur", () => {
            if (valor.input.value.trim() !== "") {
                const normalizado = converterMoedaBrasileiraParaDecimal(valor.input.value);
                valor.input.value = new Intl.NumberFormat("pt-BR", {minimumFractionDigits: 2, maximumFractionDigits: 2}).format(Number(normalizado));
            }
            atualizar();
        });
        quantidade.input.addEventListener("keydown", evento => { if(evento.key==="Enter"){evento.preventDefault();valor.input.focus();valor.input.select();} });
        valor.input.addEventListener("keydown", evento => { if(evento.key==="Enter"){evento.preventDefault();busca.focus();} });
        remover.addEventListener("click", () => { linha.remove(); reindexar(); }); atualizar();
        return quantidade.input;
    }

    function escolher(item) { selecionado = item; busca.value = item.nome; adicionar.disabled = false; fechar(); }
    async function pesquisar() { const termo = busca.value.trim(); const requisicao = ++numeroRequisicao; if (termo.length < 2) { fechar(); return; } const resposta = await fetch(`/compras/itens/buscar?termo=${encodeURIComponent(termo)}`); const dados = resposta.ok ? await resposta.json() : []; if (requisicao !== numeroRequisicao || busca.value.trim() !== termo) return; resultados.replaceChildren(); dados.forEach(item => { const botao = document.createElement("button"); botao.type = "button"; botao.className = "list-group-item list-group-item-action"; const nome=document.createElement("div");nome.textContent=item.nome;const detalhe=document.createElement("small");detalhe.className="text-body-secondary";detalhe.textContent=`${item.categoria} · ${item.unidadeDescricao} (${item.simbolo})`;botao.append(nome,detalhe); botao.addEventListener("click", () => escolher(item)); resultados.appendChild(botao); }); if (!dados.length) { const item = document.createElement("div"); item.className = "list-group-item text-body-secondary"; item.textContent = "Nenhum item ativo encontrado."; resultados.appendChild(item); } resultados.classList.remove("d-none"); }
    busca.addEventListener("input", () => { selecionado = null; adicionar.disabled = true; clearTimeout(temporizador); temporizador = setTimeout(pesquisar, 250); });
    busca.addEventListener("keydown", evento => { if (evento.key === "Escape") fechar();if(evento.key==="Enter"){evento.preventDefault();if(selecionado)adicionar.click();} });
    document.addEventListener("click", evento => { if (!busca.contains(evento.target) && !resultados.contains(evento.target)) fechar(); });
    adicionar.addEventListener("click", () => { if (!selecionado) return; const quantidade=adicionarLinha({tipoItem:selecionado.tipoItem,referenciaId: selecionado.id, nome: selecionado.nome,categoria:selecionado.categoria, unidade: selecionado.unidade, simbolo: selecionado.simbolo, quantidade: 0, valor: ""},true); selecionado = null; busca.value = ""; adicionar.disabled = true; fechar();if(quantidade){quantidade.focus();quantidade.select();} });
    formulario.querySelectorAll("[data-item-compra-inicial]").forEach(item => adicionarLinha({id:item.dataset.id,tipoItem:item.dataset.tipoItem,referenciaId:item.dataset.referenciaId,nome:item.dataset.nome,categoria:item.dataset.categoria,unidade:item.dataset.unidade,simbolo:item.dataset.simbolo,quantidade:item.dataset.quantidade,valor:item.dataset.valor}));
    busca.disabled = financeiroBloqueado;
    if (financeiroBloqueado) adicionar.disabled = true;
    formulario.addEventListener("keydown", evento => {if(evento.key==="Enter"&&evento.target instanceof HTMLInputElement&&evento.target!==busca)evento.preventDefault();});
    formulario.addEventListener("submit", evento => { reindexar(); if (evento.submitter?.matches("[data-salvar-compra]")!==true||!container.querySelector("[data-item-compra]")) { evento.preventDefault();if(!container.querySelector("[data-item-compra]"))avisar("Adicione ao menos um item à compra.");return;}evento.submitter.disabled=true; });
    reindexar();
}

function inicializarPreviewProducao() {
    const formulario = document.querySelector("[data-form-producao]");
    if (!formulario) return;
    const saldoInicial = formulario.querySelector("[data-saldo-inicial-materiais]");
    const compras = formulario.querySelector("[data-compras-materiais]");
    const saldoFinal = formulario.querySelector("[data-saldo-final-materiais]");
    const outrosCustos = formulario.querySelectorAll("[data-outro-custo-producao]");
    const saidaRecursos = formulario.querySelector("[data-recursos-producao]");
    const saidasMateriais = formulario.querySelectorAll("[data-materiais-consumidos], [data-resumo-materiais]");
    const saidaTotal = formulario.querySelector("[data-total-gasto-producao]");
    const aviso = formulario.querySelector("[data-aviso-saldo-producao]");
    const salvar = formulario.querySelector("[data-salvar-producao]");
    const formatar = valor => new Intl.NumberFormat("pt-BR", {style: "currency", currency: "BRL"}).format(valor);
    const numero = campo => Number.parseFloat(campo?.value) || 0;
    const atualizar = () => {
        const recursos = numero(saldoInicial) + numero(compras);
        const inconsistente = numero(saldoFinal) > recursos;
        const consumidos = Math.max(0, recursos - numero(saldoFinal));
        const valoresOutros = Array.from(outrosCustos).map(numero);
        const total = consumidos + valoresOutros.reduce((soma, valor) => soma + valor, 0);
        saidaRecursos.textContent = formatar(recursos);
        saidasMateriais.forEach(saida => saida.textContent = formatar(consumidos));
        saidaTotal.textContent = formatar(total);
        formulario.querySelector("[data-resumo-embalagens]").textContent = formatar(valoresOutros[0] || 0);
        formulario.querySelector("[data-resumo-gas]").textContent = formatar(valoresOutros[1] || 0);
        formulario.querySelector("[data-resumo-outros]").textContent = formatar(valoresOutros[2] || 0);
        aviso.classList.toggle("d-none", !inconsistente);
        salvar.disabled = inconsistente;
    };
    [saldoInicial, compras, saldoFinal, ...outrosCustos].forEach(campo => campo.addEventListener("input", atualizar));
    atualizar();
}


function inicializarAtalhosPeriodoRelatorio() {
    const formulario = document.querySelector("[data-relatorio-pedidos-form]");

    if (!formulario) {
        return;
    }

    const dataInicial = formulario.querySelector("#dataInicial");
    const dataFinal = formulario.querySelector("#dataFinal");
    const formatarData = data => {
        const ano = data.getFullYear();
        const mes = String(data.getMonth() + 1).padStart(2, "0");
        const dia = String(data.getDate()).padStart(2, "0");
        return `${ano}-${mes}-${dia}`;
    };

    formulario.querySelectorAll("[data-periodo-relatorio]").forEach(botao => {
        botao.addEventListener("click", () => {
            const hoje = new Date();
            let inicio = new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate());
            let fim = new Date(inicio);

            switch (botao.dataset.periodoRelatorio) {
                case "ultimos-7-dias":
                    inicio.setDate(inicio.getDate() - 6);
                    break;
                case "este-mes":
                    inicio = new Date(hoje.getFullYear(), hoje.getMonth(), 1);
                    break;
                case "mes-anterior":
                    inicio = new Date(hoje.getFullYear(), hoje.getMonth() - 1, 1);
                    fim = new Date(hoje.getFullYear(), hoje.getMonth(), 0);
                    break;
                default:
                    break;
            }

            dataInicial.value = formatarData(inicio);
            dataFinal.value = formatarData(fim);
            dataInicial.focus();
        });
    });
}


function inicializarPreviewConfiguracaoEmpresa() {
    const formulario = document.querySelector("[data-configuracao-empresa-form]");

    if (!formulario) {
        return;
    }

    const preview = formulario.querySelector("[data-config-preview]");
    const logoInput = formulario.querySelector("[data-logo-empresa-input]");
    const logosPreview = formulario.querySelectorAll("[data-logo-empresa-preview]");
    const removerLogo = formulario.querySelector("[data-remover-logo]");
    const nomeInput = formulario.querySelector("[data-preview-nome-empresa]");
    const nomePreview = formulario.querySelector("[data-preview-nome]");
    const nomeCurtoInput = formulario.querySelector("[data-preview-nome-curto]");
    const nomeCurtoPreview = formulario.querySelector("[data-preview-nome-curto-output]");
    const boasVindasInput = formulario.querySelector("[data-preview-boas-vindas]");
    const boasVindasPreview = formulario.querySelector("[data-preview-boas-vindas-output]");
    const classesTema = ["tema-marrom", "tema-azul", "tema-verde", "tema-vinho", "tema-roxo"];
    const logoInicial = logosPreview[0]?.src || "";
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

    nomeCurtoInput.addEventListener("input", () => {
        nomeCurtoPreview.textContent = nomeCurtoInput.value.trim() || "Empresa";
    });

    boasVindasInput.addEventListener("input", () => {
        boasVindasPreview.textContent = boasVindasInput.value.trim() || "Bem-vindo ao sistema.";
    });

    const atualizarLogos = url => logosPreview.forEach(imagem => {
        imagem.src = url;
    });

    logoInput.addEventListener("change", () => {
        if (urlTemporaria) {
            URL.revokeObjectURL(urlTemporaria);
        }

        const arquivo = logoInput.files?.[0];
        if (!arquivo) {
            atualizarLogos(removerLogo?.checked ? preview.dataset.logoPadrao : logoInicial);
            return;
        }

        urlTemporaria = URL.createObjectURL(arquivo);
        atualizarLogos(urlTemporaria);
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
            atualizarLogos(preview.dataset.logoPadrao);
        } else {
            atualizarLogos(logoInicial);
        }
    });

    window.addEventListener("beforeunload", () => {
        if (urlTemporaria) {
            URL.revokeObjectURL(urlTemporaria);
        }
    }, {once: true});
}


function inicializarAlternanciaSenha() {
    const atualizarEstado = (botao, campo, mostrar) => {
        campo.type = mostrar ? "text" : "password";
        const rotulo = mostrar ? "Ocultar senha" : "Mostrar senha";
        botao.setAttribute("aria-label", rotulo);
        botao.setAttribute("title", rotulo);
        botao.setAttribute("aria-pressed", String(mostrar));
        const icone = botao.querySelector("i");
        icone?.classList.toggle("bi-eye", !mostrar);
        icone?.classList.toggle("bi-eye-slash", mostrar);
    };

    document.addEventListener("click", event => {
        const botao = event.target instanceof Element
            ? event.target.closest("[data-password-toggle]") : null;
        if (!botao) return;

        const campoId = botao.dataset.passwordTarget;
        const campo = campoId ? document.getElementById(campoId) : null;
        if (!(campo instanceof HTMLInputElement) || !["password", "text"].includes(campo.type)) return;

        atualizarEstado(botao, campo, campo.type === "password");
    });

    document.addEventListener("reset", event => {
        if (!(event.target instanceof HTMLFormElement)) return;
        event.target.querySelectorAll("[data-password-toggle]").forEach(botao => {
            const campo = document.getElementById(botao.dataset.passwordTarget || "");
            if (campo instanceof HTMLInputElement) atualizarEstado(botao, campo, false);
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
            const casasDecimais = Number.parseInt(campoVisual.dataset.casasDecimais || "2", 10);
            campoVisual.value = new Intl.NumberFormat("pt-BR", {
                minimumFractionDigits: casasDecimais,
                maximumFractionDigits: casasDecimais
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
    const previaEstoque = formulario.querySelector("[data-previa-estoque]");
    const previaTitulo = formulario.querySelector("[data-previa-estoque-titulo]");
    const previaMensagem = formulario.querySelector("[data-previa-estoque-mensagem]");
    const previaComponentes = formulario.querySelector("[data-previa-componentes]");
    const previaFinanceira = formulario.querySelector("[data-previa-financeira]");
    const previaCusto = formulario.querySelector("[data-previa-custo]");
    const previaLucro = formulario.querySelector("[data-previa-lucro]");
    const previaMargem = formulario.querySelector("[data-previa-margem]");
    let temporizadorCliente;
    let temporizadorProduto;
    let requisicaoCliente = 0;
    let requisicaoProduto = 0;
    let temporizadorPrevia;
    let requisicaoPrevia = 0;

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
        agendarPreviaEstoque();
    }

    function agendarPreviaEstoque() {
        clearTimeout(temporizadorPrevia);
        temporizadorPrevia = setTimeout(carregarPreviaEstoque, 300);
    }

    async function carregarPreviaEstoque() {
        const itens = Array.from(itensContainer.querySelectorAll(".item-pedido"))
            .map(item => ({
                produtoId: Number(item.querySelector("[data-item-produto-id]").value),
                quantidade: Number(item.querySelector("[data-item-quantidade]").value),
                observacao: item.querySelector("[data-item-observacao]").value
            }))
            .filter(item => item.produtoId && item.quantidade > 0);

        if (itens.length === 0) {
            previaEstoque.className = "alert alert-secondary py-2";
            previaTitulo.textContent = "Aguardando itens";
            previaMensagem.textContent = "O estoque será baixado quando o pedido entrar em preparação.";
            previaComponentes.replaceChildren();
            previaComponentes.classList.add("d-none");
            previaFinanceira?.classList.add("d-none");
            return;
        }

        const numeroRequisicao = ++requisicaoPrevia;
        const csrf = formulario.querySelector('input[name="_csrf"]');
        const resposta = await fetch("/pedidos/preview-estoque", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                ...(csrf ? {"X-CSRF-TOKEN": csrf.value} : {})
            },
            body: JSON.stringify({itens})
        });

        if (numeroRequisicao !== requisicaoPrevia) return;
        if (!resposta.ok) {
            const erro = await resposta.json().catch(() => ({}));
            previaEstoque.className = "alert alert-warning py-2";
            previaTitulo.textContent = "Prévia indisponível";
            previaMensagem.textContent = erro.mensagem || "Confira os produtos e tente novamente.";
            return;
        }

        const previa = await resposta.json();
        previaEstoque.className = `alert py-2 ${previa.estoqueSuficiente ? "alert-success" : "alert-danger"}`;
        previaTitulo.textContent = previa.estoqueSuficiente ? "Estoque suficiente" : "Existem itens em falta";
        previaMensagem.textContent = "Nenhuma baixa é feita nesta prévia. A baixa ocorre ao iniciar a preparação.";
        previaComponentes.replaceChildren();
        previa.componentes.forEach(componente => {
            const linha = document.createElement("div");
            linha.className = `small d-flex justify-content-between gap-2 ${componente.suficiente ? "" : "text-danger fw-semibold"}`;
            const unidade = componente.unidade === "UNIDADE" ? " un." : ` ${componente.unidade === "QUILOGRAMA" ? "kg" : componente.unidade.toLowerCase()}`;
            linha.textContent = `${componente.nome}: ${componente.necessario}${unidade} necessário · ${componente.disponivel}${unidade} disponível${componente.suficiente ? "" : ` · falta ${componente.faltante}${unidade}`}`;
            previaComponentes.appendChild(linha);
        });
        previaComponentes.classList.remove("d-none");
        if (previaFinanceira) {
            previaFinanceira.classList.remove("d-none");
            previaCusto.textContent = formatarMoeda(Number(previa.custoEstimado));
            previaLucro.textContent = formatarMoeda(Number(previa.lucroBrutoEstimado));
            previaMargem.textContent = `${new Intl.NumberFormat("pt-BR", {minimumFractionDigits: 2, maximumFractionDigits: 2}).format(Number(previa.margemBrutaEstimada))}%`;
        }
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
        item.dataset.produtoTipo = produto.tipoProduto;
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
            const tipo = produto.tipoProduto === "PRODUTO_REVENDA" ? "Revenda" : "Produto comercial";
            detalhes.textContent = `${tipo} · ${produto.unidadeVenda === "UNIDADE" ? "Unidade" : "Quilograma"} · ${formatarMoeda(Number(produto.preco))}`;
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
