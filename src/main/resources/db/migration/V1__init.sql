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

-- Category Table
CREATE TABLE IF NOT EXISTS category (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

-- Seed Countries

INSERT INTO country (name, code) VALUES
  ('South Africa', 'ZA'),
  ('United States', 'US')
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
                                              ( 'Northern Cape', 1),
                                              ('California', (SELECT id FROM country WHERE code = 'US')),
                                              ('New York', (SELECT id FROM country WHERE code = 'US'))
ON CONFLICT (name, country_id) DO NOTHING;

-- Seed Cities
INSERT INTO city (name, region_id) VALUES
  ('Johannesburg', (SELECT id FROM region WHERE name = 'Gauteng')),
  ('Pretoria', (SELECT id FROM region WHERE name = 'Gauteng')),
  ('Cape Town', (SELECT id FROM region WHERE name = 'Western Cape')),
  ('Durban', (SELECT id FROM region WHERE name = 'KwaZulu-Natal')),
  ( 'Port Elizabeth', 4),
  ( 'Bloemfontein', 5),
  ( 'Polokwane', 6),
  ( 'Nelspruit', 7),
  ( 'Mahikeng', 8),
  ( 'Kimberley', 9),
  ('Los Angeles', (SELECT id FROM region WHERE name = 'California')),
  ('San Francisco', (SELECT id FROM region WHERE name = 'California')),
  ('New York City', (SELECT id FROM region WHERE name = 'New York'))
ON CONFLICT (name, region_id) DO NOTHING;

-- Insert Core Categories
INSERT INTO category (name) VALUES
('Electronics'),
('Fashion & Accessories'),
('Home & Garden'),
('Sports & Outdoors'),
('Books & Media'),
('Automotive'),
('Toys & Games'),
('Health & Beauty'),
('Art & Collectibles'),
('Musical Instruments'),
('Tools & Hardware'),
('Pet Supplies'),
('Baby & Kids'),
('Office & Business'),
('Food & Beverages')
ON CONFLICT (name) DO NOTHING;

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
    document_score DECIMAL(5,2) NOT NULL DEFAULT 0.00, -- Score from verified documents
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
    has_verified_email BOOLEAN NOT NULL DEFAULT FALSE,
    has_verified_phone BOOLEAN NOT NULL DEFAULT FALSE,
    has_id_verification BOOLEAN NOT NULL DEFAULT FALSE,
    has_address_verification BOOLEAN NOT NULL DEFAULT FALSE,
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
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- StoreBranding Table
CREATE TABLE IF NOT EXISTS store_branding (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    slug VARCHAR(100) UNIQUE,
    logo_url TEXT,
    banner_url TEXT,
    theme_color VARCHAR(20),
    about TEXT
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

-- Add store branding for reseller
INSERT INTO store_branding (user_id, slug, logo_url, banner_url, theme_color, about)
SELECT id, 'reseller-joe',
  'https://images.unsplash.com/photo-1519125323398-675f0ddb6308?q=80&w=100',
  'https://images.unsplash.com/photo-1465101046530-73398c7f28ca?q=80&w=800',
  '#e53e3e',
  'Welcome to Joe''s Reseller Store! Find top gadgets and more.'
FROM "users" WHERE email = 'reseller@marketplace.com'
ON CONFLICT (user_id) DO NOTHING;

ALTER TABLE store_branding ADD COLUMN store_name VARCHAR(100);
