# Requisitos — Dashboard Administrativo (Pediu Patrão)

## 1. Contexto

O Pediu Patrão é o sistema de pedidos de uma lanchonete/pizzaria. Já existiam o cadastro de clientes,
produtos, usuários, o fluxo de pedidos (com status, desconto e cancelamento) e a auditoria.
Faltava uma visão gerencial: o responsável pelo estabelecimento não tinha como saber **quanto vendeu,
o que mais vende, em que horário e como estão os pedidos agora**.

**Usuários do dashboard:** perfis **ADMIN** e **GERENTE**. O ATENDENTE opera os pedidos mas não vê
os números financeiros.

## 2. Perguntas que o dashboard responde

| Pergunta do gestor | Onde é respondida |
|---|---|
| Quanto faturei no período? Melhorou ou piorou? | Card *Faturamento* com variação vs. período anterior |
| Quantos pedidos tive e qual o valor médio? | Cards *Pedidos* e *Ticket médio* |
| Estou perdendo muitas vendas? | Card *Cancelamentos* (taxa %) e situação dos pedidos |
| Em que dias/semanas vendi mais? | Gráfico *Faturamento no período* (dia/semana/mês) |
| Quais produtos e categorias puxam a receita? | Ranking e gráfico *Receita por categoria* |
| Qual o horário de pico (para escala de equipe)? | Gráfico *Horários de pico* |
| Como está a fila agora? | *Situação dos pedidos* (recebido, em preparação, pronto…) |

## 3. Requisitos funcionais

| ID | Requisito | Detalhamento / critério de aceite | Implementação |
|---|---|---|---|
| **RF01** | Indicadores gerais de vendas | Exibir faturamento, total de pedidos, pedidos válidos, finalizados, em andamento, cancelados, ticket médio, taxa de cancelamento, total de descontos, itens vendidos e variação do faturamento em relação ao período anterior de mesma duração. | `GET /api/v1/dashboard/resumo` · cards no topo da tela |
| **RF02** | Análise de vendas (gráficos) | Gráfico de linha da evolução do faturamento **ou** do número de pedidos, agrupável por dia, semana ou mês, com dias sem venda exibidos como zero. Gráfico de barras de receita por categoria e de pedidos por hora do dia. Gráfico deve ter dica (tooltip) e versão em tabela. | `/vendas`, `/categorias`, `/horarios` |
| **RF03** | Situação dos pedidos | Mostrar a quantidade e o percentual de pedidos em cada status (RECEBIDO, EM_PREPARACAO, PRONTO, SAIU_PARA_ENTREGA, RETIRADO, FINALIZADO, CANCELADO), inclusive os com zero. | `/status` · painel *Situação dos pedidos* |
| **RF04** | Produtos mais vendidos | Ranking com posição, produto, categoria, quantidade e receita; ordenável por quantidade ou faturamento; limite configurável (1 a 50). | `/produtos/top` · tabela com barras |
| **RF05** | Filtros de consulta | Filtrar por período (atalhos Hoje/7/30/90 dias ou datas livres), situação, categoria e cliente. Os filtros valem para todos os indicadores e ficam na URL (podem ser compartilhados). Padrão: últimos 30 dias. | parâmetros comuns + `/filtros` |
| **RF06** | Integração com a API | A tela não acessa o banco diretamente: tudo vem da API REST em JSON, autenticada e com controle de perfil. Um endpoint consolidado (`/api/v1/dashboard`) evita várias chamadas. | `DashboardController` + `dashboard.js` |
| **RF07** | Atualização e tratamento de dados | Atualização automática configurável (manual, 30 s, 1 min, 5 min), botão de atualizar, horário da última atualização. Tratamento: cancelados fora do faturamento, desconto aplicado proporcionalmente aos itens, pedidos sem data descartados, produto sem categoria agrupado em “Outros”, validação de parâmetros com mensagens claras, exportação CSV. | `DashboardCalculadora`, `ApiExceptionHandler`, `dashboard.js` |

### Requisitos complementares (telas existentes melhoradas)

| ID | Requisito |
|---|---|
| RF08 | Tela de pedidos com filtros por situação, busca, botões só com as transições permitidas, e modais para novo pedido, desconto (com prévia do novo total), cancelamento (justificativa obrigatória) e detalhes (linha do tempo). |
| RF09 | Tela de auditoria com indicadores, filtros (texto, operação, usuário, dia), paginação, modal de antes/depois e exportação CSV. |

## 4. Regras de negócio

