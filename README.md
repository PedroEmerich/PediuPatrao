# Pediu Patrão — Dashboard Administrativo

Sistema de pedidos para lanchonete/pizzaria com **painel administrativo de vendas**.
O dashboard consome uma API REST própria (`/api/v1/dashboard`) que lê os pedidos do MongoDB e
calcula indicadores, gráficos, ranking de produtos e situação dos pedidos, com filtros por período,
situação, categoria e cliente.

![Dashboard](docs/evidencias/01-dashboard-claro.png)

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Back-end | Spring Boot 3.4 (Web, Data MongoDB, Security, Thymeleaf) |
| Banco de dados | MongoDB (local ou Atlas) |
| Front-end | Thymeleaf + Bootstrap 5.3 + Chart.js 4 + JavaScript puro |
| Testes | JUnit 5, Mockito, MockMvc, Spring Security Test |

Todas as bibliotecas do front-end (Bootstrap, ícones, Chart.js e fontes) ficam em
`src/main/resources/static/vendor`, então **o sistema funciona sem internet** (importante na hora da apresentação).

## Como rodar

### 1. Pré-requisitos
- **Java 21** instalado (`java -version` deve mostrar 21).
- Um **MongoDB** acessível: local (porta 27017), via Docker ou MongoDB Atlas.
- Não precisa instalar o Maven: o projeto usa o *Maven Wrapper* (`mvnw` / `mvnw.cmd`).

Para subir um MongoDB local com Docker:

```bash
docker run -d --name mongo -p 27017:27017 mongo:7
```

### 2. Configurar a variável `MONGO_URI`

A conexão é lida da variável de ambiente `MONGO_URI` (ver `application.properties`).
O nome do banco é `pizzaria`.

**Windows (PowerShell):**

```powershell
$env:MONGO_URI="mongodb://localhost:27017/pizzaria"
```

**Windows (CMD):**

```bat
set MONGO_URI=mongodb://localhost:27017/pizzaria
```

**Linux / macOS:**

```bash
export MONGO_URI="mongodb://localhost:27017/pizzaria"
```

**MongoDB Atlas** (exemplo):

```
mongodb+srv://USUARIO:SENHA@cluster0.xxxxx.mongodb.net/pizzaria
```

### 3. Executar

Windows:

```bat
mvnw.cmd spring-boot:run
```

Linux / macOS:

```bash
./mvnw spring-boot:run
```

Acesse **http://localhost:8080** e entre com um dos usuários abaixo.

### 4. Rodar os testes

```bash
./mvnw test
```

Os testes de integração (`DashboardIntegracaoTest`) só rodam quando `MONGO_URI` está definida;
eles usam um banco separado (`pizzaria_test`) e não mexem nos dados da aplicação.

## Usuários padrão

Criados automaticamente na primeira execução (`DataInitializer`). Senha de todos: **`teste123`**

| Usuário | Perfil | O que pode fazer |
|---|---|---|
| `admin` | ADMIN | Dashboard, auditoria, usuários, consulta de pedidos |
| `gerente` | GERENTE | Dashboard, auditoria, pedidos (inclui **desconto** e **cancelamento**), produtos |
| `atendente` | ATENDENTE | Registrar pedidos, avançar status, clientes. **Não** acessa o dashboard |

## Dados de demonstração

Com a coleção `pedidos` vazia, o sistema gera automaticamente ~430 pedidos dos **últimos 90 dias**
(12 produtos em 5 categorias, 8 clientes, horários de pico no almoço e jantar, ~7% de cancelamentos
e alguns descontos). A geração usa semente fixa, então os números se repetem a cada banco novo.

- Para **desligar**: `app.seed.demo=false` no `application.properties` (ou `--app.seed.demo=false`).
- Para **recriar** os dados: apague o banco `pizzaria` (`db.dropDatabase()` no mongosh) e reinicie.

## Endpoints da API

Documentação completa, com parâmetros, exemplos e erros: **[docs/API.md](docs/API.md)**.

