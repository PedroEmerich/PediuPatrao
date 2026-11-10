/* Pediu Patrão — utilidades comuns a todas as telas */
document.addEventListener('DOMContentLoaded', function () {
    const raiz = document.documentElement;
    const sidebar = document.getElementById('sidebar');

    // ----- menu lateral (mobile)
    document.querySelectorAll('[data-acao="abrir-menu"]').forEach(b => b.addEventListener('click', () => document.body.classList.add('menu-aberto')));
    document.querySelectorAll('[data-acao="fechar-menu"]').forEach(b => b.addEventListener('click', () => document.body.classList.remove('menu-aberto')));

    // ----- destaca o item de menu da página atual e usa o nome dele como título
    if (sidebar) {
        const caminho = window.location.pathname;
        let melhor = null;
        sidebar.querySelectorAll('.menu .menu-link').forEach(a => {
            const href = a.getAttribute('href');
            if (href === caminho || (href !== '/' && caminho.startsWith(href))) {
                if (!melhor || href.length > melhor.getAttribute('href').length) melhor = a;
            }
        });
        if (melhor) {
            melhor.classList.add('ativo');
            const titulo = document.getElementById('topbarTitulo');
            if (titulo) titulo.textContent = melhor.textContent.trim();
        }
    }

    // ----- tema claro/escuro
    const btnTema = document.getElementById('btnTema');
    function temaAtual() {
        const t = raiz.getAttribute('data-theme');
        if (t) return t;
        return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }
    function pintarBotao() {
        if (btnTema) btnTema.innerHTML = temaAtual() === 'dark' ? '<i class="bi bi-sun"></i>' : '<i class="bi bi-moon-stars"></i>';
        raiz.setAttribute('data-bs-theme', temaAtual());
    }
    if (btnTema) {
        btnTema.addEventListener('click', () => {
            const novo = temaAtual() === 'dark' ? 'light' : 'dark';
            raiz.setAttribute('data-theme', novo);
            try { localStorage.setItem('pp-tema', novo); } catch (e) { }
            pintarBotao();
            document.dispatchEvent(new CustomEvent('pp:tema', { detail: novo }));
        });
    }
    pintarBotao();
});

/* ---------- helpers globais ---------- */
const PP = {
    moeda: v => Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }),
    numero: v => Number(v || 0).toLocaleString('pt-BR'),
    percentual: v => Number(v || 0).toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 }) + '%',
    dataHora: iso => iso ? new Date(iso).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', year: '2-digit', hour: '2-digit', minute: '2-digit' }) : '—',
    escapar: s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c])),

    STATUS: {
        RECEBIDO: { rotulo: 'Recebido', icone: 'bi-inbox', classe: 'st-recebido' },
        EM_PREPARACAO: { rotulo: 'Em preparação', icone: 'bi-fire', classe: 'st-preparo' },
        PRONTO: { rotulo: 'Pronto', icone: 'bi-check2-circle', classe: 'st-pronto' },
        SAIU_PARA_ENTREGA: { rotulo: 'Saiu p/ entrega', icone: 'bi-scooter', classe: 'st-rota' },
        RETIRADO: { rotulo: 'Retirado', icone: 'bi-bag-check', classe: 'st-rota' },
        FINALIZADO: { rotulo: 'Finalizado', icone: 'bi-patch-check', classe: 'st-ok' },
        CANCELADO: { rotulo: 'Cancelado', icone: 'bi-x-octagon', classe: 'st-cancelado' }
    },
    badgeStatus(status) {
        const s = PP.STATUS[status] || { rotulo: status, icone: 'bi-question', classe: '' };
        return `<span class="badge-status ${s.classe}"><i class="bi ${s.icone}"></i>${s.rotulo}</span>`;
    },

    /** fetch com tratamento padronizado de erro (lê o JSON {mensagem} da API) */
    async api(url, opcoes = {}) {
        const resp = await fetch(url, {
            headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
            ...opcoes
        });
        if (resp.status === 401) { window.location.href = '/login'; throw new Error('Sessão expirada'); }
        if (!resp.ok) {
            let msg = 'Erro ' + resp.status;
            try { const j = await resp.json(); msg = j.mensagem || msg; } catch (e) { }
            if (resp.status === 403) msg = 'Seu perfil não tem permissão para esta operação.';
            throw new Error(msg);
        }
        return resp.status === 204 ? null : resp.json();
    },

    toast(mensagem, tipo = 'sucesso') {
        const cont = document.getElementById('toasts');
        if (!cont) { alert(mensagem); return; }
        const el = document.createElement('div');
        el.className = 'toast pp-toast ' + (tipo === 'erro' ? 'erro' : 'ok');
        el.setAttribute('role', 'status');
        el.innerHTML = `<div class="d-flex align-items-center gap-2 p-3">
            <i class="bi ${tipo === 'erro' ? 'bi-exclamation-triangle' : 'bi-check-circle'}"></i>
            <div class="flex-grow-1">${PP.escapar(mensagem)}</div>
            <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Fechar"></button></div>`;
        cont.appendChild(el);
        const t = new bootstrap.Toast(el, { delay: 4000 });
        el.addEventListener('hidden.bs.toast', () => el.remove());
        t.show();
    }
};
