CREATE TABLE IF NOT EXISTS taglie (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    codice VARCHAR(40) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_taglie_codice ON taglie (codice);
CREATE INDEX IF NOT EXISTS ix_taglie_nome ON taglie (nome);

CREATE TABLE IF NOT EXISTS product_taglie (
    product_id BIGINT NOT NULL,
    taglia_id BIGINT NOT NULL,
    PRIMARY KEY (product_id, taglia_id)
);

CREATE INDEX IF NOT EXISTS ix_product_taglie_product_id ON product_taglie (product_id);
CREATE INDEX IF NOT EXISTS ix_product_taglie_taglia_id ON product_taglie (taglia_id);

ALTER TABLE product_taglie
    ADD CONSTRAINT fk_product_taglie_product
        FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE CASCADE;

ALTER TABLE product_taglie
    ADD CONSTRAINT fk_product_taglie_taglia
        FOREIGN KEY (taglia_id) REFERENCES taglie (id) ON DELETE RESTRICT;

INSERT INTO taglie (nome, codice)
VALUES
    ('Extra Extra Small', 'XXS'),
    ('Extra Small', 'XS'),
    ('Small', 'S'),
    ('Medium', 'M'),
    ('Large', 'L'),
    ('Extra Large', 'XL'),
    ('Extra Extra Large', 'XXL'),
    ('3 Extra Large', 'XXXL'),
    ('4 Extra Large', '4XL'),
    ('5 Extra Large', '5XL'),
    ('Kids Extra Small', 'K-XS'),
    ('Kids Small', 'K-S'),
    ('Kids Medium', 'K-M'),
    ('Kids Large', 'K-L'),
    ('One Size', 'OS'),
    ('38', '38'),
    ('39', '39'),
    ('40', '40'),
    ('41', '41'),
    ('42', '42'),
    ('43', '43'),
    ('44', '44'),
    ('45', '45'),
    ('46', '46'),
    ('47', '47'),
    ('48', '48')
ON CONFLICT (codice) DO NOTHING;
