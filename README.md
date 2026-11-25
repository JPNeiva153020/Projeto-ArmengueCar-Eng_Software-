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

A interface usa a API para contas, clientes, veículos, ordens, diagnóstico, itens previstos, Kanban e estoque. O portal permite ao cliente vinculado consultar suas ordens e aprovar, recusar ou solicitar alterações em versões de orçamento. A aprovação reserva as peças disponíveis; após reposição, a equipe pode reavaliar a reserva. A entrega exige aprovação e checklist, registra o uso de peças e libera a reserva restante.

As ordens antigas de demonstração foram preservadas. Os indicadores históricos de aprovação ou reserva não criam um orçamento versionado nem reservas reais: revise essas ordens antes de prosseguir com elas. A integração externa com fornecedores não é necessária para a reposição manual.

Ainda faltam funcionalidades dos requisitos, incluindo acompanhamento detalhado da execução, fotos reais, geração de notificações, histórico e indicadores completos, tratamento de retrabalho e revisão das permissões. Consulte [docs/BACKLOG.md](docs/BACKLOG.md) para o estado por etapa.

O projeto não está pronto para produção. Revise a autenticação, os perfis, as regras de negócio e os segredos antes de usá-lo com dados reais. Configure `JWT_SECRET` com uma chave forte; a chave padrão do projeto é apenas de desenvolvimento.
