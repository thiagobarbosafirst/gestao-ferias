-- Tabela de utilizadores/colaboradores.
-- Todos os utilizadores (ADMIN, MANAGER, COLLABORATOR) vivem na mesma tabela; o papel está na coluna "role".
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL,          -- hash BCrypt, nunca a password em texto
    role        VARCHAR(20)  NOT NULL,
    manager_id  BIGINT REFERENCES users (id),   -- manager responsável (obrigatório para COLLABORATOR, validado no serviço)
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'MANAGER', 'COLLABORATOR'))
);

-- Pedidos de férias.
CREATE TABLE vacation_requests (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    start_date       DATE         NOT NULL,
    end_date         DATE         NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    notes            VARCHAR(500),
    rejection_reason VARCHAR(500),
    decided_by_id    BIGINT REFERENCES users (id) ON DELETE SET NULL,
    decided_at       TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_vacation_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_vacation_status CHECK (status IN ('PENDENTE', 'APROVADO', 'REJEITADO'))
);

CREATE INDEX idx_vacation_requests_user ON vacation_requests (user_id);
CREATE INDEX idx_vacation_requests_dates ON vacation_requests (start_date, end_date);

-- RB-001 (não sobreposição) garantida TAMBÉM pela base de dados, como rede de segurança.
-- O serviço já valida antes de gravar, mas se dois pedidos chegarem ao mesmo tempo
-- ambos podiam passar na validação; esta constraint impede que os dois sejam gravados.
-- daterange(..., '[]') = intervalo com as duas datas INCLUSIVAS (RB-002); && = "sobrepõe-se".
-- Pedidos REJEITADOS não ocupam dias, por isso ficam de fora (cláusula WHERE).
ALTER TABLE vacation_requests
    ADD CONSTRAINT no_overlapping_vacations
    EXCLUDE USING gist (daterange(start_date, end_date, '[]') WITH &&)
    WHERE (status IN ('PENDENTE', 'APROVADO'));
