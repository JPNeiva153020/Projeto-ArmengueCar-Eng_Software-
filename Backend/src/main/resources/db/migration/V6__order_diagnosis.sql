CREATE TABLE order_diagnoses (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL UNIQUE REFERENCES service_orders(id),
    damages VARCHAR(2000) NOT NULL,
    symptoms VARCHAR(2000) NOT NULL,
    observations VARCHAR(2000) NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    updated_by UUID REFERENCES users(id),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
