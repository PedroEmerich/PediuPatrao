# Roteiro do vídeo (até 3 minutos)

Dica: deixe a aplicação rodando, logada como **gerente**, com o dashboard aberto em 30 dias.
Grave a tela com o OBS ou com a gravação do Teams/PowerPoint.

| Tempo | O que mostrar | O que falar (sugestão — adapte com suas palavras) |
|---|---|---|
| 0:00–0:20 | Tela de login → entrar como gerente | "Este é o Dashboard Administrativo do Pediu Patrão. O objetivo é o dono da pizzaria acompanhar as vendas e a operação e tomar decisões. Só gerente e admin acessam." |
| 0:20–0:50 | Cards do topo | "Aqui estão os indicadores gerais: faturamento com a comparação com o período anterior, pedidos, ticket médio, itens vendidos e a taxa de cancelamento. Uma decisão importante: pedido cancelado não entra no faturamento, só na taxa." |
| 0:50–1:20 | Gráfico de faturamento; trocar R$ → Pedidos; Dia → Semana; botão de tabela | "A evolução das vendas pode ser vista por dia, semana ou mês, em reais ou em quantidade. Não usei dois eixos no mesmo gráfico para não confundir a leitura — por isso o botão alterna a métrica. Também tem a versão em tabela." |
| 1:20–1:45 | Situação dos pedidos, ranking (trocar para Faturamento), categorias, horários de pico | "Ao lado, a situação atual dos pedidos. Embaixo, os produtos mais vendidos, a receita por categoria e o horário de pico, que ajuda a planejar a equipe — aqui o pico é às 20h." |
| 1:45–2:10 | Clicar em 7 dias, escolher categoria Pizza, mostrar a URL | "Os filtros de período, situação, categoria e cliente atualizam tudo. Eles ficam na URL, então dá para compartilhar a visão. O painel também se atualiza sozinho a cada minuto." |
| 2:10–2:35 | Abrir `docs/API.md` ou rodar um curl / abrir `/api/v1/dashboard/resumo` no navegador | "Tudo vem de uma API REST que eu projetei: um endpoint por requisito e um consolidado, com filtros comuns e erros padronizados — por exemplo, data inválida retorna 400 e atendente recebe 403." |
| 2:35–3:00 | Tela de Pedidos (modal de desconto) → Auditoria; terminal com `mvnw test` verde | "Melhorei também as telas de pedidos e auditoria com modais. As regras de cálculo ficam numa classe separada, testada sem banco, e tenho testes de integração com o MongoDB: 37 testes passando. Obrigada!" |

## Principais decisões de análise e projeto (para justificar)

1. **Endpoint consolidado + endpoints por requisito** — a tela faz uma chamada só (desempenho), mas cada requisito tem seu endpoint (reuso/testes).
2. **Cálculo em Java numa classe sem banco (`DashboardCalculadora`)** — regras testáveis e fáceis de explicar; volume de uma pizzaria é pequeno.
3. **Cancelados fora do faturamento; desconto proporcional por item** — números que batem com o caixa.
4. **Comparação com o período anterior** — mostra tendência, não só o valor.
5. **Sem gráfico de dois eixos; status com ícone + texto; paleta validada para daltonismo** — leitura correta e acessível.
6. **Filtros na URL e atualização automática** — visão compartilhável e sempre atual.
7. **Bibliotecas locais** — funciona sem internet.
8. **Segurança por perfil** — atendente não vê números financeiros; API responde 401/403 em JSON.
