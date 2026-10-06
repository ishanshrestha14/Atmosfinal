-- Prices were whole numbers, so cents were silently truncated (19.99 became 19).
ALTER TABLE product ALTER COLUMN price TYPE NUMERIC(10, 2);
ALTER TABLE product ADD CONSTRAINT ck_product_price_non_negative CHECK (price >= 0);

-- Order lines record the price actually paid, so repricing a product never rewrites past orders.
-- Existing lines are back-filled from the current product price: the best information available.
ALTER TABLE web_order_content ADD COLUMN unit_price NUMERIC(10, 2);
UPDATE web_order_content line SET unit_price = product.price FROM product WHERE product.id = line.product_id;
ALTER TABLE web_order_content ALTER COLUMN unit_price SET NOT NULL;

-- Stock can never go negative, whatever code path writes it.
ALTER TABLE inventory ADD CONSTRAINT ck_inventory_quantity_non_negative CHECK (quantity >= 0);
