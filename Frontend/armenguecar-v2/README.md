# ArmengueCar — Frontend acadêmico

Protótipo frontend navegável da plataforma de gestão de oficina.

## Como executar
1. Extraia o ZIP.
2. Abra `index.html` no navegador.
3. Não há backend, banco de dados ou dependências de servidor.

## Estrutura do projeto
```
armenguecar-v2/
├── index.html
├── css/
│   ├── base.css          # variáveis, reset e utilitários
│   ├── layout.css        # sidebar, topbar e grid principal
│   ├── components.css    # botões, cards, tabelas, kanban, modais...
│   ├── login.css         # tela de login
│   └── responsive.css    # media queries
└── js/
    ├── utils.js           # helpers de DOM e formatação
    ├── data.js            # dados mockados (seed)
    ├── state.js           # estado global e persistência
    ├── views.js           # renderização das telas
    ├── order-detail.js    # detalhe da OS, evidências, transições de etapa
    ├── modals.js          # modais e seus formulários
    ├── actions.js         # eventos e dispatcher de ações
    └── main.js             # login, troca de perfil, inicialização
```

## O que foi implementado visualmente
- Visão geral com KPIs e fluxo de 8 etapas.
- Kanban operacional.
- Ordens de Serviço com busca/filtro e abertura de detalhes.
- Criação simulada de OS.
- Transição de etapas simulada.
- Regras de integridade: orçamento R$ 0 bloqueado, execução condicionada, checklist/entrega.
- Estoque com ponto de reposição e alertas.
- Indicadores de produtividade, faturamento e gargalos.
- Central de notificações.
- Perfis Gerente, Mecânico, Cliente e Administrador com troca de perfil.
- Usuários/permissões e auditoria para o perfil administrador.
- Evidências/fotos simuladas no detalhe da OS.
- Responsividade para desktop, tablet e celular.

## Observação
Os dados são mockados e ficam apenas em memória durante a sessão. A estrutura foi pensada para que um backend/Java possa substituir essas simulações posteriormente.
