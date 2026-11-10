/* Tela de Auditoria — consome /api/v1/auditoria */
(function () {
    const $ = id => document.getElementById(id);
    const POR_PAGINA = 25;
    let registros = [];
    let pagina = 0;

    const NOMES = {
        CRIACAO_PEDIDO: 'Criação de pedido', ALTERACAO_STATUS: 'Mudança de status', DESCONTO: 'Desconto',
        CANCELAMENTO: 'Cancelamento', CRIACAO_CLIENTE: 'Novo cliente', ALTERACAO_CLIENTE: 'Edição de cliente',
        EXCLUSAO_CLIENTE: 'Exclusão de cliente'
    };
    const classeOp = op => {
        const o = (op || '').toUpperCase();
        if (o.startsWith('CRIACAO')) return 'op-criacao';
        if (o.startsWith('ALTERACAO')) return 'op-alteracao';
        if (o.startsWith('DESCONTO')) return 'op-desconto';
        if (o.startsWith('CANCEL')) return 'op-cancelamento';
        if (o.startsWith('EXCLUS')) return 'op-exclusao';
        return '';
    };
    const nomeOp = op => NOMES[op] || op;
    const valor = v => v === null || v === undefined || v === '' ? '<span class="texto-3">—</span>' : PP.escapar(v);

    function filtrados() {
        const termo = $('audBusca').value.trim().toLowerCase();
        const op = $('audOperacao').value, us = $('audUsuario').value, dia = $('audData').value;
        return registros.filter(r =>
            (!op || r.operacao === op) && (!us || r.usuario === us) &&
            (!dia || (r.dataHora || '').startsWith(dia)) &&
            (!termo || [r.usuario, r.entidade, r.entidadeId, r.justificativa, nomeOp(r.operacao)].some(x => (x || '').toLowerCase().includes(termo))));
    }

    function render() {
        const lista = filtrados();
        const paginas = Math.max(1, Math.ceil(lista.length / POR_PAGINA));
        pagina = Math.min(pagina, paginas - 1);
        const fatia = lista.slice(pagina * POR_PAGINA, (pagina + 1) * POR_PAGINA);
        $('audContagem').textContent = `${lista.length} registro(s) · página ${pagina + 1} de ${paginas}`;
        $('audAnterior').disabled = pagina === 0;
        $('audProxima').disabled = pagina >= paginas - 1;
        if (!fatia.length) {
            $('tbAud').innerHTML = `<tr><td colspan="6"><div class="estado-vazio"><i class="bi bi-shield-check"></i>Nenhum registro encontrado</div></td></tr>`;
            return;
        }
        $('tbAud').innerHTML = fatia.map(r => {
            const idx = registros.indexOf(r);
            const alt = (r.alteracoes || []).slice(0, 2).map(a => `<div class="small"><span class="texto-3">${PP.escapar(a.campo)}:</span> <span class="diff-antes">${valor(a.valorAnterior)}</span> → <span class="diff-depois">${valor(a.valorNovo)}</span></div>`).join('');
            const resto = (r.alteracoes || []).length - 2;
            return `<tr>
                <td class="ps-3 small num">${PP.dataHora(r.dataHora)}</td>
                <td><span class="fw-semibold">${PP.escapar(r.usuario)}</span><div class="small texto-3">${PP.escapar(r.perfil || '')}</div></td>
                <td><span class="op-badge ${classeOp(r.operacao)}">${PP.escapar(nomeOp(r.operacao))}</span></td>
                <td class="small">${PP.escapar(r.entidade)} <span class="texto-3">#${PP.escapar((r.entidadeId || '').slice(-5).toUpperCase())}</span></td>
                <td>${alt || '<span class="texto-3 small">—</span>'}${resto > 0 ? `<div class="small texto-3">+${resto} campo(s)</div>` : ''}${r.justificativa ? `<div class="small"><i class="bi bi-chat-left-quote me-1"></i>${PP.escapar(r.justificativa)}</div>` : ''}</td>
                <td class="pe-3 text-end"><button class="btn btn-sm btn-suave" data-idx="${idx}" aria-label="Ver detalhes"><i class="bi bi-arrows-angle-expand"></i></button></td>
            </tr>`;
        }).join('');
    }

    function renderKpis() {
        const hoje = new Date().toISOString().slice(0, 10);
        const deHoje = registros.filter(r => (r.dataHora || '').startsWith(hoje)).length;
        const cancel = registros.filter(r => r.operacao === 'CANCELAMENTO').length;
        const desc = registros.filter(r => r.operacao === 'DESCONTO').length;
        const usuarios = new Set(registros.map(r => r.usuario)).size;
        const k = (icone, rotulo, v) => `<div class="painel kpi"><div class="kpi-rotulo"><i class="bi ${icone}"></i>${rotulo}</div><div class="kpi-valor">${PP.numero(v)}</div></div>`;
        $('kpisAud').innerHTML = k('bi-list-check', 'Registros', registros.length) + k('bi-calendar-day', 'Hoje', deHoje)
            + k('bi-percent', 'Descontos', desc) + k('bi-x-octagon', 'Cancelamentos', cancel) + k('bi-people', 'Usuários ativos', usuarios);
    }

    function detalhe(r) {
        $('tituloAud').innerHTML = `<span class="op-badge ${classeOp(r.operacao)} me-2">${PP.escapar(nomeOp(r.operacao))}</span>`;
        $('corpoAud').innerHTML = `
            <dl class="row small mb-3">
              <dt class="col-4 texto-3 fw-normal">Quando</dt><dd class="col-8">${PP.dataHora(r.dataHora)}</dd>
              <dt class="col-4 texto-3 fw-normal">Usuário</dt><dd class="col-8">${PP.escapar(r.usuario)} (${PP.escapar(r.perfil)})</dd>
              <dt class="col-4 texto-3 fw-normal">Entidade</dt><dd class="col-8">${PP.escapar(r.entidade)} <code>${PP.escapar(r.entidadeId)}</code></dd>
              ${r.justificativa ? `<dt class="col-4 texto-3 fw-normal">Justificativa</dt><dd class="col-8">${PP.escapar(r.justificativa)}</dd>` : ''}
            </dl>
            <table class="table table-sm"><thead><tr><th>Campo</th><th>Antes</th><th>Depois</th></tr></thead><tbody>
            ${(r.alteracoes || []).map(a => `<tr><td>${PP.escapar(a.campo)}</td><td class="diff-antes">${valor(a.valorAnterior)}</td><td class="diff-depois">${valor(a.valorNovo)}</td></tr>`).join('') || '<tr><td colspan="3" class="texto-3">Sem alterações de campo</td></tr>'}
            </tbody></table>`;
        bootstrap.Modal.getOrCreateInstance($('modalAud')).show();
    }

    function exportar() {
        const linhas = [['Data/Hora', 'Usuário', 'Perfil', 'Operação', 'Entidade', 'ID', 'Alterações', 'Justificativa']];
        filtrados().forEach(r => linhas.push([r.dataHora, r.usuario, r.perfil, r.operacao, r.entidade, r.entidadeId,
            (r.alteracoes || []).map(a => `${a.campo}: ${a.valorAnterior ?? ''} -> ${a.valorNovo ?? ''}`).join(' | '), r.justificativa || '']));
        const csv = '﻿' + linhas.map(l => l.map(c => `"${String(c ?? '').replace(/"/g, '""')}"`).join(';')).join('\n');
        const a = document.createElement('a');
        a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
        a.download = 'auditoria.csv';
        a.click();
    }

    $('tbAud').addEventListener('click', e => { const b = e.target.closest('[data-idx]'); if (b) detalhe(registros[Number(b.dataset.idx)]); });
    ['audBusca', 'audOperacao', 'audUsuario', 'audData'].forEach(id => $(id).addEventListener('input', () => { pagina = 0; render(); }));
    $('audAnterior').addEventListener('click', () => { pagina--; render(); });
    $('audProxima').addEventListener('click', () => { pagina++; render(); });
    $('btnCsvAud').addEventListener('click', exportar);

    PP.api('/api/v1/auditoria').then(lista => {
        registros = lista;
        [...new Set(lista.map(r => r.operacao))].sort().forEach(o => $('audOperacao').insertAdjacentHTML('beforeend', `<option value="${PP.escapar(o)}">${PP.escapar(nomeOp(o))}</option>`));
        [...new Set(lista.map(r => r.usuario))].sort().forEach(u => $('audUsuario').insertAdjacentHTML('beforeend', `<option>${PP.escapar(u)}</option>`));
        renderKpis();
        render();
    }).catch(e => $('tbAud').innerHTML = `<tr><td colspan="6" class="estado-vazio text-danger">${PP.escapar(e.message)}</td></tr>`);
})();
