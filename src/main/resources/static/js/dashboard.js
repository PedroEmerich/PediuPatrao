/* =====================================================================
   Dashboard Administrativo — consome /api/v1/dashboard
   RF01 indicadores · RF02 gráficos · RF03 situação · RF04 ranking
   RF05 filtros · RF06 integração com a API · RF07 atualização/tratamento
   ===================================================================== */
(function () {
    const API = '/api/v1/dashboard';
    const $ = id => document.getElementById(id);

    const estado = {
        metrica: 'faturamento',
        agrupamento: 'dia',
        ordemRanking: 'quantidade',
        painel: null,
        timer: null,
        requisicao: 0
    };
    const graficos = {};

    // ------------------------------------------------------------ datas
    const iso = d => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    const br = s => s ? s.split('-').reverse().join('/') : '';
    function aplicarPreset(dias) {
        const fim = new Date();
        const ini = new Date();
        ini.setDate(fim.getDate() - (dias - 1));
        $('fInicio').value = iso(ini);
        $('fFim').value = iso(fim);
        marcarPreset(dias);
    }
    function marcarPreset(dias) {
        document.querySelectorAll('#presets button').forEach(b => b.classList.toggle('ativo', Number(b.dataset.dias) === dias));
    }

    // ------------------------------------------------------------ filtros <-> URL
    function filtrosAtuais() {
        const p = new URLSearchParams();
        ['fInicio:dataInicio', 'fFim:dataFim', 'fStatus:status', 'fCategoria:categoria', 'fCliente:clienteId'].forEach(par => {
            const [id, nome] = par.split(':');
            const v = $(id).value;
            if (v) p.set(nome, v);
        });
        return p;
    }
    function salvarNaUrl() {
        const p = filtrosAtuais();
        p.set('agrupamento', estado.agrupamento);
        history.replaceState(null, '', location.pathname + '?' + p.toString());
    }
    function lerDaUrl() {
        const p = new URLSearchParams(location.search);
        if (!p.get('dataInicio') && !p.get('dataFim')) {
            aplicarPreset(30);
        } else {
            $('fInicio').value = p.get('dataInicio') || '';
            $('fFim').value = p.get('dataFim') || '';
            marcarPreset(-1);
        }
        $('fStatus').dataset.inicial = p.get('status') || '';
        $('fCategoria').dataset.inicial = p.get('categoria') || '';
        $('fCliente').dataset.inicial = p.get('clienteId') || '';
        if (p.get('agrupamento')) {
            estado.agrupamento = p.get('agrupamento');
            document.querySelectorAll('#agrupamento button').forEach(b => b.classList.toggle('ativo', b.dataset.agr === estado.agrupamento));
        }
    }

    async function carregarOpcoes() {
        const op = await PP.api(API + '/filtros');
        const preencher = (sel, itens, valor, rotulo) => {
            itens.forEach(i => sel.insertAdjacentHTML('beforeend',
                `<option value="${PP.escapar(valor(i))}">${PP.escapar(rotulo(i))}</option>`));
            sel.value = sel.dataset.inicial || '';
        };
        preencher($('fStatus'), op.status, s => s, s => (PP.STATUS[s] || {}).rotulo || s);
        preencher($('fCategoria'), op.categorias, c => c, c => c);
        preencher($('fCliente'), op.clientes, c => c.id, c => c.nome || '(sem nome)');
        if (op.primeiroPedido) $('fInicio').min = op.primeiroPedido;
    }

    // ------------------------------------------------------------ carga principal (RF06)
    async function carregar() {
        const fi = $('fInicio').value, ff = $('fFim').value;
        if (fi && ff && fi > ff) {
            mostrarErro('A data inicial não pode ser maior que a data final.');
            return;
        }
        const id = ++estado.requisicao; // descarta respostas antigas se o usuário trocar o filtro rápido
        const p = filtrosAtuais();
        p.set('agrupamento', estado.agrupamento);
        p.set('limite', '8');
        $('dashboard').classList.add('carregando');
        try {
            const painel = await PP.api(API + '?' + p.toString());
            if (id !== estado.requisicao) return;
            estado.painel = painel;
            esconderErro();
            renderizar(painel);
            if (estado.ordemRanking !== 'quantidade') await carregarRanking();
            salvarNaUrl();
            $('ultimaAtualizacao').textContent = 'Atualizado às ' + new Date(painel.geradoEm).toLocaleTimeString('pt-BR');
        } catch (e) {
            if (id === estado.requisicao) mostrarErro(e.message);
        } finally {
            if (id === estado.requisicao) $('dashboard').classList.remove('carregando');
        }
    }

    async function carregarRanking() {
        const p = filtrosAtuais();
        p.set('limite', '8');
        p.set('ordenarPor', estado.ordemRanking);
        try {
            renderRanking(await PP.api(API + '/produtos/top?' + p.toString()));
        } catch (e) {
            PP.toast(e.message, 'erro');
        }
    }

    function mostrarErro(msg) {
        const el = $('erroDashboard');
        el.innerHTML = `<i class="bi bi-exclamation-triangle me-2"></i>${PP.escapar(msg)}`;
        el.classList.remove('d-none');
    }
    function esconderErro() { $('erroDashboard').classList.add('d-none'); }

    // ------------------------------------------------------------ render
    function renderizar(d) {
        const f = d.filtro;
        const dias = Math.round((new Date(f.dataFim) - new Date(f.dataInicio)) / 864e5) + 1;
        $('descricaoPeriodo').textContent = `${br(f.dataInicio)} a ${br(f.dataFim)} · ${dias} dia${dias > 1 ? 's' : ''}`;
        renderKpis(d.resumo, dias);
        renderSerie(d.vendas);
        renderStatus(d.status);
        renderRanking(d.topProdutos);
        renderCategorias(d.categorias);
        renderHoras(d.horarios);
    }

    function renderKpis(r, dias) {
        $('kFaturamento').textContent = PP.moeda(r.faturamento);
        if (r.variacaoFaturamento === null || r.variacaoFaturamento === undefined) {
            $('kVariacao').textContent = 'sem dados no período anterior';
        } else {
            const sobe = r.variacaoFaturamento >= 0;
            $('kVariacao').innerHTML = `<span class="delta ${sobe ? 'sobe' : 'desce'}"><i class="bi bi-arrow-${sobe ? 'up' : 'down'}-short"></i>${PP.percentual(Math.abs(r.variacaoFaturamento))}</span> vs. ${dias} dias anteriores`;
        }
        $('kPedidos').textContent = PP.numero(r.totalPedidos);
        $('kPedidosExtra').textContent = `${PP.numero(r.pedidosFinalizados)} finalizados · ${PP.numero(r.pedidosEmAndamento)} em andamento`;
        $('kTicket').textContent = PP.moeda(r.ticketMedio);
        $('kItens').textContent = PP.numero(r.itensVendidos);
        $('kDescontos').textContent = `${PP.moeda(r.totalDescontos)} em descontos`;
        $('kCancel').textContent = PP.percentual(r.taxaCancelamento);
        $('kCancelExtra').textContent = `${PP.numero(r.pedidosCancelados)} pedido${r.pedidosCancelados === 1 ? '' : 's'} cancelado${r.pedidosCancelados === 1 ? '' : 's'}`;
    }

    function cores() {
        const css = getComputedStyle(document.documentElement);
        const v = n => css.getPropertyValue(n).trim();
        return { serie: v('--serie-1'), grade: v('--grade'), texto: v('--texto-2'), texto3: v('--texto-3'), superficie: v('--superficie'), borda: v('--borda'), fonteTexto: v('--texto') };
    }

    function rotuloPeriodo(p) {
        if (estado.agrupamento === 'mes') {
            const [a, m] = p.split('-');
            return new Date(a, m - 1, 1).toLocaleDateString('pt-BR', { month: 'short', year: '2-digit' });
        }
        const [a, m, dd] = p.split('-');
        return (estado.agrupamento === 'semana' ? 'sem. ' : '') + `${dd}/${m}`;
    }

    function opcoesBase(c, formatoY) {
        return {
            responsive: true,
            maintainAspectRatio: false,
            animation: { duration: 350 },
            interaction: { mode: 'index', intersect: false },
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: c.superficie, titleColor: c.fonteTexto, bodyColor: c.fonteTexto,
                    borderColor: c.borda, borderWidth: 1, padding: 10, cornerRadius: 10, displayColors: false,
                    callbacks: { label: ctx => formatoY(ctx.parsed.y ?? ctx.parsed.x) }
                }
            },
            scales: {
                x: { grid: { display: false }, border: { color: c.grade }, ticks: { color: c.texto3, maxRotation: 0, autoSkipPadding: 12, font: { family: 'Inter', size: 11 } } },
                y: { beginAtZero: true, grid: { color: c.grade }, border: { display: false }, ticks: { color: c.texto3, font: { family: 'Inter', size: 11 }, callback: v => formatoY(v, true) } }
            }
        };
    }

    const fmtMoedaCurta = (v, eixo) => eixo && v >= 1000 ? 'R$ ' + (v / 1000).toLocaleString('pt-BR', { maximumFractionDigits: 1 }) + ' mil' : PP.moeda(v);
    const fmtPedidos = (v) => PP.numero(v) + (v === 1 ? ' pedido' : ' pedidos');

    function desenhar(nome, canvasId, config) {
        if (graficos[nome]) graficos[nome].destroy();
        graficos[nome] = new Chart($(canvasId), config);
    }

    function renderSerie(vendas) {
        const c = cores();
        const porFat = estado.metrica === 'faturamento';
        $('tituloSerie').textContent = porFat ? 'Faturamento no período' : 'Pedidos no período';
        const valores = vendas.map(v => porFat ? Number(v.faturamento) : v.pedidos);
        const fmt = porFat ? fmtMoedaCurta : fmtPedidos;
        const opcoes = opcoesBase(c, fmt);
        opcoes.plugins.tooltip.callbacks.title = itens => {
            const p = vendas[itens[0].dataIndex].periodo;
            return estado.agrupamento === 'semana' ? 'Semana de ' + br(p) : estado.agrupamento === 'mes' ? rotuloPeriodo(p) : br(p);
        };
        opcoes.plugins.tooltip.callbacks.label = ctx => {
            const v = vendas[ctx.dataIndex];
            return [PP.moeda(v.faturamento), fmtPedidos(v.pedidos)];
        };
        desenhar('serie', 'gSerie', {
            type: 'line',
            data: {
                labels: vendas.map(v => rotuloPeriodo(v.periodo)),
                datasets: [{
                    data: valores,
                    borderColor: c.serie,
                    borderWidth: 2,
                    tension: .3,
                    pointRadius: vendas.length > 45 ? 0 : 3,
                    pointHoverRadius: 5,
                    pointBackgroundColor: c.serie,
                    pointBorderColor: c.superficie,
                    pointBorderWidth: 2,
                    fill: true,
                    backgroundColor: ctx => {
                        const { chart } = ctx;
                        if (!chart.chartArea) return 'transparent';
                        const g = chart.ctx.createLinearGradient(0, chart.chartArea.top, 0, chart.chartArea.bottom);
                        g.addColorStop(0, c.serie + '40');
                        g.addColorStop(1, c.serie + '00');
                        return g;
                    }
                }]
            },
            options: opcoes
        });

        // tabela acessível com os mesmos dados
        $('tabelaSerie').innerHTML = `<table class="table table-sm"><thead><tr><th>Período</th><th class="text-end">Pedidos</th><th class="text-end">Faturamento</th></tr></thead><tbody>
            ${vendas.map(v => `<tr><td>${rotuloPeriodo(v.periodo)}</td><td class="text-end num">${PP.numero(v.pedidos)}</td><td class="text-end num">${PP.moeda(v.faturamento)}</td></tr>`).join('')}
            </tbody></table>`;
    }

    function renderStatus(lista) {
        const total = lista.reduce((s, x) => s + x.quantidade, 0);
        $('totalStatus').textContent = `${PP.numero(total)} no total`;
        if (total === 0) {
            $('listaStatus').innerHTML = vazio('Nenhum pedido no período');
            return;
        }
        const max = Math.max(...lista.map(s => s.quantidade));
        $('listaStatus').innerHTML = lista.map(s => `
            <div class="linha-status ${s.status === 'CANCELADO' ? 'st-cancelado-linha' : ''}" title="${PP.numero(s.quantidade)} pedidos (${PP.percentual(s.percentual)})">
                <span>${PP.badgeStatus(s.status)}</span>
                <span class="trilho"><span style="width:${max ? (s.quantidade / max * 100) : 0}%"></span></span>
                <span class="text-end num fw-semibold">${PP.numero(s.quantidade)}</span>
            </div>`).join('');
    }

    function renderRanking(lista) {
        if (!lista.length) {
            $('tbRanking').innerHTML = `<tr><td colspan="5">${vazio('Nenhum produto vendido no período')}</td></tr>`;
            return;
        }
        const porFat = estado.ordemRanking === 'faturamento';
        const max = Math.max(...lista.map(p => porFat ? Number(p.faturamento) : p.quantidade));
        $('tbRanking').innerHTML = lista.map(p => {
            const v = porFat ? Number(p.faturamento) : p.quantidade;
            return `<tr>
                <td><span class="posicao ${p.posicao <= 3 ? 'p' + p.posicao : ''}">${p.posicao}</span></td>
                <td><div class="fw-semibold">${PP.escapar(p.nome)}</div><small class="texto-3">${PP.escapar(p.categoria)}</small></td>
                <td class="text-end num">${PP.numero(p.quantidade)}</td>
                <td class="text-end num">${PP.moeda(p.faturamento)}</td>
                <td><div class="barra-mini"><span style="width:${max ? v / max * 100 : 0}%"></span></div></td>
            </tr>`;
        }).join('');
    }

    function renderCategorias(lista) {
        const c = cores();
        const opcoes = opcoesBase(c, fmtMoedaCurta);
        opcoes.indexAxis = 'y';
        opcoes.interaction = { mode: 'nearest', axis: 'y', intersect: false };
        opcoes.scales = {
            x: { beginAtZero: true, grid: { color: c.grade }, border: { display: false }, ticks: { color: c.texto3, font: { size: 11 }, callback: v => fmtMoedaCurta(v, true) } },
            y: { grid: { display: false }, border: { color: c.grade }, ticks: { color: c.texto, font: { size: 12, weight: 500 } } }
        };
        opcoes.plugins.tooltip.callbacks.label = ctx => {
            const it = lista[ctx.dataIndex];
            return [PP.moeda(it.faturamento), PP.numero(it.quantidade) + ' itens'];
        };
        desenhar('categorias', 'gCategorias', {
            type: 'bar',
            data: {
                labels: lista.map(x => x.categoria),
                datasets: [{ data: lista.map(x => Number(x.faturamento)), backgroundColor: c.serie, borderRadius: 4, borderSkipped: 'start', maxBarThickness: 26 }]
            },
            options: opcoes
        });
    }

    function renderHoras(lista) {
        const c = cores();
        const pico = lista.reduce((a, b) => b.pedidos > a.pedidos ? b : a, lista[0]);
        $('picoTexto').textContent = pico && pico.pedidos > 0
            ? `Pico às ${String(pico.hora).padStart(2, '0')}h com ${fmtPedidos(pico.pedidos)}`
            : 'Pedidos por hora do dia';
        const opcoes = opcoesBase(c, fmtPedidos);
        opcoes.plugins.tooltip.callbacks.title = itens => `${String(lista[itens[0].dataIndex].hora).padStart(2, '0')}h – ${String(lista[itens[0].dataIndex].hora).padStart(2, '0')}h59`;
        opcoes.plugins.tooltip.callbacks.label = ctx => [fmtPedidos(lista[ctx.dataIndex].pedidos), PP.moeda(lista[ctx.dataIndex].faturamento)];
        opcoes.scales.y.ticks.callback = v => Number.isInteger(v) ? v : '';
        desenhar('horas', 'gHoras', {
            type: 'bar',
            data: {
                labels: lista.map(h => String(h.hora).padStart(2, '0') + 'h'),
                datasets: [{ data: lista.map(h => h.pedidos), backgroundColor: c.serie, borderRadius: 4, borderSkipped: 'start', maxBarThickness: 28 }]
            },
            options: opcoes
        });
    }

    const vazio = msg => `<div class="estado-vazio"><i class="bi bi-inbox"></i>${msg}</div>`;

    // ------------------------------------------------------------ RF07 — atualização automática
    function configurarAutoRefresh() {
        clearInterval(estado.timer);
        const seg = Number($('autoRefresh').value);
        $('pulso').classList.toggle('parado', seg === 0);
        try { localStorage.setItem('pp-auto-refresh', String(seg)); } catch (e) { }
        if (seg > 0) {
            estado.timer = setInterval(() => { if (!document.hidden) carregar(); }, seg * 1000);
        }
    }

    // ------------------------------------------------------------ exportação CSV
    function exportarCsv() {
        const d = estado.painel;
        if (!d) return;
        const linhas = [['Período', 'Pedidos', 'Faturamento (R$)']];
        d.vendas.forEach(v => linhas.push([v.periodo, v.pedidos, String(v.faturamento).replace('.', ',')]));
        linhas.push([], ['Produto', 'Categoria', 'Quantidade', 'Faturamento (R$)']);
        d.topProdutos.forEach(p => linhas.push([p.nome, p.categoria, p.quantidade, String(p.faturamento).replace('.', ',')]));
        const csv = '﻿' + linhas.map(l => l.map(c => `"${String(c ?? '').replace(/"/g, '""')}"`).join(';')).join('\n');
        const a = document.createElement('a');
        a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
        a.download = `dashboard_${d.filtro.dataInicio}_${d.filtro.dataFim}.csv`;
        a.click();
        URL.revokeObjectURL(a.href);
    }

    // ------------------------------------------------------------ eventos
    function ligarSegmentado(id, attr, aoMudar) {
        $(id).addEventListener('click', e => {
            const b = e.target.closest('button');
            if (!b) return;
            $(id).querySelectorAll('button').forEach(x => x.classList.toggle('ativo', x === b));
            aoMudar(b.dataset[attr]);
        });
    }

    document.querySelectorAll('#presets button').forEach(b => b.addEventListener('click', () => { aplicarPreset(Number(b.dataset.dias)); carregar(); }));
    ['fInicio', 'fFim'].forEach(id => $(id).addEventListener('change', () => { marcarPreset(-1); carregar(); }));
    ['fStatus', 'fCategoria', 'fCliente'].forEach(id => $(id).addEventListener('change', carregar));
    $('btnLimpar').addEventListener('click', () => {
        ['fStatus', 'fCategoria', 'fCliente'].forEach(id => $(id).value = '');
        aplicarPreset(30);
        carregar();
    });
    ligarSegmentado('metricaSerie', 'metrica', m => { estado.metrica = m; if (estado.painel) renderSerie(estado.painel.vendas); });
    ligarSegmentado('agrupamento', 'agr', a => { estado.agrupamento = a; carregar(); });
    ligarSegmentado('ordemRanking', 'ordem', o => { estado.ordemRanking = o; carregarRanking(); });
    $('btnTabelaSerie').addEventListener('click', e => {
        const tabela = $('tabelaSerie').classList.toggle('d-none');
        $('areaSerie').classList.toggle('d-none', !tabela);
        e.currentTarget.setAttribute('aria-pressed', String(!tabela));
    });
    $('btnAtualizar').addEventListener('click', carregar);
    $('btnCsv').addEventListener('click', exportarCsv);
    $('autoRefresh').addEventListener('change', configurarAutoRefresh);
    $('formFiltros').addEventListener('submit', e => { e.preventDefault(); carregar(); });
    document.addEventListener('pp:tema', () => { if (estado.painel) renderizar(estado.painel); });

    // ------------------------------------------------------------ início
    try {
        const salvo = localStorage.getItem('pp-auto-refresh');
        if (salvo !== null) $('autoRefresh').value = salvo;
    } catch (e) { }
    Chart.defaults.font.family = "'Inter', system-ui, sans-serif";
    lerDaUrl();
    carregarOpcoes().catch(e => PP.toast('Não foi possível carregar os filtros: ' + e.message, 'erro')).finally(carregar);
    configurarAutoRefresh();
})();
