# TaskFlow — Sistema de Gestão de Férias

Aplicação full-stack para gerir colaboradores e pedidos de férias, com a regra de que **dois colaboradores nunca podem estar de férias no mesmo dia**.

| Camada    | Tecnologia |
|-----------|------------|
| Backend   | Java 21, Spring Boot 3.5 (Web, Data JPA, Validation, Security), JWT (jjwt), Flyway |
| Base de dados | PostgreSQL 16 |
| Frontend  | React 19 + TypeScript + Vite |
| Documentação da API | Swagger / OpenAPI (springdoc) |
| Testes    | JUnit 5, Mockito, Testcontainers |
| Execução  | Docker Compose |

> Para perceber as decisões e o código em detalhe, ver **[docs/GUIA.md](docs/GUIA.md)**.

---

## 1. Executar com Docker (recomendado)

Pré-requisito: Docker Desktop (ou Docker Engine + Compose).

```bash
docker compose up --build
```

| O quê | URL |
|-------|-----|
| Aplicação (frontend) | http://localhost:3000 |
| API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui.html |

Para parar: `Ctrl+C` e depois `docker compose down` (acrescentar `-v` para apagar também os dados).

### Contas de demonstração

Criadas automaticamente no primeiro arranque. **Password de todas: `password123`**

| Email | Role | Equipa |
|-------|------|--------|
| admin@taskflow.com | ADMIN | — |
| marco@taskflow.com | MANAGER | João, Maria |
| sofia@taskflow.com | MANAGER | Pedro, Rita |
| joao@taskflow.com | COLLABORATOR | manager: Marco |
| maria@taskflow.com | COLLABORATOR | manager: Marco |
| pedro@taskflow.com | COLLABORATOR | manager: Sofia |
| rita@taskflow.com | COLLABORATOR | manager: Sofia |

No ecrã de login basta clicar numa conta para preencher os campos.

---

## 2. Executar no browser com GitHub Codespaces (para quem avalia)

Sem instalar nada: o repositório traz a configuração `.devcontainer/`, que arranca **base de dados, backend e frontend** automaticamente.

