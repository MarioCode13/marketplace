CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Country Table
CREATE TABLE IF NOT EXISTS country (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(10) NOT NULL UNIQUE
);

-- Region Table
CREATE TABLE IF NOT EXISTS region (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    country_id UUID REFERENCES country(id),
    CONSTRAINT unique_region_per_country UNIQUE (name, country_id)
);

-- City Table
CREATE TABLE IF NOT EXISTS city (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    region_id UUID REFERENCES region(id),
    CONSTRAINT unique_city_per_region UNIQUE (name, region_id)
);

-- User Table
CREATE TABLE IF NOT EXISTS "users" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
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
    city_id UUID REFERENCES city(id),
    custom_city VARCHAR(100),
    contact_number VARCHAR(255),
    id_number VARCHAR(255),
    plan_type VARCHAR(32),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Business Table
CREATE TABLE IF NOT EXISTS business (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
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
    is_email_verified BOOLEAN DEFAULT FALSE
);

-- BusinessUser Table
CREATE TABLE IF NOT EXISTS business_user (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'CONTRIBUTOR',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(business_id, user_id)
);

-- Category Table
CREATE TABLE IF NOT EXISTS category (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
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
INSERT INTO city (name, region_id) VALUES
    ('Johannesburg', (SELECT id FROM region WHERE name = 'Gauteng')),
    ('Pretoria', (SELECT id FROM region WHERE name = 'Gauteng')),
    ('Sandton', (SELECT id FROM region WHERE name = 'Gauteng')),
    ('Midrand', (SELECT id FROM region WHERE name = 'Gauteng')),
    ('Centurion', (SELECT id FROM region WHERE name = 'Gauteng')),
    ('Cape Town', (SELECT id FROM region WHERE name = 'Western Cape')),
    ('Stellenbosch', (SELECT id FROM region WHERE name = 'Western Cape')),
    ('Paarl', (SELECT id FROM region WHERE name = 'Western Cape')),
    ('George', (SELECT id FROM region WHERE name = 'Western Cape')),
    ('Knysna', (SELECT id FROM region WHERE name = 'Western Cape')),
    ('Durban', (SELECT id FROM region WHERE name = 'KwaZulu-Natal')),
    ('Pietermaritzburg', (SELECT id FROM region WHERE name = 'KwaZulu-Natal')),
    ('Richards Bay', (SELECT id FROM region WHERE name = 'KwaZulu-Natal')),
    ('Ballito', (SELECT id FROM region WHERE name = 'KwaZulu-Natal')),
    ('Umhlanga', (SELECT id FROM region WHERE name = 'KwaZulu-Natal')),
    ('Port Elizabeth', (SELECT id FROM region WHERE name = 'Eastern Cape')),
    ('East London', (SELECT id FROM region WHERE name = 'Eastern Cape')),
    ('Grahamstown', (SELECT id FROM region WHERE name = 'Eastern Cape')),
    ('Jeffreys Bay', (SELECT id FROM region WHERE name = 'Eastern Cape')),
    ('Bloemfontein', (SELECT id FROM region WHERE name = 'Free State')),
    ('Welkom', (SELECT id FROM region WHERE name = 'Free State')),
    ('Polokwane', (SELECT id FROM region WHERE name = 'Limpopo')),
    ('Thohoyandou', (SELECT id FROM region WHERE name = 'Limpopo')),
    ('Tzaneen', (SELECT id FROM region WHERE name = 'Limpopo')),
    ('Nelspruit', (SELECT id FROM region WHERE name = 'Mpumalanga')),
    ('White River', (SELECT id FROM region WHERE name = 'Mpumalanga')),
    ('Sabie', (SELECT id FROM region WHERE name = 'Mpumalanga')),
    ('Mahikeng', (SELECT id FROM region WHERE name = 'North West')),
    ('Rustenburg', (SELECT id FROM region WHERE name = 'North West')),
    ('Kimberley', (SELECT id FROM region WHERE name = 'Northern Cape')),
    ('Upington', (SELECT id FROM region WHERE name = 'Northern Cape'))
ON CONFLICT (name, region_id) DO NOTHING;


-- =========================
-- Core Categories (now using UUIDs)
INSERT INTO category (id, name, parent_id) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Electronics', NULL),
    ('22222222-2222-2222-2222-222222222222', 'Fashion & Accessories', NULL),
    ('33333333-3333-3333-3333-333333333333', 'Home & Garden', NULL),
    ('44444444-4444-4444-4444-444444444444', 'Sports & Outdoors', NULL),
    ('55555555-5555-5555-5555-555555555555', 'Books & Media', NULL),
    ('66666666-6666-6666-6666-666666666666', 'Automotive', NULL),
    ('77777777-7777-7777-7777-777777777777', 'Toys & Games', NULL),
    ('88888888-8888-8888-8888-888888888888', 'Health & Beauty', NULL),
    ('99999999-9999-9999-9999-999999999999', 'Art & Collectibles', NULL),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Musical Instruments', NULL),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Tools & Hardware', NULL),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'Pet Supplies', NULL),
    ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'Baby & Kids', NULL),
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'Office & Business', NULL),
    ('ffffffff-ffff-ffff-ffff-ffffffffffff', 'Food & Beverages', NULL)
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Electronics subcategories
INSERT INTO category (id, name, parent_id) VALUES
    ('11111111-1111-1111-1111-111111111112', 'Computers & Tablets', '11111111-1111-1111-1111-111111111111'),
    ('11111111-1111-1111-1111-111111111113', 'Phones & Accessories', '11111111-1111-1111-1111-111111111111'),
    ('11111111-1111-1111-1111-111111111114', 'Cameras & Photography', '11111111-1111-1111-1111-111111111111'),
    ('11111111-1111-1111-1111-111111111115', 'Audio & Video', '11111111-1111-1111-1111-111111111111'),
    ('11111111-1111-1111-1111-111111111116', 'Gaming', '11111111-1111-1111-1111-111111111111')
