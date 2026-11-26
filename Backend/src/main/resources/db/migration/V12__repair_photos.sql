CREATE TABLE repair_photos (
 id UUID PRIMARY KEY, order_id UUID NOT NULL REFERENCES service_orders(id),
 stage VARCHAR(40) NOT NULL, content_type VARCHAR(20) NOT NULL, content BYTEA NOT NULL,
 visible_to_client BOOLEAN NOT NULL DEFAULT FALSE, uploaded_by UUID NOT NULL REFERENCES users(id),
 created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX repair_photos_order_idx ON repair_photos(order_id);
