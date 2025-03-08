-- User Table
CREATE TABLE IF NOT EXISTS "users" (
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'HAS_ACCOUNT',
    profile_image BYTEA,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Category Table
CREATE TABLE IF NOT EXISTS category (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

-- Listing Table
CREATE TABLE IF NOT EXISTS listing (
    id SERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category_id BIGINT,
    price DOUBLE PRECISION NOT NULL,
    sold BOOLEAN DEFAULT FALSE,
    location VARCHAR(255),
    condition VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    CONSTRAINT fk_listing_user FOREIGN KEY (user_id) REFERENCES "user" (id) ON DELETE CASCADE,
    CONSTRAINT fk_listing_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE SET NULL,
    CONSTRAINT unique_user_title UNIQUE(user_id, title)  -- Added unique constraint on user_id + title
);


-- Listing Category Junction Table
CREATE TABLE IF NOT EXISTS listing_category (
    listing_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (listing_id, category_id),
    CONSTRAINT fk_listing FOREIGN KEY (listing_id) REFERENCES listing (id) ON DELETE CASCADE,
    CONSTRAINT fk_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE CASCADE
);

-- Listing Image Table
CREATE TABLE IF NOT EXISTS listing_image (
    id SERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    image TEXT NOT NULL,
    CONSTRAINT fk_listing_image FOREIGN KEY (listing_id) REFERENCES listing (id) ON DELETE CASCADE
);

-- Insert Default Users
INSERT INTO "users" (username, email, password, role) VALUES
('admin', 'admin@example.com', '$2a$10$WjFZkFh.e/7X1SzXC1HzN.2NVcELo4FSY79URy.uXexxArYhU1B7K', 'SUBSCRIBED')
ON CONFLICT (email) DO NOTHING;

INSERT INTO "users" (username, email, password, role) VALUES
('testuser', 'test@example.com', '$2a$10$WjFZkFh.e/7X1SzXC1HzN.2NVcELo4FSY79URy.uXexxArYhU1B7K', 'HAS_ACCOUNT')
ON CONFLICT (email) DO NOTHING;

-- Insert Categories
INSERT INTO category (name) VALUES
('Electronics'),
('Fashion'),
('Home & Garden')
ON CONFLICT (name) DO NOTHING;

-- Insert Listings (Assuming user and category IDs exist)
INSERT INTO listing (user_id, title, description, price, condition, location, created_at, expires_at, sold)
VALUES
(1, 'iPhone 13 Pro', 'Mint condition, barely used.', 799.99, 'NEW', 'New York', NOW(), NOW() + INTERVAL '30 days', false),
(2, 'Gaming Laptop', 'RTX 3060, 16GB RAM, 512GB SSD.', 1299.00, 'GOOD', 'San Francisco', NOW(), NOW() + INTERVAL '30 days', false)
ON CONFLICT (user_id, title) DO NOTHING;


-- Insert Listing-Category Relationships
INSERT INTO listing_category (listing_id, category_id)
VALUES
((SELECT id FROM listing WHERE title = 'iPhone 13 Pro'), (SELECT id FROM category WHERE name = 'Electronics')),
((SELECT id FROM listing WHERE title = 'Gaming Laptop'), (SELECT id FROM category WHERE name = 'Electronics'))
ON CONFLICT (listing_id, category_id) DO NOTHING;

-- Insert Listing Images
INSERT INTO listing_image (listing_id, image) VALUES
((SELECT id FROM listing WHERE title = 'iPhone 13 Pro'), 'base64-image-data-1'),
((SELECT id FROM listing WHERE title = 'Gaming Laptop'), 'base64-image-data-2')
ON CONFLICT DO NOTHING;