ON CONFLICT (id) DO NOTHING;

-- Computers sub-subcategories
INSERT INTO category (id, name, parent_id) VALUES
    ('11111111-1111-1111-1111-111111111201', 'Laptops', '11111111-1111-1111-1111-111111111112'),
    ('11111111-1111-1111-1111-111111111202', 'Desktops', '11111111-1111-1111-1111-111111111112'),
    ('11111111-1111-1111-1111-111111111203', 'Tablets', '11111111-1111-1111-1111-111111111112'),
    ('11111111-1111-1111-1111-111111111204', 'Components', '11111111-1111-1111-1111-111111111112'),
    ('11111111-1111-1111-1111-111111111205', 'Accessories & Peripherals', '11111111-1111-1111-1111-111111111112')
ON CONFLICT (id) DO NOTHING;

-- Components
INSERT INTO category (id, name, parent_id) VALUES
    ('11111111-1111-1111-1111-111111111301', 'CPU', '11111111-1111-1111-1111-111111111204'),
    ('11111111-1111-1111-1111-111111111302', 'GPU', '11111111-1111-1111-1111-111111111204'),
    ('11111111-1111-1111-1111-111111111303', 'RAM & Storage', '11111111-1111-1111-1111-111111111204'),
    ('11111111-1111-1111-1111-111111111304', 'Motherboards', '11111111-1111-1111-1111-111111111204'),
    ('11111111-1111-1111-1111-111111111305', 'Power Supplies', '11111111-1111-1111-1111-111111111204'),
    ('11111111-1111-1111-1111-111111111306', 'Cooling', '11111111-1111-1111-1111-111111111204')
ON CONFLICT (id) DO NOTHING;

-- Accessories & Peripherals
INSERT INTO category (id, name, parent_id) VALUES
    ('11111111-1111-1111-1111-111111111307', 'Keyboards', '11111111-1111-1111-1111-111111111205'),
    ('11111111-1111-1111-1111-111111111308', 'Mice', '11111111-1111-1111-1111-111111111205'),
    ('11111111-1111-1111-1111-111111111309', 'Monitors', '11111111-1111-1111-1111-111111111205'),
    ('11111111-1111-1111-1111-111111111310', 'Cables & Hubs', '11111111-1111-1111-1111-111111111205'),
    ('11111111-1111-1111-1111-111111111311', 'Docking Stations', '11111111-1111-1111-1111-111111111205')
