# ArmengueCar — plano de implementação

Referência principal: relatório de requisitos revisado em 07/10/2026 (RF01–RF30 e RNF01–RNF15) e especificação UC001–UC031. O backlog de 30/09 é histórico e usa uma numeração incompleta.

Estado inicial verificado em 25/11/2026, commit c8bbe23: autenticação, consulta/cadastro básicos de clientes e ordens, orçamento simplificado, checklist, estoque e visualização de notificações estão conectados à API. Não equivalem à conclusão dos respectivos requisitos.

| Etapa | Escopo | Requisitos | Situação |
| --- | --- | --- | --- |
| 1 | Backlog, documentação e testes por fluxo | Transversal | Em andamento; plano rastreado e testes básicos criados |
| 2 | Contas, permissões, vínculo cliente/usuário e veículos | RF01–RF04, RF30 | Em andamento: administração de contas, vínculo e cadastro de veículos implementados; matriz completa de permissões e vínculo obrigatório do veículo à OS pendentes |
| 3 | OS, histórico, cadastro de veículo e diagnóstico | RF05–RF08 | Em andamento: novas ordens usam veículo cadastrado e registros anteriores foram vinculados quando possível; diagnóstico básico e itens previstos de peças e mão de obra implementados; histórico completo pendente |
| 4 | Orçamento por itens e versões, decisão do cliente | RF09–RF13 | Pendente |
| 5 | Reserva por OS, indisponibilidade e reposição | RF14–RF17, RF24–RF25 | Pendente |
| 6 | Execução, checklist, entrega e baixa idempotente | RF19–RF23 | Pendente |
| 7 | Kanban, portal, fotos e notificações internas | RF18, RF26 | Em andamento: Kanban conectado e consulta inicial de ordens do cliente; decisões, fotos e geração de notificações pendentes |
| 8 | Indicadores, auditoria, qualidade e recuperação | RF27–RF29, RNF01–RNF15 | Pendente |

Critério de conclusão por etapa: requisito rastreável, regras de negócio aplicadas na API, migração de dados existente quando necessária, testes de sucesso e falha e validação da interface. Atualizar esta tabela somente com base em evidências.

Restrição de escopo da revisão: integração externa de mensagens não está confirmada, e pagamento online, aplicativo nativo, exportação na primeira versão e fornecedor externo estão fora do escopo inicial. A consulta de produtividade detalhada é desejável.

25/11/2026: administração de contas, revogação de sessões e vínculo explícito entre cliente e conta implementados; portal inicial apenas consulta as ordens próprias. Migrações V2 e V3 validadas em PostgreSQL; fotos, decisões do cliente e entidade de veículo ainda pendentes.

25/11/2026: cadastro independente de veículos criado e registros inequívocos migrados em V4. A criação de OS ainda usa placa e modelo textuais; a vinculação da OS ao cadastro de veículo pertence à etapa 3.

25/11/2026: migração V5 vinculou as seis ordens existentes aos veículos. Duas placas já tinham mais de uma ordem não encerrada nos dados de demonstração; foram preservadas para revisão. Novas ordens verificam ordens não encerradas, inclusive prontas para entrega.

25/11/2026: diagnóstico básico registra avarias, sintomas, observações e conclusão; elaboração de orçamento exige diagnóstico concluído. Os itens previstos, histórico completo e versionamento de orçamento continuam pendentes.

25/11/2026: quadro Kanban conectado às ordens não encerradas, com oito etapas e filtro textual por ordem, veículo ou responsável; transições continuam pelas ações existentes na ordem.

25/11/2026: backup PostgreSQL restaurado em banco temporário; contagens de clientes, veículos, ordens e versão Flyway conferidas. A integridade completa dos dados e a rotina periódica de backup ainda exigem validação.

25/11/2026: itens previstos de peças e mão de obra podem ser cadastrados e removidos na etapa de diagnóstico, sem alterar estoque. Ainda não compõem o orçamento nem geram reservas. Migração V7 aplicada após backup; testes de estoque e permissões aprovados.
