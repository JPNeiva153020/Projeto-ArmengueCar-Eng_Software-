# ArmengueCar

Projeto acadêmico de gestão de oficina, com uma API Java e um protótipo de interface.

> Estado atual: o frontend usa dados de demonstração no navegador e ainda não está conectado à API. Não use o projeto como sistema de produção.

## Estrutura

```text
Backend/     API Spring Boot e testes
Frontend/    interface estática, estilos e scripts
```

## Backend

Requisitos: Java 17 e Maven 3.8 ou superior. Na pasta `Backend`:

```powershell
mvn test
mvn spring-boot:run
```

O perfil padrão `dev` usa H2 em arquivo, em `Backend/data/`, e popula dados de demonstração. A API usa a porta 8080; `GET /api/health` é público. O login é feito em `POST /api/auth/login` com JSON contendo `email` e `password`; o token retornado é enviado como `Authorization: Bearer <token>`.

No perfil de desenvolvimento, os usuários demonstrativos têm senha `1234`:

| Perfil | E-mail |
| --- | --- |
| Gerente | cristina@armenguecar.local |
| Mecânico | marcos@armenguecar.local |
| Cliente | eduardo@armenguecar.local |
| Administrador | paula@armenguecar.local |

O perfil `postgres` utiliza `DB_URL`, `DB_USER` e `DB_PASSWORD` e executa migrações Flyway. Suas configurações e a segurança devem ser revisadas antes de qualquer uso real. A chave JWT e as credenciais padrão existentes são apenas de desenvolvimento.

## Frontend

Abra `Frontend/index.html` em um navegador moderno. Consulte [Frontend/README.md](Frontend/README.md). Login, troca de perfil e dados são simulados; alterações ficam no armazenamento local do navegador, não no banco da API.

## Próximos passos

- Revisar regras de negócio, autorização por perfil e acesso aos dados de cada cliente.
- Integrar autenticação e operações do frontend à API por etapas, com tratamento de erros.
- Implementar e testar o armazenamento real de fotos e as regras de estoque e entrega.
- Ampliar testes de integração e documentar contratos e implantação.