ON CONFLICT (id) DO NOTHING;

-- Phones & Accessories
INSERT INTO category (id, name, parent_id) VALUES
    ('11111111-1111-1111-1111-111111111312', 'Smartphones', '11111111-1111-1111-1111-111111111113'),
    ('11111111-1111-1111-1111-111111111313', 'Cases & Covers', '11111111-1111-1111-1111-111111111113'),
    ('11111111-1111-1111-1111-111111111314', 'Chargers & Cables', '11111111-1111-1111-1111-111111111113'),
    ('11111111-1111-1111-1111-111111111315', 'Headphones & Earphones', '11111111-1111-1111-1111-111111111113')
ON CONFLICT (id) DO NOTHING;

-- Cameras
INSERT INTO category (id, name, parent_id) VALUES
    ('11111111-1111-1111-1111-111111111316', 'DSLR & Mirrorless', '11111111-1111-1111-1111-111111111114'),
    ('11111111-1111-1111-1111-111111111317', 'Lenses & Filters', '11111111-1111-1111-1111-111111111114'),
    ('11111111-1111-1111-1111-111111111318', 'Tripods & Mounts', '11111111-1111-1111-1111-111111111114'),
    ('11111111-1111-1111-1111-111111111319', 'Camera Bags & Storage', '11111111-1111-1111-1111-111111111114')
ON CONFLICT (id) DO NOTHING;

-- Audio & Video
INSERT INTO category (id, name, parent_id) VALUES
    ('11111111-1111-1111-1111-111111111320', 'Speakers', '11111111-1111-1111-1111-111111111115'),
    ('11111111-1111-1111-1111-111111111321', 'Headphones', '11111111-1111-1111-1111-111111111115'),
    ('11111111-1111-1111-1111-111111111322', 'TVs & Projectors', '11111111-1111-1111-1111-111111111115'),
    ('11111111-1111-1111-1111-111111111323', 'Streaming Devices', '11111111-1111-1111-1111-111111111115')
ON CONFLICT (id) DO NOTHING;

-- Gaming
INSERT INTO category (id, name, parent_id) VALUES
    ('11111111-1111-1111-1111-111111111324', 'Consoles', '11111111-1111-1111-1111-111111111116'),
    ('11111111-1111-1111-1111-111111111325', 'Games', '11111111-1111-1111-1111-111111111116'),
    ('11111111-1111-1111-1111-111111111326', 'Controllers & Accessories', '11111111-1111-1111-1111-111111111116'),
    ('11111111-1111-1111-1111-111111111327', 'VR Headsets', '11111111-1111-1111-1111-111111111116')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Fashion & Accessories
INSERT INTO category (id, name, parent_id) VALUES
    ('22222222-2222-2222-2222-222222222401', 'Men''s Fashion', '22222222-2222-2222-2222-222222222222'),
    ('22222222-2222-2222-2222-222222222402', 'Women''s Fashion', '22222222-2222-2222-2222-222222222222'),
    ('22222222-2222-2222-2222-222222222403', 'Jewelry & Watches', '22222222-2222-2222-2222-222222222222'),
    ('22222222-2222-2222-2222-222222222404', 'Bags & Luggage', '22222222-2222-2222-2222-222222222222'),
    ('22222222-2222-2222-2222-222222222405', 'Accessories', '22222222-2222-2222-2222-222222222222')
ON CONFLICT (id) DO NOTHING;

-- Men
INSERT INTO category (id, name, parent_id) VALUES
    ('22222222-2222-2222-2222-222222225501', 'Tops', '22222222-2222-2222-2222-222222222401'),
    ('22222222-2222-2222-2222-222222225502', 'Bottoms', '22222222-2222-2222-2222-222222222401'),
    ('22222222-2222-2222-2222-222222225503', 'Shoes', '22222222-2222-2222-2222-222222222401'),
    ('22222222-2222-2222-2222-222222225504', 'Underwear', '22222222-2222-2222-2222-222222222401'),
    ('22222222-2222-2222-2222-222222225505', 'Outerwear', '22222222-2222-2222-2222-222222222401')
