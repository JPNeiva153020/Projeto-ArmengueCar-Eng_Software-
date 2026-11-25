ALTER TABLE stock_items ADD COLUMN reserved_quantity DOUBLE PRECISION NOT NULL DEFAULT 0;
ALTER TABLE service_orders ADD COLUMN closed_reason VARCHAR(30);
CREATE TABLE budgets (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES service_orders(id),
    version INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    total NUMERIC(14,2) NOT NULL,
    published_by UUID NOT NULL REFERENCES users(id),
    published_at TIMESTAMP WITH TIME ZONE NOT NULL,
    decided_by UUID REFERENCES users(id),
    decided_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(order_id, version)
);
CREATE TABLE budget_lines (
    id UUID PRIMARY KEY,
    budget_id UUID NOT NULL REFERENCES budgets(id),
    item_type VARCHAR(20) NOT NULL,
    stock_item_id UUID REFERENCES stock_items(id),
    description VARCHAR(255) NOT NULL,
    quantity NUMERIC(12,2) NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL
);
CREATE TABLE part_reservations (
    id UUID PRIMARY KEY,
    budget_id UUID NOT NULL REFERENCES budgets(id),
    stock_item_id UUID NOT NULL REFERENCES stock_items(id),
    quantity NUMERIC(12,2) NOT NULL CHECK (quantity > 0),
    UNIQUE(budget_id, stock_item_id)
);
CREATE INDEX idx_budgets_order ON budgets(order_id);
