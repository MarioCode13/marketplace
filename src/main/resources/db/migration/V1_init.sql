-- ////////////////////
-- //// Users ////--
-- ////////////////////
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'HAS_ACCOUNT',
    profile_image BYTEA,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default users
INSERT INTO users (username, email, password, role) VALUES
('admin', 'admin@example.com', '$2a$10$WjFZkFh.e/7X1SzXC1HzN.2NVcELo4FSY79URy.uXexxArYhU1B7K', 'SUBSCRIBED'), --admin123
('testuser', 'test@example.com', '$2a$10$WjFZkFh.e/7X1SzXC1HzN.2NVcELo4FSY79URy.uXexxArYhU1B7K', 'HAS_ACCOUNT'); --admin123


-- ////////////////////
-- //// Category (Handles Both Categories & Subcategories) ////--
-- ////////////////////
CREATE TABLE category (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    parent_id BIGINT,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category (id) ON DELETE CASCADE
);

-- Insert parent categories
INSERT INTO category (name) VALUES
('Electronics'),
('Fashion'),
('Home & Garden');

-- Insert subcategories under parent categories
INSERT INTO category (name, parent_id) VALUES
('Smartphones', (SELECT id FROM category WHERE name = 'Electronics')),
('Laptops', (SELECT id FROM category WHERE name = 'Electronics')),
('Men''s Clothing', (SELECT id FROM category WHERE name = 'Fashion')),
('Women''s Clothing', (SELECT id FROM category WHERE name = 'Fashion')),
('Furniture', (SELECT id FROM category WHERE name = 'Home & Garden')),
('Kitchen Appliances', (SELECT id FROM category WHERE name = 'Home & Garden'));

-- Insert sub-subcategories (optional)
INSERT INTO category (name, parent_id) VALUES
('Android Phones', (SELECT id FROM category WHERE name = 'Smartphones')),
('iPhones', (SELECT id FROM category WHERE name = 'Smartphones')),
('Gaming Laptops', (SELECT id FROM category WHERE name = 'Laptops')),
('Ultrabooks', (SELECT id FROM category WHERE name = 'Laptops')),
('T-Shirts', (SELECT id FROM category WHERE name = 'Men''s Clothing')),
('Dresses', (SELECT id FROM category WHERE name = 'Women''s Clothing')),
('Sofas', (SELECT id FROM category WHERE name = 'Furniture')),
('Microwaves', (SELECT id FROM category WHERE name = 'Kitchen Appliances'));


-- ////////////////////
-- //// Listings ////--
-- ////////////////////
CREATE TABLE listings (
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
    CONSTRAINT fk_listings_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_listings_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE SET NULL
);

-- Insert default listings (assuming user IDs exist)
INSERT INTO listing (user_id, title, description, price, condition, location, created_at, expires_at, sold)
VALUES
(1, 'iPhone 13 Pro', 'Mint condition, barely used.', 799.99, 'NEW', 'New York', NOW(), NOW() + INTERVAL '30 days', false),
(2, 'Gaming Laptop', 'RTX 3060, 16GB RAM, 512GB SSD.', 1299.00, 'GOOD', 'San Francisco', NOW(), NOW() + INTERVAL '30 days', false);



-- ////////////////////////////////////////////
-- //// Listing Categories Junction Table /////
-- ////////////////////////////////////////////
CREATE TABLE listing_categories (
    listing_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (listing_id, category_id),
    CONSTRAINT fk_listing FOREIGN KEY (listing_id) REFERENCES listings (id) ON DELETE CASCADE,
    CONSTRAINT fk_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE CASCADE
);

-- Insert example listing-category relationships
INSERT INTO listing_categories (listing_id, category_id)
VALUES
((SELECT id FROM listings WHERE title = 'iPhone 13 Pro'), (SELECT id FROM category WHERE name = 'iPhones')),
((SELECT id FROM listings WHERE title = 'Gaming Laptop'), (SELECT id FROM category WHERE name = 'Gaming Laptops'));


-- ////////////////////
-- //// Listing Images ////
-- ////////////////////
CREATE TABLE listing_images (
    id SERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    image TEXT NOT NULL, -- Storing images as Base64 strings
    CONSTRAINT fk_listing_image FOREIGN KEY (listing_id) REFERENCES listings (id) ON DELETE CASCADE
);

-- Insert example images for listings
INSERT INTO listing_images (listing_id, images)
VALUES
((SELECT id FROM listing WHERE title = 'iPhone 13 Pro'), 'base64-image-data-1'),
((SELECT id FROM listing WHERE title = 'iPhone 13 Pro'), 'base64-image-data-2'),
((SELECT id FROM listing WHERE title = 'Gaming Laptop'), 'base64-image-data-3');
