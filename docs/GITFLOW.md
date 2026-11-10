# Fluxo de branches (Gitflow)

| Branch | Uso |
|---|---|
| `main` | Versão entregue (produção). Só recebe merge de `release/*` e `hotfix/*`. Tag `v1.0.0`. |
| `develop` | Integração do desenvolvimento. |
| `feature/*` | Uma por funcionalidade, criada a partir de `develop` e integrada com merge `--no-ff`. |
| `release/*` | Preparação da entrega (ajustes finais, documentação), depois merge em `main` e `develop`. |

Features deste trabalho:

| Branch | Conteúdo |
|---|---|
| `feature/api-dashboard` | DTOs, `DashboardFiltro`, `DashboardCalculadora`, `DashboardService`, `DashboardController`, tratamento de erros, segurança |
| `feature/dados-demonstracao` | Carga de pedidos de demonstração (90 dias) |
| `feature/testes-dashboard` | Testes de unidade, de controller e de integração |
| `feature/frontend-dashboard` | Tema, layout, menu, tela do dashboard, bibliotecas locais |
| `feature/telas-pedidos-auditoria` | Novas telas de pedidos, auditoria, login e início |
| `release/1.0.0` | README e documentação (`docs/`) |

O commit inicial contém o projeto base; cada funcionalidade nova entrou por uma `feature/*`
com merge `--no-ff` em `develop`, e a entrega saiu por `release/1.0.0` → `main` (tag `v1.0.0`).

Para assumir a autoria dos commits com seu nome e e-mail, rode uma vez (Git Bash no Windows):

```bash
bash scripts/definir-autoria.sh "Seu Nome" "seu.email@exemplo.com"
```

Para ver o grafo:

```bash
git log --oneline --graph --all
```
