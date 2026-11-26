CREATE TABLE notification_outbox (
 id UUID PRIMARY KEY, recipient_id UUID NOT NULL REFERENCES users(id), order_id UUID NOT NULL REFERENCES service_orders(id),
 title VARCHAR(200) NOT NULL, message VARCHAR(1000) NOT NULL, target_view VARCHAR(30) NOT NULL,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL, processed_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX notification_outbox_pending_idx ON notification_outbox(recipient_id, processed_at, created_at);
