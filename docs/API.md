# Documentação da API — Dashboard Administrativo

Base: `http://localhost:8080`
Formato: JSON (UTF-8). Valores monetários em reais com 2 casas. Datas no formato ISO (`AAAA-MM-DD`).

## Autenticação e permissões

- **Sessão** (após login em `/login`) — usada pelas telas.
- **HTTP Basic** — para testes com curl/Postman: `curl -u gerente:teste123 ...`

| Endpoint | ADMIN | GERENTE | ATENDENTE | sem login |
|---|---|---|---|---|
| `/api/v1/dashboard/**` | ✅ | ✅ | ❌ 403 | ❌ 401 |

## Parâmetros de filtro (RF05)

Comuns a todos os endpoints do dashboard (exceto `/filtros`). Todos opcionais.

| Parâmetro | Tipo | Padrão | Descrição |
|---|---|---|---|
| `dataInicio` | data `AAAA-MM-DD` | `dataFim` − 29 dias | Início do período (inclusive) |
| `dataFim` | data `AAAA-MM-DD` | hoje | Fim do período (inclusive, até 23:59:59) |
| `status` | texto | todos | `RECEBIDO`, `EM_PREPARACAO`, `PRONTO`, `SAIU_PARA_ENTREGA`, `RETIRADO`, `FINALIZADO`, `CANCELADO` (maiúsc./minúsc. indiferente) |
| `categoria` | texto | todas | Categoria (tipo) do produto, ex.: `Pizza`. Considera só os itens dessa categoria |
| `clienteId` | texto | todos | Id do cliente |

Regras: `dataInicio` ≤ `dataFim`; período máximo de 366 dias.

## Formato de erro

Erros de validação e de negócio retornam:

```json
{
  "timestamp": "2026-11-10T22:11:40.905",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Status inválido: 'ENTREGUE'. Valores aceitos: [RECEBIDO, EM_PREPARACAO, ...]",
  "caminho": "/api/v1/dashboard/resumo"
}
```

| Código | Quando acontece |
|---|---|
| 200 | Sucesso |
| 400 | Data mal formatada, período invertido ou > 366 dias, status inexistente, `agrupamento` inválido, `limite` fora de 1–50 ou não numérico, `ordenarPor` inválido |
| 401 | Sem autenticação ou senha errada (corpo padrão do Spring Security) |
| 403 | Perfil sem permissão (ex.: ATENDENTE no dashboard) |
| 404 | Recurso não encontrado (ex.: pedido inexistente) |
| 409 | Operação proibida pelo estado (ex.: avançar pedido cancelado) |
| 500 | Erro inesperado (mensagem genérica; detalhes vão para o log) |

---

## `GET /api/v1/dashboard` — painel completo

Retorna tudo o que a tela precisa em uma única chamada. É o endpoint usado pelo front-end.

Parâmetros extras:

| Parâmetro | Padrão | Descrição |
|---|---|---|
| `agrupamento` | `dia` | `dia`, `semana` (segunda-feira como início) ou `mes` |
| `limite` | `5` | Tamanho do ranking de produtos (1–50) |

**Exemplo**

```
GET /api/v1/dashboard?dataInicio=2026-11-08&dataFim=2026-11-10&limite=2
```

```json
{
  "filtro": { "dataInicio": "2026-11-08", "dataFim": "2026-11-10", "status": null, "categoria": null, "clienteId": null },
  "geradoEm": "2026-11-10T22:13:01.054",
  "resumo": {
    "faturamento": 805.7, "totalPedidos": 11, "pedidosValidos": 8, "pedidosCancelados": 3,
    "pedidosEmAndamento": 3, "pedidosFinalizados": 5, "ticketMedio": 100.71,
    "taxaCancelamento": 27.27, "totalDescontos": 51.3, "itensVendidos": 23, "variacaoFaturamento": -47.16
  },
  "vendas": [
    { "periodo": "2026-11-08", "faturamento": 259.2, "pedidos": 3 },
    { "periodo": "2026-11-09", "faturamento": 204.0, "pedidos": 2 },
    { "periodo": "2026-11-10", "faturamento": 342.5, "pedidos": 3 }
  ],
  "status": [
    { "status": "RECEBIDO", "quantidade": 2, "percentual": 18.18 },
    { "status": "EM_PREPARACAO", "quantidade": 1, "percentual": 9.09 },
    "... (os 7 status, inclusive os zerados)"
  ],
  "topProdutos": [
    { "posicao": 1, "produtoId": "6af3...4673", "nome": "Pizza Calabresa Especial", "categoria": "Pizza", "quantidade": 6, "faturamento": 288.0 },
    { "posicao": 2, "produtoId": "6af3...4674", "nome": "Pizza Quatro Queijos", "categoria": "Pizza", "quantidade": 4, "faturamento": 184.6 }
  ],
  "categorias": [
    { "categoria": "Pizza", "quantidade": 10, "faturamento": 472.6 },
    { "categoria": "Sobremesa", "quantidade": 4, "faturamento": 151.2 }
  ],
  "horarios": [
    { "hora": 18, "pedidos": 1, "faturamento": 108.0 },
    "... (24 posições, de 0 a 23)"
  ]
}
```

