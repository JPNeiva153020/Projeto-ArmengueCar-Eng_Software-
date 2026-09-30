# ArmengueCar — Backend

API REST do sistema ArmengueCar. **Este repositório contém somente o backend**; o frontend permanece em outro projeto e pode ser desenvolvido no VS Code.

## Stack

- Java 17
- Spring Boot 3.5.5
- Spring Web
- Spring Validation
- Spring Data JPA / Hibernate
- Spring Security
- JWT (JJWT)
- H2 para desenvolvimento local
- PostgreSQL para o ambiente com banco real
- Flyway para migrações PostgreSQL
- Lombok

## Estrutura

```text
src/main/java/br/com/armenguecar/
├── config/       # Configuração e dados de demonstração
├── controller/   # Endpoints HTTP
├── dto/          # Objetos de entrada/saída da API
├── entity/       # Entidades JPA
├── enums/        # Enumerações do domínio
├── exception/    # Tratamento centralizado de erros
├── repository/   # Acesso aos dados
├── security/     # JWT e filtro de autenticação
└── service/      # Regras de negócio
```

Fluxo principal:

```text
Frontend → Controller → Service → Repository → Banco
```

Os controllers ficam responsáveis por HTTP, validação e autorização. As regras de negócio ficam nos services. Os repositories cuidam da persistência.

## Rodar no IntelliJ

1. Abra **somente esta pasta** no IntelliJ IDEA.
2. Aguarde o Maven baixar as dependências.
3. Execute `ArmengueCarApplication`.
4. A API ficará disponível em `http://localhost:8080`.
5. Com o perfil `dev`, o H2 será criado automaticamente em `./data/armenguecar`.

Não é necessário instalar ou criar PostgreSQL para começar a desenvolver.

## Usuários de demonstração

Todos usam a senha `1234` no perfil `dev`:

| E-mail | Perfil |
|---|---|
| cristina@armenguecar.local | GERENTE |
| marcos@armenguecar.local | MECANICO |
| eduardo@armenguecar.local | CLIENTE |
| paula@armenguecar.local | ADMIN |

Login:

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "cristina@armenguecar.local",
  "password": "1234"
}
```

Depois, envie o token retornado no header:

```http
Authorization: Bearer SEU_TOKEN
```

## Endpoints

### Autenticação

- `POST /api/auth/login`
- `GET /api/me`

### Dashboard

- `GET /api/dashboard`
- `GET /api/health`

### Ordens de serviço

- `GET /api/orders`
- `GET /api/orders/{id}`
- `POST /api/orders`
- `PATCH /api/orders/{id}/advance`
- `PATCH /api/orders/{id}/retreat`
- `PATCH /api/orders/{id}/budget`
- `PATCH /api/orders/{id}/decision`
- `PATCH /api/orders/{id}/checklist`
- `POST /api/orders/{id}/deliver`
- `POST /api/orders/{id}/photos`

### Clientes

- `GET /api/clients`
- `POST /api/clients`

### Estoque

- `GET /api/stock`
- `POST /api/stock/movements`
- `PUT /api/stock/{id}`

### Notificações

- `GET /api/notifications`
- `PATCH /api/notifications/{id}/read`
- `POST /api/notifications/read-all`

### Administração

- `GET /api/users` — ADMIN
- `PATCH /api/users/{id}/toggle` — ADMIN
- `GET /api/audit` — ADMIN

## PostgreSQL depois

Quando o banco real for configurado, use o profile `postgres`:

```text
SPRING_PROFILES_ACTIVE=postgres
DB_URL=jdbc:postgresql://localhost:5432/armenguecar
DB_USER=postgres
DB_PASSWORD=sua_senha
```

Nesse profile o JPA usa `validate`, portanto a aplicação não modifica a estrutura do banco. O Flyway executa `V1__initial_schema.sql`.

## Importante

O arquivo `application.yml` contém uma chave JWT de desenvolvimento. Antes de publicar o sistema, substitua essa chave por uma variável de ambiente forte e mantenha credenciais reais fora do código-fonte.