### Dashboard (ADMIN, GERENTE)

| Método | Endpoint | Descrição | Requisito |
|---|---|---|---|
| GET | `/api/v1/dashboard` | Painel completo em uma chamada | RF01–RF06 |
| GET | `/api/v1/dashboard/resumo` | Indicadores gerais (faturamento, ticket médio, cancelamentos…) | RF01 |
| GET | `/api/v1/dashboard/vendas` | Série de vendas por dia/semana/mês | RF02 |
| GET | `/api/v1/dashboard/categorias` | Receita por categoria de produto | RF02 |
| GET | `/api/v1/dashboard/horarios` | Pedidos por hora do dia | RF02 |
| GET | `/api/v1/dashboard/status` | Quantidade de pedidos por situação | RF03 |
| GET | `/api/v1/dashboard/produtos/top` | Ranking de produtos mais vendidos | RF04 |
| GET | `/api/v1/dashboard/filtros` | Opções para os filtros da tela | RF05 |

Filtros aceitos (todos opcionais): `dataInicio`, `dataFim` (AAAA-MM-DD), `status`, `categoria`, `clienteId`.

### Demais recursos

| Método | Endpoint | Perfis |
|---|---|---|
| GET | `/api/v1/pedidos`, `/api/v1/pedidos/{id}` | todos |
| POST | `/api/v1/pedidos` | GERENTE, ATENDENTE |
| PUT | `/api/v1/pedidos/{id}/status` | GERENTE, ATENDENTE |
| POST | `/api/v1/pedidos/{id}/desconto` | GERENTE |
| POST | `/api/v1/pedidos/{id}/cancelar` | GERENTE |
| GET | `/api/v1/auditoria` | ADMIN, GERENTE |
| GET/POST/PUT/DELETE | `/api/clientes` | GERENTE, ATENDENTE (DELETE só GERENTE) |
| GET/POST/PUT/DELETE | `/api/produtos` | leitura: todos · escrita: GERENTE |
| GET/POST/PUT/DELETE | `/api/usuarios` | ADMIN |

A API aceita sessão do navegador (login no formulário) ou **HTTP Basic**:

```bash
curl -u gerente:teste123 "http://localhost:8080/api/v1/dashboard/resumo?dataInicio=2026-11-01&dataFim=2026-11-10"
```

## Telas

| Tela | Rota | Destaques |
|---|---|---|
| Login | `/login` | layout próprio, mensagens de erro/saída |
| Dashboard | `/dashboard` | cards de KPI, 3 gráficos, ranking, situação, filtros, atualização automática, CSV, modo escuro |
| Pedidos | `/pedidos` | tabela interativa, filtro por situação e busca, modais de novo pedido / desconto / cancelamento / detalhes |
| Auditoria | `/auditoria` | indicadores, filtros, paginação, modal com antes/depois, exportação CSV |
| Clientes, Produtos, Usuários | `/clientes`, `/produtos`, `/usuarios` | CRUDs existentes, com o novo tema |

## Estrutura do projeto

```
src/main/java/com/umc/pediupatrao
├── config/        segurança, carga inicial de dados
├── controller/    REST (API) e MVC (telas) + tratamento de erros
├── dto/           objetos de entrada/saída da API
├── entity/        documentos do MongoDB e regras de domínio
├── repository/    acesso ao MongoDB (Spring Data)
└── service/       regras de negócio (DashboardService, DashboardCalculadora…)
src/main/resources
├── templates/     telas Thymeleaf
└── static/        css, js das telas e bibliotecas (vendor/)
docs/              requisitos, API, testes, roteiro da apresentação e evidências
```

## Documentação

- [Requisitos funcionais e não funcionais](docs/REQUISITOS.md)
- [Documentação dos endpoints](docs/API.md)
- [Evidências de testes](docs/TESTES.md)
- [Roteiro da apresentação (vídeo)](docs/ROTEIRO-APRESENTACAO.md)
- [Fluxo de branches (Gitflow)](docs/GITFLOW.md)
