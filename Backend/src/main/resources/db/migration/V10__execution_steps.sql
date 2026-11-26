CREATE TABLE execution_steps (
 id UUID PRIMARY KEY, order_id UUID NOT NULL REFERENCES service_orders(id),
 kind VARCHAR(40) NOT NULL, status VARCHAR(20) NOT NULL,
 started_at TIMESTAMP WITH TIME ZONE NOT NULL, active_since TIMESTAMP WITH TIME ZONE,
 ended_at TIMESTAMP WITH TIME ZONE, active_millis BIGINT NOT NULL DEFAULT 0,
 started_by UUID NOT NULL REFERENCES users(id), finished_by UUID REFERENCES users(id),
 CONSTRAINT execution_step_status CHECK (status IN ('EM_ANDAMENTO','PAUSADA','CONCLUIDA')),
 CONSTRAINT execution_step_time CHECK (active_millis >= 0)
);
CREATE INDEX execution_steps_order_idx ON execution_steps(order_id);
