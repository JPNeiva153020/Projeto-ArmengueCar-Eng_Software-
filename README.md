# ArmengueCar

Sistema acadêmico de gestão de oficina, com frontend em HTML, CSS e JavaScript e API Spring Boot ligada ao PostgreSQL.

## Estrutura

- `Backend/`: API, migrações Flyway e testes.
- `Frontend/`: interface web e estilos. `js/api-app.js` é a interface integrada; os demais scripts são do protótipo anterior e não são carregados por `index.html`.

## Preparar o PostgreSQL

Crie um banco vazio chamado `armenguecar`, com usuário autorizado a criar tabelas. A API usa PostgreSQL por padrão e o Flyway cria a estrutura na primeira inicialização. Por padrão a conexão é `jdbc:postgresql://localhost:5432/armenguecar` com usuário `postgres`. Ajuste `DB_URL` e `DB_USER` se necessário. Informe a senha via variável de ambiente `DB_PASSWORD`; não a grave no repositório.

## Executar a API

Instale Java 17 e Maven. Em PowerShell, dentro de `Backend/`:

```powershell
$env:DB_PASSWORD = Read-Host "Senha do PostgreSQL" -MaskInput
mvn spring-boot:run
```

O backend atende em `http://localhost:8080`. Para inserir usuários e dados **de demonstração** em um banco sem usuários, ative o perfil `demo` junto com `postgres`:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=postgres,demo"
```

O perfil `dev` usa H2 e dados de demonstração, útil para testes. Para executar os testes: `mvn test`.

## Executar a interface

Com a API em execução, sirva a pasta `Frontend/` em um servidor HTTP local. Por exemplo, com Python instalado:

```powershell
cd Frontend
python -m http.server 5500 --bind 127.0.0.1
```

Abra `http://127.0.0.1:5500`. A interface chama a API em `http://localhost:8080/api`. Não abra `index.html` diretamente como arquivo: a configuração CORS espera uma origem HTTP local.

No perfil `demo`, as contas de demonstração incluem `cristina@armenguecar.local` (gerente), `marcos@armenguecar.local` (mecânico) e `paula@armenguecar.local` (administradora), todas com senha `1234`. São exclusivamente para desenvolvimento.

## Estado da integração

Login, painel, Kanban, listagem e cadastro de clientes e ordens, orçamento, decisão, avanço e retorno de etapas, checklist, consulta e movimentação de estoque, notificações e ativação de usuários usam a API. Os dados exibidos não são mais os dados locais simulados do protótipo.

O portal inicial permite que uma conta de cliente consulte suas próprias ordens após vínculo explícito feito pela administradora em Clientes. Orçamentos, decisão do cliente e fotos ainda não estão disponíveis no portal. A API continua restringindo os endpoints operacionais aos perfis da equipe. Fotos reais, reserva e baixa automática de peças, registro definitivo de entrega, permissões finas, encerramento completo, orçamento por itens e versões, indicadores avançados e histórico auditável completo ainda precisam de implementação. O cadastro independente de veículos já está disponível. A administradora pode criar, editar, ativar, desativar e redefinir senhas de contas; mudanças de credenciais ou perfil invalidam sessões existentes. A interface anterior e suas simulações não são carregadas.

O projeto não está pronto para produção. Revise a autenticação, os perfis, as regras de negócio e os segredos antes de usá-lo com dados reais. Configure `JWT_SECRET` com uma chave forte; a chave padrão do projeto é apenas de desenvolvimento.

O plano rastreável por requisito está em [docs/BACKLOG.md](docs/BACKLOG.md).

Novas ordens exigem selecionar um veículo cadastrado. A migração preserva os dados de ordens anteriores; vínculos que não puderem ser identificados permanecem pendentes para revisão.

A OS em diagnóstico permite registrar avarias, sintomas e observações; concluir esses campos é necessário antes de informar o orçamento. Peças cadastradas e mão de obra podem ser incluídas como itens previstos, sem reservar nem baixar estoque. A integração desses itens ao orçamento ainda está pendente.
