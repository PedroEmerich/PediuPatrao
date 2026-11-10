/* Tela de Pedidos — tabela interativa sobre /api/v1/pedidos */
(function () {
    const API = '/api/v1/pedidos';
    const $ = id => document.getElementById(id);
    const perfil = $('telaPedidos').dataset.perfil;
    const podeOperar = perfil === 'GERENTE' || perfil === 'ATENDENTE';
    const ehGerente = perfil === 'GERENTE';

    const PROXIMOS = {
        RECEBIDO: ['EM_PREPARACAO'],
        EM_PREPARACAO: ['PRONTO'],
        PRONTO: ['SAIU_PARA_ENTREGA', 'RETIRADO'],
        SAIU_PARA_ENTREGA: ['FINALIZADO'],
        RETIRADO: ['FINALIZADO']
    };
    const FASE_INICIAL = ['RECEBIDO', 'EM_PREPARACAO', 'PRONTO'];

    let pedidos = [];
    let filtroStatus = '';
    let pedidoAtual = null;
    const carrinho = {};

    if (!podeOperar) $('btnNovoPedido').classList.add('d-none');

    // ------------------------------------------------------------ carga
    async function carregar() {
        try {
            pedidos = await PP.api(API);
            renderChips();
            renderTabela();
        } catch (e) {
            $('tbPedidos').innerHTML = `<tr><td colspan="7" class="estado-vazio text-danger">${PP.escapar(e.message)}</td></tr>`;
        }
    }

    function renderChips() {
        const contagem = { '': pedidos.length };
        pedidos.forEach(p => contagem[p.status] = (contagem[p.status] || 0) + 1);
        const ordem = ['', 'RECEBIDO', 'EM_PREPARACAO', 'PRONTO', 'SAIU_PARA_ENTREGA', 'RETIRADO', 'FINALIZADO', 'CANCELADO'];
        $('chipsStatus').innerHTML = ordem.filter(s => s === '' || contagem[s]).map(s => `
            <button class="chip ${filtroStatus === s ? 'ativo' : ''}" data-status="${s}" role="tab" aria-selected="${filtroStatus === s}">
                ${s ? PP.STATUS[s].rotulo : 'Todos'}<span class="qtd">${contagem[s] || 0}</span>
            </button>`).join('');
    }

    function filtrados() {
        const termo = $('busca').value.trim().toLowerCase();
        return pedidos.filter(p => !filtroStatus || p.status === filtroStatus)
            .filter(p => !termo || (p.clienteNome || '').toLowerCase().includes(termo)
                || p.itens.some(i => (i.nomeProduto || '').toLowerCase().includes(termo)));
    }

    function renderTabela() {
        const lista = filtrados();
        $('rodapeContagem').textContent = `${lista.length} de ${pedidos.length} pedidos`;
        $('rodapeSoma').textContent = 'Soma (exceto cancelados): ' + PP.moeda(lista.filter(p => p.status !== 'CANCELADO').reduce((s, p) => s + Number(p.total), 0));
        if (!lista.length) {
            $('tbPedidos').innerHTML = `<tr><td colspan="7"><div class="estado-vazio"><i class="bi bi-inbox"></i>Nenhum pedido encontrado</div></td></tr>`;
            return;
        }
        $('tbPedidos').innerHTML = lista.slice(0, 300).map(p => {
            const itens = p.itens.map(i => `${i.quantidade}× ${PP.escapar(i.nomeProduto)}`);
            const proximos = PROXIMOS[p.status] || [];
            const inicial = FASE_INICIAL.includes(p.status);
            const acoes = [];
            if (podeOperar) {
                proximos.forEach(s => acoes.push(`<button class="btn btn-sm btn-outline-primary" data-acao="status" data-id="${p.id}" data-status="${s}" title="Mover para ${PP.STATUS[s].rotulo}"><i class="bi ${PP.STATUS[s].icone}"></i><span class="d-none d-xl-inline ms-1">${PP.STATUS[s].rotulo}</span></button>`));
            }
            const menu = [];
            menu.push(`<li><button class="dropdown-item" data-acao="detalhe" data-id="${p.id}"><i class="bi bi-eye me-2"></i>Detalhes</button></li>`);
            if (ehGerente && inicial) {
                menu.push(`<li><button class="dropdown-item" data-acao="desconto" data-id="${p.id}"><i class="bi bi-percent me-2"></i>Desconto</button></li>`);
                menu.push(`<li><button class="dropdown-item text-danger" data-acao="cancelar" data-id="${p.id}"><i class="bi bi-x-octagon me-2"></i>Cancelar</button></li>`);
            }
            return `<tr>
                <td class="ps-3"><button class="btn btn-link p-0 text-decoration-none fw-semibold" data-acao="detalhe" data-id="${p.id}">#${p.id.slice(-5).toUpperCase()}</button></td>
                <td>${PP.escapar(p.clienteNome)}</td>
                <td class="small" title="${itens.join(', ')}">${itens.slice(0, 2).join('<br>')}${itens.length > 2 ? `<br><span class="texto-3">+${itens.length - 2} item(ns)</span>` : ''}</td>
                <td class="text-end num"><strong>${PP.moeda(p.total)}</strong>${Number(p.percentualDesconto) > 0 ? `<br><span class="small texto-3">−${Number(p.percentualDesconto)}%</span>` : ''}</td>
                <td>${PP.badgeStatus(p.status)}</td>
                <td class="small"><div>${PP.dataHora(p.dataHoraEntrada)}</div><span class="texto-3">${PP.escapar(p.responsavelEntrada || '')}</span></td>
                <td class="text-end pe-3"><div class="d-inline-flex gap-1 align-items-center">${acoes.join('')}
                    <div class="dropdown"><button class="btn btn-sm btn-suave" data-bs-toggle="dropdown" aria-label="Mais ações"><i class="bi bi-three-dots"></i></button>
                    <ul class="dropdown-menu dropdown-menu-end">${menu.join('')}</ul></div></div></td>
            </tr>`;
        }).join('');
    }

    // ------------------------------------------------------------ ações
    async function mudarStatus(id, status, botao) {
        botao.disabled = true;
        try {
            await PP.api(`${API}/${id}/status`, { method: 'PUT', body: JSON.stringify({ status }) });
            PP.toast(`Pedido movido para "${PP.STATUS[status].rotulo}"`);
            await carregar();
        } catch (e) {
            PP.toast(e.message, 'erro');
            botao.disabled = false;
        }
    }

    function abrirDetalhe(p) {
        $('tituloDetalhe').innerHTML = `Pedido #${p.id.slice(-5).toUpperCase()} ${PP.badgeStatus(p.status)}`;
        const eventos = [[p.dataHoraEntrada, `Recebido por <b>${PP.escapar(p.responsavelEntrada || '—')}</b>`]];
        if (p.dataHoraSaida) eventos.push([p.dataHoraSaida, `Saída registrada por <b>${PP.escapar(p.responsavelSaida || '—')}</b>`]);
        if (p.dataHoraCancelamento) eventos.push([p.dataHoraCancelamento, `Cancelado por <b>${PP.escapar(p.canceladoPor || '—')}</b>: “${PP.escapar(p.justificativaCancelamento)}”`]);
        $('corpoDetalhe').innerHTML = `
            <div class="mb-3"><span class="texto-3 small">Cliente</span><div class="fw-semibold">${PP.escapar(p.clienteNome)}</div></div>
            <table class="table table-sm mb-3"><thead><tr><th>Item</th><th class="text-end">Qtd</th><th class="text-end">Unit.</th><th class="text-end">Subtotal</th></tr></thead><tbody>
            ${p.itens.map(i => `<tr><td>${PP.escapar(i.nomeProduto)}</td><td class="text-end">${i.quantidade}</td><td class="text-end num">${PP.moeda(i.precoUnitario)}</td><td class="text-end num">${PP.moeda(i.subtotal)}</td></tr>`).join('')}
            </tbody><tfoot>
              <tr><td colspan="3" class="text-end texto-3">Subtotal</td><td class="text-end num">${PP.moeda(p.subtotal)}</td></tr>
              ${Number(p.percentualDesconto) > 0 ? `<tr><td colspan="3" class="text-end texto-3">Desconto (${Number(p.percentualDesconto)}%)</td><td class="text-end num">−${PP.moeda(p.subtotal - p.total)}</td></tr>` : ''}
              <tr><td colspan="3" class="text-end fw-semibold">Total</td><td class="text-end num fw-bold">${PP.moeda(p.total)}</td></tr>
            </tfoot></table>
            <div class="texto-3 small mb-2">Linha do tempo</div>
            <div class="linha-tempo">${eventos.map(([d, t]) => `<div class="evento"><div class="small texto-3">${PP.dataHora(d)}</div><div>${t}</div></div>`).join('')}</div>`;
        bootstrap.Modal.getOrCreateInstance($('modalDetalhe')).show();
    }

    function abrirDesconto(p) {
        pedidoAtual = p;
        $('percDesconto').value = 10;
        $('rangeDesconto').value = 10;
        atualizarPrevia();
        bootstrap.Modal.getOrCreateInstance($('modalDesconto')).show();
    }
    function atualizarPrevia() {
        if (!pedidoAtual) return;
        const perc = Number($('percDesconto').value) || 0;
        const novo = Number(pedidoAtual.subtotal) * (1 - perc / 100);
        $('previaDesconto').innerHTML = `Total passa de <b>${PP.moeda(pedidoAtual.subtotal)}</b> para <b>${PP.moeda(novo)}</b>`;
    }

    function abrirCancelar(p) {
        pedidoAtual = p;
        $('motivoCancelamento').value = '';
        bootstrap.Modal.getOrCreateInstance($('modalCancelar')).show();
    }

    // ------------------------------------------------------------ novo pedido
    function montarNovoPedido() {
        const clientes = window.PP_CLIENTES || [];
        const produtos = (window.PP_PRODUTOS || []).filter(p => p.ativo !== false);
        $('novoCliente').innerHTML = clientes.map(c => `<option value="${PP.escapar(c.id)}">${PP.escapar(c.nome)}</option>`).join('')
            || '<option value="">Cadastre um cliente primeiro</option>';
        Object.keys(carrinho).forEach(k => delete carrinho[k]);
        $('gradeProdutos').innerHTML = produtos.map(p => `
            <div class="col-md-6"><div class="produto-card" data-produto="${PP.escapar(p.id)}">
                <div class="flex-grow-1 min-w-0"><div class="fw-semibold text-truncate">${PP.escapar(p.nome)}</div>
                <small class="texto-3">${PP.escapar(p.tipo || '')} · ${PP.moeda(p.preco)}</small></div>
                <div class="qtd-controle"><button type="button" data-delta="-1" aria-label="Remover">−</button><span>0</span><button type="button" data-delta="1" aria-label="Adicionar">+</button></div>
            </div></div>`).join('');
        atualizarTotalNovo();
    }
    function atualizarTotalNovo() {
        const produtos = window.PP_PRODUTOS || [];
        const total = Object.entries(carrinho).reduce((s, [id, q]) => s + q * (produtos.find(p => p.id === id)?.preco || 0), 0);
        $('novoTotal').textContent = PP.moeda(total);
    }
    $('gradeProdutos').addEventListener('click', e => {
        const b = e.target.closest('button[data-delta]');
        if (!b) return;
        const card = b.closest('.produto-card');
        const id = card.dataset.produto;
        carrinho[id] = Math.max(0, (carrinho[id] || 0) + Number(b.dataset.delta));
        card.querySelector('.qtd-controle span').textContent = carrinho[id];
        card.classList.toggle('selecionado', carrinho[id] > 0);
        atualizarTotalNovo();
    });
    $('modalNovo').addEventListener('show.bs.modal', montarNovoPedido);
    $('btnSalvarPedido').addEventListener('click', async e => {
        const botao = e.currentTarget;
        const itens = Object.entries(carrinho).filter(([, q]) => q > 0).map(([produtoId, quantidade]) => ({ produtoId, quantidade }));
        if (!$('novoCliente').value) return PP.toast('Selecione um cliente.', 'erro');
        if (!itens.length) return PP.toast('Adicione ao menos 1 item.', 'erro');
        botao.disabled = true;
        try {
            await PP.api(API, { method: 'POST', body: JSON.stringify({ clienteId: $('novoCliente').value, itens }) });
            bootstrap.Modal.getInstance($('modalNovo')).hide();
            PP.toast('Pedido registrado!');
            await carregar();
        } catch (err) {
            PP.toast(err.message, 'erro');
        } finally {
            botao.disabled = false;
        }
    });

    // ------------------------------------------------------------ eventos
    $('tbPedidos').addEventListener('click', e => {
        const b = e.target.closest('[data-acao]');
        if (!b) return;
        const p = pedidos.find(x => x.id === b.dataset.id);
        if (!p) return;
        if (b.dataset.acao === 'status') mudarStatus(p.id, b.dataset.status, b);
        if (b.dataset.acao === 'detalhe') abrirDetalhe(p);
        if (b.dataset.acao === 'desconto') abrirDesconto(p);
        if (b.dataset.acao === 'cancelar') abrirCancelar(p);
    });
    $('chipsStatus').addEventListener('click', e => {
        const b = e.target.closest('[data-status]');
        if (!b) return;
        filtroStatus = b.dataset.status;
        renderChips();
        renderTabela();
    });
    $('busca').addEventListener('input', renderTabela);
    $('btnRecarregar').addEventListener('click', carregar);
    $('rangeDesconto').addEventListener('input', e => { $('percDesconto').value = e.target.value; atualizarPrevia(); });
    $('percDesconto').addEventListener('input', atualizarPrevia);
    $('formDesconto').addEventListener('submit', async e => {
        e.preventDefault();
        try {
            await PP.api(`${API}/${pedidoAtual.id}/desconto`, { method: 'POST', body: JSON.stringify({ percentual: Number($('percDesconto').value) }) });
            bootstrap.Modal.getInstance($('modalDesconto')).hide();
            PP.toast('Desconto aplicado.');
            await carregar();
        } catch (err) { PP.toast(err.message, 'erro'); }
    });
    $('motivosRapidos').addEventListener('click', e => {
        const b = e.target.closest('.chip');
        if (b) $('motivoCancelamento').value = b.textContent.trim();
    });
    $('formCancelar').addEventListener('submit', async e => {
        e.preventDefault();
        try {
            await PP.api(`${API}/${pedidoAtual.id}/cancelar`, { method: 'POST', body: JSON.stringify({ motivo: $('motivoCancelamento').value.trim() }) });
            bootstrap.Modal.getInstance($('modalCancelar')).hide();
            PP.toast('Pedido cancelado.');
            await carregar();
        } catch (err) { PP.toast(err.message, 'erro'); }
    });

    carregar();
})();