---

## `GET /api/v1/dashboard/resumo` — indicadores gerais (RF01)

| Campo | Significado |
|---|---|
| `faturamento` | Soma dos pedidos não cancelados, já com desconto |
| `totalPedidos` | Todos os pedidos do período (inclui cancelados) |
| `pedidosValidos` | `totalPedidos − pedidosCancelados` |
| `pedidosFinalizados` / `pedidosEmAndamento` | Válidos com status FINALIZADO / demais válidos |
| `ticketMedio` | `faturamento ÷ pedidosValidos` |
| `taxaCancelamento` | % de cancelados sobre o total |
| `totalDescontos` | Valor abatido por descontos |
| `itensVendidos` | Soma das quantidades dos itens válidos |
| `variacaoFaturamento` | % vs. período anterior de mesma duração (`null` se não havia faturamento) |

```
GET /api/v1/dashboard/resumo?dataInicio=2026-11-01&dataFim=2026-11-10
```

```json
{
  "faturamento": 4215.9, "totalPedidos": 46, "pedidosValidos": 41, "pedidosCancelados": 5,
  "pedidosEmAndamento": 3, "pedidosFinalizados": 38, "ticketMedio": 102.83,
  "taxaCancelamento": 10.87, "totalDescontos": 149.1, "itensVendidos": 128, "variacaoFaturamento": -15.34
}
```

## `GET /api/v1/dashboard/vendas` — série temporal (RF02)

Parâmetro extra: `agrupamento` = `dia` | `semana` | `mes`. Períodos sem venda aparecem com zero.

```
GET /api/v1/dashboard/vendas?dataInicio=2026-11-04&dataFim=2026-11-10&agrupamento=dia
```

```json
[
  { "periodo": "2026-11-04", "faturamento": 196.0, "pedidos": 2 },
  { "periodo": "2026-11-05", "faturamento": 282.2, "pedidos": 2 },
  "..."
]
```

`periodo`: `AAAA-MM-DD` (dia), segunda-feira da semana `AAAA-MM-DD` (semana) ou `AAAA-MM` (mês).

## `GET /api/v1/dashboard/categorias` — receita por categoria (RF02)

Ordenado por faturamento (maior primeiro). Produtos sem categoria aparecem como `"Outros"`.

```json
[ { "categoria": "Pizza", "quantidade": 10, "faturamento": 472.6 }, { "categoria": "Bebida", "quantidade": 3, "faturamento": 42.0 } ]
```

## `GET /api/v1/dashboard/horarios` — pedidos por hora (RF02)

Sempre 24 posições (`hora` de 0 a 23).

```json
[ { "hora": 0, "pedidos": 0, "faturamento": 0.0 }, "...", { "hora": 20, "pedidos": 24, "faturamento": 2464.3 } ]
```

## `GET /api/v1/dashboard/status` — situação dos pedidos (RF03)

Aceita `dataInicio`, `dataFim`, `categoria`, `clienteId` (o filtro de status não se aplica aqui).
Retorna os 7 status na ordem do fluxo, com quantidade e percentual.

```json
[
  { "status": "RECEBIDO", "quantidade": 2, "percentual": 1.46 },
  { "status": "EM_PREPARACAO", "quantidade": 0, "percentual": 0.0 },
  "...",
  { "status": "CANCELADO", "quantidade": 13, "percentual": 9.49 }
]
```

