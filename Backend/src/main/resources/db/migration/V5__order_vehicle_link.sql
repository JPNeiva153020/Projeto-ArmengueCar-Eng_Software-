ALTER TABLE service_orders ADD COLUMN vehicle_id UUID REFERENCES vehicles(id);
ALTER TABLE service_orders ADD COLUMN closed_at TIMESTAMP WITH TIME ZONE;

-- Associa apenas quando placa e proprietário correspondem; o restante permanece
-- sem vínculo para revisão e os campos históricos originais são preservados.
UPDATE service_orders o
SET vehicle_id = v.id
FROM vehicles v
WHERE UPPER(TRIM(o.plate)) = UPPER(TRIM(v.plate))
  AND o.client_id = v.client_id;

CREATE INDEX idx_orders_vehicle ON service_orders(vehicle_id);