ON CONFLICT (id) DO NOTHING;

-- Women
INSERT INTO category (id, name, parent_id) VALUES
    ('22222222-2222-2222-2222-222222225506', 'Tops', '22222222-2222-2222-2222-222222222402'),
    ('22222222-2222-2222-2222-222222225507', 'Bottoms', '22222222-2222-2222-2222-222222222402'),
    ('22222222-2222-2222-2222-222222225508', 'Dresses', '22222222-2222-2222-2222-222222222402'),
    ('22222222-2222-2222-2222-222222225509', 'Shoes', '22222222-2222-2222-2222-222222222402'),
    ('22222222-2222-2222-2222-222222225510', 'Underwear', '22222222-2222-2222-2222-222222222402'),
    ('22222222-2222-2222-2222-222222225511', 'Outerwear', '22222222-2222-2222-2222-222222222402')
ON CONFLICT (id) DO NOTHING;

-- Jewelry & Watches
INSERT INTO category (id, name, parent_id) VALUES
    ('22222222-2222-2222-2222-222222225512', 'Rings', '22222222-2222-2222-2222-222222222403'),
    ('22222222-2222-2222-2222-222222225513', 'Necklaces', '22222222-2222-2222-2222-222222222403'),
    ('22222222-2222-2222-2222-222222225514', 'Bracelets', '22222222-2222-2222-2222-222222222403'),
    ('22222222-2222-2222-2222-222222225515', 'Watches', '22222222-2222-2222-2222-222222222403')
ON CONFLICT (id) DO NOTHING;

-- Bags & Luggage
INSERT INTO category (id, name, parent_id) VALUES
    ('22222222-2222-2222-2222-222222225516', 'Handbags', '22222222-2222-2222-2222-222222222404'),
    ('22222222-2222-2222-2222-222222225517', 'Backpacks', '22222222-2222-2222-2222-222222222404'),
    ('22222222-2222-2222-2222-222222225518', 'Suitcases', '22222222-2222-2222-2222-222222222404')
ON CONFLICT (id) DO NOTHING;

-- Accessories
INSERT INTO category (id, name, parent_id) VALUES
    ('22222222-2222-2222-2222-222222225519', 'Belts', '22222222-2222-2222-2222-222222222405'),
    ('22222222-2222-2222-2222-222222225520', 'Hats & Caps', '22222222-2222-2222-2222-222222222405'),
    ('22222222-2222-2222-2222-222222225521', 'Sunglasses', '22222222-2222-2222-2222-222222222405'),
    ('22222222-2222-2222-2222-222222225522', 'Scarves', '22222222-2222-2222-2222-222222222405')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Home & Garden
INSERT INTO category (id, name, parent_id) VALUES
    ('33333333-3333-3333-3333-333333336001', 'Furniture', '33333333-3333-3333-3333-333333333333'),
    ('33333333-3333-3333-3333-333333336002', 'Kitchen & Dining', '33333333-3333-3333-3333-333333333333'),
    ('33333333-3333-3333-3333-333333336003', 'Garden & Outdoor', '33333333-3333-3333-3333-333333333333'),
    ('33333333-3333-3333-3333-333333336004', 'Home Decor', '33333333-3333-3333-3333-333333333333'),
    ('33333333-3333-3333-3333-333333336005', 'Bedding & Bath', '33333333-3333-3333-3333-333333333333')
ON CONFLICT (id) DO NOTHING;

-- Furniture
INSERT INTO category (id, name, parent_id) VALUES
    ('33333333-3333-3333-3333-333333337001', 'Living Room', '33333333-3333-3333-3333-333333336001'),
    ('33333333-3333-3333-3333-333333337002', 'Bedroom', '33333333-3333-3333-3333-333333336001'),
    ('33333333-3333-3333-3333-333333337003', 'Office Furniture', '33333333-3333-3333-3333-333333336001')
ON CONFLICT (id) DO NOTHING;

