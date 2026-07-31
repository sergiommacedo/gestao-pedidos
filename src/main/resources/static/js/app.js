document.addEventListener("DOMContentLoaded", () => {

    inicializarMascaras();
    inicializarBuscaCep();
    inicializarModalExclusao();
    inicializarSelecaoPedidos();
    inicializarImpressaoComandas();

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
