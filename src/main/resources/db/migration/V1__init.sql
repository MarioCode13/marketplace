-- PostgreSQL 13+ built-in; no uuid-ossp extension needed.
-- Ensure droplet uses SPRING_PROFILES_ACTIVE=prod and DB_* so this runs against PostgreSQL, not H2.
-- NOTE: GraphQL duplicate-type errors (Country/Region/City) arise from multiple .graphqls files
-- defining the same types. Keep a single definition in src/main/resources/graphql/schema.graphqls
-- and use "extend type" in other files if you need to add fields.

-- Country Table
CREATE TABLE IF NOT EXISTS country (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(10) NOT NULL UNIQUE
);

-- Region Table
CREATE TABLE IF NOT EXISTS region (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    country_id UUID REFERENCES country(id),
    CONSTRAINT unique_region_per_country UNIQUE (name, country_id)
);

-- City Table
CREATE TABLE IF NOT EXISTS city (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,
    region_id UUID REFERENCES region(id),
    CONSTRAINT unique_city_per_region UNIQUE (name, region_id)
);

-- User Table
CREATE TABLE IF NOT EXISTS "users" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'HAS_ACCOUNT',
    profile_image_url TEXT,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    bio TEXT,
    city_id UUID REFERENCES city(id),
    custom_city VARCHAR(100),
    contact_number VARCHAR(255),
    id_number VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Business Table
CREATE TABLE IF NOT EXISTS business (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    contact_number VARCHAR(255),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city_id UUID REFERENCES city(id),
    postal_code VARCHAR(20),
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    business_type VARCHAR(50),
    archived BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    email_verification_token VARCHAR(255),
    is_email_verified BOOLEAN DEFAULT FALSE,
    slug VARCHAR(255) UNIQUE
);

-- BusinessUser Table
CREATE TABLE IF NOT EXISTS business_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'CONTRIBUTOR',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(business_id, user_id)
);

-- Invitation Table
CREATE TABLE IF NOT EXISTS invitation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_id UUID REFERENCES users(id) ON DELETE SET NULL,
    recipient_email VARCHAR(255),
    business_id UUID NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    role VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Category Table
CREATE TABLE IF NOT EXISTS category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    parent_id UUID REFERENCES category(id) ON DELETE SET NULL,
    CONSTRAINT unique_category_name_per_parent UNIQUE (name, parent_id)
);

-- Seed Countries
INSERT INTO country (name, code) VALUES
  ('South Africa', 'ZA')
ON CONFLICT (code) DO NOTHING;

-- South African provinces (regions)
INSERT INTO region (name, country_id) VALUES
    ('Gauteng', (SELECT id FROM country WHERE code = 'ZA')),
    ('Western Cape', (SELECT id FROM country WHERE code = 'ZA')),
    ('KwaZulu-Natal', (SELECT id FROM country WHERE code = 'ZA')),
    ('Eastern Cape', (SELECT id FROM country WHERE code = 'ZA')),
    ('Free State', (SELECT id FROM country WHERE code = 'ZA')),
    ('Limpopo', (SELECT id FROM country WHERE code = 'ZA')),
    ('Mpumalanga', (SELECT id FROM country WHERE code = 'ZA')),
    ('North West', (SELECT id FROM country WHERE code = 'ZA')),
    ('Northern Cape', (SELECT id FROM country WHERE code = 'ZA'))
ON CONFLICT (name, country_id) DO NOTHING;

