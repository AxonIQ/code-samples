ALTER TABLE saga_entry ADD COLUMN serialized_saga_bytea BYTEA;
UPDATE saga_entry  SET serialized_saga_bytea = lo_get(serialized_saga);
ALTER TABLE saga_entry  DROP COLUMN serialized_saga;
ALTER TABLE saga_entry  RENAME COLUMN serialized_saga_bytea to serialized_saga;