[![Abrir no GitHub Codespaces](https://github.com/codespaces/badge.svg)](https://codespaces.new/tmbthiagobarbosa/gestao-ferias)

1. Clicar no botão acima (ou, no GitHub: **Code → Codespaces → Create codespace on main**).
2. Esperar o primeiro arranque (5–8 minutos: cria o ambiente, descarrega imagens e compila o backend).
   Para acompanhar, no terminal: `docker compose logs -f backend` (pronto quando aparecer *Started VacationsApplication*).
3. No separador **Ports** do editor:
   - **3000** → aplicação (clicar no ícone do globo);
   - **8080** → acrescentar `/swagger-ui.html` ao URL para o Swagger.
4. Entrar com `admin@taskflow.com` / `password123` (restantes contas na tabela acima).

> As contas pessoais do GitHub têm horas gratuitas de Codespaces por mês, mais do que suficientes para avaliar. Alternativa sem Codespaces: clonar o repositório e correr `docker compose up --build` (secção 1).

---

## 3. Executar localmente (sem Docker para o backend/frontend)

Pré-requisitos: **JDK 21**, **Maven 3.9+** (o IntelliJ já traz Maven), **Node 20+**, e um PostgreSQL.

### 3.1 Base de dados

A forma mais simples é subir só o Postgres do compose:

```bash
docker compose up db
```

(Ou criar manualmente uma base `ferias` com utilizador/password `ferias`/`ferias` na porta 5432.)

### 3.2 Backend (IntelliJ IDEA)

1. `File → Open…` e escolher a pasta `backend` (o IntelliJ deteta o `pom.xml`).
2. Confirmar que o **Project SDK é Java 21** (`File → Project Structure → Project`).
3. Correr a classe `VacationsApplication` (botão ▶ ao lado do `main`).

Ou pela linha de comandos:

```bash
cd backend
mvn spring-boot:run
```

O Flyway cria as tabelas no arranque e o `DataSeeder` insere os dados de exemplo.

Variáveis de ambiente opcionais (valores por omissão em `application.yml`):

| Variável | Omissão |
|----------|---------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/ferias` |
| `DB_USER` / `DB_PASSWORD` | `ferias` / `ferias` |
| `JWT_SECRET` | valor de desenvolvimento (mín. 32 caracteres) |
| `JWT_EXPIRATION_MINUTES` | `480` |
| `SEED_DATA` | `true` |

### 3.3 Frontend

```bash
cd frontend
npm install
npm run dev
```

Abrir http://localhost:5173. O Vite reencaminha `/api` para `http://localhost:8080`.

### 3.4 Testes

```bash
cd backend
mvn test
```

- `VacationRequestServiceTest` e `UserServiceTest`: testes unitários das regras de negócio (não precisam de BD).
- `VacationApiIntegrationTest`: teste ponta a ponta com PostgreSQL real via **Testcontainers** (precisa de Docker a correr; é ignorado automaticamente se não houver Docker).

---

## 4. API

Documentação interativa completa no **Swagger** (`/swagger-ui.html`). Para testar lá:
1. `POST /api/auth/login` com `{"email":"admin@taskflow.com","password":"password123"}`;
2. copiar o `token` da resposta;
3. clicar em **Authorize** e colar o token.

### Endpoints

| Método | Endpoint | Quem | Descrição |
|--------|----------|------|-----------|
| POST | `/api/auth/login` | público | Login, devolve JWT |
| GET | `/api/auth/me` | autenticado | Utilizador atual |
| GET | `/api/users` | ADMIN | Listar (filtros `search`, `role`, `managerId`; `page`, `size`, `sort`) |
| GET | `/api/users/{id}` | ADMIN | Detalhe |
| POST | `/api/users` | ADMIN | Criar |
| PUT | `/api/users/{id}` | ADMIN | Editar |
| DELETE | `/api/users/{id}` | ADMIN | Remover |
| GET | `/api/vacations` | autenticado | Listar pedidos visíveis (filtros `status`, `userId`, `employeeName`, `from`, `to`; paginação) |
| GET | `/api/vacations/calendar?from=&to=` | autenticado | Dias ocupados para o calendário |
| GET | `/api/vacations/{id}` | dono / manager / ADMIN | Detalhe |
| POST | `/api/vacations` | autenticado | Criar pedido (fica `PENDENTE`) |
| PUT | `/api/vacations/{id}` | dono / manager / ADMIN | Editar pedido `PENDENTE` |
| DELETE | `/api/vacations/{id}` | dono / manager / ADMIN | Cancelar pedido |
| PATCH | `/api/vacations/{id}/approve` | manager direto / ADMIN | Aprovar |
| PATCH | `/api/vacations/{id}/reject` | manager direto / ADMIN | Rejeitar (`{"reason": "..."}` opcional) |

### Formato de erro (igual em toda a API)

```json
{
  "timestamp": "2026-09-26T10:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "O período de 05/08/2026 a 10/08/2026 sobrepõe-se a férias já marcadas por outro colaborador (01/08/2026 a 05/08/2026, APROVADO)",
  "path": "/api/vacations"
}
```

| Status | Quando |
|--------|--------|
| 400 | Corpo inválido / campos em falta (`fieldErrors` com o detalhe por campo) |
| 401 | Sem token, token inválido/expirado, ou login errado |
| 403 | Sem permissão (ex.: MANAGER a criar utilizadores) |
| 404 | Recurso não existe |
| 409 | Conflito: férias sobrepostas, email duplicado, pedido já decidido |
| 422 | Regra de negócio: fim antes do início, datas no passado, colaborador sem manager |

---

## 5. Estrutura do projeto

```text
gestao-ferias/
├── docker-compose.yml
├── .devcontainer/               ← GitHub Codespaces / Dev Containers (arranca tudo)
├── README.md
├── docs/GUIA.md                  ← explicação detalhada das decisões e do código
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/taskflow/vacations/
│       ├── controller/           ← endpoints REST (recebem/devolvem JSON)
│       ├── service/              ← regras de negócio e permissões
│       ├── model/                ← entidades JPA (User, VacationRequest) e enums
│       ├── repository/           ← acesso a dados + filtros dinâmicos
│       ├── dto/                  ← objetos de entrada/saída da API
│       ├── security/             ← JWT (geração, validação, filtro)
│       ├── exception/            ← exceções e handler global de erros
│       └── config/               ← segurança, Swagger, dados de exemplo
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/         ← scripts Flyway (esquema da BD)
└── frontend/
    ├── Dockerfile, nginx.conf
    └── src/
        ├── api.ts                ← cliente HTTP
        ├── auth.tsx              ← sessão (login/logout)
        ├── pages/                ← Login, Pedidos, Calendário, Colaboradores
        └── components/           ← Modal, Paginação, Badges, Alertas
```

---

## 6. Checklist

Obrigatórios: gestão de colaboradores (CRUD) · manager associado · gestão de utilizadores só para ADMIN · CRUD de pedidos · validação de sobreposição · datas inclusivas · estados PENDENTE/APROVADO/REJEITADO · aprovação por manager responsável ou ADMIN · visibilidade por role · validações e erros · JSON consistente · Swagger · README · docker-compose.

Bónus: login com JWT · paginação · filtros · calendário.
