CREATE TABLE migration_smoke_test (
    id INTEGER PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