| ID | Regra |
|---|---|
| RN01 | Pedido **CANCELADO** não entra em faturamento, ticket médio, ranking, categorias nem horários — só na situação dos pedidos e na taxa de cancelamento. |
| RN02 | Faturamento = soma dos itens × (1 − desconto do pedido). |
| RN03 | Ticket médio = faturamento ÷ pedidos válidos (não cancelados). Sem pedidos, vale 0. |
| RN04 | Taxa de cancelamento = cancelados ÷ total de pedidos do período × 100. |
| RN05 | Variação = (faturamento atual − anterior) ÷ anterior × 100, comparando com o período imediatamente anterior de mesmo tamanho. Sem faturamento anterior, a variação é nula (não exibida). |
| RN06 | Com filtro de **categoria**, só os itens daquela categoria contam; pedidos sem itens dela saem da consulta. |
| RN07 | O período consultado é de no máximo 366 dias; `dataInicio` não pode ser maior que `dataFim`. |
| RN08 | Dashboard acessível apenas a ADMIN e GERENTE. |

## 5. Requisitos não funcionais

| ID | Categoria | Requisito |
|---|---|---|
| RNF01 | Segurança | Autenticação por sessão (formulário) ou HTTP Basic; senhas com BCrypt; autorização por perfil no Spring Security. API sem login responde **401** (não redireciona). |
| RNF02 | Desempenho | O painel completo é montado com uma consulta ao período atual e uma ao período anterior, filtradas no MongoDB (data, status, cliente). Resposta típica abaixo de 300 ms com ~450 pedidos. |
| RNF03 | Usabilidade | Interface em português, responsiva (funciona em celular sem rolagem horizontal), estados de carregamento, vazio e erro. |
| RNF04 | Acessibilidade | Cores dos gráficos com paleta validada para daltonismo e contraste; status sempre com ícone + texto (não só cor); gráfico de vendas com alternativa em tabela; rótulos `aria`. |
| RNF05 | Aparência | Tema próprio (“Forno a Lenha”), modo claro e escuro com preferência salva no navegador. |
| RNF06 | Disponibilidade | Bibliotecas front-end servidas localmente: funciona sem internet. |
| RNF07 | Manutenibilidade | Cálculos isolados em `DashboardCalculadora` (sem banco), testáveis por unidade; DTOs como `record`; camadas controller → service → repository. |
| RNF08 | Confiabilidade | Erros da API em formato JSON padronizado (`timestamp, status, erro, mensagem, caminho`). |
| RNF09 | Portabilidade | Java 21 + Maven Wrapper; conexão com o banco por variável de ambiente `MONGO_URI` (local ou Atlas). |
| RNF10 | Qualidade | Testes automatizados de unidade, de controller (contrato, validação e permissão) e de integração com MongoDB real. |

## 6. Modelo de dados (MongoDB)

O dashboard **não cria coleção nova**: ele agrega os dados já existentes.

```
pedidos                         produtos               clientes
├── _id                         ├── _id                ├── _id
├── clienteId ───────────────►  ├── nome               ├── nome
├── clienteNome                 ├── tipo (categoria)   ├── telefone
├── itens[]                     ├── preco              └── endereço...
│   ├── produtoId ───────────►  └── ativo
│   ├── nomeProduto
│   ├── quantidade
│   └── precoUnitario (Decimal128)
├── subtotal, percentualDesconto, total (Decimal128)
├── status
├── dataHoraEntrada  ◄── filtro de período / agrupamento por dia e hora
├── responsavelEntrada, responsavelSaida, dataHoraSaida
└── canceladoPor, dataHoraCancelamento, justificativaCancelamento

auditoria  (usuario, perfil, dataHora, operacao, entidade, entidadeId, alteracoes[], justificativa)
usuarios   (username, password BCrypt, role)
```

Decisões:
- **Nome e preço do produto são copiados para o item do pedido** (desnormalização): o histórico de vendas
  não muda se o produto for renomeado ou tiver o preço alterado.
- A **categoria** vem do cadastro atual do produto (`produtos.tipo`); se o produto foi excluído, o item vai para “Outros”.
- Valores monetários em `Decimal128`/`BigDecimal` para não ter erro de arredondamento de `double`.

## 7. Arquitetura

```
Navegador (Thymeleaf + dashboard.js + Chart.js)
        │  fetch JSON  (sessão)
        ▼
DashboardController  ──►  DashboardService  ──►  MongoTemplate (consulta filtrada)
  (valida parâmetros          │                        │
   via DashboardFiltro)       ▼                        ▼
                       DashboardCalculadora        MongoDB
                       (regras RN01–RN06,
                        sem acesso a banco)
ApiExceptionHandler → erros JSON padronizados
SecurityConfig      → ADMIN/GERENTE, 401/403
```

Por que calcular em Java e não com *aggregation pipeline* do MongoDB? O volume de uma pizzaria por período é
pequeno (centenas/milhares de pedidos), e manter as regras em Java deixa o cálculo **testável sem banco** e fácil
de explicar. Se o volume crescer, o `DashboardService` pode trocar a busca por agregações sem mudar a API.
