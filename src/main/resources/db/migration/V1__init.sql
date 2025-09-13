-- Country Table
CREATE TABLE IF NOT EXISTS country (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(10) NOT NULL UNIQUE
);

-- Region Table
CREATE TABLE IF NOT EXISTS region (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    country_id INTEGER REFERENCES country(id),
    CONSTRAINT unique_region_per_country UNIQUE (name, country_id)
);

-- City Table
CREATE TABLE IF NOT EXISTS city (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    region_id INTEGER REFERENCES region(id),
    CONSTRAINT unique_city_per_region UNIQUE (name, region_id)
);

-- User Table
CREATE TABLE IF NOT EXISTS "users" (
   id SERIAL PRIMARY KEY,
   username VARCHAR(255) UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'HAS_ACCOUNT',
    profile_image_url TEXT,
    drivers_license_url TEXT,
    id_photo_url TEXT,
    proof_of_address_url TEXT,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    bio TEXT,
    city_id INTEGER REFERENCES city(id),
    custom_city VARCHAR(100),
    contact_number VARCHAR(255),
    id_number VARCHAR(255),
    plan_type VARCHAR(32),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Business Table
CREATE TABLE IF NOT EXISTS business (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    contact_number VARCHAR(255),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city_id INTEGER REFERENCES city(id),
    postal_code VARCHAR(20),
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- BusinessUser Table (join table for business team members)
CREATE TABLE IF NOT EXISTS business_user (
    id SERIAL PRIMARY KEY,
    business_id INTEGER NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'CONTRIBUTOR', -- OWNER, MANAGER, CONTRIBUTOR
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(business_id, user_id)
);

-- Category Table
CREATE TABLE IF NOT EXISTS category (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    parent_id INTEGER REFERENCES category(id) ON DELETE SET NULL,
    CONSTRAINT unique_category_name_per_parent UNIQUE (name, parent_id)
);

-- Seed Countries

INSERT INTO country (name, code) VALUES
  ('South Africa', 'ZA')
ON CONFLICT (code) DO NOTHING;

-- South African provinces (regions)
INSERT INTO region ( name, country_id) VALUES
                                              ( 'Gauteng', 1),
                                              ( 'Western Cape', 1),
                                              ( 'KwaZulu-Natal', 1),
                                              ( 'Eastern Cape', 1),
                                              ( 'Free State', 1),
                                              ( 'Limpopo', 1),
                                              ( 'Mpumalanga', 1),
                                              ( 'North West', 1),
                                              ( 'Northern Cape', 1)
ON CONFLICT (name, country_id) DO NOTHING;

-- Expanded South African cities

INSERT INTO city (name, region_id) VALUES
                                       -- Gauteng
                                       ('Johannesburg', (SELECT id FROM region WHERE name = 'Gauteng')),
                                       ('Pretoria', (SELECT id FROM region WHERE name = 'Gauteng')),
                                       ('Sandton', (SELECT id FROM region WHERE name = 'Gauteng')),
                                       ('Midrand', (SELECT id FROM region WHERE name = 'Gauteng')),
                                       ('Centurion', (SELECT id FROM region WHERE name = 'Gauteng')),

                                       -- Western Cape
                                       ('Cape Town', (SELECT id FROM region WHERE name = 'Western Cape')),
                                       ('Stellenbosch', (SELECT id FROM region WHERE name = 'Western Cape')),
                                       ('Paarl', (SELECT id FROM region WHERE name = 'Western Cape')),
                                       ('George', (SELECT id FROM region WHERE name = 'Western Cape')),
                                       ('Knysna', (SELECT id FROM region WHERE name = 'Western Cape')),

                                       -- KwaZulu-Natal
                                       ('Durban', (SELECT id FROM region WHERE name = 'KwaZulu-Natal')),
                                       ('Pietermaritzburg', (SELECT id FROM region WHERE name = 'KwaZulu-Natal')),
                                       ('Richards Bay', (SELECT id FROM region WHERE name = 'KwaZulu-Natal')),
                                       ('Ballito', (SELECT id FROM region WHERE name = 'KwaZulu-Natal')),
                                       ('Umhlanga', (SELECT id FROM region WHERE name = 'KwaZulu-Natal')),

                                       -- Eastern Cape
                                       ('Port Elizabeth', (SELECT id FROM region WHERE name = 'Eastern Cape')),
                                       ('East London', (SELECT id FROM region WHERE name = 'Eastern Cape')),
                                       ('Grahamstown', (SELECT id FROM region WHERE name = 'Eastern Cape')),
                                       ('Jeffreys Bay', (SELECT id FROM region WHERE name = 'Eastern Cape')),

                                       -- Free State
                                       ('Bloemfontein', (SELECT id FROM region WHERE name = 'Free State')),
                                       ('Welkom', (SELECT id FROM region WHERE name = 'Free State')),

                                       -- Limpopo
                                       ('Polokwane', (SELECT id FROM region WHERE name = 'Limpopo')),
                                       ('Thohoyandou', (SELECT id FROM region WHERE name = 'Limpopo')),
                                       ('Tzaneen', (SELECT id FROM region WHERE name = 'Limpopo')),

                                       -- Mpumalanga
                                       ('Nelspruit', (SELECT id FROM region WHERE name = 'Mpumalanga')),
                                       ('White River', (SELECT id FROM region WHERE name = 'Mpumalanga')),
                                       ('Sabie', (SELECT id FROM region WHERE name = 'Mpumalanga')),

                                       -- North West
                                       ('Mahikeng', (SELECT id FROM region WHERE name = 'North West')),
                                       ('Rustenburg', (SELECT id FROM region WHERE name = 'North West')),

                                       -- Northern Cape
                                       ('Kimberley', (SELECT id FROM region WHERE name = 'Northern Cape')),
                                       ('Upington', (SELECT id FROM region WHERE name = 'Northern Cape'))
ON CONFLICT (name, region_id) DO NOTHING;


-- =========================
-- Core Categories
INSERT INTO category (id, name, parent_id) VALUES
                                               (1, 'Electronics', NULL),
                                               (2, 'Fashion & Accessories', NULL),
                                               (3, 'Home & Garden', NULL),
                                               (4, 'Sports & Outdoors', NULL),
                                               (5, 'Books & Media', NULL),
                                               (6, 'Automotive', NULL),
                                               (7, 'Toys & Games', NULL),
                                               (8, 'Health & Beauty', NULL),
                                               (9, 'Art & Collectibles', NULL),
                                               (10, 'Musical Instruments', NULL),
                                               (11, 'Tools & Hardware', NULL),
                                               (12, 'Pet Supplies', NULL),
                                               (13, 'Baby & Kids', NULL),
                                               (14, 'Office & Business', NULL),
                                               (15, 'Food & Beverages', NULL)
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Electronics subcategories
INSERT INTO category (id, name, parent_id) VALUES
                                               (101, 'Computers & Tablets', 1),
                                               (102, 'Phones & Accessories', 1),
                                               (103, 'Cameras & Photography', 1),
                                               (104, 'Audio & Video', 1),
                                               (105, 'Gaming', 1)
ON CONFLICT (id) DO NOTHING;

-- Computers sub-subcategories
INSERT INTO category (id, name, parent_id) VALUES
                                               (201, 'Laptops', 101),
                                               (202, 'Desktops', 101),
                                               (203, 'Tablets', 101),
                                               (204, 'Components', 101),
                                               (205, 'Accessories & Peripherals', 101)
ON CONFLICT (id) DO NOTHING;

-- Components
INSERT INTO category (id, name, parent_id) VALUES
                                               (301, 'CPU', 204),
                                               (302, 'GPU', 204),
                                               (303, 'RAM & Storage', 204),
                                               (304, 'Motherboards', 204),
                                               (305, 'Power Supplies', 204),
                                               (306, 'Cooling', 204)
ON CONFLICT (id) DO NOTHING;

-- Accessories & Peripherals
INSERT INTO category (id, name, parent_id) VALUES
                                               (307, 'Keyboards', 205),
                                               (308, 'Mice', 205),
                                               (309, 'Monitors', 205),
                                               (310, 'Cables & Hubs', 205),
                                               (311, 'Docking Stations', 205)
ON CONFLICT (id) DO NOTHING;

-- Phones & Accessories
INSERT INTO category (id, name, parent_id) VALUES
                                               (312, 'Smartphones', 102),
                                               (313, 'Cases & Covers', 102),
                                               (314, 'Chargers & Cables', 102),
                                               (315, 'Headphones & Earphones', 102)
ON CONFLICT (id) DO NOTHING;

-- Cameras
INSERT INTO category (id, name, parent_id) VALUES
                                               (316, 'DSLR & Mirrorless', 103),
                                               (317, 'Lenses & Filters', 103),
                                               (318, 'Tripods & Mounts', 103),
                                               (319, 'Camera Bags & Storage', 103)
ON CONFLICT (id) DO NOTHING;

-- Audio & Video
INSERT INTO category (id, name, parent_id) VALUES
                                               (320, 'Speakers', 104),
                                               (321, 'Headphones', 104),
                                               (322, 'TVs & Projectors', 104),
                                               (323, 'Streaming Devices', 104)
ON CONFLICT (id) DO NOTHING;

-- Gaming
INSERT INTO category (id, name, parent_id) VALUES
                                               (324, 'Consoles', 105),
                                               (325, 'Games', 105),
                                               (326, 'Controllers & Accessories', 105),
                                               (327, 'VR Headsets', 105)
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Fashion & Accessories
INSERT INTO category (id, name, parent_id) VALUES
                                               (401, 'Men''s Fashion', 2),
                                               (402, 'Women''s Fashion', 2),
                                               (403, 'Jewelry & Watches', 2),
                                               (404, 'Bags & Luggage', 2),
                                               (405, 'Accessories', 2)
ON CONFLICT (id) DO NOTHING;

-- Men
INSERT INTO category (id, name, parent_id) VALUES
                                               (501, 'Tops', 401),
                                               (502, 'Bottoms', 401),
                                               (503, 'Shoes', 401),
                                               (504, 'Underwear', 401),
                                               (505, 'Outerwear', 401)
ON CONFLICT (id) DO NOTHING;

-- Women
INSERT INTO category (id, name, parent_id) VALUES
                                               (506, 'Tops', 402),
                                               (507, 'Bottoms', 402),
                                               (508, 'Dresses', 402),
                                               (509, 'Shoes', 402),
                                               (510, 'Underwear', 402),
                                               (511, 'Outerwear', 402)
ON CONFLICT (id) DO NOTHING;

-- Jewelry & Watches
INSERT INTO category (id, name, parent_id) VALUES
                                               (512, 'Rings', 403),
                                               (513, 'Necklaces', 403),
                                               (514, 'Bracelets', 403),
                                               (515, 'Watches', 403)
ON CONFLICT (id) DO NOTHING;

-- Bags & Luggage
INSERT INTO category (id, name, parent_id) VALUES
                                               (516, 'Handbags', 404),
                                               (517, 'Backpacks', 404),
                                               (518, 'Suitcases', 404)
ON CONFLICT (id) DO NOTHING;

-- Accessories
INSERT INTO category (id, name, parent_id) VALUES
                                               (519, 'Belts', 405),
                                               (520, 'Hats & Caps', 405),
                                               (521, 'Sunglasses', 405),
                                               (522, 'Scarves', 405)
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Home & Garden
INSERT INTO category (id, name, parent_id) VALUES
                                               (601, 'Furniture', 3),
                                               (602, 'Kitchen & Dining', 3),
                                               (603, 'Garden & Outdoor', 3),
                                               (604, 'Home Decor', 3),
                                               (605, 'Bedding & Bath', 3)
ON CONFLICT (id) DO NOTHING;

-- Furniture
INSERT INTO category (id, name, parent_id) VALUES
                                               (701, 'Living Room', 601),
                                               (702, 'Bedroom', 601),
                                               (703, 'Office Furniture', 601)
ON CONFLICT (id) DO NOTHING;

-- Kitchen & Dining
INSERT INTO category (id, name, parent_id) VALUES
                                               (704, 'Cookware', 602),
                                               (705, 'Tableware', 602),
                                               (706, 'Small Appliances', 602)
ON CONFLICT (id) DO NOTHING;

-- Garden & Outdoor
INSERT INTO category (id, name, parent_id) VALUES
                                               (707, 'Garden Tools', 603),
                                               (708, 'Outdoor Furniture', 603),
                                               (709, 'Grills & BBQ', 603)
ON CONFLICT (id) DO NOTHING;

-- Home Decor
INSERT INTO category (id, name, parent_id) VALUES
                                               (710, 'Lighting', 604),
                                               (711, 'Rugs & Carpets', 604),
                                               (712, 'Wall Art & Decor', 604)
ON CONFLICT (id) DO NOTHING;

-- Bedding & Bath
INSERT INTO category (id, name, parent_id) VALUES
                                               (713, 'Bedding', 605),
                                               (714, 'Bath', 605)
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Sports & Outdoors
INSERT INTO category (id, name, parent_id) VALUES
                                               (801, 'Fitness', 4),
                                               (802, 'Outdoor Recreation', 4),
                                               (803, 'Team Sports', 4),
                                               (804, 'Cycling', 4)
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Automotive
INSERT INTO category (id, name, parent_id) VALUES
                                               (901, 'Car Parts & Accessories', 6),
                                               (902, 'Motorcycles & Scooters', 6),
                                               (903, 'Tires & Wheels', 6),
                                               (904, 'Car Electronics', 6),
                                               (905, 'Tools & Garage', 6)
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Toys & Games
INSERT INTO category (id, name, parent_id) VALUES
                                               (1001, 'Action Figures & Collectibles', 7),
                                               (1002, 'Board Games & Puzzles', 7),
                                               (1003, 'Dolls & Accessories', 7),
                                               (1004, 'Outdoor Play', 7),
                                               (1005, 'Educational Toys', 7)
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Health & Beauty
INSERT INTO category (id, name, parent_id) VALUES
                                               (1101, 'Makeup', 8),
                                               (1102, 'Skincare', 8),
                                               (1103, 'Hair Care', 8),
                                               (1104, 'Personal Care', 8),
                                               (1105, 'Supplements', 8)
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Art & Collectibles
INSERT INTO category (id, name, parent_id) VALUES
                                               (1201, 'Paintings', 9),
                                               (1202, 'Sculptures', 9),
                                               (1203, 'Collectibles', 9)
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Musical Instruments
INSERT INTO category (id, name, parent_id) VALUES
                                               (1301, 'Guitars', 10),
                                               (1302, 'Keyboards & Pianos', 10),
                                               (1303, 'Drums & Percussion', 10),
                                               (1304, 'Wind Instruments', 10)
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Tools & Hardware
INSERT INTO category (id, name, parent_id) VALUES
                                               (1401, 'Hand Tools', 11),
                                               (1402, 'Power Tools', 11),
                                               (1403, 'Tool Storage', 11),
                                               (1404, 'Safety Equipment', 11)
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Pet Supplies
INSERT INTO category (id, name, parent_id) VALUES
                                               (1501, 'Dog Supplies', 12),
                                               (1502, 'Cat Supplies', 12),
                                               (1503, 'Fish & Aquatic', 12),
                                               (1504, 'Small Pets', 12),
                                               (1505, 'Birds', 12)
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Baby & Kids
INSERT INTO category (id, name, parent_id) VALUES
                                               (1601, 'Clothing', 13),
                                               (1602, 'Toys', 13),
                                               (1603, 'Gear & Accessories', 13),
                                               (1604, 'Feeding & Nursing', 13)
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Office & Business
INSERT INTO category (id, name, parent_id) VALUES
                                               (1701, 'Office Supplies', 14),
                                               (1702, 'Stationery', 14),
                                               (1703, 'Furniture', 14),
                                               (1704, 'Technology', 14)
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Food & Beverages
INSERT INTO category (id, name, parent_id) VALUES
                                               (1801, 'Snacks', 15),
                                               (1802, 'Beverages', 15),
                                               (1803, 'Groceries', 15),
                                               (1804, 'Specialty Foods', 15)
ON CONFLICT (id) DO NOTHING;



-- Listing Table
CREATE TABLE IF NOT EXISTS listing (
   id SERIAL PRIMARY KEY,
   user_id BIGINT NOT NULL,
   title VARCHAR(255) NOT NULL,
    description TEXT,
    category_id BIGINT,
    price DOUBLE PRECISION NOT NULL,
    sold BOOLEAN DEFAULT FALSE,
    city_id INTEGER REFERENCES city(id),
    custom_city VARCHAR(100),
    condition VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    CONSTRAINT fk_listing_user FOREIGN KEY (user_id) REFERENCES "users" (id) ON DELETE CASCADE,  -- Change "user" to "users"
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

-- Trust Rating System
-- Trust Rating table
CREATE TABLE trust_rating (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    overall_score DECIMAL(5,2) NOT NULL DEFAULT 0.00, -- Score out of 100
    verification_score DECIMAL(5,2) NOT NULL DEFAULT 0.00, -- Score from verified documents
    profile_score DECIMAL(5,2) NOT NULL DEFAULT 0.00, -- Score from profile completion
    review_score DECIMAL(5,2) NOT NULL DEFAULT 0.00, -- Score from positive reviews
    transaction_score DECIMAL(5,2) NOT NULL DEFAULT 0.00, -- Score from successful transactions
    total_reviews INTEGER NOT NULL DEFAULT 0,
    positive_reviews INTEGER NOT NULL DEFAULT 0,
    total_transactions INTEGER NOT NULL DEFAULT 0,
    successful_transactions INTEGER NOT NULL DEFAULT 0,
    last_calculated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Verification Documents table
CREATE TABLE verification_document (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    document_type VARCHAR(50) NOT NULL, -- 'ID_CARD', 'DRIVERS_LICENSE', 'PROOF_OF_ADDRESS', 'PROFILE_PHOTO'
    document_url VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'APPROVED', 'REJECTED'
    verified_at TIMESTAMP,
    verified_by BIGINT, -- Admin who verified
    rejection_reason VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (verified_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Transaction System table
CREATE TABLE "transaction" (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    sale_price DECIMAL(10,2) NOT NULL,
    sale_date TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'COMPLETED', 'CANCELLED', 'DISPUTED'
    payment_method VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (listing_id) REFERENCES listing(id) ON DELETE CASCADE,
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX unique_completed_transaction_per_listing
ON "transaction" (listing_id)
WHERE status = 'COMPLETED';

-- Review System table
CREATE TABLE review (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    reviewer_id BIGINT NOT NULL, -- User giving the review
    reviewed_user_id BIGINT NOT NULL, -- User being reviewed
    transaction_id BIGINT NOT NULL, -- The transaction that was completed
    rating DECIMAL(2,1) NOT NULL CHECK (rating >= 0.5 AND rating <= 5.0), -- 0.5 to 5.0 stars
    comment TEXT,
    is_positive BOOLEAN NOT NULL, -- true if rating >= 3.5
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (reviewer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (transaction_id) REFERENCES "transaction"(id) ON DELETE CASCADE,
    UNIQUE (reviewer_id, transaction_id) -- One review per transaction per user
);

-- Profile Completion tracking
CREATE TABLE profile_completion (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    has_profile_photo BOOLEAN NOT NULL DEFAULT FALSE,
    has_bio BOOLEAN NOT NULL DEFAULT FALSE,
    has_contact_number BOOLEAN NOT NULL DEFAULT FALSE,
    has_location BOOLEAN NOT NULL DEFAULT FALSE,
    has_id_document BOOLEAN NOT NULL DEFAULT FALSE,
    has_drivers_license BOOLEAN NOT NULL DEFAULT FALSE,
    has_proof_of_address BOOLEAN NOT NULL DEFAULT FALSE,
    completion_percentage DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    last_calculated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Subscription System table
CREATE TABLE subscription (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    business_id BIGINT NOT NULL,
    stripe_subscription_id VARCHAR(255) UNIQUE,
    stripe_customer_id VARCHAR(255),
    plan_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    billing_cycle VARCHAR(20) NOT NULL,
    current_period_start TIMESTAMP,
    current_period_end TIMESTAMP,
    cancel_at_period_end BOOLEAN DEFAULT FALSE,
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE CASCADE
);

-- StoreBranding Table
CREATE TABLE IF NOT EXISTS store_branding (
    business_id INTEGER PRIMARY KEY REFERENCES business(id) ON DELETE CASCADE,
    slug VARCHAR(255),
    logo_url TEXT,
    banner_url TEXT,
    theme_color VARCHAR(32),
    about TEXT,
    store_name VARCHAR(255),
    primary_color VARCHAR(32),
    secondary_color VARCHAR(32),
    light_or_dark VARCHAR(16),
    text_color VARCHAR(32),
    card_text_color VARCHAR(32)
);

-- Indexes for performance
CREATE INDEX idx_trust_rating_user_id ON trust_rating(user_id);
CREATE INDEX idx_verification_document_user_id ON verification_document(user_id);
CREATE INDEX idx_verification_document_status ON verification_document(status);
CREATE INDEX idx_review_reviewed_user_id ON review(reviewed_user_id);
CREATE INDEX idx_transaction_listing_id ON "transaction"(listing_id);
CREATE INDEX idx_transaction_buyer_id ON "transaction"(buyer_id);
CREATE INDEX idx_transaction_seller_id ON "transaction"(seller_id);
CREATE INDEX idx_transaction_status ON "transaction"(status);
CREATE INDEX idx_review_transaction_id ON review(transaction_id);
CREATE INDEX idx_review_rating ON review(rating);
CREATE INDEX idx_profile_completion_user_id ON profile_completion(user_id);
CREATE INDEX idx_subscription_user_id ON subscription(user_id);
CREATE INDEX idx_subscription_status ON subscription(status);
CREATE INDEX idx_subscription_stripe_id ON subscription(stripe_subscription_id);
CREATE INDEX idx_business_owner_id ON business(owner_id);
CREATE INDEX idx_business_user_business_id ON business_user(business_id);
CREATE INDEX idx_business_user_user_id ON business_user(user_id);
CREATE INDEX idx_business_user_role ON business_user(role);
CREATE INDEX idx_store_branding_business_id ON store_branding(business_id);

-- Add reseller user
INSERT INTO "users" (username, email, password, role, plan_type, first_name, last_name, bio, city_id, contact_number, created_at)
VALUES (
  'resellerjoe',
  'reseller@marketplace.com',
  '$2a$10$7QJ8QwQwQwQwQwQwQwQwQeQwQwQwQwQwQwQwQwQwQwQwQwQwQwQw', -- bcrypt for 'password'
  'HAS_ACCOUNT',
  'RESELLER',
  'Joe',
  'Reseller',
  'We sell the best gadgets and accessories!',
  (SELECT id FROM city WHERE name = 'Cape Town'),
  '+27111222333',
  NOW()
)
ON CONFLICT (email) DO NOTHING;

-- Add business for reseller
INSERT INTO business (name, email, contact_number, address_line1, city_id, owner_id)
SELECT 'Joe''s Reseller Store', 'reseller@marketplace.com', '+27111222333', '123 Main Street', 
       (SELECT id FROM city WHERE name = 'Cape Town'), id
FROM "users" WHERE email = 'reseller@marketplace.com'
ON CONFLICT DO NOTHING;

-- Add business user relationship (owner)
INSERT INTO business_user (business_id, user_id, role)
SELECT b.id, u.id, 'OWNER'
FROM business b
JOIN "users" u ON u.email = b.email
WHERE b.email = 'reseller@marketplace.com'
ON CONFLICT (business_id, user_id) DO NOTHING;

-- Add store branding for reseller business
INSERT INTO store_branding (business_id, logo_url, banner_url, theme_color, about, store_name)
SELECT b.id, 'https://images.unsplash.com/photo-1519125323398-675f0ddb6308?q=80&w=100',
  'https://images.unsplash.com/photo-1465101046530-73398c7f28ca?q=80&w=800',
  '#e53e3e',
  'Welcome to Joe''s Reseller Store! Find top gadgets and more.',
  'Joe''s Reseller Store'
FROM business b
WHERE b.email = 'reseller@marketplace.com'
ON CONFLICT (business_id) DO NOTHING;