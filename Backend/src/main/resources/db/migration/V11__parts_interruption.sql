ALTER TABLE service_orders ADD COLUMN interrupted_stage VARCHAR(40);
ALTER TABLE service_orders ADD COLUMN parts_block_reason VARCHAR(1000);
