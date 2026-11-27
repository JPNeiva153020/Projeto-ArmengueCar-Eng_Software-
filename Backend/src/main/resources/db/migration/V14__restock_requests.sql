CREATE TABLE restock_requests (
 id UUID PRIMARY KEY, item_id UUID NOT NULL REFERENCES stock_items(id), quantity NUMERIC(12,2) NOT NULL CHECK(quantity>0),
 status VARCHAR(20) NOT NULL CHECK(status IN ('ABERTA','RECEBIDA','CANCELADA')), note VARCHAR(1000),
 created_by UUID NOT NULL REFERENCES users(id), created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 resolved_by UUID REFERENCES users(id), resolved_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX restock_requests_created_idx ON restock_requests(created_at);
