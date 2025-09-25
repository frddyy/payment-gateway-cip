-- V1__Create_transactions_table.sql

-- Fungsi ini akan otomatis memperbarui kolom 'updated_at' setiap kali ada perubahan baris
CREATE OR REPLACE FUNCTION trigger_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Perintah untuk membuat tabel 'transactions'
CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    order_id VARCHAR(255) NOT NULL UNIQUE,
    channel VARCHAR(50) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    account VARCHAR(255) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'IDR',
    payment_method VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    corebank_reference VARCHAR(255),
    biller_reference VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Menerapkan fungsi trigger di atas ke tabel 'transactions'
CREATE TRIGGER set_timestamp
BEFORE UPDATE ON transactions
FOR EACH ROW
EXECUTE FUNCTION trigger_set_timestamp();

-- Menambahkan indeks pada 'order_id' untuk mempercepat pencarian
CREATE INDEX idx_transactions_order_id ON transactions(order_id);