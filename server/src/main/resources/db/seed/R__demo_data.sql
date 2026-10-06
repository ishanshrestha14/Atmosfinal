-- Demo data for local development and demos (repeatable migration: re-runs whenever this file changes).
-- Every insert is idempotent. Production leaves this location out: FLYWAY_LOCATIONS=classpath:db/migration

INSERT INTO role (name, created_date) VALUES
    ('CUSTOMER', CAST(NOW() AS VARCHAR)),
    ('STAFF', CAST(NOW() AS VARCHAR))
ON CONFLICT (name) DO NOTHING;

INSERT INTO category (name) VALUES
    ('Audio'),
    ('Phones'),
    ('Laptops'),
    ('Wearables'),
    ('Gaming')
ON CONFLICT (name) DO NOTHING;

INSERT INTO product (name, file_path, short_description, long_description, brand, price, category_id)
SELECT p.name, p.file_path, p.short_description, p.long_description, p.brand, p.price, c.id
FROM (VALUES
    ('AirPods Pro', '/img/cards/airPodsPro.jpg', 'Active noise cancellation earbuds',
     'Wireless earbuds with active noise cancellation, transparency mode and personalised spatial audio.', 'Apple', 249, 'Audio'),
    ('AirPods Max', '/img/hero/AirPodsMax.png', 'Over-ear headphones with spatial audio',
     'High-fidelity over-ear headphones with computational audio and up to 20 hours of battery life.', 'Apple', 549, 'Audio'),
    ('HomePod', '/img/cards/homePod.jpg', 'Smart speaker with room-filling sound',
     'Smart speaker with high-excursion woofer, beamforming tweeters and built-in smart home hub.', 'Apple', 299, 'Audio'),
    ('iPhone 15', '/img/hero/iPhone.jpg', '6.1-inch display, 48MP camera',
     'Dynamic Island, 48MP main camera, USB-C and an all-day battery in a durable glass and aluminium design.', 'Apple', 799, 'Phones'),
    ('MacBook Air', '/img/cards/macBook.jpg', 'Thin and light laptop with M-series chip',
     'Fanless laptop with a Liquid Retina display, up to 18 hours of battery life and a 1080p FaceTime camera.', 'Apple', 999, 'Laptops'),
    ('Apple Watch', '/img/cards/appleWatch.jpeg', 'Fitness, health and connectivity on your wrist',
     'Always-on Retina display, heart-rate and sleep tracking, crash detection and water resistance to 50m.', 'Apple', 399, 'Wearables'),
    ('Apple Vision Pro', '/img/cards/manAppleVisionPro.jpg', 'Spatial computer',
     'Blends digital content with your physical space, controlled with your eyes, hands and voice.', 'Apple', 3499, 'Wearables'),
    ('PlayStation 5', '/img/cards/ps5.png', 'Next-gen console with ultra-fast SSD',
     'Lightning-fast loading, haptic feedback, adaptive triggers and 3D audio for deeper immersion.', 'Sony', 499, 'Gaming')
) AS p(name, file_path, short_description, long_description, brand, price, category_name)
JOIN category c ON c.name = p.category_name
ON CONFLICT (name) DO NOTHING;

INSERT INTO inventory (product_id, quantity)
SELECT p.id, s.quantity
FROM (VALUES
    ('AirPods Pro', 50),
    ('AirPods Max', 20),
    ('HomePod', 30),
    ('iPhone 15', 40),
    ('MacBook Air', 15),
    ('Apple Watch', 35),
    ('Apple Vision Pro', 5),
    ('PlayStation 5', 10)
) AS s(product_name, quantity)
JOIN product p ON p.name = s.product_name
ON CONFLICT (product_id) DO NOTHING;

-- Pre-activated demo accounts
--   customer: demo  / Demo1234!
--   staff:    staff / Staff1234!
INSERT INTO app_user (username, email, password, enabled, account_locked, created_date) VALUES
    ('demo', 'demo@atmos.local', '$2y$10$5mcGpKIq.j35.Fblg6QqA.1lHTPZoxU0OvDHSAbgCnIZZ3UJT5Li6', true, false, CAST(NOW() AS VARCHAR)),
    ('staff', 'staff@atmos.local', '$2y$10$APGM7UT5eOMgYQurvtpBcuoxWXJpcycQnvFnHkIDMK4qDhR.uyuZe', true, false, CAST(NOW() AS VARCHAR))
ON CONFLICT (username) DO NOTHING;

INSERT INTO app_user_roles (users_id, roles_id)
SELECT u.id, r.id
FROM (VALUES ('demo', 'CUSTOMER'), ('staff', 'STAFF')) AS m(username, role_name)
JOIN app_user u ON u.username = m.username
JOIN role r ON r.name = m.role_name
WHERE NOT EXISTS (
    SELECT 1 FROM app_user_roles ur WHERE ur.users_id = u.id AND ur.roles_id = r.id
);