-- Kitchen & Dining
INSERT INTO category (id, name, parent_id) VALUES
    ('33333333-3333-3333-3333-333333337041', 'Cookware', '33333333-3333-3333-3333-333333336002'),
    ('33333333-3333-3333-3333-333333337042', 'Tableware', '33333333-3333-3333-3333-333333336002'),
    ('33333333-3333-3333-3333-333333337043', 'Small Appliances', '33333333-3333-3333-3333-333333336002')
ON CONFLICT (id) DO NOTHING;

-- Garden & Outdoor
INSERT INTO category (id, name, parent_id) VALUES
    ('33333333-3333-3333-3333-333333337071', 'Garden Tools', '33333333-3333-3333-3333-333333336003'),
    ('33333333-3333-3333-3333-333333337072', 'Outdoor Furniture', '33333333-3333-3333-3333-333333336003'),
    ('33333333-3333-3333-3333-333333337073', 'Grills & BBQ', '33333333-3333-3333-3333-333333336003')
ON CONFLICT (id) DO NOTHING;

-- Home Decor
INSERT INTO category (id, name, parent_id) VALUES
    ('33333333-3333-3333-3333-333333337101', 'Lighting', '33333333-3333-3333-3333-333333336004'),
    ('33333333-3333-3333-3333-333333337102', 'Rugs & Carpets', '33333333-3333-3333-3333-333333336004'),
    ('33333333-3333-3333-3333-333333337103', 'Wall Art & Decor', '33333333-3333-3333-3333-333333336004')
ON CONFLICT (id) DO NOTHING;

-- Bedding & Bath
INSERT INTO category (id, name, parent_id) VALUES
    ('33333333-3333-3333-3333-333333337131', 'Bedding', '33333333-3333-3333-3333-333333336005'),
    ('33333333-3333-3333-3333-333333337132', 'Bath', '33333333-3333-3333-3333-333333336005')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Sports & Outdoors
INSERT INTO category (id, name, parent_id) VALUES
    ('44444444-4444-4444-4444-444444448001', 'Fitness', '44444444-4444-4444-4444-444444444444'),
    ('44444444-4444-4444-4444-444444448002', 'Outdoor Recreation', '44444444-4444-4444-4444-444444444444'),
    ('44444444-4444-4444-4444-444444448003', 'Team Sports', '44444444-4444-4444-4444-444444444444'),
    ('44444444-4444-4444-4444-444444448004', 'Cycling', '44444444-4444-4444-4444-444444444444')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Automotive
INSERT INTO category (id, name, parent_id) VALUES
    ('66666666-6666-6666-6666-666666669001', 'Car Parts & Accessories', '66666666-6666-6666-6666-666666666666'),
    ('66666666-6666-6666-6666-666666669002', 'Motorcycles & Scooters', '66666666-6666-6666-6666-666666666666'),
    ('66666666-6666-6666-6666-666666669003', 'Tires & Wheels', '66666666-6666-6666-6666-666666666666'),
    ('66666666-6666-6666-6666-666666669004', 'Car Electronics', '66666666-6666-6666-6666-666666666666'),
    ('66666666-6666-6666-6666-666666669005', 'Tools & Garage', '66666666-6666-6666-6666-666666666666')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Toys & Games
INSERT INTO category (id, name, parent_id) VALUES
    ('77777777-7777-7777-7777-777777779001', 'Action Figures & Collectibles', '77777777-7777-7777-7777-777777777777'),
    ('77777777-7777-7777-7777-777777779002', 'Board Games & Puzzles', '77777777-7777-7777-7777-777777777777'),
    ('77777777-7777-7777-7777-777777779003', 'Dolls & Accessories', '77777777-7777-7777-7777-777777777777'),
    ('77777777-7777-7777-7777-777777779004', 'Outdoor Play', '77777777-7777-7777-7777-777777777777'),
    ('77777777-7777-7777-7777-777777779005', 'Educational Toys', '77777777-7777-7777-7777-777777777777')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Health & Beauty