-- Expanded South African cities
INSERT INTO city (name, slug, region_id) VALUES
    ('Johannesburg', lower(regexp_replace('Johannesburg', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'Gauteng')),
    ('Pretoria', lower(regexp_replace('Pretoria', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'Gauteng')),
    ('Sandton', lower(regexp_replace('Sandton', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'Gauteng')),
    ('Midrand', lower(regexp_replace('Midrand', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'Gauteng')),
    ('Centurion', lower(regexp_replace('Centurion', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'Gauteng')),
    ('Cape Town', lower(regexp_replace('Cape Town', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'Western Cape')),
    ('Stellenbosch', lower(regexp_replace('Stellenbosch', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'Western Cape')),
    ('Paarl', lower(regexp_replace('Paarl', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'Western Cape')),
    ('George', lower(regexp_replace('George', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'Western Cape')),
    ('Knysna', lower(regexp_replace('Knysna', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'Western Cape')),
    ('Durban', lower(regexp_replace('Durban', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'KwaZulu-Natal')),
    ('Pietermaritzburg', lower(regexp_replace('Pietermaritzburg', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'KwaZulu-Natal')),
    ('Richards Bay', lower(regexp_replace('Richards Bay', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'KwaZulu-Natal')),
    ('Ballito', lower(regexp_replace('Ballito', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'KwaZulu-Natal')),
    ('Umhlanga', lower(regexp_replace('Umhlanga', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'KwaZulu-Natal')),
    ('Port Elizabeth', lower(regexp_replace('Port Elizabeth', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'Eastern Cape')),
    ('East London', lower(regexp_replace('East London', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'Eastern Cape')),
    ('Grahamstown', lower(regexp_replace('Grahamstown', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'Eastern Cape')),
    ('Jeffreys Bay', lower(regexp_replace('Jeffreys Bay', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'Eastern Cape')),
    ('Bloemfontein', lower(regexp_replace('Bloemfontein', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'Free State')),
    ('Welkom', lower(regexp_replace('Welkom', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'Free State')),
    ('Polokwane', lower(regexp_replace('Polokwane', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'Limpopo')),
    ('Thohoyandou', lower(regexp_replace('Thohoyandou', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'Limpopo')),
    ('Tzaneen', lower(regexp_replace('Tzaneen', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'Limpopo')),
    ('Nelspruit', lower(regexp_replace('Nelspruit', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'Mpumalanga')),
    ('White River', lower(regexp_replace('White River', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'Mpumalanga')),
    ('Sabie', lower(regexp_replace('Sabie', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'Mpumalanga')),
    ('Mahikeng', lower(regexp_replace('Mahikeng', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'North West')),
    ('Rustenburg', lower(regexp_replace('Rustenburg', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'North West')),
    ('Kimberley', lower(regexp_replace('Kimberley', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'Northern Cape')),
    ('Upington', lower(regexp_replace('Upington', '[^a-zA-Z0-9]+', '-', 'g')), (SELECT id FROM region WHERE name = 'Northern Cape'))
ON CONFLICT (name, region_id) DO NOTHING;


-- =========================
-- Core Categories (now using UUIDs)
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Electronics', lower(regexp_replace('Electronics', '[^a-zA-Z0-9]+', '-', 'g')), NULL),
    ('22222222-2222-2222-2222-222222222222', 'Fashion & Accessories', lower(regexp_replace('Fashion & Accessories', '[^a-zA-Z0-9]+', '-', 'g')), NULL),
    ('33333333-3333-3333-3333-333333333333', 'Home & Garden', lower(regexp_replace('Home & Garden', '[^a-zA-Z0-9]+', '-', 'g')), NULL),
    ('44444444-4444-4444-4444-444444444444', 'Sports & Outdoors', lower(regexp_replace('Sports & Outdoors', '[^a-zA-Z0-9]+', '-', 'g')), NULL),
    ('55555555-5555-5555-5555-555555555555', 'Books & Media', lower(regexp_replace('Books & Media', '[^a-zA-Z0-9]+', '-', 'g')), NULL),
    ('66666666-6666-6666-6666-666666666666', 'Automotive', lower(regexp_replace('Automotive', '[^a-zA-Z0-9]+', '-', 'g')), NULL),
    ('77777777-7777-7777-7777-777777777777', 'Toys & Games', lower(regexp_replace('Toys & Games', '[^a-zA-Z0-9]+', '-', 'g')), NULL),
    ('88888888-8888-8888-8888-888888888888', 'Health & Beauty', lower(regexp_replace('Health & Beauty', '[^a-zA-Z0-9]+', '-', 'g')), NULL),
    ('99999999-9999-9999-9999-999999999999', 'Art & Collectibles', lower(regexp_replace('Art & Collectibles', '[^a-zA-Z0-9]+', '-', 'g')), NULL),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Musical Instruments', lower(regexp_replace('Musical Instruments', '[^a-zA-Z0-9]+', '-', 'g')), NULL),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Tools & Hardware', lower(regexp_replace('Tools & Hardware', '[^a-zA-Z0-9]+', '-', 'g')), NULL),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'Pet Supplies', lower(regexp_replace('Pet Supplies', '[^a-zA-Z0-9]+', '-', 'g')), NULL),
    ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'Baby & Kids', lower(regexp_replace('Baby & Kids', '[^a-zA-Z0-9]+', '-', 'g')), NULL),
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'Office & Business', lower(regexp_replace('Office & Business', '[^a-zA-Z0-9]+', '-', 'g')), NULL),
    ('ffffffff-ffff-ffff-ffff-ffffffffffff', 'Food & Beverages', lower(regexp_replace('Food & Beverages', '[^a-zA-Z0-9]+', '-', 'g')), NULL)
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Electronics subcategories
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('11111111-1111-1111-1111-111111111112', 'Computers & Tablets',
     (SELECT c.slug || '-' || lower(regexp_replace('Computers & Tablets','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111111'),
     '11111111-1111-1111-1111-111111111111'),
    ('11111111-1111-1111-1111-111111111113', 'Phones & Accessories',
     (SELECT c.slug || '-' || lower(regexp_replace('Phones & Accessories','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111111'),
     '11111111-1111-1111-1111-111111111111'),
    ('11111111-1111-1111-1111-111111111114', 'Cameras & Photography',
     (SELECT c.slug || '-' || lower(regexp_replace('Cameras & Photography','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111111'),
     '11111111-1111-1111-1111-111111111111'),
    ('11111111-1111-1111-1111-111111111115', 'Audio & Video',
     (SELECT c.slug || '-' || lower(regexp_replace('Audio & Video','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111111'),
     '11111111-1111-1111-1111-111111111111'),
    ('11111111-1111-1111-1111-111111111116', 'Gaming',
     (SELECT c.slug || '-' || lower(regexp_replace('Gaming','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111111'),
     '11111111-1111-1111-1111-111111111111')
ON CONFLICT (id) DO NOTHING;

-- Computers sub-subcategories
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('11111111-1111-1111-1111-111111111201', 'Laptops',
     (SELECT c.slug || '-' || lower(regexp_replace('Laptops','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111112'),
     '11111111-1111-1111-1111-111111111112'),
    ('11111111-1111-1111-1111-111111111202', 'Desktops',
     (SELECT c.slug || '-' || lower(regexp_replace('Desktops','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111112'),
     '11111111-1111-1111-1111-111111111112'),
    ('11111111-1111-1111-1111-111111111203', 'Tablets',
     (SELECT c.slug || '-' || lower(regexp_replace('Tablets','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111112'),
     '11111111-1111-1111-1111-111111111112'),
    ('11111111-1111-1111-1111-111111111204', 'Components',
     (SELECT c.slug || '-' || lower(regexp_replace('Components','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111112'),
     '11111111-1111-1111-1111-111111111112'),
    ('11111111-1111-1111-1111-111111111205', 'Accessories & Peripherals',
     (SELECT c.slug || '-' || lower(regexp_replace('Accessories & Peripherals','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111112'),
     '11111111-1111-1111-1111-111111111112')
ON CONFLICT (id) DO NOTHING;

-- Components
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('11111111-1111-1111-1111-111111111301', 'CPU',
     (SELECT c.slug || '-' || lower(regexp_replace('CPU','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111204'),
     '11111111-1111-1111-1111-111111111204'),
    ('11111111-1111-1111-1111-111111111302', 'GPU',
     (SELECT c.slug || '-' || lower(regexp_replace('GPU','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111204'),
     '11111111-1111-1111-1111-111111111204'),
    ('11111111-1111-1111-1111-111111111303', 'RAM & Storage',
     (SELECT c.slug || '-' || lower(regexp_replace('RAM & Storage','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111204'),
     '11111111-1111-1111-1111-111111111204'),
    ('11111111-1111-1111-1111-111111111304', 'Motherboards',
     (SELECT c.slug || '-' || lower(regexp_replace('Motherboards','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111204'),
     '11111111-1111-1111-1111-111111111204'),
    ('11111111-1111-1111-1111-111111111305', 'Power Supplies',
     (SELECT c.slug || '-' || lower(regexp_replace('Power Supplies','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111204'),
     '11111111-1111-1111-1111-111111111204'),
    ('11111111-1111-1111-1111-111111111306', 'Cooling',
     (SELECT c.slug || '-' || lower(regexp_replace('Cooling','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111204')
     , '11111111-1111-1111-1111-111111111204')
ON CONFLICT (id) DO NOTHING;

-- Accessories & Peripherals
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('11111111-1111-1111-1111-111111111307', 'Keyboards',
     (SELECT c.slug || '-' || lower(regexp_replace('Keyboards','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111205'),
     '11111111-1111-1111-1111-111111111205'),
    ('11111111-1111-1111-1111-111111111308', 'Mice',
     (SELECT c.slug || '-' || lower(regexp_replace('Mice','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111205'),
     '11111111-1111-1111-1111-111111111205'),
    ('11111111-1111-1111-1111-111111111309', 'Monitors',
     (SELECT c.slug || '-' || lower(regexp_replace('Monitors','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111205'),
     '11111111-1111-1111-1111-111111111205'),
    ('11111111-1111-1111-1111-111111111310', 'Cables & Hubs',
     (SELECT c.slug || '-' || lower(regexp_replace('Cables & Hubs','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111205'),
     '11111111-1111-1111-1111-111111111205'),
    ('11111111-1111-1111-1111-111111111311', 'Docking Stations',
     (SELECT c.slug || '-' || lower(regexp_replace('Docking Stations','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111205'),
     '11111111-1111-1111-1111-111111111205')
ON CONFLICT (id) DO NOTHING;

-- Phones & Accessories
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('11111111-1111-1111-1111-111111111312', 'Smartphones',
     (SELECT c.slug || '-' || lower(regexp_replace('Smartphones','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111113'),
     '11111111-1111-1111-1111-111111111113'),
    ('11111111-1111-1111-1111-111111111313', 'Cases & Covers',
     (SELECT c.slug || '-' || lower(regexp_replace('Cases & Covers','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111113'),
     '11111111-1111-1111-1111-111111111113'),
    ('11111111-1111-1111-1111-111111111314', 'Chargers & Cables',
     (SELECT c.slug || '-' || lower(regexp_replace('Chargers & Cables','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111113'),
     '11111111-1111-1111-1111-111111111113'),
    ('11111111-1111-1111-1111-111111111315', 'Headphones & Earphones',
     (SELECT c.slug || '-' || lower(regexp_replace('Headphones & Earphones','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111113'),
     '11111111-1111-1111-1111-111111111113')
ON CONFLICT (id) DO NOTHING;

-- Cameras
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('11111111-1111-1111-1111-111111111316', 'DSLR & Mirrorless',
     (SELECT c.slug || '-' || lower(regexp_replace('DSLR & Mirrorless','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111114'),
     '11111111-1111-1111-1111-111111111114'),
    ('11111111-1111-1111-1111-111111111317', 'Lenses & Filters',
     (SELECT c.slug || '-' || lower(regexp_replace('Lenses & Filters','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111114'),
     '11111111-1111-1111-1111-111111111114'),
    ('11111111-1111-1111-1111-111111111318', 'Tripods & Mounts',
     (SELECT c.slug || '-' || lower(regexp_replace('Tripods & Mounts','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111114'),
     '11111111-1111-1111-1111-111111111114'),
    ('11111111-1111-1111-1111-111111111319', 'Camera Bags & Storage',
     (SELECT c.slug || '-' || lower(regexp_replace('Camera Bags & Storage','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111114'),
     '11111111-1111-1111-1111-111111111114')
ON CONFLICT (id) DO NOTHING;

-- Audio & Video
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('11111111-1111-1111-1111-111111111320', 'Speakers',
     (SELECT c.slug || '-' || lower(regexp_replace('Speakers','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111115'),
     '11111111-1111-1111-1111-111111111115'),
    ('11111111-1111-1111-1111-111111111321', 'Headphones',
     (SELECT c.slug || '-' || lower(regexp_replace('Headphones','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111115'),
     '11111111-1111-1111-1111-111111111115'),
    ('11111111-1111-1111-1111-111111111322', 'TVs & Projectors',
     (SELECT c.slug || '-' || lower(regexp_replace('TVs & Projectors','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111115'),
     '11111111-1111-1111-1111-111111111115'),
    ('11111111-1111-1111-1111-111111111323', 'Streaming Devices',
     (SELECT c.slug || '-' || lower(regexp_replace('Streaming Devices','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111115'),
     '11111111-1111-1111-1111-111111111115')
ON CONFLICT (id) DO NOTHING;

-- Gaming
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('11111111-1111-1111-1111-111111111324', 'Consoles',
     (SELECT c.slug || '-' || lower(regexp_replace('Consoles','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111116'),
     '11111111-1111-1111-1111-111111111116'),
    ('11111111-1111-1111-1111-111111111325', 'Games',
     (SELECT c.slug || '-' || lower(regexp_replace('Games','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111116'),
     '11111111-1111-1111-1111-111111111116'),
    ('11111111-1111-1111-1111-111111111326', 'Controllers & Accessories',
     (SELECT c.slug || '-' || lower(regexp_replace('Controllers & Accessories','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111116'),
     '11111111-1111-1111-1111-111111111116'),
    ('11111111-1111-1111-1111-111111111327', 'VR Headsets',
     (SELECT c.slug || '-' || lower(regexp_replace('VR Headsets','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '11111111-1111-1111-1111-111111111116'),
     '11111111-1111-1111-1111-111111111116')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Fashion & Accessories
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('22222222-2222-2222-2222-222222222401', 'Men''s Fashion',
     (SELECT c.slug || '-' || lower(regexp_replace('Men''s Fashion','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '22222222-2222-2222-2222-222222222222'),
     '22222222-2222-2222-2222-222222222222'),
    ('22222222-2222-2222-2222-222222222402', 'Women''s Fashion',
     (SELECT c.slug || '-' || lower(regexp_replace('Women''s Fashion','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '22222222-2222-2222-2222-222222222222'),
     '22222222-2222-2222-2222-222222222222'),
    ('22222222-2222-2222-2222-222222222403', 'Jewelry & Watches',
     (SELECT c.slug || '-' || lower(regexp_replace('Jewelry & Watches','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '22222222-2222-2222-2222-222222222222'),
     '22222222-2222-2222-2222-222222222222'),
    ('22222222-2222-2222-2222-222222222404', 'Bags & Luggage',
     (SELECT c.slug || '-' || lower(regexp_replace('Bags & Luggage','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '22222222-2222-2222-2222-222222222222'),
     '22222222-2222-2222-2222-222222222222'),
    ('22222222-2222-2222-2222-222222222405', 'Accessories',
     (SELECT c.slug || '-' || lower(regexp_replace('Accessories','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '22222222-2222-2222-2222-222222222222'),
     '22222222-2222-2222-2222-222222222222')
ON CONFLICT (id) DO NOTHING;

-- Men
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('22222222-2222-2222-2222-222222225501', 'Tops',
     (SELECT c.slug || '-' || lower(regexp_replace('Tops','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '22222222-2222-2222-2222-222222222401'),
     '22222222-2222-2222-2222-222222222401'),
    ('22222222-2222-2222-2222-222222225502', 'Bottoms',
     (SELECT c.slug || '-' || lower(regexp_replace('Bottoms','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '22222222-2222-2222-2222-222222222401'),
     '22222222-2222-2222-2222-222222222401'),
    ('22222222-2222-2222-2222-222222225503', 'Shoes',
     (SELECT c.slug || '-' || lower(regexp_replace('Shoes','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '22222222-2222-2222-2222-222222222401'),
     '22222222-2222-2222-2222-222222222401'),
    ('22222222-2222-2222-2222-222222225504', 'Underwear',
     (SELECT c.slug || '-' || lower(regexp_replace('Underwear','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '22222222-2222-2222-2222-222222222401'),
     '22222222-2222-2222-2222-222222222401'),
    ('22222222-2222-2222-2222-222222225505', 'Outerwear',
     (SELECT c.slug || '-' || lower(regexp_replace('Outerwear','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '22222222-2222-2222-2222-222222222401'),
     '22222222-2222-2222-2222-222222222401')
ON CONFLICT (id) DO NOTHING;

-- Women
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('22222222-2222-2222-2222-222222225506', 'Tops',
     (SELECT c.slug || '-' || lower(regexp_replace('Tops','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '22222222-2222-2222-2222-222222222402'),
     '22222222-2222-2222-2222-222222222402'),
    ('22222222-2222-2222-2222-222222225507', 'Bottoms',
     (SELECT c.slug || '-' || lower(regexp_replace('Bottoms','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '22222222-2222-2222-2222-222222222402'),
     '22222222-2222-2222-2222-222222222402'),
    ('22222222-2222-2222-2222-222222225508', 'Dresses',
     (SELECT c.slug || '-' || lower(regexp_replace('Dresses','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '22222222-2222-2222-2222-222222222402'),
     '22222222-2222-2222-2222-222222222402'),
    ('22222222-2222-2222-2222-222222225509', 'Shoes',
     (SELECT c.slug || '-' || lower(regexp_replace('Shoes','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '22222222-2222-2222-2222-222222222402'),
     '22222222-2222-2222-2222-222222222402'),
    ('22222222-2222-2222-2222-222222225510', 'Underwear',
     (SELECT c.slug || '-' || lower(regexp_replace('Underwear','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '22222222-2222-2222-2222-222222222402'),
     '22222222-2222-2222-2222-222222222402'),
    ('22222222-2222-2222-2222-222222225511', 'Outerwear',
     (SELECT c.slug || '-' || lower(regexp_replace('Outerwear','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '22222222-2222-2222-2222-222222222402'),
     '22222222-2222-2222-2222-222222222402')
ON CONFLICT (id) DO NOTHING;

-- Jewelry & Watches
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('22222222-2222-2222-2222-222222225512', 'Rings',
     (SELECT c.slug || '-' || lower(regexp_replace('Rings','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '22222222-2222-2222-2222-222222222403'),
     '22222222-2222-2222-2222-222222222403'),
    ('22222222-2222-2222-2222-222222225513', 'Necklaces',
     (SELECT c.slug || '-' || lower(regexp_replace('Necklaces','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '22222222-2222-2222-2222-222222222403'),
     '22222222-2222-2222-2222-222222222403'),
    ('22222222-2222-2222-2222-222222225514', 'Bracelets',
     (SELECT c.slug || '-' || lower(regexp_replace('Bracelets','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '22222222-2222-2222-2222-222222222403'),
     '22222222-2222-2222-2222-222222222403'),
    ('22222222-2222-2222-2222-222222225515', 'Watches',
     (SELECT c.slug || '-' || lower(regexp_replace('Watches','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '22222222-2222-2222-2222-222222222403'),
     '22222222-2222-2222-2222-222222222403')
ON CONFLICT (id) DO NOTHING;

-- Bags & Luggage
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('22222222-2222-2222-2222-222222225516', 'Handbags',
     (SELECT c.slug || '-' || lower(regexp_replace('Handbags','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '22222222-2222-2222-2222-222222222404'),
     '22222222-2222-2222-2222-222222222404'),
    ('22222222-2222-2222-2222-222222225517', 'Backpacks',
     (SELECT c.slug || '-' || lower(regexp_replace('Backpacks','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '22222222-2222-2222-2222-222222222404'),
     '22222222-2222-2222-2222-222222222404'),
    ('22222222-2222-2222-2222-222222225518', 'Suitcases',
     (SELECT c.slug || '-' || lower(regexp_replace('Suitcases','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '22222222-2222-2222-2222-222222222404'),
     '22222222-2222-2222-2222-222222222404')
ON CONFLICT (id) DO NOTHING;

-- Accessories
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('22222222-2222-2222-2222-222222225519', 'Belts',
     (SELECT c.slug || '-' || lower(regexp_replace('Belts','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '22222222-2222-2222-2222-222222222405'),
     '22222222-2222-2222-2222-222222222405'),
    ('22222222-2222-2222-2222-222222225520', 'Hats & Caps',
     (SELECT c.slug || '-' || lower(regexp_replace('Hats & Caps','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '22222222-2222-2222-2222-222222222405'),
     '22222222-2222-2222-2222-222222222405'),
    ('22222222-2222-2222-2222-222222225521', 'Sunglasses',
     (SELECT c.slug || '-' || lower(regexp_replace('Sunglasses','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '22222222-2222-2222-2222-222222222405'),
     '22222222-2222-2222-2222-222222222405'),
    ('22222222-2222-2222-2222-222222225522', 'Scarves',
     (SELECT c.slug || '-' || lower(regexp_replace('Scarves','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '22222222-2222-2222-2222-222222222405'),
     '22222222-2222-2222-2222-222222222405')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Home & Garden
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('33333333-3333-3333-3333-333333336001', 'Furniture',
     (SELECT c.slug || '-' || lower(regexp_replace('Furniture','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '33333333-3333-3333-3333-333333333333'),
     '33333333-3333-3333-3333-333333333333'),
    ('33333333-3333-3333-3333-333333336002', 'Kitchen & Dining',
     (SELECT c.slug || '-' || lower(regexp_replace('Kitchen & Dining','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '33333333-3333-3333-3333-333333333333'),
     '33333333-3333-3333-3333-333333333333'),
    ('33333333-3333-3333-3333-333333336003', 'Garden & Outdoor',
     (SELECT c.slug || '-' || lower(regexp_replace('Garden & Outdoor','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '33333333-3333-3333-3333-333333333333'),
     '33333333-3333-3333-3333-333333333333'),
    ('33333333-3333-3333-3333-333333336004', 'Home Decor',
     (SELECT c.slug || '-' || lower(regexp_replace('Home Decor','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '33333333-3333-3333-3333-333333333333'),
     '33333333-3333-3333-3333-333333333333'),
    ('33333333-3333-3333-3333-333333336005', 'Bedding & Bath',
     (SELECT c.slug || '-' || lower(regexp_replace('Bedding & Bath','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '33333333-3333-3333-3333-333333333333'),
     '33333333-3333-3333-3333-333333333333')
ON CONFLICT (id) DO NOTHING;

-- Furniture
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('33333333-3333-3333-3333-333333337001', 'Living Room',
     (SELECT c.slug || '-' || lower(regexp_replace('Living Room','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '33333333-3333-3333-3333-333333336001'),
     '33333333-3333-3333-3333-333333336001'),
    ('33333333-3333-3333-3333-333333337002', 'Bedroom',
     (SELECT c.slug || '-' || lower(regexp_replace('Bedroom','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '33333333-3333-3333-3333-333333336001'),
     '33333333-3333-3333-3333-333333336001'),
    ('33333333-3333-3333-3333-333333337003', 'Office Furniture',
     (SELECT c.slug || '-' || lower(regexp_replace('Office Furniture','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '33333333-3333-3333-3333-333333336001'),
     '33333333-3333-3333-3333-333333336001')
ON CONFLICT (id) DO NOTHING;

-- Kitchen & Dining
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('33333333-3333-3333-3333-333333337041', 'Cookware',
     (SELECT c.slug || '-' || lower(regexp_replace('Cookware','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '33333333-3333-3333-3333-333333336002'),
     '33333333-3333-3333-3333-333333336002'),
    ('33333333-3333-3333-3333-333333337042', 'Tableware',
     (SELECT c.slug || '-' || lower(regexp_replace('Tableware','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '33333333-3333-3333-3333-333333336002'),
     '33333333-3333-3333-3333-333333336002'),
    ('33333333-3333-3333-3333-333333337043', 'Small Appliances',
     (SELECT c.slug || '-' || lower(regexp_replace('Small Appliances','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '33333333-3333-3333-3333-333333336002'),
     '33333333-3333-3333-3333-333333336002')
ON CONFLICT (id) DO NOTHING;

-- Garden & Outdoor
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('33333333-3333-3333-3333-333333337071', 'Garden Tools',
     (SELECT c.slug || '-' || lower(regexp_replace('Garden Tools','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '33333333-3333-3333-3333-333333336003'),
     '33333333-3333-3333-3333-333333336003'),
    ('33333333-3333-3333-3333-333333337072', 'Outdoor Furniture',
     (SELECT c.slug || '-' || lower(regexp_replace('Outdoor Furniture','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '33333333-3333-3333-3333-333333336003'),
     '33333333-3333-3333-3333-333333336003'),
    ('33333333-3333-3333-3333-333333337073', 'Grills & BBQ',
     (SELECT c.slug || '-' || lower(regexp_replace('Grills & BBQ','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '33333333-3333-3333-3333-333333336003'),
     '33333333-3333-3333-3333-333333336003')
ON CONFLICT (id) DO NOTHING;

-- Home Decor
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('33333333-3333-3333-3333-333333337101', 'Lighting',
     (SELECT c.slug || '-' || lower(regexp_replace('Lighting','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '33333333-3333-3333-3333-333333336004'),
     '33333333-3333-3333-3333-333333336004'),
    ('33333333-3333-3333-3333-333333337102', 'Rugs & Carpets',
     (SELECT c.slug || '-' || lower(regexp_replace('Rugs & Carpets','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '33333333-3333-3333-3333-333333336004'),
     '33333333-3333-3333-3333-333333336004'),
    ('33333333-3333-3333-3333-333333337103', 'Wall Art & Decor',
     (SELECT c.slug || '-' || lower(regexp_replace('Wall Art & Decor','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '33333333-3333-3333-3333-333333336004'),
     '33333333-3333-3333-3333-333333336004')
ON CONFLICT (id) DO NOTHING;

-- Bedding & Bath
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('33333333-3333-3333-3333-333333337131', 'Bedding',
     (SELECT c.slug || '-' || lower(regexp_replace('Bedding','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '33333333-3333-3333-3333-333333336005'),
     '33333333-3333-3333-3333-333333336005'),
    ('33333333-3333-3333-3333-333333337132', 'Bath',
     (SELECT c.slug || '-' || lower(regexp_replace('Bath','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '33333333-3333-3333-3333-333333336005'),
     '33333333-3333-3333-3333-333333336005')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Sports & Outdoors
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('44444444-4444-4444-4444-444444448001', 'Fitness',
     (SELECT c.slug || '-' || lower(regexp_replace('Fitness','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '44444444-4444-4444-4444-444444444444'),
     '44444444-4444-4444-4444-444444444444'),
    ('44444444-4444-4444-4444-444444448002', 'Outdoor Recreation',
     (SELECT c.slug || '-' || lower(regexp_replace('Outdoor Recreation','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '44444444-4444-4444-4444-444444444444'),
     '44444444-4444-4444-4444-444444444444'),
    ('44444444-4444-4444-4444-444444448003', 'Team Sports',
     (SELECT c.slug || '-' || lower(regexp_replace('Team Sports','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '44444444-4444-4444-4444-444444444444'),
     '44444444-4444-4444-4444-444444444444'),
    ('44444444-4444-4444-4444-444444448004', 'Cycling',
     (SELECT c.slug || '-' || lower(regexp_replace('Cycling','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '44444444-4444-4444-4444-444444444444'),
     '44444444-4444-4444-4444-444444444444')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Automotive
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('66666666-6666-6666-6666-666666669001', 'Car Parts & Accessories',
     (SELECT c.slug || '-' || lower(regexp_replace('Car Parts & Accessories','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '66666666-6666-6666-6666-666666666666'),
     '66666666-6666-6666-6666-666666666666'),
    ('66666666-6666-6666-6666-666666669002', 'Motorcycles & Scooters',
     (SELECT c.slug || '-' || lower(regexp_replace('Motorcycles & Scooters','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '66666666-6666-6666-6666-666666666666'),
     '66666666-6666-6666-6666-666666666666'),
    ('66666666-6666-6666-6666-666666669003', 'Tires & Wheels',
     (SELECT c.slug || '-' || lower(regexp_replace('Tires & Wheels','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '66666666-6666-6666-6666-666666666666'),
     '66666666-6666-6666-6666-666666666666'),
    ('66666666-6666-6666-6666-666666669004', 'Car Electronics',
     (SELECT c.slug || '-' || lower(regexp_replace('Car Electronics','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '66666666-6666-6666-6666-666666666666'),
     '66666666-6666-6666-6666-666666666666'),
    ('66666666-6666-6666-6666-666666669005', 'Tools & Garage',
     (SELECT c.slug || '-' || lower(regexp_replace('Tools & Garage','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '66666666-6666-6666-6666-666666666666'),
     '66666666-6666-6666-6666-666666666666')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Toys & Games
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('77777777-7777-7777-7777-777777779001', 'Action Figures & Collectibles',
     (SELECT c.slug || '-' || lower(regexp_replace('Action Figures & Collectibles','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '77777777-7777-7777-7777-777777777777'),
     '77777777-7777-7777-7777-777777777777'),
    ('77777777-7777-7777-7777-777777779002', 'Board Games & Puzzles',
     (SELECT c.slug || '-' || lower(regexp_replace('Board Games & Puzzles','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '77777777-7777-7777-7777-777777777777'),
     '77777777-7777-7777-7777-777777777777'),
    ('77777777-7777-7777-7777-777777779003', 'Dolls & Accessories',
     (SELECT c.slug || '-' || lower(regexp_replace('Dolls & Accessories','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '77777777-7777-7777-7777-777777777777'),
     '77777777-7777-7777-7777-777777777777'),
    ('77777777-7777-7777-7777-777777779004', 'Outdoor Play',
     (SELECT c.slug || '-' || lower(regexp_replace('Outdoor Play','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '77777777-7777-7777-7777-777777777777'),
     '77777777-7777-7777-7777-777777777777'),
    ('77777777-7777-7777-7777-777777779005', 'Educational Toys',
     (SELECT c.slug || '-' || lower(regexp_replace('Educational Toys','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '77777777-7777-7777-7777-777777777777'),
     '77777777-7777-7777-7777-777777777777')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Health & Beauty
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('88888888-8888-8888-8888-888888889001', 'Makeup',
     (SELECT c.slug || '-' || lower(regexp_replace('Makeup','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '88888888-8888-8888-8888-888888888888'),
     '88888888-8888-8888-8888-888888888888'),
    ('88888888-8888-8888-8888-888888889002', 'Skincare',
     (SELECT c.slug || '-' || lower(regexp_replace('Skincare','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '88888888-8888-8888-8888-888888888888'),
     '88888888-8888-8888-8888-888888888888'),
    ('88888888-8888-8888-8888-888888889003', 'Hair Care',
     (SELECT c.slug || '-' || lower(regexp_replace('Hair Care','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '88888888-8888-8888-8888-888888888888'),
     '88888888-8888-8888-8888-888888888888'),
    ('88888888-8888-8888-8888-888888889004', 'Personal Care',
     (SELECT c.slug || '-' || lower(regexp_replace('Personal Care','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '88888888-8888-8888-8888-888888888888'),
     '88888888-8888-8888-8888-888888888888'),
    ('88888888-8888-8888-8888-888888889005', 'Supplements',
     (SELECT c.slug || '-' || lower(regexp_replace('Supplements','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '88888888-8888-8888-8888-888888888888'),
     '88888888-8888-8888-8888-888888888888')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Art & Collectibles
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('99999999-9999-9999-9999-999999999201', 'Paintings',
     (SELECT c.slug || '-' || lower(regexp_replace('Paintings','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '99999999-9999-9999-9999-999999999999'),
     '99999999-9999-9999-9999-999999999999'),
    ('99999999-9999-9999-9999-999999999202', 'Sculptures',
     (SELECT c.slug || '-' || lower(regexp_replace('Sculptures','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '99999999-9999-9999-9999-999999999999'),
     '99999999-9999-9999-9999-999999999999'),
    ('99999999-9999-9999-9999-999999999203', 'Collectibles',
     (SELECT c.slug || '-' || lower(regexp_replace('Collectibles','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = '99999999-9999-9999-9999-999999999999'),
     '99999999-9999-9999-9999-999999999999')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Musical Instruments
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', 'Guitars',
     (SELECT c.slug || '-' || lower(regexp_replace('Guitars','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', 'Keyboards & Pianos',
     (SELECT c.slug || '-' || lower(regexp_replace('Keyboards & Pianos','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3', 'Drums & Percussion',
     (SELECT c.slug || '-' || lower(regexp_replace('Drums & Percussion','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4', 'Wind Instruments',
     (SELECT c.slug || '-' || lower(regexp_replace('Wind Instruments','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Tools & Hardware
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'Hand Tools',
     (SELECT c.slug || '-' || lower(regexp_replace('Hand Tools','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
     'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', 'Power Tools',
     (SELECT c.slug || '-' || lower(regexp_replace('Power Tools','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
     'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3', 'Tool Storage',
     (SELECT c.slug || '-' || lower(regexp_replace('Tool Storage','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
     'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb4', 'Safety Equipment',
     (SELECT c.slug || '-' || lower(regexp_replace('Safety Equipment','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
     'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Pet Supplies
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('cccccccc-cccc-cccc-cccc-ccccccccccc1', 'Dog Supplies',
     (SELECT c.slug || '-' || lower(regexp_replace('Dog Supplies','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = 'cccccccc-cccc-cccc-cccc-cccccccccccc'),
     'cccccccc-cccc-cccc-cccc-cccccccccccc'),
    ('cccccccc-cccc-cccc-cccc-ccccccccccc2', 'Cat Supplies',
     (SELECT c.slug || '-' || lower(regexp_replace('Cat Supplies','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = 'cccccccc-cccc-cccc-cccc-cccccccccccc'),
     'cccccccc-cccc-cccc-cccc-cccccccccccc'),
    ('cccccccc-cccc-cccc-cccc-ccccccccccc3', 'Fish & Aquatic',
     (SELECT c.slug || '-' || lower(regexp_replace('Fish & Aquatic','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = 'cccccccc-cccc-cccc-cccc-cccccccccccc'),
     'cccccccc-cccc-cccc-cccc-cccccccccccc'),
    ('cccccccc-cccc-cccc-cccc-ccccccccccc4', 'Small Pets',
     (SELECT c.slug || '-' || lower(regexp_replace('Small Pets','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = 'cccccccc-cccc-cccc-cccc-cccccccccccc'),
     'cccccccc-cccc-cccc-cccc-cccccccccccc'),
    ('cccccccc-cccc-cccc-cccc-ccccccccccc5', 'Birds',
     (SELECT c.slug || '-' || lower(regexp_replace('Birds','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = 'cccccccc-cccc-cccc-cccc-cccccccccccc'),
     'cccccccc-cccc-cccc-cccc-cccccccccccc')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Baby & Kids
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('dddddddd-dddd-dddd-dddd-ddddddddddd1', 'Clothing',
     (SELECT c.slug || '-' || lower(regexp_replace('Clothing','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = 'dddddddd-dddd-dddd-dddd-dddddddddddd'),
     'dddddddd-dddd-dddd-dddd-dddddddddddd'),
    ('dddddddd-dddd-dddd-dddd-ddddddddddd2', 'Toys',
     (SELECT c.slug || '-' || lower(regexp_replace('Toys','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = 'dddddddd-dddd-dddd-dddd-dddddddddddd'),
     'dddddddd-dddd-dddd-dddd-dddddddddddd'),
    ('dddddddd-dddd-dddd-dddd-ddddddddddd3', 'Gear & Accessories',
     (SELECT c.slug || '-' || lower(regexp_replace('Gear & Accessories','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = 'dddddddd-dddd-dddd-dddd-dddddddddddd'),
     'dddddddd-dddd-dddd-dddd-dddddddddddd'),
    ('dddddddd-dddd-dddd-dddd-ddddddddddd4', 'Feeding & Nursing',
     (SELECT c.slug || '-' || lower(regexp_replace('Feeding & Nursing','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = 'dddddddd-dddd-dddd-dddd-dddddddddddd'),
     'dddddddd-dddd-dddd-dddd-dddddddddddd')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Office & Business
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee1', 'Office Supplies',
     (SELECT c.slug || '-' || lower(regexp_replace('Office Supplies','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee'),
     'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee'),
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee2', 'Stationery',
     (SELECT c.slug || '-' || lower(regexp_replace('Stationery','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee'),
     'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee'),
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee3', 'Furniture',
     (SELECT c.slug || '-' || lower(regexp_replace('Furniture','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee'),
     'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee'),
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee4', 'Technology',
     (SELECT c.slug || '-' || lower(regexp_replace('Technology','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee'),
     'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Food & Beverages
INSERT INTO category (id, name, slug, parent_id) VALUES
    ('ffffffff-ffff-ffff-ffff-fffffffffff1', 'Beverages',
     (SELECT c.slug || '-' || lower(regexp_replace('Beverages','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = 'ffffffff-ffff-ffff-ffff-ffffffffffff'),
     'ffffffff-ffff-ffff-ffff-ffffffffffff'),
    ('ffffffff-ffff-ffff-ffff-fffffffffff2', 'Snacks',
     (SELECT c.slug || '-' || lower(regexp_replace('Snacks','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = 'ffffffff-ffff-ffff-ffff-ffffffffffff'),
     'ffffffff-ffff-ffff-ffff-ffffffffffff'),
    ('ffffffff-ffff-ffff-ffff-fffffffffff3', 'Groceries',
     (SELECT c.slug || '-' || lower(regexp_replace('Groceries','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = 'ffffffff-ffff-ffff-ffff-ffffffffffff'),
     'ffffffff-ffff-ffff-ffff-ffffffffffff'),
    ('ffffffff-ffff-ffff-ffff-fffffffffff4', 'Specialty Foods',
     (SELECT c.slug || '-' || lower(regexp_replace('Specialty Foods','[^a-zA-Z0-9]+','-','g')) FROM category c WHERE c.id = 'ffffffff-ffff-ffff-ffff-ffffffffffff'),
     'ffffffff-ffff-ffff-ffff-ffffffffffff')
ON CONFLICT (id) DO NOTHING;

-- Listing Table
CREATE TABLE IF NOT EXISTS listing (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    business_id UUID REFERENCES business(id),
    created_by UUID REFERENCES users(id),
    category_id UUID NOT NULL REFERENCES category(id),
    price DOUBLE PRECISION NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    sold BOOLEAN DEFAULT FALSE,
    sold_at TIMESTAMP,
    sold_price DECIMAL(10,2),
    city_id UUID REFERENCES city(id),
    custom_city VARCHAR(100),
    condition VARCHAR(32),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    archived BOOLEAN DEFAULT FALSE,
    CONSTRAINT unique_user_listing_title UNIQUE (user_id, title),
    CONSTRAINT unique_business_listing_title UNIQUE (business_id, title),
    CONSTRAINT listing_user_or_business CHECK ((user_id IS NOT NULL) OR (business_id IS NOT NULL))
);

-- Listing Image Table
CREATE TABLE IF NOT EXISTS listing_image (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES listing(id) ON DELETE CASCADE,
    image TEXT NOT NULL
);

-- Trust Rating System
CREATE TABLE trust_rating (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    overall_score DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    verified_id BOOLEAN NOT NULL DEFAULT FALSE,
    profile_score DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    verification_score DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    review_score DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    transaction_score DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    total_reviews INTEGER NOT NULL DEFAULT 0,
    positive_reviews INTEGER NOT NULL DEFAULT 0,
    total_transactions INTEGER NOT NULL DEFAULT 0,
    successful_transactions INTEGER NOT NULL DEFAULT 0,
    last_calculated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Business Trust Rating System
CREATE TABLE IF NOT EXISTS business_trust_rating (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL UNIQUE REFERENCES business(id) ON DELETE CASCADE,
    overall_score DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    verified_with_third_party BOOLEAN NOT NULL DEFAULT FALSE,
    verification_score DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    profile_score DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    review_score DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    transaction_score DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    total_reviews INTEGER NOT NULL DEFAULT 0,
    positive_reviews INTEGER NOT NULL DEFAULT 0,
    total_transactions INTEGER NOT NULL DEFAULT 0,
    successful_transactions INTEGER NOT NULL DEFAULT 0,
    last_calculated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Verification Documents table
CREATE TABLE verification_document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    business_id UUID REFERENCES business(id),
    document_type VARCHAR(50) NOT NULL,
    document_url TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    verified_at TIMESTAMP,
    verified_by UUID REFERENCES users(id),
    rejection_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Transaction System table
CREATE TABLE "transaction" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES listing(id) ON DELETE CASCADE,
    seller_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    buyer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    business_id UUID REFERENCES business(id),
    quantity INTEGER NOT NULL DEFAULT 1,
    sale_price DECIMAL(10,2) NOT NULL,
    sale_date TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Review System table
CREATE TABLE review (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reviewer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reviewed_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    transaction_id UUID NOT NULL REFERENCES "transaction"(id) ON DELETE CASCADE,
    business_id UUID REFERENCES business(id),
    rating DECIMAL(2,1) NOT NULL CHECK (rating >= 0.5 AND rating <= 5.0),
    comment TEXT,
    is_positive BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (reviewer_id, transaction_id)
);

-- Profile Completion tracking
CREATE TABLE profile_completion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Subscription System table
CREATE TABLE subscription (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    business_id UUID REFERENCES business(id) ON DELETE CASCADE,
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
    payfast_profile_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- StoreBranding Table
CREATE TABLE IF NOT EXISTS store_branding (
    business_id UUID PRIMARY KEY REFERENCES business(id) ON DELETE CASCADE,
    logo_url TEXT,
    banner_url TEXT,
    theme_color VARCHAR(32),
    about TEXT,
    store_name VARCHAR(255),
    primary_color VARCHAR(32),
    secondary_color VARCHAR(32),
    light_or_dark VARCHAR(16),
    text_color VARCHAR(32),
    card_text_color VARCHAR(32),
    background_color VARCHAR(32),
    version BIGINT
);

-- Notification Table
CREATE TABLE IF NOT EXISTS notification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    data TEXT,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    action_required BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_trust_rating_user_id ON trust_rating(user_id);
CREATE INDEX idx_verification_document_user_id ON verification_document(user_id);
CREATE INDEX idx_verification_document_status ON verification_document(status);
CREATE INDEX idx_review_reviewed_user_id ON review(reviewed_user_id);
CREATE INDEX idx_transaction_listing_id ON "transaction"(listing_id);
CREATE INDEX idx_transaction_seller_id ON "transaction"(seller_id);
CREATE INDEX idx_transaction_status ON "transaction"(status);
CREATE INDEX idx_transaction_business_id ON "transaction"(business_id);
CREATE INDEX idx_transaction_quantity ON "transaction"(quantity);
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
CREATE INDEX idx_listing_sold_at ON listing(sold_at);

-- Add reseller and admin user
INSERT INTO "users" (username, email, password, role, first_name, last_name, bio, city_id, contact_number, created_at, profile_image_url)
VALUES
    ('resellerjoe', 'reseller@marketplace.com', '$2a$10$7QJ8QwQwQwQwQwQwQwQwQeQwQwQwQwQwQwQwQwQwQwQwQwQwQw','HAS_ACCOUNT',
     'Joe','Reseller','We sell the best gadgets and accessories!', (SELECT id FROM city WHERE name = 'Cape Town'),'+27111222333',NOW(), 'https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?q=80&w=880&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
    ('admin', 'admin@admin.com', '$2a$10$r1d0EfJx3L7OSW9ofStBPueFKHXQtyrUVhwf09h4pLOEOSMKGJmPm', 'SUBSCRIBED',
     'Admin', 'User', 'System administrator and marketplace enthusiast. I love testing new features and helping users.',  (SELECT id FROM city WHERE name = 'Johannesburg'), '+27-10-555-0100', NOW(), 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?q=80&w=688&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D')
ON CONFLICT (email) DO NOTHING;

-- RESELLER JOE
-- Add business for reseller
INSERT INTO business (name, email, contact_number, address_line1, business_type, city_id, owner_id)
SELECT 'Joe''s Reseller Store', 'reseller@marketplace.com', '+27111222333', '123 Main Street', 'RESELLER',
       (SELECT id FROM city WHERE name = 'Cape Town'), id
FROM "users" WHERE email = 'reseller@marketplace.com'
ON CONFLICT DO NOTHING;

-- Link reseller user to reseller business as OWNER
INSERT INTO business_user (business_id, user_id, role)
SELECT b.id, u.id, 'OWNER'
FROM business b
         JOIN "users" u ON b.owner_id = u.id
WHERE u.email = 'reseller@marketplace.com'
ON CONFLICT DO NOTHING;

-- Add store branding for reseller
INSERT INTO store_branding (business_id, logo_url, theme_color, primary_color, secondary_color, light_or_dark, about, store_name)
SELECT b.id,
       'https://images.unsplash.com/photo-1519125323398-675f0ddb6308?q=80&w=100',
       '#e53e3e',
       '#8a5357',
       '#ffffff',
       'light',
       'Welcome to Joe''s Reseller Store! Find top gadgets and more.',
       'Joe''s Reseller Store'
FROM business b
         JOIN "users" u ON u.id = b.owner_id
WHERE u.email = 'reseller@marketplace.com'
ON CONFLICT (business_id) DO NOTHING;

-- ResellerJoe's Listings
INSERT INTO listing (business_id, created_by, title, description, category_id, price, condition, city_id, custom_city, created_at, expires_at, sold)
SELECT
    b.id,
    u.id,
    'Wireless Earbuds Pro',
    'Brand new wireless earbuds with noise cancellation, 24h battery, and fast charging. Includes case and all accessories.',
    c.id,
    89.99,
    'NEW',
    city.id,
    NULL,
    NOW() - INTERVAL '2 days',
    NOW() + INTERVAL '28 days',
    false
FROM business b
         JOIN users u ON b.owner_id = u.id
         JOIN category c ON c.name = 'Home & Garden'
         JOIN city ON city.name = 'Cape Town'
WHERE u.email = 'reseller@marketplace.com';

INSERT INTO listing (business_id, created_by, title, description, category_id, price, condition, city_id, custom_city, created_at, expires_at, sold)
SELECT
    b.id,
    u.id,
    'Smart LED Desk Lamp',
    'Modern LED desk lamp with adjustable brightness, color temperature, and USB charging port. Perfect for home or office.',
    c.id,
    39.99,
    'NEW',
    city.id,
    NULL,
    NOW() - INTERVAL '1 days',
    NOW() + INTERVAL '29 days',
    false
FROM business b
         JOIN users u ON b.owner_id = u.id
         JOIN category c ON c.name = 'Home & Garden'
         JOIN city ON city.name = 'Cape Town'
WHERE u.email = 'reseller@marketplace.com';

-- Images for ResellerJoe's Listings
INSERT INTO listing_image (listing_id, image)
SELECT l.id, 'https://images.unsplash.com/photo-1722439667098-f32094e3b1d4?q=80&w=735&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'
FROM listing l
         JOIN business b ON l.business_id = b.id
         JOIN users u ON b.owner_id = u.id
WHERE l.title = 'Wireless Earbuds Pro' AND u.email = 'reseller@marketplace.com';

INSERT INTO listing_image (listing_id, image)
SELECT l.id, 'https://plus.unsplash.com/premium_photo-1672166939372-5b16118eee45?q=80&w=627&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'
FROM listing l
         JOIN business b ON l.business_id = b.id
         JOIN users u ON b.owner_id = u.id
WHERE l.title = 'Smart LED Desk Lamp' AND u.email = 'reseller@marketplace.com';

-- ADMIN STORE
-- Add businesses for admin
INSERT INTO business (name, email, contact_number, address_line1, city_id, owner_id, business_type, slug)
SELECT
    CASE
        WHEN u.email = 'admin@admin.com' THEN 'Admin Marketplace Store'
        END,
    u.email,
    u.contact_number,
    CASE
        WHEN u.city_id IS NOT NULL THEN '123 Business Street'
        ELSE '456 Main Road'
        END,
    COALESCE(u.city_id, (SELECT id FROM city WHERE name = 'Johannesburg')),
    u.id,
    'PRO_STORE',
    'admin-store'
FROM "users" u
WHERE u.email IN ('admin@admin.com')
ON CONFLICT DO NOTHING;

-- Link admin user to admin business as OWNER
INSERT INTO business_user (business_id, user_id, role)
SELECT b.id, u.id, 'OWNER'
FROM business b
         JOIN "users" u ON b.owner_id = u.id
WHERE u.email = 'admin@admin.com'
ON CONFLICT DO NOTHING;

-- Store branding for admin (pro store)
INSERT INTO store_branding (business_id, logo_url, banner_url, theme_color, primary_color, secondary_color, light_or_dark, text_color, card_text_color, background_color, about, store_name )
SELECT b.id,
       'https://images.unsplash.com/photo-1614851099518-055a1000e6d5?q=80&w=1470&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D',
       'https://images.unsplash.com/photo-1579548122080-c35fd6820ecb?q=80&w=1740&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D',
       '#6470ff',
       '#2d35a4',
       '#c4c4c4',
       'light',
       '#f39bfd',
       '#eeeeee',
           '#1d2329',
       'Welcome to the Admin Pro Store! We offer the best tech and collectibles.',
       'Admin Pro Store'
FROM business b
         JOIN "users" u ON u.id = b.owner_id
WHERE u.email = 'admin@admin.com'
ON CONFLICT (business_id) DO NOTHING;

-- Insert Admin's Listings
INSERT INTO listing (business_id, created_by, title, description, category_id, price, condition, city_id, custom_city, created_at, expires_at, sold) VALUES
 ((SELECT id FROM business WHERE owner_id = (SELECT id FROM users WHERE email = 'admin@admin.com')), (SELECT id FROM users WHERE email = 'admin@admin.com'), 'iPhone 16 Pro - Mint Condition', 'Mint condition iPhone 16 Pro with original box and accessories.', '11111111-1111-1111-1111-111111111113', 799.99, 'EXCELLENT', (SELECT id FROM city WHERE name = 'Johannesburg'), NULL, NOW() - INTERVAL '30 days', NOW() + INTERVAL '30 days', false),
 ((SELECT id FROM business WHERE owner_id = (SELECT id FROM users WHERE email = 'admin@admin.com')), (SELECT id FROM users WHERE email = 'admin@admin.com'), 'Gaming Laptop - RTX 3060', 'High-performance gaming laptop with RTX 3060 GPU, 16GB RAM, and 512GB SSD.', '11111111-1111-1111-1111-111111111201', 1299.00, 'GOOD', (SELECT id FROM city WHERE name = 'Johannesburg'), NULL, NOW() - INTERVAL '25 days', NOW() + INTERVAL '35 days', false),
 ((SELECT id FROM business WHERE owner_id = (SELECT id FROM users WHERE email = 'admin@admin.com')), (SELECT id FROM users WHERE email = 'admin@admin.com'), 'Vintage Wolf Art Print', 'Rare vintage wolf art print, perfect for collectors and art lovers.', '99999999-9999-9999-9999-999999999201', 150.00, 'EXCELLENT', (SELECT id FROM city WHERE name = 'Johannesburg'), NULL, NOW() - INTERVAL '20 days', NOW() + INTERVAL '40 days', false),
 ((SELECT id FROM business WHERE owner_id = (SELECT id FROM users WHERE email = 'admin@admin.com')), (SELECT id FROM users WHERE email = 'admin@admin.com'), 'Professional Camera Lens', 'Professional camera lens, suitable for portrait and landscape photography.', '11111111-1111-1111-1111-111111111317', 450.00, 'LIKE_NEW', (SELECT id FROM city WHERE name = 'Johannesburg'), NULL, NOW() - INTERVAL '15 days', NOW() + INTERVAL '45 days', false),
 ((SELECT id FROM business WHERE owner_id = (SELECT id FROM users WHERE email = 'admin@admin.com')), (SELECT id FROM users WHERE email = 'admin@admin.com'), 'iPhone 13 Pro Max - Space Gray', 'Space Gray iPhone 13 Pro Max, lightly used, excellent battery health.', '11111111-1111-1111-1111-111111111312', 999.99, 'LIKE_NEW', (SELECT id FROM city WHERE name = 'Johannesburg'), NULL, NOW() - INTERVAL '10 days', NOW() + INTERVAL '50 days', false);

-- Images for Admin's Listings
INSERT INTO listing_image (listing_id, image)
SELECT
    l.id,
    li.image_path
FROM listing l
         JOIN (
    VALUES
        ('iPhone 16 Pro - Mint Condition', 'https://images.unsplash.com/photo-1616410011236-7a42121dd981?q=80&w=1632&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
        ('Gaming Laptop - RTX 3060', 'https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?q=80&w=764&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
        ('Gaming Laptop - RTX 3060', 'https://plus.unsplash.com/premium_photo-1670274609267-202ec99f8620?q=80&w=736&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
        ('Vintage Wolf Art Print', 'https://images.unsplash.com/photo-1518443855757-dfadac7101ae?q=80&w=1470&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
        ('Professional Camera Lens', 'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?q=80&w=764&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
        ('iPhone 13 Pro Max - Space Gray', 'https://images.unsplash.com/photo-1616348436168-de43ad0db179?q=80&w=781&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D')

) AS li(listing_title, image_path) ON l.title = li.listing_title;

-- Add trust rating for Joe's Reseller Store
INSERT INTO business_trust_rating (
    business_id,
    overall_score,
    verification_score,
    profile_score,
    review_score,
    transaction_score,
    total_reviews,
    positive_reviews,
    total_transactions,
    successful_transactions,
    last_calculated,
    created_at,
    updated_at
)
SELECT
    b.id,
    4.90,
    4.70,
    4.80,
    4.80,
    4.50,
    20,
    18,
    20,
    18,
    NOW(),
    NOW(),
    NOW()
FROM business b
         JOIN users u ON b.owner_id = u.id
WHERE u.email = 'reseller@marketplace.com'
ON CONFLICT DO NOTHING;

-- Add trust rating for Admin Store
INSERT INTO business_trust_rating (
    business_id,
    overall_score,
    verification_score,
    profile_score,
    review_score,
    transaction_score,
    total_reviews,
    positive_reviews,
    total_transactions,
    successful_transactions,
    last_calculated,
    created_at,
    updated_at
)
SELECT
    b.id,
    4.90,
    4.70,
    4.80,
    4.80,
    4.50,
    20,
    18,
    20,
    18,
    NOW(),
    NOW(),
    NOW()
FROM business b
         JOIN users u ON b.owner_id = u.id
WHERE u.email = 'admin@admin.com'
ON CONFLICT DO NOTHING;


-- Add subscriptions for resellerjoe and admin
INSERT INTO subscription (
    user_id, business_id, plan_type, status, amount, currency, billing_cycle, current_period_start, current_period_end, created_at, updated_at
) VALUES
    (
        (SELECT id FROM users WHERE email = 'reseller@marketplace.com'),
        (SELECT id FROM business WHERE owner_id = (SELECT id FROM users WHERE email = 'reseller@marketplace.com')),
        'RESELLER',
        'ACTIVE',
        99.00,
        'USD',
        'MONTHLY',
        NOW() - INTERVAL '1 day',
        NOW() + INTERVAL '29 days',
        NOW(),
        NOW()
    ),
    (
        (SELECT id FROM users WHERE email = 'admin@admin.com'),
        (SELECT id FROM business WHERE owner_id = (SELECT id FROM users WHERE email = 'admin@admin.com')),
        'PRO_STORE',
        'ACTIVE',
        199.00,
        'USD',
        'MONTHLY',
        NOW() - INTERVAL '1 day',
        NOW() + INTERVAL '29 days',
        NOW(),
        NOW()
    )
ON CONFLICT DO NOTHING;
