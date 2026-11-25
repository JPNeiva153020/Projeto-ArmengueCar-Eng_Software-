CREATE TABLE planned_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES service_orders(id),
    item_type VARCHAR(20) NOT NULL CHECK (item_type IN ('PECA', 'MAO_DE_OBRA')),
    stock_item_id UUID REFERENCES stock_items(id),
    description VARCHAR(255) NOT NULL,
    quantity NUMERIC(12,2) NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CHECK ((item_type = 'PECA' AND stock_item_id IS NOT NULL)
        OR (item_type = 'MAO_DE_OBRA' AND stock_item_id IS NULL))
);
CREATE INDEX idx_planned_items_order ON planned_items(order_id);