INSERT INTO category (id, name, parent_id) VALUES
    ('88888888-8888-8888-8888-888888889001', 'Makeup', '88888888-8888-8888-8888-888888888888'),
    ('88888888-8888-8888-8888-888888889002', 'Skincare', '88888888-8888-8888-8888-888888888888'),
    ('88888888-8888-8888-8888-888888889003', 'Hair Care', '88888888-8888-8888-8888-888888888888'),
    ('88888888-8888-8888-8888-888888889004', 'Personal Care', '88888888-8888-8888-8888-888888888888'),
    ('88888888-8888-8888-8888-888888889005', 'Supplements', '88888888-8888-8888-8888-888888888888')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Art & Collectibles
INSERT INTO category (id, name, parent_id) VALUES
    ('99999999-9999-9999-9999-999999999201', 'Paintings', '99999999-9999-9999-9999-999999999999'),
    ('99999999-9999-9999-9999-999999999202', 'Sculptures', '99999999-9999-9999-9999-999999999999'),
    ('99999999-9999-9999-9999-999999999203', 'Collectibles', '99999999-9999-9999-9999-999999999999')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Musical Instruments
INSERT INTO category (id, name, parent_id) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', 'Guitars', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', 'Keyboards & Pianos', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3', 'Drums & Percussion', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4', 'Wind Instruments', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Tools & Hardware
INSERT INTO category (id, name, parent_id) VALUES
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'Hand Tools', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', 'Power Tools', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3', 'Tool Storage', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb4', 'Safety Equipment', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Pet Supplies
INSERT INTO category (id, name, parent_id) VALUES
    ('cccccccc-cccc-cccc-cccc-ccccccccccc1', 'Dog Supplies', 'cccccccc-cccc-cccc-cccc-cccccccccccc'),
    ('cccccccc-cccc-cccc-cccc-ccccccccccc2', 'Cat Supplies', 'cccccccc-cccc-cccc-cccc-cccccccccccc'),
    ('cccccccc-cccc-cccc-cccc-ccccccccccc3', 'Fish & Aquatic', 'cccccccc-cccc-cccc-cccc-cccccccccccc'),
    ('cccccccc-cccc-cccc-cccc-ccccccccccc4', 'Small Pets', 'cccccccc-cccc-cccc-cccc-cccccccccccc'),
    ('cccccccc-cccc-cccc-cccc-ccccccccccc5', 'Birds', 'cccccccc-cccc-cccc-cccc-cccccccccccc')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Baby & Kids
INSERT INTO category (id, name, parent_id) VALUES
    ('dddddddd-dddd-dddd-dddd-ddddddddddd1', 'Clothing', 'dddddddd-dddd-dddd-dddd-dddddddddddd'),
    ('dddddddd-dddd-dddd-dddd-ddddddddddd2', 'Toys', 'dddddddd-dddd-dddd-dddd-dddddddddddd'),
    ('dddddddd-dddd-dddd-dddd-ddddddddddd3', 'Gear & Accessories', 'dddddddd-dddd-dddd-dddd-dddddddddddd'),
    ('dddddddd-dddd-dddd-dddd-ddddddddddd4', 'Feeding & Nursing', 'dddddddd-dddd-dddd-dddd-dddddddddddd')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Office & Business
INSERT INTO category (id, name, parent_id) VALUES
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee1', 'Office Supplies', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee'),
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee2', 'Stationery', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee'),
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee3', 'Furniture', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee'),
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee4', 'Technology', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Food & Beverages
INSERT INTO category (id, name, parent_id) VALUES
    ('ffffffff-ffff-ffff-ffff-fffffffffff1', 'Beverages', 'ffffffff-ffff-ffff-ffff-ffffffffffff'),
    ('ffffffff-ffff-ffff-ffff-fffffffffff2', 'Snacks', 'ffffffff-ffff-ffff-ffff-ffffffffffff'),
    ('ffffffff-ffff-ffff-ffff-fffffffffff3', 'Groceries', 'ffffffff-ffff-ffff-ffff-ffffffffffff'),
    ('ffffffff-ffff-ffff-ffff-fffffffffff4', 'Specialty Foods', 'ffffffff-ffff-ffff-ffff-ffffffffffff')
ON CONFLICT (id) DO NOTHING;

