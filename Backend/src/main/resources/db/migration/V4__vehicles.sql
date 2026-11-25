CREATE TABLE vehicles (
    id UUID PRIMARY KEY,
    client_id UUID NOT NULL REFERENCES clients(id),
    plate VARCHAR(255) NOT NULL UNIQUE,
    model VARCHAR(255) NOT NULL,
    brand VARCHAR(255),
    model_year INTEGER
);

-- Apenas placas com um único proprietário são migradas automaticamente.
-- Casos ambíguos permanecem nos dados originais para revisão manual.
INSERT INTO vehicles (id, client_id, plate, model)
SELECT gen_random_uuid(), c.id, UPPER(TRIM(c.plate)), COALESCE(NULLIF(TRIM(c.vehicle), ''), 'Não informado')
FROM clients c
WHERE c.plate IS NOT NULL AND TRIM(c.plate) <> ''
  AND (SELECT COUNT(*) FROM clients other
       WHERE UPPER(TRIM(other.plate)) = UPPER(TRIM(c.plate))) = 1;
