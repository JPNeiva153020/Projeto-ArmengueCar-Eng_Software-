ALTER TABLE service_orders ADD COLUMN closed_by UUID REFERENCES users(id);
ALTER TABLE stock_movements ADD COLUMN order_id UUID REFERENCES service_orders(id);
ALTER TABLE part_reservations ADD COLUMN used_quantity NUMERIC(12,2);
ALTER TABLE part_reservations ADD COLUMN released_quantity NUMERIC(12,2);