-- Listing Table
CREATE TABLE IF NOT EXISTS listing (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    business_id UUID REFERENCES business(id),
    created_by UUID REFERENCES users(id),
    category_id UUID NOT NULL REFERENCES category(id),
    price DOUBLE PRECISION NOT NULL,
    sold BOOLEAN DEFAULT FALSE,
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
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    listing_id UUID NOT NULL REFERENCES listing(id) ON DELETE CASCADE,
    image TEXT NOT NULL
);

-- Trust Rating System
CREATE TABLE trust_rating (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    overall_score DECIMAL(5,2) NOT NULL DEFAULT 0.00,
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

-- Business Trust Rating System
CREATE TABLE IF NOT EXISTS business_trust_rating (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID NOT NULL UNIQUE REFERENCES business(id) ON DELETE CASCADE,
    overall_score DECIMAL(5,2) NOT NULL DEFAULT 0.00,
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
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
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
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    listing_id UUID NOT NULL REFERENCES listing(id) ON DELETE CASCADE,
    seller_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    buyer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sale_price DECIMAL(10,2) NOT NULL,
    sale_date TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX unique_completed_transaction_per_listing
ON "transaction" (listing_id)
WHERE status = 'COMPLETED';

-- Review System table
CREATE TABLE review (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reviewer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reviewed_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    transaction_id UUID NOT NULL REFERENCES "transaction"(id) ON DELETE CASCADE,
    rating DECIMAL(2,1) NOT NULL CHECK (rating >= 0.5 AND rating <= 5.0),
    comment TEXT,
    is_positive BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (reviewer_id, transaction_id)
);

-- Profile Completion tracking
CREATE TABLE profile_completion (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
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
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- StoreBranding Table
CREATE TABLE IF NOT EXISTS store_branding (
    business_id UUID PRIMARY KEY REFERENCES business(id) ON DELETE CASCADE,
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
    card_text_color VARCHAR(32),
    background_color VARCHAR(32)
);

-- Notification Table
CREATE TABLE IF NOT EXISTS notification (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
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

-- Add reseller and admin user
INSERT INTO "users" (username, email, password, role, plan_type, first_name, last_name, bio, city_id, contact_number, created_at, profile_image_url)
VALUES
    ('resellerjoe', 'reseller@marketplace.com', '$2a$10$7QJ8QwQwQwQwQwQwQwQwQeQwQwQwQwQwQwQwQwQwQwQwQwQwQwQw','HAS_ACCOUNT',
     'RESELLER','Joe','Reseller','We sell the best gadgets and accessories!', (SELECT id FROM city WHERE name = 'Cape Town'),'+27111222333',NOW(), 'https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?q=80&w=880&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
    ('admin', 'admin@admin.com', '$2a$10$r1d0EfJx3L7OSW9ofStBPueFKHXQtyrUVhwf09h4pLOEOSMKGJmPm', 'SUBSCRIBED', 'PRO_STORE', 'Admin', 'User', 'System administrator and marketplace enthusiast. I love testing new features and helping users.',  (SELECT id FROM city WHERE name = 'Cape Town'), '+27-10-555-0100', NOW(), 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?q=80&w=688&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D')
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
INSERT INTO store_branding (business_id, logo_url, banner_url, theme_color, primary_color, secondary_color, light_or_dark, about, store_name)
SELECT b.id,
       'https://images.unsplash.com/photo-1519125323398-675f0ddb6308?q=80&w=100',
       'https://images.unsplash.com/photo-1465101046530-73398c7f28ca?q=80&w=800',
       '#e53e3e',
       '#e53e3e',
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
INSERT INTO business (name, email, contact_number, address_line1, city_id, owner_id, business_type)
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
    'PRO_STORE'
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
INSERT INTO store_branding (business_id, slug, logo_url, banner_url, theme_color, primary_color, secondary_color, light_or_dark, about, store_name )
SELECT b.id,
       'admin-pro-store',
       'https://images.unsplash.com/photo-1614851099518-055a1000e6d5?q=80&w=1470&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D',
       'https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?q=80&w=1470',
       '#6470ff',
       '#ff131a',
       '#ffffff',
       'light',
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