## `GET /api/v1/dashboard/produtos/top` — ranking de produtos (RF04)

| Parâmetro | Padrão | Descrição |
|---|---|---|
| `limite` | 5 | 1 a 50 |
| `ordenarPor` | `quantidade` | `quantidade` ou `faturamento` (desempate pelo outro critério) |

```
GET /api/v1/dashboard/produtos/top?limite=3&ordenarPor=faturamento
```

```json
[
  { "posicao": 1, "produtoId": "...", "nome": "Pizza Frango com Catupiry", "categoria": "Pizza", "quantidade": 44, "faturamento": 2305.8 },
  { "posicao": 2, "produtoId": "...", "nome": "Pizza Calabresa Especial", "categoria": "Pizza", "quantidade": 45, "faturamento": 2128.8 },
  { "posicao": 3, "produtoId": "...", "nome": "Pizza Quatro Queijos", "categoria": "Pizza", "quantidade": 31, "faturamento": 1586.0 }
]
```

Erro: `?limite=99` → **400** `"Parâmetro 'limite' deve estar entre 1 e 50"`.

## `GET /api/v1/dashboard/filtros` — opções dos filtros (RF05)

Sem parâmetros. Alimenta os `<select>` da tela.

```json
{
  "status": ["RECEBIDO", "EM_PREPARACAO", "PRONTO", "SAIU_PARA_ENTREGA", "RETIRADO", "FINALIZADO", "CANCELADO"],
  "categorias": ["Bebida", "Esfiha", "Lanche", "Pizza", "Sobremesa"],
  "clientes": [ { "id": "6af3...4680", "nome": "Ana Souza" }, { "id": "6af3...4681", "nome": "Bruno Lima" } ],
  "primeiroPedido": "2026-08-13",
  "ultimoPedido": "2026-11-10"
}
```

---

## Endpoints de pedidos e auditoria (usados pelas telas)

### `GET /api/v1/pedidos` · `GET /api/v1/pedidos/{id}`
Lista (mais recentes primeiro) ou busca um pedido. `404` se o id não existir.

### `POST /api/v1/pedidos` — GERENTE, ATENDENTE

```json
{ "clienteId": "6af3...4680", "itens": [ { "produtoId": "6af3...4673", "quantidade": 2 } ] }
```
**201** com o pedido criado (status `RECEBIDO`, totais calculados). **400** se faltar cliente/itens; **404** se cliente ou produto não existir.

### `PUT /api/v1/pedidos/{id}/status` — GERENTE, ATENDENTE

```json
{ "status": "EM_PREPARACAO" }
```
Fluxo permitido: `RECEBIDO → EM_PREPARACAO → PRONTO → (SAIU_PARA_ENTREGA | RETIRADO) → FINALIZADO`.
**409** para transição proibida; **400** para status inexistente.

### `POST /api/v1/pedidos/{id}/desconto` — GERENTE

```json
{ "percentual": 10 }
```
Entre 0,01% e 20%, só antes da saída do pedido. **400** fora da faixa; **409** se o pedido já saiu; **403** para outros perfis.

### `POST /api/v1/pedidos/{id}/cancelar` — GERENTE

```json
{ "motivo": "Cliente desistiu" }
```
Justificativa obrigatória (**400**), só antes da saída (**409**). Fica registrada na auditoria.

### `GET /api/v1/auditoria` — ADMIN, GERENTE

Filtros opcionais: `usuario`, `operacao`, `entidade`.

```json
[
  {
    "id": "...", "usuario": "gerente", "perfil": "GERENTE", "dataHora": "2026-11-10T22:11:20",
    "operacao": "DESCONTO", "entidade": "Pedido", "entidadeId": "6af3...483b",
    "alteracoes": [
      { "campo": "percentualDesconto", "valorAnterior": "0", "valorNovo": "10.00" },
      { "campo": "total", "valorAnterior": "105.00", "valorNovo": "94.50" }
    ],
    "justificativa": null
  }
]
```

Respostas reais capturadas da aplicação: [`evidencias/api-requisicoes.txt`](evidencias/api-requisicoes.txt).
